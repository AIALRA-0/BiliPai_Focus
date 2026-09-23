package com.android.purebilibili.feature.video.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class TabletVideoHomeNavigationContractTest {

    @Test
    fun tabletLayoutsExposeDedicatedHomeClickCallback() {
        val largeScreenSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/screen/LargeScreenVideoLayout.kt"
        )
        val tabletSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/screen/TabletVideoLayout.kt"
        )

        assertTrue(
            extractFunctionSignature(largeScreenSource, "LargeScreenVideoLayout").contains("onHomeClick: () -> Unit"),
            "LargeScreenVideoLayout 必须显式接收首页按钮回调，避免退化成返回键"
        )
        assertTrue(
            extractFunctionSignature(tabletSource, "TabletVideoLayout").contains("onHomeClick: () -> Unit"),
            "TabletVideoLayout 必须显式接收首页按钮回调，避免退化成返回键"
        )
    }

    @Test
    fun tabletLayoutsForwardHomeClickToVideoPlayerSection() {
        val largeScreenSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/screen/LargeScreenVideoLayout.kt"
        )
        val tabletSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/screen/TabletVideoLayout.kt"
        )

        assertTrue(
            firstCallBlock(largeScreenSource, "VideoPlayerSection").contains("onHomeClick = onHomeClick"),
            "LargeScreenVideoLayout 内的播放器必须使用专用首页回调"
        )
        assertTrue(
            firstCallBlock(tabletSource, "VideoPlayerSection").contains("onHomeClick = onHomeClick"),
            "TabletVideoLayout 内的播放器必须使用专用首页回调"
        )
    }

    @Test
    fun videoDetailTabletBranchPassesHomeActionToLargeScreenLayout() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt"
        )
        val largeScreenCall = firstCallBlock(source, "LargeScreenVideoLayout")

        assertTrue(
            largeScreenCall.contains("onHomeClick ="),
            "VideoDetailScreen 大屏分支必须给 LargeScreenVideoLayout 传入首页回调"
        )
        assertTrue(
            largeScreenCall.contains("resolveVideoDetailTopBarAction(isHomeButton = true)"),
            "首页回调必须解析为 HOME 行为，而不是复用普通返回"
        )
    }

    private fun loadSource(path: String): String {
        val candidates = listOf(
            File(path),
            File("src/main/java/${path.substringAfter("app/src/main/java/")}")
        )
        return candidates.firstOrNull { it.exists() }?.readText()
            ?: error("Cannot locate $path from ${File(".").absolutePath}")
    }

    private fun extractFunctionSignature(source: String, functionName: String): String {
        val start = source.indexOf("fun $functionName(")
        require(start >= 0) { "Cannot find function $functionName" }
        val openParen = source.indexOf('(', start)
        val closeParen = findMatchingParenthesis(source, openParen)
        return source.substring(start, closeParen + 1)
    }

    private fun firstCallBlock(source: String, functionName: String): String {
        val start = source.indexOf("$functionName(")
        require(start >= 0) { "Cannot find call $functionName" }
        val openParen = source.indexOf('(', start)
        val closeParen = findMatchingParenthesis(source, openParen)
        return source.substring(start, closeParen + 1)
    }

    private fun findMatchingParenthesis(source: String, openIndex: Int): Int {
        require(openIndex >= 0 && source[openIndex] == '(') { "Invalid open parenthesis index" }
        var depth = 0
        for (index in openIndex until source.length) {
            when (source[index]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return index
                }
            }
        }
        error("No matching parenthesis")
    }
}
