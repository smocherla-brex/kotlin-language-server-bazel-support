package org.javacs.kt.sourcejars

import java.io.File
import java.util.jar.JarFile


data class SourceFileInfo(
    val contents: String,
    val pathInJar: String,
    val isJava: Boolean = false,
)

object SourceJarParser {

    fun findSourceFileInfo(
        sourcesJarPath: String,
        packageName: String,
        className: String
    ): SourceFileInfo? {
        JarFile(File(sourcesJarPath)).use { jar ->
            val ktPath = "${className}.kt"
            val entry = JarFile(sourcesJarPath).entries().toList().firstOrNull {
                it.toString().endsWith(ktPath)
            }

            if(entry != null) {
                return SourceFileInfo(
                    contents = jar.getInputStream(entry).bufferedReader().readText(),
                    pathInJar = entry.toString(),
                    isJava = false
                )
            }

            val javaPath = "${className}.java"
            val javaEntry = JarFile(sourcesJarPath).entries().toList().firstOrNull {
                it.toString().endsWith(javaPath)
            }

            if(javaEntry != null) {
                return SourceFileInfo(
                    contents = jar.getInputStream(javaEntry).bufferedReader().readText(),
                    pathInJar = entry.toString(),
                    isJava = true
                )
            }

            return null
        }
    }
}
