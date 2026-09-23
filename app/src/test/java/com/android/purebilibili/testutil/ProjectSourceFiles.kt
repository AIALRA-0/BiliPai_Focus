package com.android.purebilibili.testutil

import java.io.File

/** Resolves production source paths when Gradle runs tests from either the repo or app root. */
internal fun projectSourceFile(path: String): File {
    val normalizedPath = path.replace('\\', '/').removePrefix("./")
    val appRelativePath = normalizedPath.removePrefix("app/")
    val candidates = linkedSetOf<File>()
    var directory: File? = File(".").canonicalFile

    repeat(6) {
        val root = directory ?: return@repeat
        candidates += File(root, normalizedPath)
        if (normalizedPath.startsWith("app/")) {
            candidates += File(root, appRelativePath)
        } else if (normalizedPath.startsWith("src/")) {
            candidates += File(root, "app/$normalizedPath")
        }
        directory = root.parentFile
    }

    return candidates.firstOrNull(File::exists)?.canonicalFile
        ?: error(
            "Cannot locate project source '$path' from ${File(".").canonicalPath}; " +
                "searched ${candidates.joinToString()}"
        )
}

internal fun readProjectSource(path: String): String = projectSourceFile(path).readText()
