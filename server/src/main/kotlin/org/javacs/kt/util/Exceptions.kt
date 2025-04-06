package org.javacs.kt.util

import java.nio.file.Path

class KotlinFileNotCompiledYetException(val filePath: Path, message: String): Exception(message)
