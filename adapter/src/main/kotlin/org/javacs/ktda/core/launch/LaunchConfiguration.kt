package org.javacs.ktda.core.launch

import org.javacs.kt.classpath.SourceJVMClassNames
import java.nio.file.Path

@Suppress("LongParameterList")
class LaunchConfiguration(
	val classpath: Set<Path>,
	val mainClass: String,
    val bazelTarget: String,
    val sourcesJVMClassNames: Set<SourceJVMClassNames>,
	val workspaceRoot: Path,
	val vmArguments: String = "",
    val javaHome: String?,
    val additionalArguments: List<String> = emptyList()
)
