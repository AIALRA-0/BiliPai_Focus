package com.android.purebilibili.core.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse

class AppTabThemeConsistencyStructureTest {
    @Test
    fun `feature tabs cannot bypass the theme adaptive renderer`() {
        val sourceRoot = File("app/src/main/java/com/android/purebilibili/feature")
        require(sourceRoot.isDirectory) { "Cannot locate ${sourceRoot.path}" }

        val forbiddenCalls = listOf(
            "AppScrollableTabRow(",
            "AppPrimaryTabRow(",
            "AppPrimaryScrollableTabRow(",
            "AppTab(",
        )
        sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { sourceFile ->
                val source = sourceFile.readText()
                forbiddenCalls.forEach { call ->
                    assertFalse(
                        source.contains(call),
                        "${sourceFile.path} must use AppThemeAdaptiveTabRow so MD3, Miuix, and liquid-glass chrome stay consistent; found $call",
                    )
                }
            }
    }
}
