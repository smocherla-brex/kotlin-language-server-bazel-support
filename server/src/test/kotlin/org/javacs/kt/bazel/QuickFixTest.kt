package org.javacs.kt.bazel

import org.eclipse.lsp4j.CodeActionKind
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.javacs.kt.BazelLanguageServerTextFixture
import org.junit.Ignore
import org.junit.Test

class QuickFixTest: BazelLanguageServerTextFixture("src/QuickFix.kt", indexingEnabled = true) {

    @Test
    @Ignore("disabled because CI doesn't have DB support yet, re-enable in a follow-up")
    fun TestMissingImportsFix() {
        val only = listOf(CodeActionKind.QuickFix)
        val params = codeActionParams("src/QuickFix.kt", 3, 9, 3, 10, diagnostics,only)
        val codeActions = languageServer.textDocumentService.codeAction(params).get()
        assertThat(codeActions, hasSize(1))
    }
}
