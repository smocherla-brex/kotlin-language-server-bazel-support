package org.javacs.kt.classpath

import org.jetbrains.exposed.sql.Database
import java.nio.file.Path

fun defaultClassPathResolver(workspaceRoots: Collection<Path>, db: Database? = null): ClassPathResolver {
    val childResolver = WithStdlibResolver(
        ShellClassPathResolver.global(workspaceRoots.firstOrNull())
            .or(BazelClassPathResolver.global(workspaceRoots.firstOrNull())
    ).or(BackupClassPathResolver))

    return db?.let { CachedClassPathResolver(childResolver, it) } ?: childResolver
}
