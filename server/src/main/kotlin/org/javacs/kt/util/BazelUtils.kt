package org.javacs.kt.util

import java.nio.file.Path

fun isExternalJar(jarPath: String): Boolean {
    return jarPath.contains("external/")
}

fun isProtoJar(jarPath: String): Boolean {
    return jarPath.contains("-speed")
}

fun getSourceJarPath(workspaceRoot: Path, jarPath: String): Path {
    if(isExternalJar(jarPath)) {
        return workspaceRoot.resolve(jarPath.replace("header_", "").replace(".jar", "-sources.jar"))
    } else if (isProtoJar(jarPath)) {
        return workspaceRoot.resolve(jarPath.replace("-hjar", "-src").replace("libbrex", "brex"))
    } else {
        return workspaceRoot.resolve(jarPath.replace(".abi.jar", "-sources.jar"))
    }
}
