package org.javacs.kt.formatting

import org.javacs.kt.Configuration
import org.javacs.kt.FormattingConfiguration
import org.eclipse.lsp4j.FormattingOptions as LspFromattingOptions

private const val DEFAULT_INDENT = 4

class FormattingService(private val config: FormattingConfiguration) {

    private val formatter: Formatter get() = when (config.formatter) {
        "ktfmt" -> KtfmtFormatter(config.ktfmt)
        "ktlint" -> KtlintFormatter(config.ktlint)
        "none" -> NopFormatter
        else -> KtfmtFormatter(config.ktfmt)
    }

    fun formatKotlinCode(
        code: String,
        options: LspFromattingOptions = LspFromattingOptions(DEFAULT_INDENT, true)
    ): String = this.formatter.format(code, options)
}
