package org.javacs.kt.brex

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.hasItem
import org.javacs.kt.LOG
import org.javacs.kt.classpath.defaultClassPathResolver
import org.javacs.kt.testResourcesRoot
import org.junit.Assert.assertEquals
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

        // Has all the metadata.json entries
        print(resolvers.classpath.map { it.jarMetadataJsons }.size)
        assertEquals(resolvers.classpath.size, 67)

        assertThat(classPath, hasItem(containsString("accounting")))
        assertThat(classPath, hasItem(containsString("protos")))
    }
}
