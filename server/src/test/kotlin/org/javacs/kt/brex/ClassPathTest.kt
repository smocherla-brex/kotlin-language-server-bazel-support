package org.javacs.kt.brex

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.hasItem
import org.javacs.kt.LOG
import org.javacs.kt.classpath.defaultClassPathResolver
import org.javacs.kt.testResourcesRoot
import org.junit.Assert.assertTrue
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
        val workspaceRoot = testResourcesRoot().resolve("brex")
        val buildFile = workspaceRoot.resolve("WORKSPACE")

        assertTrue(Files.exists(buildFile))

        val resolvers = defaultClassPathResolver(listOf(workspaceRoot))
        print(resolvers)
        val classPath = resolvers.classpathOrEmpty.map { it.toString() }

        assertThat(classPath, hasItem(containsString("accounting")))
    }
}
