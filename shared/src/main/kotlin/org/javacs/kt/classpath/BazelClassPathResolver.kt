package org.javacs.kt.classpath

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import org.javacs.kt.LOG
import org.javacs.kt.proto.LspInfo
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

    override val sourceJvmClassNames: Set<SourceJVMClassNames> get() {
        val lspInfos = getLspInfos()
        val sourcesJvmClassNames = mutableSetOf<SourceJVMClassNames>()
        lspInfos.forEach{
            LspInfo.fromJson(it).sourceFilesList.forEach { source ->
                sourcesJvmClassNames.add(SourceJVMClassNames(
                    sourceFile = Paths.get(source.path),
                    jvmNames = source.jvmClassNamesList
                ))
            }
        }
        return sourcesJvmClassNames
    }

    private fun getLspInfos(): Set<Path> {
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
        return lspInfos
    }

    override val packageSourceJarMappings: Set<PackageSourceMapping> get() {
        val lspInfos = getLspInfos()
        val packageSourceMappings = mutableSetOf<PackageSourceMapping>()
        lspInfos.forEach {
            LspInfo.fromJson(it).packageSourceMappingsList.forEach { mapping ->
                packageSourceMappings.add(PackageSourceMapping(
                    sourceJar = workspaceRoot.resolve(mapping.sourceJarPath),
                    sourcePackage = mapping.packageName,
                ))
            }
        }

        LOG.debug("Found source jar/package mapping files: {}", packageSourceMappings)
        return packageSourceMappings
    }

    private fun jarAbsolutePath(bazelOutPath: String): Path {
        if(bazelOutPath.isEmpty()) {
            return Paths.get(bazelOutPath)
        }

        return workspaceRoot.resolve(bazelOutPath)
    }

    private fun getBazelClassPathEntries(): Set<ClassPathEntry> {
        val bazelOut = Paths.get(workspaceRoot.toAbsolutePath().toString(), "bazel-out")

        if(!bazelOut.exists()) {
            return emptySet()
        }
        // Process
        val lspInfos = getLspInfos()
        val cp = mutableSetOf<ClassPathEntry>()
        val targetInfos = lspInfos.map {
            LspInfo.fromJson(it)
        }
        targetInfos.forEach {
            it.classpathList.forEach { entry ->
                cp.add(ClassPathEntry(
                    compiledJar = jarAbsolutePath(entry.compileJar),
                    sourceJar = jarAbsolutePath(entry.sourceJar)
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

        private fun hasWorkspace(root: Path): Boolean = root.resolve("WORKSPACE").exists() || root.resolve("WORKSPACE.bazel").exists() || root.resolve("WORKSPACE.bzlmod").exists()

        private fun hasModuleBazel(root: Path): Boolean = root.resolve("MODULE.bazel").exists()

        fun global(workspaceRoot: Path?): ClassPathResolver {
            LOG.info { "Initializing BazelClassPathResolver at ${workspaceRoot?.toAbsolutePath()}" }
            return workspaceRoot?.let {
                if(hasWorkspace(it) || hasModuleBazel(it)) {
                    LOG.info { "Resolving BazelClassPathResolver at ${it.toAbsolutePath()}" }
                    return BazelClassPathResolver(workspaceRoot)
                }
                ClassPathResolver.empty
            } ?: ClassPathResolver.empty
        }
    }
}
