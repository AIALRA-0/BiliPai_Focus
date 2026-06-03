package com.android.purebilibili.core.ui.lint

import java.io.File

/**
 * Shared scan utilities for the three style-lint tests. Test working directory
 * is normally the `app` module dir (Gradle convention), but we fall back to a
 * repo-root invocation just in case.
 */
internal object StyleLintSupport {

    private val candidateRoots = listOf(
        "src/main/java/com/android/purebilibili/feature" to ".",
        "app/src/main/java/com/android/purebilibili/feature" to "app"
    )

    fun featureKtFiles(): Sequence<Pair<File, String>> {
        val (rootPath, basePath) = candidateRoots
            .firstOrNull { (root, _) -> File(root).exists() }
            ?: error(
                "Cannot locate feature/ source root from cwd=" +
                    File(".").absoluteFile.canonicalPath
            )
        val base = File(basePath)
        return File(rootPath).walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .map { file ->
                val relative = file.toRelativeString(base).replace('\\', '/')
                file to relative
            }
    }

    fun findOffenders(pattern: Regex, allowlist: Set<String>): List<String> {
        val offenders = mutableListOf<String>()
        featureKtFiles().forEach { (file, relativePath) ->
            if (relativePath in allowlist) return@forEach
            file.useLines { lines ->
                lines.forEachIndexed { idx, line ->
                    if (pattern.containsMatchIn(line)) {
                        offenders.add("$relativePath:${idx + 1}: ${line.trim()}")
                    }
                }
            }
        }
        return offenders
    }
}
