package org.javacs.kt.util

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.isCompanionObject
import java.nio.file.Path

inline fun<reified Find> PsiElement.findParent() =
        this.parentsWithSelf.filterIsInstance<Find>().firstOrNull()

fun PsiElement.preOrderTraversal(shouldTraverse: (PsiElement) -> Boolean = { true }): Sequence<PsiElement> {
    val root = this

    return sequence {
        if (shouldTraverse(root)) {
            yield(root)

            for (child in root.children) {
                if (shouldTraverse(child)) {
                    yieldAll(child.preOrderTraversal(shouldTraverse))
                }
            }
        }
    }
}

fun PsiFile.toPath(): Path =
        winCompatiblePathOf(this.originalFile.viewProvider.virtualFile.path)

fun PsiElement.getRange(): Range {
    val document = containingFile.viewProvider.document
        ?: return Range(Position(0, 0), Position(0, 0))

    val startOffset = textRange.startOffset
    val endOffset = textRange.endOffset

    val startLine = document.getLineNumber(startOffset)
    val startLineStart = document.getLineStartOffset(startLine)
    val startColumn = startOffset - startLineStart

    val endLine = document.getLineNumber(endOffset)
    val endLineStart = document.getLineStartOffset(endLine)
    val endColumn = endOffset - endLineStart

    return Range(
        Position(startLine, startColumn),
        Position(endLine, endColumn)
    )
}

fun descriptorOfContainingClass(descriptor: DeclarationDescriptor): ClassDescriptor? {
    // if it's a companion object, then to get the class descriptor we need the parent class as the generated bytecode has a `.Companion` class
    if(descriptor is ClassDescriptor && descriptor.isCompanionObject()) return descriptor.containingDeclaration as ClassDescriptor
    if(descriptor is ClassDescriptor && descriptor !is FunctionDescriptor) return descriptor
    var current: DeclarationDescriptor = descriptor.containingDeclaration ?: return null

    while (current !is ClassDescriptor) {
        current = current.containingDeclaration ?: return null
    }

    if(current.isCompanionObject()) {
        current = current.containingDeclaration as ClassDescriptor
    }
    return current
}

fun TextRange.toRange(document: Document): Range {
    val startLine = document.getLineNumber(startOffset)
    val startChar = startOffset - document.getLineStartOffset(startLine)
    val endLine = document.getLineNumber(endOffset)
    val endChar = endOffset - document.getLineStartOffset(endLine)

    return Range(
        Position(startLine, startChar),
        Position(endLine, endChar)
    )
}
