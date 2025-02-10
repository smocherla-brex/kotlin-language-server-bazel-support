package org.javacs.kt.classpath

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import kotlin.io.path.exists
import org.javacs.kt.LOG
import java.io.File
import java.nio.file.FileVisitOption
import kotlin.io.path.absolutePathString

internal class BazelClassPathResolver(private val workspaceRoot: Path): ClassPathResolver
{
    override val resolverType: String
        get() = "Bazel"
    override val classpath: Set<ClassPathEntry> get() {
        return getBazelClassPathEntries()
    }

    init {
        LOG.info("Initializing BazelClassPathResolver at ${workspaceRoot.toAbsolutePath()}")
    }

    private fun getBazelClassPathEntries(): Set<ClassPathEntry> {
        val bazelOut = Paths.get(workspaceRoot.toAbsolutePath().toString(), "bazel-out")

        // Process files in a single walk but collect separately
        val sourcePaths = mutableSetOf<Path>()
        val compilePaths = mutableSetOf<Path>()
        val metadataPaths = mutableMapOf<String, Path>() // key will be the base path of the jar

        Files.walk(bazelOut, FileVisitOption.FOLLOW_LINKS).use { paths ->
            paths.filter { Files.isRegularFile(it) }
                .forEach { path ->
                    when {
                        path.fileName.toString().endsWith("klsp-sources.txt") -> sourcePaths.add(path)
                        path.fileName.toString().endsWith("klsp-compile.txt") -> compilePaths.add(path)
                        path.fileName.toString().endsWith("klsp-metadata.json") -> {
                            // Store metadata path by its directory path for later lookup
                            metadataPaths[path.parent.toString()] = path
                        }
                    }
                }
        }

        // Process source jars
        val sourceJars = sourcePaths
            .flatMap { Files.readAllLines(it) }
            .filter { it.endsWith("sources.jar") || it.endsWith("src.jar") }
            .toSet()

        // Process compile jars
        val compileJars = compilePaths
            .flatMap { Files.readAllLines(it) }
            .toSet()

        // Create lookup map for source jars
        val sourceJarMap = sourceJars.associateBy {
            it.removeSuffix("-sources.jar").removeSuffix("-src.jar")
        }

        // Create final classpath entries
        val cp = compileJars.map { compileJar ->
            val normalizedCompileJar = compileJar.substringBeforeLast(".jar")
            val compiledJarPath = workspaceRoot.resolve(compileJar).toAbsolutePath()

            val sourceJar = sourceJarMap.entries
                .firstOrNull { normalizedCompileJar.contains(it.key) }
                ?.value
                ?.let { workspaceRoot.resolve(it).toAbsolutePath() }

            val metadataJson = metadataPaths.getOrDefault(compiledJarPath.parent.toString(), null)
            // Look up metadata json in the same directory as the compiled jar

            ClassPathEntry(
                compiledJar = compiledJarPath,
                sourceJar = sourceJar,
                jarMetadataJson = metadataJson,
            )
        }.toSet()

        cp.mapNotNull { it.jarMetadataJson }.map {
            LOG.info("Found jar metadata: $it")
        }
        return cp
    }
    override val currentBuildFileVersion: Long
        get() {
            if(workspaceRoot.resolve("bazel-out/volatile-status.txt").exists()) {
                return Files.readAllLines(workspaceRoot.resolve("bazel-out/volatile-status.txt")).first().split(" ").last().toLong()
            } else {
                return 0
            }
        }


    companion object {

        fun global(workspaceRoot: Path?): ClassPathResolver {
            LOG.info { "Initializing BazelClassPathResolver at ${workspaceRoot?.toAbsolutePath()}" }
            return workspaceRoot?.let {
                LOG.info { "Resolving BazelClassPathResolver at ${it.toAbsolutePath()}" }
                if(it.resolve("WORKSPACE").exists() || it.resolve("WORKSPACE.bazel").exists()) {
                    return BazelClassPathResolver(workspaceRoot)
                }
                ClassPathResolver.empty
            } ?: ClassPathResolver.empty
        }
    }
}
