package org.javacs.kt.classpath

import org.jetbrains.exposed.sql.Database
import java.nio.file.Path

fun defaultClassPathResolver(workspaceRoots: Collection<Path>, db: Database? = null): ClassPathResolver {
    val childResolver = WithStdlibResolver(
        BazelClassPathResolver.global(workspaceRoots.firstOrNull())
    )

    return db?.let { CachedClassPathResolver(childResolver, it) } ?: childResolver
}
