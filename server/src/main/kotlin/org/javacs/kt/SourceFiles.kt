package org.javacs.kt

import com.intellij.openapi.util.text.StringUtil.convertLineSeparators
import com.intellij.lang.Language
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.eclipse.lsp4j.TextDocumentContentChangeEvent
import org.javacs.kt.util.KotlinLSException
import org.javacs.kt.util.filePath
import org.javacs.kt.util.describeURIs
import org.javacs.kt.util.describeURI
import org.javacs.kt.proto.LspInfo
import java.io.BufferedReader
import java.io.StringReader
import java.io.StringWriter
import java.io.IOException
import java.io.FileNotFoundException
import java.net.URI
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import kotlin.io.path.absolute
import kotlin.io.path.isRegularFile

private class SourceVersion(val content: String, val version: Int, val language: Language?, val isTemporary: Boolean)

/**
 * Notify SourcePath whenever a file changes
 */
private class NotifySourcePath(private val sp: SourcePath) {
    private val files = mutableMapOf<URI, SourceVersion>()

    operator fun get(uri: URI): SourceVersion? = files[uri]

    operator fun set(uri: URI, source: SourceVersion) {
        val content = convertLineSeparators(source.content)

        files[uri] = source
        sp.put(uri, content, source.language, source.isTemporary)
    }

    fun remove(uri: URI) {
        files.remove(uri)
        sp.delete(uri)
    }

    fun removeIfTemporary(uri: URI): Boolean =
        if (sp.deleteIfTemporary(uri)) {
            files.remove(uri)
            true
        } else {
            false
        }

    fun removeAll(rm: Collection<URI>) {
        files -= rm

        rm.forEach(sp::delete)
    }

    val keys get() = files.keys
}

/**
 * Keep track of the text of all files in the workspace
 */
