package com.android.purebilibili

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ComposeCollectAsStateUsageTest {

    @Test
    fun `video entry collectAsState calls use explicit non-null context`() {
        val offenders = kotlinSourceFiles()
            .flatMap { file ->
                collectAsStateCalls(file)
            }
            .toList()

        assertTrue(
            offenders.isEmpty(),
            "Use named arguments and an explicit non-null context with collectAsState(...), " +
                "for example collectAsState(initial = value, context = EmptyCoroutineContext):\n" +
                offenders.joinToString(separator = "\n")
        )
    }

    private fun kotlinSourceFiles(): Sequence<File> {
        return productionSourceRoots()
            .asSequence()
            .filter { it.exists() }
            .flatMap { root ->
                root.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
            }
    }

    private fun productionSourceRoots(): List<File> {
        return listOf(
            File("app/src/main/java"),
            File("src/main/java"),
        ).flatMap { root ->
            listOf(
                File(root, "com/android/purebilibili/feature/video"),
                File(root, "com/android/purebilibili/feature/home"),
                File(root, "com/android/purebilibili/feature/watchlater"),
                File(root, "com/android/purebilibili/feature/list"),
                File(root, "com/android/purebilibili/feature/profile"),
                File(root, "com/android/purebilibili/navigation")
            )
        }
    }

    private fun collectAsStateCalls(file: File): Sequence<String> = sequence {
        val source = file.readText()
        var searchStart = 0

        while (true) {
            val callStart = source.indexOf(".collectAsState(", startIndex = searchStart)
            if (callStart == -1) break

            val argsStart = callStart + ".collectAsState(".length
            val argsEnd = findMatchingParenthesis(source, argsStart - 1)
            if (argsEnd == -1) {
                searchStart = argsStart
                continue
            }

            val args = source.substring(argsStart, argsEnd).trim()
            if (args.isEmpty() || firstArgumentIsPositional(args) || !hasTopLevelNamedArgument(args, "context")) {
                val lineNumber = source.take(callStart).count { it == '\n' } + 1
                yield("${file.path}:$lineNumber")
            }
            searchStart = argsEnd + 1
        }
    }

    private fun findMatchingParenthesis(source: String, openIndex: Int): Int {
        var depth = 0
        var index = openIndex
        while (index < source.length) {
            when (source[index]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return index
                }
            }
            index++
        }
        return -1
    }

    private fun firstArgumentIsPositional(args: String): Boolean {
        var depth = 0
        args.forEach { char ->
            when (char) {
                '(', '[', '{' -> depth++
                ')', ']', '}' -> depth--
                '=' -> if (depth == 0) return false
                ',' -> if (depth == 0) return true
            }
        }
        return true
    }

    private fun hasTopLevelNamedArgument(args: String, name: String): Boolean {
        var depth = 0
        var tokenStart = 0
        var index = 0
        while (index <= args.length) {
            val char = args.getOrNull(index)
            when (char) {
                '(', '[', '{' -> depth++
                ')', ']', '}' -> depth--
                ',', null -> {
                    if (depth == 0) {
                        val token = args.substring(tokenStart, index).trim()
                        if (token.startsWith("$name =")) return true
                        tokenStart = index + 1
                    }
                }
            }
            index++
        }
        return false
    }
}
