package com.android.purebilibili.feature.video.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class TabletVideoHomeNavigationContractTest {

    @Test
    fun tabletLayoutsExposeDedicatedHomeClickCallback() {
        val cinemaSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/screen/TabletCinemaLayout.kt"
        )
        val tabletSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/screen/TabletVideoLayout.kt"
        )

        assertTrue(
            extractFunctionSignature(cinemaSource, "TabletCinemaLayout").contains("onHomeClick: () -> Unit"),
            "TabletCinemaLayout 必须显式接收首页按钮回调，避免退化成返回键"
        )
        assertTrue(
            extractFunctionSignature(tabletSource, "TabletVideoLayout").contains("onHomeClick: () -> Unit"),
            "TabletVideoLayout 必须显式接收首页按钮回调，避免退化成返回键"
        )
    }

    @Test
    fun tabletLayoutsForwardHomeClickToVideoPlayerSection() {
        val cinemaSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/screen/TabletCinemaLayout.kt"
        )
        val tabletSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/screen/TabletVideoLayout.kt"
        )

        assertTrue(
            firstCallBlock(cinemaSource, "VideoPlayerSection").contains("onHomeClick = onHomeClick"),
            "TabletCinemaLayout 内的播放器必须使用专用首页回调"
        )
        assertTrue(
            firstCallBlock(tabletSource, "VideoPlayerSection").contains("onHomeClick = onHomeClick"),
            "TabletVideoLayout 内的播放器必须使用专用首页回调"
        )
    }

    @Test
    fun videoDetailTabletBranchPassesHomeActionToTabletCinemaLayout() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt"
        )
        val tabletCinemaCall = firstCallBlock(source, "TabletCinemaLayout")

        assertTrue(
            tabletCinemaCall.contains("onHomeClick ="),
            "VideoDetailScreen 平板分支必须给 TabletCinemaLayout 传入首页回调"
        )
        assertTrue(
            tabletCinemaCall.contains("resolveVideoDetailTopBarAction(isHomeButton = true)"),
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
