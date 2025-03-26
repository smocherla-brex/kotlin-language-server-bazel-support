pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://cache-redirector.jetbrains.com/kotlin.bintray.com/kotlin-plugin")
    }
}

rootProject.name = "kotlin-language-server"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

include(
    "platform",
    "shared",
    "server"
)
