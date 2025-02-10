package org.javacs.kt.sourcejars

import org.javacs.kt.util.isExternalJar
import java.io.File
import java.util.jar.JarFile



data class SourceFileInfo(
    val contents: String,
    val pathInJar: String,
    val isJava: Boolean = false,
)

class SourceJarParser {

    private fun getSourceFilePath(packageName: String, className: String, kt: Boolean): String {
        val suffix = if(kt) {
            ".kt"
        } else {
            ".java"
        }
        return if (packageName.isEmpty()) {
            "$className$suffix"
        } else {
            "${packageName.replace('.', '/')}/$className$suffix"
        }
    }
    fun findSourceFileInfo(
        sourcesJarPath: String,
        packageName: String,
        className: String
    ): SourceFileInfo? {
        JarFile(File(sourcesJarPath)).use { jar ->
            var ktSourceFilePath = getSourceFilePath(packageName, className, true)
            val javaSourceFilePath = getSourceFilePath(packageName, className, false)

            // TODO: this is a bit of a hack, find a better solution
            // but we don't follow a standard structure of the sources/tests expected in a JAR
            // so we need to inject additional parts into the path
            val baseKtFileName = ktSourceFilePath
            if (!isExternalJar(sourcesJarPath)) {
                ktSourceFilePath = if (!sourcesJarPath.contains("test/")) {
                    "main/kotlin/$ktSourceFilePath"
                } else if(sourcesJarPath.contains("test/")) {
                    "kotlin/$ktSourceFilePath"
                } else{
                    ktSourceFilePath
                }
            }


            var entry = jar.getJarEntry(ktSourceFilePath)
            if(entry == null) {
                // if we still couldn't find it, try looking through all the entries that may match
                // because our jar structures aren't consistent
                entry = jar.entries().toList().filter { it.toString().endsWith(baseKtFileName) }.firstOrNull()
            }
            var isJava = false
            // if we couldn't find it, try for a Java source file as we have Java dependencies
            if (entry == null) {
               entry = jar.getJarEntry(javaSourceFilePath)
                isJava = true
            }

            return entry?.let {
                SourceFileInfo(
                    contents = jar.getInputStream(entry).bufferedReader().readText(),
                    pathInJar = if(isJava) javaSourceFilePath else ktSourceFilePath,
                    isJava = isJava,
                )
            }

        }
    }
}
