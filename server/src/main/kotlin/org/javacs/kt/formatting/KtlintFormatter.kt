package org.javacs.kt.formatting

import org.eclipse.lsp4j.FormattingOptions
import org.javacs.kt.KtlintConfiguration
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Paths
import java.util.ServiceLoader

/**
 * KtlintFormatter formats the code using ktlint API
 */
class KtlintFormatter(private val ktlintConfig: KtlintConfiguration): Formatter {
    override fun format(code: String, options: FormattingOptions): String {
        var ruleProviders = setOf<RuleProvider>()
        ktlintConfig.rulesetJarPath?.let {
            ruleProviders = listOf(Paths.get(it).toUri().toURL()).loadCustomRuleProviders()
        }

        val ktlintCode = Code.fromSnippet(
            content = code
        )

        val ktlintRuleEngine = KtLintRuleEngine(
            ruleProviders = ruleProviders,
        )
        return ktlintRuleEngine.format(ktlintCode)
    }


    /**
     * Load custom ruleset providers using ServiceLoader pattern. This is how its done within the Ktlint Cli/IntelliJ plugin
     */
    private fun List<URL>.loadCustomRuleProviders(): Set<RuleProvider> =
        RuleSetProviderV3::class.java
            .loadCustomRuleProvidersFromJarFiles(this)
            .flatMap { it.getRuleProviders() }
            .toSet()

    private fun Class<RuleSetProviderV3>.loadCustomRuleProvidersFromJarFiles(urls: List<URL>): Set<RuleSetProviderV3> {
        val providersFromCustomJars =
            urls
                .distinct()
                .flatMap { url ->
                    loadProvidersFromJars(url)
                        .filterNot { it.id == RuleSetId.STANDARD }
                }.toSet()
        return providersFromCustomJars.toSet()
    }

    private fun <T> Class<T>.loadProvidersFromJars(url: URL?): Set<T> {
        return try {
                ServiceLoader.load(this, URLClassLoader(url.toArray())).toSet()
        } catch (e: Exception) {
                emptySet()
        }
    }

    private fun URL?.toArray() = this?.let { arrayOf(this) }.orEmpty()
}
