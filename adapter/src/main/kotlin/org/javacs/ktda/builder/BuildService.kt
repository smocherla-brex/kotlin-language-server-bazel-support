package org.javacs.ktda.builder

import java.nio.file.Path
import java.util.concurrent.CompletableFuture

interface BuildService {
    fun build(workspaceRoot: Path, targets: List<String>, args: List<String>): CompletableFuture<Void>
    fun classpath(workspaceRoot: Path, target: String, args: List<String>): Set<Path>
}
