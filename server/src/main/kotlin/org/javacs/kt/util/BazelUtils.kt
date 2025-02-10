package org.javacs.kt.util

fun isExternalJar(jarPath: String): Boolean {
    return jarPath.contains("external/")
}

fun getSourceJarPath(jarPath: String): String {
    if(isExternalJar(jarPath)) {
        return jarPath.replace("header_", "").replace(".jar", "-sources.jar")
    } else {
        return jarPath.replace(".abi.jar", "-sources.jar")
    }
}
