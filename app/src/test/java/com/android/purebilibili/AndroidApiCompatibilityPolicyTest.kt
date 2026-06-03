package com.android.purebilibili

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidApiCompatibilityPolicyTest {

    @Test
    fun `production sources avoid API 35 list removeFirst calls`() {
        val sourceRoot = listOf(
            File("app/src/main/java"),
            File("src/main/java")
        ).firstOrNull { it.exists() } ?: error("Cannot locate production source root")

        val offendingLines = sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    if (line.contains(".removeFirst(")) {
                        "${file.relativeTo(sourceRoot)}:${index + 1}: ${line.trim()}"
                    } else {
                        null
                    }
                }
            }
            .toList()

        assertTrue(
            offendingLines.isEmpty(),
            "Use removeAt(0) or an Android-compatible queue API instead of List.removeFirst():\n" +
                offendingLines.joinToString(separator = "\n")
        )
    }

    @Test
    fun `manifest opts out of system predictive back while feature is paused`() {
        val manifest = listOf(
            File("app/src/main/AndroidManifest.xml"),
            File("src/main/AndroidManifest.xml")
        ).firstOrNull { it.exists() } ?: error("Cannot locate AndroidManifest.xml")

        val source = manifest.readText()

        assertFalse(
            source.contains("""android:enableOnBackInvokedCallback="true""""),
            "预测性返回功能暂停期间，不能继续全局接入系统返回预览。"
        )
        assertTrue(
            source.contains("""android:enableOnBackInvokedCallback="false""""),
            "预测性返回功能暂停期间，必须全局退出系统返回预览以避免和应用返回动画冲突。"
        )
    }
}
