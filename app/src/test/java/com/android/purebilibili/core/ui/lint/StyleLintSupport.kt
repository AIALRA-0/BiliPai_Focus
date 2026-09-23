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

    private fun findMatches(file: File, relativePath: String, pattern: Regex): List<String> =
        findMatches(file.readText(), relativePath, pattern)

    internal fun findMatches(source: String, relativePath: String, pattern: Regex): List<String> {
        val scannedSource = source.withoutComments()
        return pattern.findAll(scannedSource).map { match ->
            val lineNumber = source.substring(0, match.range.first).count { it == '\n' }
            val lineStart = source.lastIndexOf('\n', match.range.first - 1) + 1
            val lineEnd = source.indexOf('\n', match.range.last + 1).let { if (it == -1) source.length else it }
            "$relativePath:${lineNumber + 1}: ${source.substring(lineStart, lineEnd).trim()}"
        }.toList()
    }

    /**
     * Replace Kotlin line and nested block comments with spaces while preserving offsets.
     * Style guards should describe executable source; code examples in KDoc must not count as
     * new UI usages. Strings and character literals are preserved so comment markers inside
     * literals do not hide subsequent source.
     */
    private fun String.withoutComments(): String {
        val result = toCharArray()
        var index = 0
        var blockCommentDepth = 0
        var inLineComment = false
        var inString = false
        var inRawString = false
        var inCharacter = false
        var escaped = false

        while (index < length) {
            val current = this[index]
            val next = getOrNull(index + 1)

            when {
                inLineComment -> {
                    if (current == '\n' || current == '\r') {
                        inLineComment = false
                    } else {
                        result[index] = ' '
                    }
                }

                blockCommentDepth > 0 -> {
                    when {
                        current == '/' && next == '*' -> {
                            result[index] = ' '
                            result[index + 1] = ' '
                            blockCommentDepth++
                            index++
                        }

                        current == '*' && next == '/' -> {
                            result[index] = ' '
                            result[index + 1] = ' '
                            blockCommentDepth--
                            index++
                        }

                        current != '\n' && current != '\r' -> result[index] = ' '
                    }
                }

                inRawString -> {
                    if (current == '"' && getOrNull(index + 1) == '"' && getOrNull(index + 2) == '"') {
                        inRawString = false
                        index += 2
                    }
                }

                inString || inCharacter -> {
                    if (escaped) {
                        escaped = false
                    } else if (current == '\\') {
                        escaped = true
                    } else if ((inString && current == '"') || (inCharacter && current == '\'')) {
                        inString = false
                        inCharacter = false
                    }
                }

                current == '/' && next == '/' -> {
                    result[index] = ' '
                    result[index + 1] = ' '
                    inLineComment = true
                    index++
                }

                current == '/' && next == '*' -> {
                    result[index] = ' '
                    result[index + 1] = ' '
                    blockCommentDepth = 1
                    index++
                }

                current == '"' && next == '"' && getOrNull(index + 2) == '"' -> {
                    inRawString = true
                    index += 2
                }

                current == '"' -> inString = true
                current == '\'' -> inCharacter = true
            }
            index++
        }

        return String(result)
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
