package bazel.lsp_fixtures

import bazel.lsp_fixtures.extensions.toBase64
import javax.inject.Inject

class App {
    private val member: String = "10"
    val greeting: String
        get() {
            val local: Int = 123
            return "Hello world."
        }

    override fun toString(): String = "App"

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println(App().greeting)
        }
    }
}
