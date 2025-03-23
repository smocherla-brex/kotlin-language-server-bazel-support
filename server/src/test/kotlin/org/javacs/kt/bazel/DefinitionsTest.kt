package org.javacs.kt.bazel

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.javacs.kt.BazelLanguageServerTextFixture
import org.junit.Test

class DefinitionsTest: BazelLanguageServerTextFixture("libraries/kotlin/lsp_test_fixtures/src/kotlin/brex/lsp_fixtures/Foo.kt") {

    @Test
    fun `go to definition on a extension function`() {
        val definitions = languageServer.textDocumentService.definition(definitionParams(file, 7, 40)).get().left
        val uris = definitions.map { it.uri }

        assertThat(definitions, hasSize(1))
        assertThat(uris, hasItem(containsString("file:///Users/smocherla/src/kotlin-language-server/server/build/resources/test/brex/libraries/kotlin/lsp_test_fixtures/src/kotlin/brex/lsp_fixtures/Extensions.kt")))
    }

    @Test
    fun `go to definition on a java proto`() {
        val definitions = languageServer.textDocumentService.definition(definitionParams(file, 3, 71)).get().left
        val uris = definitions.map { it.uri }

        assertThat(definitions, hasSize(1))
        assertThat(uris, hasItem(containsString(".java")))
    }

    @Test
    fun `go to definition on a kotlin proto dsl`() {
        val definitions = languageServer.textDocumentService.definition(definitionParams(file, 5, 76)).get().left
        val uris = definitions.map { it.uri }

        // TOOD: fix this test
        assertThat(definitions, hasSize(1))
        assertThat(uris, hasItem(containsString(".kt")))
    }

    @Test
    fun `go to definition on an external import`() {
        val definitions = languageServer.textDocumentService.definition(definitionParams(file, 8, 24)).get().left
        val uris = definitions.map { it.uri }

        assertThat(definitions, hasSize(1))
        assertThat(uris, hasItem(containsString(".java")))
    }
}
