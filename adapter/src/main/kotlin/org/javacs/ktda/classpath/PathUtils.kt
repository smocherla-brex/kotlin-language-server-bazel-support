package org.javacs.ktda.classpath

import org.javacs.kt.LOG
import org.javacs.kt.classpath.SourceJVMClassNames
import org.javacs.ktda.util.firstNonNull
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.Path
import kotlin.io.path.relativeTo

private val fileSeparator by lazy { "[/\\\\]".toRegex() }
private val sourceFileExtensions = setOf(".kt", ".kts", ".java")

/**
 * Retrieves the JVM name represenations computed for the source file
**/
fun toJVMClassNames(workspaceRoot: Path, filePath: String, sourcesJVMClassNames: Set<SourceJVMClassNames>): List<String>? {
	return sourcesJVMClassNames.filter {
        it.sourceFile == Paths.get(filePath).relativeTo(workspaceRoot)
    }.map {
        it.jvmNames
    }.flatten()
}

// TODO: Better path resolution, especially when dealing with
// *.class files inside JARs
fun findValidKtFilePath(filePathToClass: Path, sourceName: String?) =
	filePathToClass.resolveSibling(sourceName).ifExists()
	?: filePathToClass.withExtension(".kt").ifExists()

private fun Path.ifExists() = if (Files.exists(this)) this else null

private fun Path.withExtension(extension: String) = resolveSibling(fileName.toString() + extension)

private fun String.capitalizeCharAt(index: Int) =
	take(index) + this[index].uppercaseChar() + substring(index + 1)
