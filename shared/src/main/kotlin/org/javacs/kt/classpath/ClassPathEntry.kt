package org.javacs.kt.classpath

import java.nio.file.Path

data class ClassPathEntry(
    val compiledJar: Path,
    val sourceJar: Path? = null,
)


data class PackageSourceMapping(
    val sourcePackage: String,
    val sourceJar: Path
)
