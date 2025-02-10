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
import org.javacs.kt.externalsources.toKlsURI
import org.javacs.kt.externalsources.KlsURI
import org.javacs.kt.position.location
import org.javacs.kt.position.isZero
import org.javacs.kt.position.position
import org.javacs.kt.sourcejars.SourceJarParser
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.javacs.kt.compiler.Compiler
import org.javacs.kt.sourcejars.SourceFileInfo
import org.javacs.kt.util.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
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
    val l = locationFromClassPath(target, cp.classPath, cp.compiler, tempDir)
    if(l != null) {
        LOG.info("Found declaration descriptor {}", l)
        return l
    }

    var destination = location(target)

    val psi = target.findPsi()

    LOG.info("Destination is {}", destination)
    if (psi is KtNamedDeclaration) {
        destination = psi.nameIdentifier?.let(::location) ?: destination
    }

    if (destination != null) {
        val rawClassURI = destination.uri

        LOG.info { "Searching inside ${rawClassURI}" }
        if (isInsideArchive(rawClassURI, cp)) {
            LOG.info { "Found declaration descriptor inside archive $rawClassURI" }
            parseURI(rawClassURI).toKlsURI()?.let { klsURI ->
                val (klsSourceURI, content) = classContentProvider.contentOf(klsURI)

                if (config.useKlsScheme) {
                    // Defer decompilation until a jarClassContents request is sent
                    destination.uri = klsSourceURI.toString()
                } else {
                    // Return the path to a temporary file
                    // since the client has not opted into
                    // or does not support KLS URIs
                    val tmpFile = cachedTempFiles[klsSourceURI] ?: run {
                        val name = klsSourceURI.fileName.partitionAroundLast(".").first
                        val extensionWithoutDot = klsSourceURI.fileExtension
                        val extension = if (extensionWithoutDot != null) ".$extensionWithoutDot" else ""
                        tempDir.createTempFile(name, extension)
                            .also {
                                it.toFile().writeText(content)
                                cachedTempFiles[klsSourceURI] = it
                            }
                    }

                    destination.uri = tmpFile.toUri().toString()
                }

                if (destination.range.isZero) {
                    // Try to find the definition inside the source directly
                    val name = when (target) {
                        is ConstructorDescriptor -> target.constructedClass.name.toString()
                        else -> target.name.toString()
                    }
                    definitionPattern.findAll(content)
                        .map { it.groups[1]!! }
                        .find { it.value == name }
                        ?.let { it.range }
                        ?.let { destination.range = Range(position(content, it.first), position(content, it.last)) }
                }
            }
        } else {
            LOG.info { "Didn't find declaration descriptor inside archive." }
        }
    }

    return destination
}

private fun isInsideArchive(uri: String, cp: CompilerClassPath) =
    uri.contains(".jar!") || uri.contains(".zip!") || cp.javaHome?.let {
        Paths.get(parseURI(uri)).toString().startsWith(File(it).path)
    } ?: false


private fun locationFromClassPath(target: DeclarationDescriptor, classPathEntries: Set<ClassPathEntry>, compiler: Compiler, tempDir: TemporaryDirectory): Location? {
    val jarMetadata = classPathEntries.mapNotNull { it.jarMetadataJson }
    if (jarMetadata.isEmpty()) return null

    when(target) {
        is ClassDescriptor -> {
            return locationForClass(jarMetadata, target, compiler, tempDir)
        }
        // TODO: add support for methods
        else -> return null
    }
}


private fun locationForClass(
    jarMetadata: List<Path?>,
    descriptor: ClassDescriptor,
    compiler: Compiler,
    tempDir: TemporaryDirectory
): Location? {
    // Helper function to process a single jar entry
    fun processJarEntry(jarEntry: Path?): Location? {
        val analysis = JarMetadata.fromMetadataJsonFile(jarEntry?.toFile()) ?: return null
        val sourceJar = analysis.classes[descriptor.fqNameSafe.asString()]?.sourceJars?.firstOrNull() ?: return null
        val packageName = descriptor.containingPackage()?.asString() ?: return null
        val className = descriptor.name.asString()

        return findLocation(sourceJar, packageName, className, compiler, tempDir)
    }

    // Try each jar entry until we find a location
    return jarMetadata.firstNotNullOfOrNull { processJarEntry(it) }
}

private fun findLocation(
    sourceJar: String,
    packageName: String,
    className: String,
    compiler: Compiler,
    tempDir: TemporaryDirectory
): Location? {
    val actualSourceJar = getSourceJarPath(sourceJar)
    val sourceFileInfo = SourceJarParser().findSourceFileInfo(
        sourcesJarPath = actualSourceJar,
        packageName = packageName,
        className = className
    ) ?: return null

    val range = compiler.findDeclarationRange(
        sourceFileInfo.contents,
        packageName,
        className,
        declarationName = className
    )

    return when {
        !isExternalJar(sourceJar) -> {
            getLocalSourcePath(sourceJar, className)?.let { sourcePath ->
                Location(sourcePath.toUri().toString(), range)
            }
        }

        else -> Location(
            getSourceFilePathInJar(actualSourceJar, sourceFileInfo, tempDir),
            range
        )
    }
}

private fun getLocalSourcePath(jarPath: String, className: String): Path? {
    val baseDir = jarPath.substringBeforeLast("/").replace(Regex("bazel-out/[^/]+/bin/"), "")
    val sourceFile = "${className.replace('.', '/')}.kt"
    return Files.walk(Paths.get(baseDir).toRealPath()).use { paths ->
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
