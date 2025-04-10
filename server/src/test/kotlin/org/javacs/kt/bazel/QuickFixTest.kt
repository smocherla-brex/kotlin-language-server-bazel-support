package org.javacs.kt.bazel

import org.eclipse.lsp4j.CodeActionKind
import org.eclipse.lsp4j.CodeActionParams
import org.eclipse.lsp4j.Diagnostic
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.javacs.kt.BazelLanguageServerTextFixture
import org.junit.Test

class QuickFixTest: BazelLanguageServerTextFixture("src/QuickFix.kt") {

    @Test
    fun TestMissingImportsFix() {
        val only = listOf(CodeActionKind.QuickFix)
        val params = codeActionParams("src/QuickFix.kt", 3, 9, 3, 10, diagnostics,only)
        val codeActions = languageServer.textDocumentService.codeAction(params).get()
        assertThat(codeActions, hasSize(1))
    }
}
