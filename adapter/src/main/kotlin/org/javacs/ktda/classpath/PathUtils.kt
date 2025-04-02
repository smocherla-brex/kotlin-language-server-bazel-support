package org.javacs.ktda.classpath

import org.javacs.kt.LOG
import org.javacs.kt.classpath.SourceJVMClassNames
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.Path
import kotlin.io.path.relativeTo

/**
 * Retrieves the JVM name represenations computed for the source file
**/
fun toJVMClassNames(workspaceRoot: Path, filePath: String, sourcesJVMClassNames: Set<SourceJVMClassNames>): List<String>? {
	val jvmNames = sourcesJVMClassNames.filter {
        it.sourceFile == Paths.get(filePath).relativeTo(workspaceRoot)
    }.map {
        it.jvmNames
    }.flatten()
    if(jvmNames.isEmpty()) {
        LOG.warn("Source JVM mappings is empty, breakpoints may not work for ${filePath}")
    }
    return jvmNames
}

// TODO: Better path resolution, especially when dealing with
// *.class files inside JARs
fun findValidKtFilePath(filePathToClass: Path, sourceName: String?) =
	filePathToClass.resolveSibling(sourceName).ifExists()
	?: filePathToClass.withExtension(".kt").ifExists()

private fun Path.ifExists() = if (Files.exists(this)) this else null

private fun Path.withExtension(extension: String) = resolveSibling(fileName.toString() + extension)

