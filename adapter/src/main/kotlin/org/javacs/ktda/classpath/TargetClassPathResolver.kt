package org.javacs.ktda.classpath

import org.javacs.ktda.builder.BuildService
import java.nio.file.Path

/**
 * TargetClassPathResolver provides the runtime classpath for a bazel
 * binary target (java_binary, kt_jvm_target). This is different from the classpath retrieved by the LSP
 * because it's a global classpath across the workspace which causes runtime issues if there's conflicting jars
 */
class TargetClassPathResolver(private val workspceRoot: Path, private val bazelTarget: String, private val bazelArgs: List<String>, private val bazelService: BuildService) {
    val classpath: Set<Path> get() = bazelService.classpath(workspceRoot, bazelTarget, bazelArgs)
}
