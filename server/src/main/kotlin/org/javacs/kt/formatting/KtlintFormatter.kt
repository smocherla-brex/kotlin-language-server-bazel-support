package org.javacs.kt.formatting

import org.eclipse.lsp4j.FormattingOptions
import org.javacs.kt.KtlintConfiguration

/**
 * KtlintFormatter formats the code using ktlint API
 */
class KtlintFormatter(private val ktlintConfig: KtlintConfiguration): Formatter {
    override fun format(code: String, options: FormattingOptions): String {
        requireNotNull(ktlintConfig.ktlintPath)
        val ktlintArgs = mutableListOf<String>("--stdin", "--format", "--log-level=none")
        ktlintConfig.editorConfigPath?.let {
            ktlintArgs.add("--editorconfig=${ktlintConfig.editorConfigPath}")
        }
        val process = ProcessBuilder(listOf(ktlintConfig.ktlintPath) + ktlintArgs)
            .start()

        process.outputStream.bufferedWriter().use { writer ->
            writer.write(code)
            writer.flush()
            writer.close()  // important: signal EOF to stdin
        }

        val exitCode = process.waitFor()
        val formatted = process.inputStream.bufferedReader().readText()

        if (exitCode != 0) {
            throw KtlintException("ktlint failed with exit code $exitCode. Output: $formatted")
        }

        return formatted
    }
}


class KtlintException(message: String) : Exception(message)
