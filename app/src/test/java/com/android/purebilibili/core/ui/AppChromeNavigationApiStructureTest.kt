package com.android.purebilibili.core.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppChromeNavigationApiStructureTest {

    @Test
    fun neutralChromeApisDelegateToExistingAdaptiveRenderers() {
        val chrome = loadSource("design-system/src/main/java/com/android/purebilibili/core/ui/AdaptiveChrome.kt")
        val navigation = loadSource("app/src/main/java/com/android/purebilibili/core/ui/SplitLayout.kt")

        assertTrue(chrome.contains("fun AppScaffold("))
        assertTrue(chrome.contains(") = AdaptiveScaffold("))
        assertTrue(chrome.contains("fun AppTopBar("))
        assertTrue(chrome.contains(") = AdaptiveTopAppBar("))
        assertTrue(navigation.contains("fun AppSplitLayout("))
        assertTrue(navigation.contains("AppAdaptiveSplitLayout("))
    }

    @Test
    fun migratedFeatureCallersDoNotUseLegacyChromeApis() {
        val sourceRoot = listOf(
            File("app/src/main/java/com/android/purebilibili/feature"),
            File("src/main/java/com/android/purebilibili/feature"),
        ).first(File::isDirectory)
        val legacyCalls = Regex(
            """\b(AdaptiveScaffold|AdaptiveTopAppBar|AdaptiveSplitLayout)\s*\("""
        )
        val offenders = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { legacyCalls.containsMatchIn(it.readText()) }
            .toList()

        assertFalse(offenders.isNotEmpty(), "Legacy chrome callers: ${offenders.joinToString { it.path }}")
    }

    @Test
    fun miuixSmallBarsKeepCompactPaddingsAndLargeBarsKeepUpstreamInsets() {
        // Miuix 标题可用宽度 = (总宽 - 导航 - actions) × 0.9 - titlePadding×2，
        // 默认 26dp×2 + 多 actions 会把「历史记录」等标题挤成省略号。
        val chrome = loadSource("design-system/src/main/java/com/android/purebilibili/core/ui/AdaptiveChrome.kt")

        val miuixBranches = chrome.substringAfter("if (rememberIsNativeMiuixEnabled())")
            .substringBefore("val topBarWindowInsets")

        val largeBar = miuixBranches.substringAfter("MiuixTopAppBar(")
            .substringBefore("AdaptiveTopAppBarStyle.SMALL")
        assertFalse(largeBar.contains("titlePadding = 0.dp"))
        assertFalse(largeBar.contains("navigationIconPadding = 0.dp"))
        assertTrue(miuixBranches.contains("titlePadding = 0.dp"))
        assertTrue(miuixBranches.contains("navigationIconPadding = 0.dp"))
        assertTrue(miuixBranches.contains("actionIconPadding = 0.dp"))
        val smallBar = miuixBranches.substringAfter("AdaptiveTopAppBarStyle.SMALL")
            .substringBefore("AdaptiveTopAppBarStyle.CENTERED")
        assertTrue(smallBar.contains("isMiuixNonGlassEnabled()"))
        assertTrue(smallBar.contains("MiuixSmallTopAppBar("))
        assertTrue(smallBar.contains("TopAppBar("))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/").removePrefix("design-system/")
        return listOf(File(path), File("../$path"), File(normalizedPath))
            .firstOrNull(File::exists)
            ?.readText()
            ?: error("Cannot locate $path from ${File(".").absolutePath}")
    }
}
