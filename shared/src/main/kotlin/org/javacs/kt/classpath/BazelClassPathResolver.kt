package org.javacs.kt.classpath

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import org.javacs.kt.LOG
import org.javacs.kt.proto.LspInfoExtractor
import java.nio.file.FileVisitOption

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

    override val packageSourceJarMappings: Set<PackageSourceMapping> get() {
        val lspInfos = mutableSetOf<Path>()
        val packageSourceMappings = mutableSetOf<PackageSourceMapping>()
        val bazelOut = Paths.get(workspaceRoot.toAbsolutePath().toString(), "bazel-out")
        if(!bazelOut.exists()) {
            return emptySet()
        }
        Files.walk(bazelOut, FileVisitOption.FOLLOW_LINKS).use { paths ->
            paths.filter { Files.isRegularFile(it) }
                .forEach { path ->
                    when {
                        path.fileName.toString().endsWith("kotlin-lsp.json") -> lspInfos.add(path)
                    }
                }
        }

        lspInfos.forEach {
            LspInfoExtractor.fromJson(it).packageSourceMappingsList.forEach { mapping ->
                packageSourceMappings.add(PackageSourceMapping(
                    sourceJar = workspaceRoot.resolve(mapping.sourceJarPath),
                    sourcePackage = mapping.packageName,
                ))
            }
        }

        LOG.info("Found source jar/package mapping files: {}", packageSourceMappings)
        return packageSourceMappings
    }

    private fun getBazelClassPathEntries(): Set<ClassPathEntry> {
        val bazelOut = Paths.get(workspaceRoot.toAbsolutePath().toString(), "bazel-out")

        if(!bazelOut.exists()) {
            return emptySet()
        }
        // Process
        val lspInfos = mutableSetOf<Path>()
        val cp = mutableSetOf<ClassPathEntry>()

        Files.walk(bazelOut, FileVisitOption.FOLLOW_LINKS).use { paths ->
            paths.filter { Files.isRegularFile(it) }
                .forEach { path ->
                    when {
                        path.fileName.toString().endsWith("-kotlin-lsp.json") -> lspInfos.add(path)
                    }
                }
        }
        val targetInfos = lspInfos.map {
            LspInfoExtractor.fromJson(it)
        }
        targetInfos.forEach {
            it.classpathList.forEach { entry ->
                cp.add(ClassPathEntry(
                    compiledJar = Paths.get(entry.compileJar),
                    sourceJar = Paths.get(entry.sourceJar),
                ))
            }

        }
        return cp
    }


    override val currentBuildFileVersion: Long
        get() {
            if(workspaceRoot.resolve("bazel-out/volatile-status.txt").exists()) {
                return Files.readAllLines(workspaceRoot.resolve("bazel-out/volatile-status.txt")).first().split(" ").last().toLong()
            } else {
                LOG.warn("volatile-status.txt doesn't exist, cache maybe stale...")
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