class SourceFiles(
    private val sp: SourcePath,
    private val contentProvider: URIContentProvider,
    private val scriptsConfig: ScriptsConfiguration,
) {
    private val workspaceRoots = mutableSetOf<Path>()
    private var exclusions = SourceExclusions(workspaceRoots, scriptsConfig)
    private val files = NotifySourcePath(sp)
    private val open = mutableSetOf<URI>()
    var lazyCompilation: Boolean = false


    fun open(uri: URI, content: String, version: Int) {
        if (isIncluded(uri)) {
            files[uri] = SourceVersion(content, version, languageOf(uri), isTemporary = false)
            open.add(uri)
        }
    }

    fun close(uri: URI) {
        if (uri in open) {
            open.remove(uri)
            val removed = files.removeIfTemporary(uri)

            if (!removed) {
                val disk = readFromDisk(uri, temporary = false)

                if (disk != null) {
                    files[uri] = disk
                } else {
                    files.remove(uri)
                }
            }
        }
    }

    fun edit(uri: URI, newVersion: Int, contentChanges: List<TextDocumentContentChangeEvent>) {
        if (isIncluded(uri)) {
            val existing = files[uri]!!
            var newText = existing.content

            if (newVersion <= existing.version) {
                LOG.warn("Ignored {} version {}", describeURI(uri), newVersion)
                return
            }

            for (change in contentChanges) {
                if (change.range == null) newText = change.text
                else newText = patch(newText, change)
            }

            files[uri] = SourceVersion(newText, newVersion, existing.language, existing.isTemporary)
        }
    }

    fun createdOnDisk(uri: URI) {
        changedOnDisk(uri)
    }

    fun deletedOnDisk(uri: URI) {
        if (isSource(uri)) {
            files.remove(uri)
        }
    }

    fun changedOnDisk(uri: URI) {
        if (isSource(uri)) {
            files[uri] = readFromDisk(uri, files[uri]?.isTemporary ?: true)
                ?: throw KotlinLSException("Could not read source file '$uri' after being changed on disk")
        }
    }

    private fun readFromDisk(uri: URI, temporary: Boolean): SourceVersion? = try {
        val content = contentProvider.contentOf(uri)
        SourceVersion(content, -1, languageOf(uri), isTemporary = temporary)
    } catch (e: FileNotFoundException) {
        null
    } catch (e: IOException) {
        LOG.warn("Exception while reading source file {}", describeURI(uri))
        null
    }

    private fun isSource(uri: URI): Boolean = isIncluded(uri) && languageOf(uri) != null

    private fun languageOf(uri: URI): Language? {
        val fileName = uri.filePath?.fileName?.toString() ?: return null
        return when {
            fileName.endsWith(".kt") || fileName.endsWith(".kts") -> KotlinLanguage.INSTANCE
            else -> null
        }
    }

    fun addWorkspaceRoot(root: Path) {
        LOG.info("Searching $root using exclusions: ${exclusions.excludedPatterns}")
        val allSourceFiles = findSourceFiles(root)
        val addSources = if (lazyCompilation) {
            // We need atleast once source to compile, get a moduledescriptor and all the dependencies for symbol indexing
            LOG.info("Lazy compilation enabled, files will be compiled on-demand.")
            allSourceFiles.takeIf { it.isNotEmpty() }?.take(1) ?: emptySet()
        } else {
            LOG.info("Lazy compilation disabled, all files in the transitive closure will be compiled.")
            allSourceFiles
        }

        logAdded(addSources, root)

        for (uri in addSources) {
            readFromDisk(uri, temporary = false)?.let {
                files[uri] = it
            } ?: LOG.warn("Could not read source file '{}'", uri.path)
        }

        workspaceRoots.add(root)
        updateExclusions()
    }

    fun removeWorkspaceRoot(root: Path) {
        val rmSources = files.keys.filter { it.filePath?.startsWith(root) ?: false }

        logRemoved(rmSources, root)

        files.removeAll(rmSources)
        workspaceRoots.remove(root)
        updateExclusions()
    }

    private fun findSourceFiles(root: Path): Set<URI> {
        val bazelOut = root.resolve("bazel-out")
        if(!Files.exists(bazelOut)) {
            return emptySet()
        }

        // TODO: we walk bazel-out here again to collect the files emitted by the aspect
        // this is redundant as we also do it in classpath resolver, so it might be worth unifying the logic
        // to reduce filesystem calls
        return Files.walk(bazelOut, FileVisitOption.FOLLOW_LINKS).use { paths ->
            paths.filter { it.isRegularFile() && it.fileName.toString().endsWith("kotlin-lsp.json") }
                .map { path: Path -> LspInfo.fromJson(path) }
                .map { it.sourceFilesList }
                .collect(Collectors.toList())
                .flatten()
                // we want to use only Kt source files for compiling here
                // java source files are passed separately. If we pass Java source files directly,
                // we run into an obscure "Unable to find script compilation configuration for the script KtFile" error
                .filter { it.path.endsWith(".kt") && !it.path.startsWith("external/") }
                .map { root.resolve(it.path).toUri() }
                .toSet()
        }
    }

    fun updateExclusions() {
        exclusions = SourceExclusions(workspaceRoots, scriptsConfig)
        LOG.info("Updated exclusions: ${exclusions.excludedPatterns}")
    }

    fun isOpen(uri: URI): Boolean = (uri in open)

    fun isIncluded(uri: URI): Boolean = exclusions.isURIIncluded(uri)
}

private fun patch(sourceText: String, change: TextDocumentContentChangeEvent): String {
    val range = change.range
    val reader = BufferedReader(StringReader(sourceText))
    val writer = StringWriter()

    // Skip unchanged lines
    var line = 0

    while (line < range.start.line) {
        writer.write(reader.readLine() + '\n')
        line++
    }

    // Skip unchanged chars
    for (character in 0 until range.start.character) {
        writer.write(reader.read())
    }

    // Write replacement text
    writer.write(change.text)

    // Skip replaced text
    for (i in 0 until (range.end.line - range.start.line)) {
        reader.readLine()
    }
    if (range.start.line == range.end.line) {
        reader.skip((range.end.character - range.start.character).toLong())
    } else {
        reader.skip(range.end.character.toLong())
    }

    // Write remaining text
    while (true) {
        val next = reader.read()

        if (next == -1) return writer.toString()
        else writer.write(next)
    }
}

private fun logAdded(sources: Collection<URI>, rootPath: Path?) {
    sources.map {
        LOG.info("Adding $it for $rootPath")
    }
    LOG.info("Adding {} under {} to source path", describeURIs(sources), rootPath)
}

private fun logRemoved(sources: Collection<URI>, rootPath: Path?) {
    LOG.info("Removing {} under {} to source path", describeURIs(sources), rootPath)
}
