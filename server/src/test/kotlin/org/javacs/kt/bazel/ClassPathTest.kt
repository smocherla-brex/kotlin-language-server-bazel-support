package org.javacs.kt.bazel

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.hasItem
import org.javacs.kt.LOG
import org.javacs.kt.classpath.defaultClassPathResolver
import org.javacs.kt.testResourcesRoot
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import java.nio.file.Files

class ClassPathTest {
    companion object {
        @JvmStatic @BeforeClass
        fun setupLogger() {
            LOG.connectStdioBackend()
        }
    }

    @Test
    fun `find bazel classpath`() {
        val workspaceRoot = testResourcesRoot().resolve("bazel")
        val buildFile = workspaceRoot.resolve("WORKSPACE")

        assertTrue(Files.exists(buildFile))

        val resolvers = defaultClassPathResolver(listOf(workspaceRoot))
        val classPath = resolvers.classpathOrEmpty.map { it.toString() }

        // Has all the classpath entries
        assertEquals(resolvers.classpath.size, 26)
        assertThat(classPath, hasItem(containsString("external/")))
    }
}
