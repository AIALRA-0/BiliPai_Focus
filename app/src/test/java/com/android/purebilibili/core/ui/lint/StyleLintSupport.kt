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
            offenders += findMatches(file, relativePath, pattern)
        }
        return offenders
    }

    fun findOffendersInMigratedFeatures(
        pattern: Regex,
        allowlist: Set<String> = emptySet(),
    ): List<String> {
        val offenders = mutableListOf<String>()
        featureKtFiles().forEach { (file, relativePath) ->
            if (StyleLintAllowlist.MIGRATED_TOKEN_PREFIXES.none(relativePath::startsWith)) {
                return@forEach
            }
            if (relativePath in allowlist) {
                return@forEach
            }
            if (isTestedNamedTokenException(file, relativePath)) {
                return@forEach
            }
            offenders += findMatches(file, relativePath, pattern)
        }
        return offenders
    }

    private fun findMatches(file: File, relativePath: String, pattern: Regex): List<String> {
        val source = file.readText()
        return pattern.findAll(source).map { match ->
            val lineNumber = source.substring(0, match.range.first).count { it == '\n' }
            val lineStart = source.lastIndexOf('\n', match.range.first - 1) + 1
            val lineEnd = source.indexOf('\n', match.range.last + 1).let { if (it == -1) source.length else it }
            "$relativePath:${lineNumber + 1}: ${source.substring(lineStart, lineEnd).trim()}"
        }.toList()
    }

    private fun isTestedNamedTokenException(file: File, relativePath: String): Boolean {
        if (
            !relativePath.endsWith("Policy.kt") &&
            !relativePath.endsWith("Spec.kt") &&
            !relativePath.endsWith("Palette.kt")
        ) {
            return false
        }
        val testRoot = when {
            File("src/test/java").exists() -> File("src/test/java")
            File("app/src/test/java").exists() -> File("app/src/test/java")
            else -> return false
        }
        val expectedNames = setOf(
            file.nameWithoutExtension + "Test.kt",
            file.nameWithoutExtension + "PolicyTest.kt",
        )
        return testRoot.walkTopDown().any { it.isFile && it.name in expectedNames }
    }
}
