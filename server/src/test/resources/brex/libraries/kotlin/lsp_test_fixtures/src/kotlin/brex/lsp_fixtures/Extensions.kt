package brex.lsp_fixtures.extensions

import java.util.Base64

fun String.toBase64(): String {
    return Base64.getEncoder().encodeToString(this.toByteArray())
}
