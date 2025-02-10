package bazel.lsp_fixtures

import bazel.lsp_fixtures.extensions.toBase64
import javax.inject.Inject

class Foo {

    fun hello() {
        val foo = "foo"
        println(foo.toBase64())
    }
}
