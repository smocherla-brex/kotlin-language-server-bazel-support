package org.javacs.kt

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.DiagnosticSeverity
import java.lang.reflect.Type
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths

public data class SnippetsConfiguration(
    /** Whether code completion should return VSCode-style snippets. */
    var enabled: Boolean = true
)

public data class CodegenConfiguration(
    /** Whether to enable code generation to a temporary build directory for Java interoperability. */
    var enabled: Boolean = false
)

public data class CompletionConfiguration(
    val snippets: SnippetsConfiguration = SnippetsConfiguration()
)

public data class DiagnosticsConfiguration(
    /** Whether diagnostics are enabled. */
    var enabled: Boolean = true,
    /** The minimum severity of enabled diagnostics. */
    var level: DiagnosticSeverity = DiagnosticSeverity.Hint,
    /** The time interval between subsequent lints in ms. */
    var debounceTime: Long = 250L
)

public data class JVMConfiguration(
    /** Which JVM target the Kotlin compiler uses. See Compiler.jvmTargetFrom for possible values. */
    var target: String = "17"
)

public data class CompilerConfiguration(
    val jvm: JVMConfiguration = JVMConfiguration(),
    // whether to do lazy compilation, on demand whenever files are open-ed/referenced
    var lazyCompilation: Boolean = false
)

public data class IndexingConfiguration(
    /** Whether an index of global symbols should be built in the background. */
    var enabled: Boolean = true
)

public data class ExternalSourcesConfiguration(
    /** Whether kls-URIs should be sent to the client to describe classes in JARs. */
    var useKlsScheme: Boolean = false,
    /** Whether external classes should be automatically converted to Kotlin. */
    var autoConvertToKotlin: Boolean = false
)

data class InlayHintsConfiguration(
    var typeHints: Boolean = true,
    var parameterHints: Boolean = false,
    var chainedHints: Boolean = false
)

data class KtfmtConfiguration(
    var style: String = "google",
    var indent: Int = 4,
    var maxWidth: Int = 100,
    var continuationIndent: Int = 8,
    var removeUnusedImports: Boolean = true,
)

data class KtlintConfiguration(
    var editorConfigPath: String? = null,
    var ktlintPath: String? = "ktlint",
)

data class FormattingConfiguration(
    var formatter: String = "ktlint",
    var ktfmt: KtfmtConfiguration = KtfmtConfiguration(),
    var ktlint: KtlintConfiguration = KtlintConfiguration(),
)

fun getInitializationOptions(params: InitializeParams): InitializationOptions? {
    params.initializationOptions?.let { initializationOptions ->
        val gson = GsonBuilder().registerTypeHierarchyAdapter(Path::class.java, GsonPathConverter()).create()
        val options = gson.fromJson(initializationOptions as JsonElement, InitializationOptions::class.java)

        return options
    }

    return null
}

data class InitializationOptions(
    // A path to a directory used by the language server to store data. Used for caching purposes.
    val storagePath: Path?,
    // If lazy compilation is to be enabled by the language server. Used for performance improvements
    val lazyCompilation: Boolean = false,
    // The JVM configuration, which encapsulates the Java version used by the Kotlin compiler
    val jvmConfiguration: JVMConfiguration? = JVMConfiguration(),
    // The formatting configuration
    val formattingConfiguration: FormattingConfiguration? = FormattingConfiguration(),
)

class GsonPathConverter : JsonDeserializer<Path?> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, type: Type?, context: JsonDeserializationContext?): Path? {
        return try {
            Paths.get(json.asString)
        } catch (ex: InvalidPathException) {
            LOG.printStackTrace(ex)
            null
        }
    }
}

public data class Configuration(
    val codegen: CodegenConfiguration = CodegenConfiguration(),
    val compiler: CompilerConfiguration = CompilerConfiguration(),
    val completion: CompletionConfiguration = CompletionConfiguration(),
    val diagnostics: DiagnosticsConfiguration = DiagnosticsConfiguration(),
    val scripts: ScriptsConfiguration = ScriptsConfiguration(),
    val indexing: IndexingConfiguration = IndexingConfiguration(),
    val externalSources: ExternalSourcesConfiguration = ExternalSourcesConfiguration(),
    val inlayHints: InlayHintsConfiguration = InlayHintsConfiguration(),
    val formatting: FormattingConfiguration = FormattingConfiguration(),
)
