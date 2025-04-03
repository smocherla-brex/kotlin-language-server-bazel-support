package org.javacs.ktda.builder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.javacs.kt.LOG
import org.javacs.ktda.util.KotlinDAException
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture

class BazelBuildService: BuildService {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun build(workspaceRoot: Path, targets: List<String>, args: List<String>): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()
        coroutineScope.launch {
            val command = listOf("bazel", "build") + targets + args
            LOG.info("Running ${command}")
            val process = ProcessBuilder().command(command)
                .directory(workspaceRoot.toFile())
                .start()

            launch { streamLineByLine(process.inputStream, "stdout") }
            launch { streamLineByLine(process.errorStream, "stderr") }
            val exitCode = withContext(Dispatchers.IO) {
                process.waitFor()
            }

            if (exitCode == 0) {
                future.complete(null)
            } else {
                future.completeExceptionally(RuntimeException("bazel build failed with $exitCode"))
            }
        }

        return future
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun streamLineByLine(inputStream: InputStream, category: String) {
        withContext(Dispatchers.IO) {
            try {
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        line?.let {
                            when(category) {
                                "stdout" -> LOG.info(it)
                                "stderr" -> LOG.error(it)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                LOG.warn("Error streaming bazel output: {}", e.message)
            }
        }
    }

    override fun classpath(workspaceRoot: Path, target: String, buildFlags: List<String>): Set<Path> {
            val command = listOf("bazel", "cquery", target) + buildFlags + listOf("--output=starlark", "--starlark:expr='\n'.join([j.path for j in providers(target)['@@_builtins//:common/java/java_info.bzl%JavaInfo'].transitive_runtime_jars.to_list()])")
            val process = ProcessBuilder().command(command)
                .directory(workspaceRoot.toFile())
                .start()

            val returnCode = process.waitFor()
            val classpathEntries = mutableSetOf<Path>()
            if(returnCode == 0) {
                process.inputStream.bufferedReader(Charsets.UTF_8).use {
                    it.lines().forEach { line ->
                        classpathEntries.add(Paths.get(line))
                    }
                }
            } else {
                throw KotlinDAException("Unable to determine runtime classpath: bazel cquery failed with exit code $returnCode")
            }
        return classpathEntries
    }
}
