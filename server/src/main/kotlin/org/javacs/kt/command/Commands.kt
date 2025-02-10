package org.javacs.kt.command

const val JAVA_TO_KOTLIN_COMMAND = "convertJavaToKotlin"
const val BAZEL_REFRESH_CLASSPATH = "kotlinRefreshBazelClassPath"
const val KOTEST_TESTS_INFO = "kotestTestsInfo"

val ALL_COMMANDS = listOf(
    JAVA_TO_KOTLIN_COMMAND,
    BAZEL_REFRESH_CLASSPATH,
    KOTEST_TESTS_INFO,
)
