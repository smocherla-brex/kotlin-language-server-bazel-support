package org.javacs.kt.compiler

import org.eclipse.lsp4j.Range

data class KotestClassInfo(
    val className: String,
    val range: Range?,
    val describes: List<DescribeInfo>
)

data class DescribeInfo(
    val describe: String,
    val range: Range?,
    val its: List<ItInfo>
)

data class ItInfo(
    val it: String,
    val range: Range?,
)
