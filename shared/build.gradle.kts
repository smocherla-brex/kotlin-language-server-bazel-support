plugins {
    id("maven-publish")
    kotlin("jvm")
    id("kotlin-language-server.publishing-conventions")
    id("kotlin-language-server.kotlin-conventions")
    alias(libs.plugins.com.google.protobuf)
}

repositories {
    mavenCentral()
}

dependencies {
    // dependencies are constrained to versions defined
    // in /platform/build.gradle.kts
    implementation(platform(project(":platform")))

    implementation(kotlin("stdlib"))
    implementation(libs.com.google.code.gson)
    implementation(libs.org.jetbrains.exposed.core)
    implementation(libs.org.jetbrains.exposed.dao)
    implementation(libs.com.google.protobuf.java)
    implementation(libs.com.google.protobuf.java.util)

    testImplementation(libs.hamcrest.all)
    testImplementation(libs.junit.junit)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.30.1"
    }
}
