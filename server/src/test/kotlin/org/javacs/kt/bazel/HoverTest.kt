package org.javacs.kt

import org.hamcrest.Matchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Ignore
import org.junit.Test

class HoverLiteralsTest : BazelLanguageServerTextFixture("hover/Literals.kt") {
    @Test fun `string reference`() {
        val hover = languageServer.textDocumentService.hover(hoverParams(file, 3, 19)).get()!!
        val contents = hover.contents.right

        assertThat(contents.value, containsString("val stringLiteral: String"))
    }
}

class HoverFunctionReferenceTest : BazelLanguageServerTextFixture("hover/FunctionReference.kt") {
    @Test fun `function reference`() {
        val hover = languageServer.textDocumentService.hover(hoverParams(file, 2, 45)).get()!!
        val contents = hover.contents.right

        assertThat(contents.value, containsString("fun isFoo(s: String): Boolean"))
    }
}

class HoverObjectReferenceTest : BazelLanguageServerTextFixture("hover/ObjectReference.kt") {
    @Test fun `object reference`() {
        val hover = languageServer.textDocumentService.hover(hoverParams(file, 2, 7)).get()!!
        val contents = hover.contents.right

        assertThat(contents.value, containsString("object AnObject"))
    }

    @Test fun `object reference with incomplete method`() {
        val hover = languageServer.textDocumentService.hover(hoverParams(file, 6, 7)).get()!!
        val contents = hover.contents.right

        assertThat(contents.value, containsString("object AnObject"))
    }

    @Test fun `object reference with method`() {
        val hover = languageServer.textDocumentService.hover(hoverParams(file, 10, 7)).get()!!
        val contents = hover.contents.right

        assertThat(contents.value, containsString("object AnObject"))
    }

    @Test fun `object method`() {
        val hover = languageServer.textDocumentService.hover(hoverParams(file, 10, 15)).get()!!
        val contents = hover.contents.right

        assertThat(contents.value, containsString("fun doh(): Unit"))
    }
}

class HoverRecoverTest : BazelLanguageServerTextFixture("hover/Recover.kt") {
    @Test fun `incrementally repair a single-expression function`() {
        replace(file, 2, 9, "\"Foo\"", "intFunction()")

        val hover = languageServer.textDocumentService.hover(hoverParams(file, 2, 11)).get()!!
        val contents = hover.contents.right

        assertThat(contents.value, containsString("fun intFunction(): Int"))
    }

    @Test fun `incrementally repair a block function`() {
        replace(file, 5, 13, "\"Foo\"", "intFunction()")

        val hover = languageServer.textDocumentService.hover(hoverParams(file, 5, 13)).get()!!
        val contents = hover.contents.right

        assertThat(contents.value, containsString("fun intFunction(): Int"))
    }
}
