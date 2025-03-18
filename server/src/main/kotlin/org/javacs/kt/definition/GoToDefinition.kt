package org.javacs.kt.definition

import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Range
import java.nio.file.Path
import org.javacs.kt.CompiledFile
import org.javacs.kt.CompilerClassPath
import org.javacs.kt.LOG
import org.javacs.kt.ExternalSourcesConfiguration
import org.javacs.kt.classpath.ClassPathEntry
import org.javacs.kt.classpath.JarMetadata
import org.javacs.kt.externalsources.ClassContentProvider
import org.javacs.kt.externalsources.KlsURI
import org.javacs.kt.sourcejars.SourceJarParser
import org.jetbrains.kotlin.descriptors.*
import org.javacs.kt.compiler.Compiler
import org.javacs.kt.sourcejars.SourceFileInfo
import org.javacs.kt.util.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.writeText

private val cachedTempFiles = mutableMapOf<KlsURI, Path>()
private val definitionPattern = Regex("(?:class|interface|object|fun)\\s+(\\w+)")

fun goToDefinition(
    file: CompiledFile,
    cursor: Int,
    classContentProvider: ClassContentProvider,
    tempDir: TemporaryDirectory,
    config: ExternalSourcesConfiguration,
    cp: CompilerClassPath
): Location? {
    val (_, target) = file.referenceExpressionAtPoint(cursor) ?: return null


    LOG.info("Found declaration descriptor {}", target)

    // Try finding the location from source jars first,
    // if we don't find it, then maybe try the decompiling class file option
    return locationFromClassPath(cp.workspaceRoots.first(), target, cp.classPath, cp.compiler, tempDir)
}

private fun isInsideArchive(uri: String, cp: CompilerClassPath) =
    uri.contains(".jar!") || uri.contains(".zip!") || cp.javaHome?.let {
        Paths.get(parseURI(uri)).toString().startsWith(File(it).path)
    } ?: false


private fun locationFromClassPath(workspaceRoot: Path, target: DeclarationDescriptor, classPathEntries: Set<ClassPathEntry>, compiler: Compiler, tempDir: TemporaryDirectory): Location? {
    val jarMetadata = classPathEntries.mapNotNull { it.jarMetadataJsons }.flatten()
    if (jarMetadata.isEmpty()) return null

    return locationForDescriptor(workspaceRoot, jarMetadata, target, compiler, tempDir)
}

private fun locationForDescriptor(
    workspaceRoot: Path,
    jarMetadata: List<Path?>,
    descriptor: DeclarationDescriptor,
    compiler: Compiler,
    tempDir: TemporaryDirectory
): Location? {

    // Helper function to process a single jar entry
    fun processJarEntry(jarEntry: Path?): Location? {
        val analysis = JarMetadata.fromMetadataJsonFile(jarEntry?.toFile()) ?: return null
        val classDescriptor = descriptorOfContainingClass(descriptor)
        // this only works if the symbol is contained directly in a class/object
        if (classDescriptor != null) {
            val sourceJar = analysis.classes[classDescriptor.fqNameSafe.asString()]?.sourceJars?.firstOrNull() ?: return null
            val packageName = descriptor.containingPackage()?.asString() ?: return null
            val className = classDescriptor.name.toString() ?: return null
            val symbolName = descriptor.name.asString()

            return findLocation(workspaceRoot, sourceJar, packageName, className, symbolName, compiler, tempDir)
        } else {
            // other possibilities - extension functions
            if(descriptor is DeserializedSimpleFunctionDescriptor && descriptor.extensionReceiverParameter != null) {
                LOG.debug { "Symbol is an extension function" }
                val packageName = descriptor.containingDeclaration.fqNameSafe.asString()

                val possibleMatches = analysis.classes.keys.filter { it.startsWith(packageName) }
                possibleMatches.forEach {
                    val analysisInfo = analysis.classes[it]
                    analysisInfo?.sourceJars?.firstOrNull()?.let { sourceJar ->
                        return findLocation(workspaceRoot, sourceJar, packageName, analysisInfo.name.removeSuffix("Kt"), descriptor.name.asString(), compiler, tempDir)
                    }
                }
            } else if(descriptor is DeserializedSimpleFunctionDescriptor && descriptor.isInline) {
                LOG.info { "Symbol is an inline function" }
                val packageName = descriptor.containingDeclaration.fqNameSafe.asString()

                val possibleMatches = analysis.classes.keys.filter { it.startsWith(packageName) }
                possibleMatches.forEach {
                    val analysisInfo = analysis.classes[it]
                    analysisInfo?.sourceJars?.firstOrNull()?.let { sourceJar ->
                        return findLocation(workspaceRoot, sourceJar, packageName, analysisInfo.name.removeSuffix("Kt"), descriptor.name.asString(), compiler, tempDir)
                    }
                }
            }
        }
        return null

    }

    // Try each jar entry until we find a location
    return jarMetadata.firstNotNullOfOrNull { processJarEntry(it) }
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
    val actualSourceJar = getSourceJarPath(workspaceRoot, sourceJar).toAbsolutePath().toString()
    val sourceFileInfo = SourceJarParser().findSourceFileInfo(
        sourcesJarPath = actualSourceJar,
        packageName = packageName,
        className = className
    ) ?: return null

    val range = compiler.findDeclarationRange(
        sourceFileInfo.contents,
        declarationName = symbolName,
    )

    return when {
        isProtoJar(sourceJar) -> {
            Location(
                getSourceFilePathInJar(actualSourceJar, sourceFileInfo, tempDir),
                range
            )
        }
        !isExternalJar(sourceJar) -> {
            getLocalSourcePath(workspaceRoot, sourceJar, className)?.let { sourcePath ->
                Location(sourcePath.toUri().toString(), range)
            }
        }

        else -> Location(
            getSourceFilePathInJar(actualSourceJar, sourceFileInfo, tempDir),
            range
        )
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

fun getSourceFilePathInJar(sourceJar: String, sourceFileInfo: SourceFileInfo, tempDir: TemporaryDirectory): String {
    val tempSourceFile = tempDir.createTempFile(suffix = if(sourceFileInfo.isJava) ".java" else ".kt")
    tempSourceFile.writeText(sourceFileInfo.contents)
    return tempSourceFile.toUri().toString()
}
