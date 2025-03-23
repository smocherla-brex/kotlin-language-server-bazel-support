package org.javacs.kt.hover

import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.MarkupContent
import org.eclipse.lsp4j.Range
import com.intellij.psi.PsiDocCommentBase
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.RenderingFormat
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.javacs.kt.CompiledFile
import org.javacs.kt.CompilerClassPath
import org.javacs.kt.classpath.ClassPathEntry
import org.javacs.kt.classpath.PackageSourceMapping
import org.javacs.kt.compiler.Compiler
import org.javacs.kt.completion.DECL_RENDERER
import org.javacs.kt.position.position
import org.javacs.kt.util.findParent
import org.javacs.kt.signaturehelp.getDocString
import org.javacs.kt.sourcejars.SourceJarParser
import org.javacs.kt.util.descriptorOfContainingClass
import org.javacs.kt.util.getSourceJarPath
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isCompanionObject
import org.jetbrains.kotlin.utils.IDEAPluginsCompatibilityAPI
import java.nio.file.Path
import kotlin.io.path.absolutePathString

fun hoverAt(file: CompiledFile, compiler: Compiler, compilerClassPath: CompilerClassPath, cursor: Int): Hover? {
    val (ref, target) = file.referenceAtPoint(cursor) ?: return typeHoverAt(file, cursor)
    val sourceDoc = docFromSourceJars(compilerClassPath.workspaceRoots.first(), target, compiler, compilerClassPath.packageSourceMappings)
    val javaDoc = getDocString(file, cursor)
    val doc = if(sourceDoc != null && sourceDoc.isNotEmpty()) sourceDoc else javaDoc
    val location = ref.textRange
    val hoverText = DECL_RENDERER.render(target)
    val hover = MarkupContent("markdown", listOf("```kotlin\n$hoverText\n```", doc).filter { it.isNotEmpty() }.joinToString("\n---\n"))
    val range = Range(
            position(file.content, location.startOffset),
            position(file.content, location.endOffset))
    return Hover(hover, range)
}

private fun typeHoverAt(file: CompiledFile, cursor: Int): Hover? {
    val expression = file.parseAtPoint(cursor)?.findParent<KtExpression>() ?: return null
    val javaDoc: String = expression.children.mapNotNull { (it as? PsiDocCommentBase)?.text }.map(::renderJavaDoc).firstOrNull() ?: ""
    val scope = file.scopeAtPoint(cursor) ?: return null
    val context = file.bindingContextOf(expression, scope) ?: return null
    val hoverText = renderTypeOf(expression, context)
    val hover = MarkupContent("markdown", listOf("```kotlin\n$hoverText\n```", javaDoc).filter { it.isNotEmpty() }.joinToString("\n---\n"))
    return Hover(hover)
}

// Source: https://github.com/JetBrains/kotlin/blob/master/idea/src/org/jetbrains/kotlin/idea/codeInsight/KotlinExpressionTypeProvider.kt

private val TYPE_RENDERER: DescriptorRenderer by lazy { DescriptorRenderer.COMPACT.withOptions {
    textFormat = RenderingFormat.PLAIN
    classifierNamePolicy = object: ClassifierNamePolicy {
        override fun renderClassifier(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String {
            if (DescriptorUtils.isAnonymousObject(classifier)) {
                return "<anonymous object>"
            }
            return ClassifierNamePolicy.SHORT.renderClassifier(classifier, renderer)
        }
    }
} }

private fun renderJavaDoc(text: String): String {
    val split = text.split('\n')
    return split.mapIndexed { i, it ->
        val ret: String
        if (i == 0) ret = it.substring(it.indexOf("/**") + 3) // get rid of the start comment characters
        else if (i == split.size - 1) ret = it.substring(it.indexOf("*/") + 2) // get rid of the end comment characters
        else ret = it.substring(it.indexOf('*') + 1) // get rid of any leading *
        ret
    }.joinToString("\n")
}

@OptIn(IDEAPluginsCompatibilityAPI::class)
private fun renderTypeOf(element: KtExpression, bindingContext: BindingContext): String? {
    if (element is KtCallableDeclaration) {
        val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, element]
        if (descriptor != null) {
            when (descriptor) {
                is CallableDescriptor -> return descriptor.returnType?.let(TYPE_RENDERER::renderType)
            }
        }
    }

    val expressionType = bindingContext[BindingContext.EXPRESSION_TYPE_INFO, element]?.type ?: element.getType(bindingContext)
    val result = expressionType?.let { TYPE_RENDERER.renderType(it) } ?: return null

    val smartCast = bindingContext[BindingContext.SMARTCAST, element]
    if (smartCast != null && element is KtReferenceExpression) {
        val declaredType = (bindingContext[BindingContext.REFERENCE_TARGET, element] as? CallableDescriptor)?.returnType
        if (declaredType != null) {
            return result + " (smart cast from " + TYPE_RENDERER.renderType(declaredType) + ")"
        }
    }
    return result
}

private fun docFromSourceJars(workspaceRoot: Path, target: DeclarationDescriptor, compiler: Compiler, packageSourceMappings: Set<PackageSourceMapping>): String? {
    if (packageSourceMappings.isEmpty()) return null

    return kDocForDescriptor(workspaceRoot, packageSourceMappings, target, compiler)
}

private fun descriptorFqNameForClass(descriptor: ClassDescriptor?): String? {
    if(descriptor != null && descriptor.isCompanionObject) {
        return descriptor.fqNameSafe.asString().replace(".Companion", "")
    }
    return descriptor?.fqNameSafe?.asString()
}

private fun kDocForDescriptor(
    workspaceRoot: Path,
    packageSourceMappings: Set<PackageSourceMapping>,
    descriptor: DeclarationDescriptor,
    compiler: Compiler
): String? {

    fun processSourceJar(mapping: PackageSourceMapping): String? {
        val classDescriptor = descriptorOfContainingClass(descriptor)
        val descriptorFqName = descriptorFqNameForClass(classDescriptor)
        val packageName = descriptor.containingPackage()?.asString() ?: return null
        val className = classDescriptor?.name?.toString() ?: return null
        val symbolName = if(descriptor.isCompanionObject()) classDescriptor.name.asString().replace(".Companion", "") else descriptor.name.asString()
        val sourceJar = mapping.sourceJar

        return findKdoc(workspaceRoot, sourceJar.absolutePathString(), packageName, className, symbolName, compiler)
    }

    val possibleMappings = packageSourceMappings.filter { it.sourcePackage == descriptor.containingPackage()?.asString() }
    return possibleMappings.firstNotNullOfOrNull { processSourceJar(it) }
}

private fun findKdoc(workspaceRoot: Path, sourceJar: String, packageName: String, className: String, symbolName: String, compiler: Compiler): String? {
    val actualSourceJar = getSourceJarPath(workspaceRoot, sourceJar).toAbsolutePath().toString()
    val sourceFileInfo = SourceJarParser.findSourceFileInfo(
        sourcesJarPath = actualSourceJar,
        packageName = packageName,
        className = className
    ) ?: return null

    return compiler.findDeclarationComment(
        sourceFileInfo.contents,
        declarationName = symbolName,
    )
}
