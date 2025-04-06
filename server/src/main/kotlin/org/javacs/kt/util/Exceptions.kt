package org.javacs.kt.util

import java.nio.file.Path

class KotlinFilesNotCompiledYetException(val files: Set<Path>, message: String): Exception(message)
