package org.javacs.ktda.core.launch

import org.javacs.kt.classpath.SourceJVMClassNames
import java.nio.file.Path

class AttachConfiguration(
	val workspaceRoot: Path,
	val hostName: String,
	val port: Int,
	val timeout: Int,
    val sourcesJVMClassNames: Set<SourceJVMClassNames>
)
