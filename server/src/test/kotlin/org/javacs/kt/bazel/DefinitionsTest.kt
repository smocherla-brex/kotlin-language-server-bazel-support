package org.javacs.kt.bazel

import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.javacs.kt.BazelLanguageServerTextFixture
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class SourceJarDefinitionsTest: BazelLanguageServerTextFixture("libraries/kotlin/lsp_test_fixtures/src/kotlin/bazel/lsp_fixtures/Foo.kt") {

    @Test
    @Ignore("Extension functions are not supported yet")
    fun `go to definition on a extension function`() {
        val definitions = languageServer.textDocumentService.definition(definitionParams(file, 7, 40)).get().left
        val uris = definitions.map { it.uri }

        assertThat(definitions, hasSize(1))
        assertThat(uris, hasItem(containsString("Extensions.kt")))
    }

    @Test
    @Ignore("Inline functions are not supported yet")
    fun `go to definition on a kotlin proto dsl`() {
        val definitions = languageServer.textDocumentService.definition(definitionParams(file, 5, 76)).get().left
        val uris = definitions.map { it.uri }

        // TOOD: fix this test
        assertThat(definitions, hasSize(1))
        assertThat(uris, hasItem(containsString(".kt")))
    }

    @Test
    fun `go to definition on an external import`() {
        val definitions = languageServer.textDocumentService.definition(definitionParams(file, 4, 24)).get().left
        val uris = definitions.map { it.uri }

        assertThat(definitions, hasSize(1))
        assertThat(uris, hasItem(containsString(".java")))
    }
}


class DefinitionTest : BazelLanguageServerTextFixture("definition/GoFrom.kt") {

    @Before fun `open GoFrom`() {
        open(file)
    }

    @Test
    fun `go to a definition in the same file`() {
        val definitions = languageServer.textDocumentService.definition(definitionParams(file, 3, 24)).get().left
        val uris = definitions.map { it.uri }

        assertThat(definitions, hasSize(1))
        assertThat(uris, hasItem(containsString("GoFrom.kt")))
    }
}


class GoToDefinitionOfPropertiesTest : BazelLanguageServerTextFixture("definition/GoToProperties.kt") {

    @Test
    fun `go to definition of object property`() {
        assertGoToProperty(
            of = position(15, 20),
            expect = range(4, 15, 4, 32)
        )
    }

    @Test
    fun `go to definition of top level property`() {
        assertGoToProperty(
            of = position(17, 20),
            expect = range(11, 11, 11, 23)
        )
    }

    @Test
    fun `go to definition of class level property`() {
        assertGoToProperty(
            of = position(16, 20),
            expect = range(8, 9, 8, 25)
        )
    }

    @Test
    fun `go to definition of local property`() {
        assertGoToProperty(
            of = position(18, 18),
            expect = range(14, 9, 14, 20)
        )
    }

    private fun assertGoToProperty(of: Position, expect: Range) {
        val definitions = languageServer.textDocumentService.definition(definitionParams(file, of)).get().left
        val uris = definitions.map { it.uri }
        val ranges = definitions.map { it.range }

        assertThat(definitions, hasSize(1))
        assertThat(uris, hasItem(containsString(file)))
        assertThat(ranges, hasItem(equalTo(expect)))
    }
}
