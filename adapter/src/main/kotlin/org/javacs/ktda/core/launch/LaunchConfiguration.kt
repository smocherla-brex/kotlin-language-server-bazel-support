package org.javacs.ktda.core.launch

import java.nio.file.Path

class LaunchConfiguration(
	val classpath: Set<Path>,
	val mainClass: String,
    val bazelTarget: String,
    val buildArgs: List<String>,
	val workspaceRoot: Path,
	val vmArguments: String = ""
)
