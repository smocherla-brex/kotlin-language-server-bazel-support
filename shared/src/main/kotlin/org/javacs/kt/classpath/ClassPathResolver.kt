package org.javacs.kt.classpath

import org.javacs.kt.LOG
import java.nio.file.Path
import kotlin.math.max

/** A source for creating class paths */
interface ClassPathResolver {
    val resolverType: String

    val classpath: Set<ClassPathEntry> // may throw exceptions
    val classpathOrEmpty: Set<ClassPathEntry> // does not throw exceptions
        get() = try {
            classpath
        } catch (e: Exception) {
            LOG.warn("Could not resolve classpath using {}: {}", resolverType, e.message)
            emptySet<ClassPathEntry>()
        }

    val buildScriptClasspath: Set<Path>
        get() = emptySet<Path>()
    val buildScriptClasspathOrEmpty: Set<Path>
        get() = try {
            buildScriptClasspath
        } catch (e: Exception) {
            LOG.warn("Could not resolve buildscript classpath using {}: {}", resolverType, e.message)
            emptySet<Path>()
        }

    val packageSourceJarMappings: Set<PackageSourceMapping>

    val packageSourceJarsMappingOrEmpty: Set<PackageSourceMapping>
        get() = try {
            packageSourceJarMappings
        } catch (e: Exception) {
            LOG.warn("Could not resolve package->source jar mapping using {}: {}", resolverType, e.message)
            emptySet<PackageSourceMapping>()
        }

    val sourceJvmClassNames: Set<SourceJVMClassNames>

    val sourceJvmClassNamesOrEmpty: Set<SourceJVMClassNames>
        get() = try {
            sourceJvmClassNames
        } catch (e: Exception) {
            LOG.warn("Could not resolve source JVM class names {}: {}", resolverType, e.message)
            emptySet<SourceJVMClassNames>()
        }

    val classpathWithSources: Set<ClassPathEntry> get() = classpath

    /**
     * This should return the current build file version.
     * It usually translates to the file's lastModified time.
     * Resolvers that don't have a build file use the default (i.e., 1).
     * We use 1, because this will prevent any attempt to cache non cacheable resolvers
     * (see [CachedClassPathResolver.dependenciesChanged]).
     */
    val currentBuildFileVersion: Long
        get() = 1L

    companion object {
        /** A default empty classpath implementation */
        val empty = object : ClassPathResolver {
            override val resolverType = "[]"
            override val classpath = emptySet<ClassPathEntry>()
            override val packageSourceJarMappings = emptySet<PackageSourceMapping>()
            override val sourceJvmClassNames = emptySet<SourceJVMClassNames>()
        }
    }
}

val Sequence<ClassPathResolver>.joined get() = fold(ClassPathResolver.empty) { accum, next -> accum + next }

val Collection<ClassPathResolver>.joined get() = fold(ClassPathResolver.empty) { accum, next -> accum + next }

/** Combines two classpath resolvers. */
operator fun ClassPathResolver.plus(other: ClassPathResolver): ClassPathResolver = UnionClassPathResolver(this, other)

/** Uses the left-hand classpath if not empty, otherwise uses the right. */
infix fun ClassPathResolver.or(other: ClassPathResolver): ClassPathResolver = FirstNonEmptyClassPathResolver(this, other)

/** The union of two class path resolvers. */
internal class UnionClassPathResolver(val lhs: ClassPathResolver, val rhs: ClassPathResolver) : ClassPathResolver {
    override val resolverType: String get() = "(${lhs.resolverType} + ${rhs.resolverType})"
    override val classpath get() = lhs.classpath + rhs.classpath
    override val classpathOrEmpty get() = lhs.classpathOrEmpty + rhs.classpathOrEmpty
    override val buildScriptClasspath get() = lhs.buildScriptClasspath + rhs.buildScriptClasspath
    override val buildScriptClasspathOrEmpty get() = lhs.buildScriptClasspathOrEmpty + rhs.buildScriptClasspathOrEmpty
    override val classpathWithSources get() = lhs.classpathWithSources + rhs.classpathWithSources
    override val currentBuildFileVersion: Long get() = max(lhs.currentBuildFileVersion, rhs.currentBuildFileVersion)
    override val packageSourceJarMappings: Set<PackageSourceMapping> = lhs.packageSourceJarMappings + rhs.packageSourceJarsMappingOrEmpty
    override val sourceJvmClassNames: Set<SourceJVMClassNames> = lhs.sourceJvmClassNames + rhs.sourceJvmClassNames
    override val sourceJvmClassNamesOrEmpty: Set<SourceJVMClassNames>
        get() = lhs.sourceJvmClassNamesOrEmpty + rhs.sourceJvmClassNamesOrEmpty
}

internal class FirstNonEmptyClassPathResolver(val lhs: ClassPathResolver, val rhs: ClassPathResolver) : ClassPathResolver {
    override val resolverType: String get() = "(${lhs.resolverType} or ${rhs.resolverType})"
    override val classpath get() = lhs.classpath.takeIf { it.isNotEmpty() } ?: rhs.classpath
    override val classpathOrEmpty get() = lhs.classpathOrEmpty.takeIf { it.isNotEmpty() } ?: rhs.classpathOrEmpty
    override val buildScriptClasspath get() = lhs.buildScriptClasspath.takeIf { it.isNotEmpty() } ?: rhs.buildScriptClasspath
    override val buildScriptClasspathOrEmpty get() = lhs.buildScriptClasspathOrEmpty.takeIf { it.isNotEmpty() } ?: rhs.buildScriptClasspathOrEmpty
    override val classpathWithSources get() = lhs.classpathWithSources.takeIf {
        it.isNotEmpty()
    } ?: rhs.classpathWithSources
    override val packageSourceJarMappings: Set<PackageSourceMapping> = lhs.packageSourceJarMappings.takeIf { it.isNotEmpty() } ?: rhs.packageSourceJarsMappingOrEmpty
    override val currentBuildFileVersion: Long get() = max(lhs.currentBuildFileVersion, rhs.currentBuildFileVersion)
    override val sourceJvmClassNames: Set<SourceJVMClassNames> = lhs.sourceJvmClassNames.takeIf { it.isNotEmpty() } ?: rhs.sourceJvmClassNames
    override val sourceJvmClassNamesOrEmpty: Set<SourceJVMClassNames>
        get() = lhs.sourceJvmClassNamesOrEmpty.takeIf { it.isNotEmpty() } ?: rhs.sourceJvmClassNamesOrEmpty
}
