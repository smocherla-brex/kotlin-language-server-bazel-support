package org.javacs.kt.definition

import org.eclipse.lsp4j.Location
import java.nio.file.Path
import org.javacs.kt.CompiledFile
import org.javacs.kt.CompilerClassPath
import org.javacs.kt.LOG
import org.javacs.kt.ExternalSourcesConfiguration
import org.javacs.kt.classpath.PackageSourceMapping
import org.javacs.kt.externalsources.ClassContentProvider
import org.javacs.kt.externalsources.KlsURI
import org.javacs.kt.sourcejars.SourceJarParser
import org.jetbrains.kotlin.descriptors.*
import org.javacs.kt.compiler.Compiler
import org.javacs.kt.sourcejars.SourceFileInfo
import org.javacs.kt.util.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeText


fun goToDefinition(
    file: CompiledFile,
    cursor: Int,
    tempDir: TemporaryDirectory,
    cp: CompilerClassPath
): Location? {
    val (_, target) = file.referenceExpressionAtPoint(cursor) ?: return null


    LOG.info("Found declaration descriptor {}", target)

    // Try finding the location from source jars first,
    // if we don't find it, then maybe try the decompiling class file option
    val location = locationFromClassPath(cp.workspaceRoots.first(), target, cp.packageSourceMappings, cp.compiler, tempDir)
    if(location == null) {
        LOG.warn("Didn't find location for {}", target)
    }
    return location
}

private fun locationFromClassPath(workspaceRoot: Path, target: DeclarationDescriptor, packageSourceMappings: Set<PackageSourceMapping>, compiler: Compiler, tempDir: TemporaryDirectory): Location? {
    if (packageSourceMappings.isEmpty()) {
        LOG.info("Package source mappings is empty, go-to will not work")
        return null
    }

    return locationForDescriptor(workspaceRoot, packageSourceMappings, target, compiler, tempDir)
}

private fun locationForDescriptor(
    workspaceRoot: Path,
    packageSourceMappings: Set<PackageSourceMapping>,
    descriptor: DeclarationDescriptor,
    compiler: Compiler,
    tempDir: TemporaryDirectory
): Location? {

    // Helper function to process a single jar entry
    fun processJarEntry(packageSourceMapping: PackageSourceMapping): Location? {
        val classDescriptor = descriptorOfContainingClass(descriptor)
        // this only works if the symbol is contained directly in a class/object
        if (classDescriptor != null) {
            val packageName = descriptor.containingPackage().toString()
            val className = classDescriptor.name.toString() ?: return null
            val symbolName = descriptor.name.asString()
            val sourceJar = packageSourceMapping.sourceJar.absolutePathString()
            return findLocation(workspaceRoot, sourceJar, packageName, className, symbolName, compiler, tempDir)
        }
        return null
    }

    // Try source jars that match the mappings we detrmined
    val possibleSourceMappings = packageSourceMappings.filter { it.sourcePackage == descriptor.containingPackage().toString() }
    return possibleSourceMappings.firstNotNullOfOrNull { processJarEntry(it) }
}

private fun findLocation(
    workspaceRoot: Path,
    sourceJar: String,
    packageName: String,
    className: String,
    symbolName: String,
    compiler: Compiler,
    tempDir: TemporaryDirectory
): Location? {
    val sourceFileInfo = SourceJarParser.findSourceFileInfo(
        sourcesJarPath = sourceJar,
        packageName = packageName,
        className = className
    ) ?: return null

    val range = compiler.findDeclarationRange(
        sourceFileInfo.contents,
        declarationName = symbolName,
    )

    return when {
        // Need a better way to do this - this is to ensure we try to
        // extract from JARs for external and proto jars
        sourceJar.contains("external/") || sourceJar.contains("-speed") -> {

            Location(
                getSourceFilePathInJar(sourceFileInfo, tempDir),
                range
            )
        }

        else -> getLocalSourcePath(workspaceRoot, sourceJar, className)?.let { sourcePath ->
            Location(sourcePath.toUri().toString(), range)
        }
    }
}

private fun getLocalSourcePath(workspaceRoot: Path, jarPath: String, className: String): Path? {
    val baseDir = jarPath.substringBeforeLast("/").replace(Regex("bazel-out/[^/]+/bin/"), "")
    val sourceFile = "${className.replace('.', '/')}.kt"
    return Files.walk(workspaceRoot.resolve(baseDir).toRealPath()).use { paths ->
        paths
            .filter { it.fileName.toString() == sourceFile }
            .findFirst()
            .orElse(null)
    }
}

fun getSourceFilePathInJar(sourceFileInfo: SourceFileInfo, tempDir: TemporaryDirectory): String {
    val tempSourceFile = tempDir.createTempFile(suffix = if(sourceFileInfo.isJava) ".java" else ".kt")
    tempSourceFile.writeText(sourceFileInfo.contents)
    return tempSourceFile.toUri().toString()
}
