package com.android.purebilibili.feature.video.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 详情页弹幕会话必须**只有一份**，且由 `currentBvid` 建立。
 *
 * 症状背景：正文层/平板布局原先各自以不同 key（`success.info.bvid`、路由参数 `bvid`）
 * 再 acquire 一次 `DanmakuManager`。同页切集时这些 key 与共享会话的 `currentBvid` 分叉，
 * 于是：① 弹幕开关实际改的是另一个 manager（功能失效）；② 额外 acquire/release 引发
 * 渲染器重建与抖动，叠加在转场帧成本上表现为「重复进入更卡」。
 */
class VideoDetailDanmakuSessionSharingStructureTest {

    @Test
    fun detailBodyAndTabletLayoutsReceiveTheSharedDanmakuSession() {
        val phone = appSource("feature/video/screen/VideoDetailPhoneContent.kt")
        val tablet = appSource("feature/video/screen/TabletVideoLayout.kt")
        val large = appSource("feature/video/screen/LargeScreenVideoLayout.kt")
        val cinema = appSource("feature/video/screen/TabletCinemaLayout.kt")

        // 正文层：不再自行 acquire，改为接收参数。
        assertFalse(
            phone.contains("rememberDanmakuManager("),
            "detail body must not acquire its own danmaku session",
        )
        assertTrue(phone.contains("danmakuManager: DanmakuManager,"))

        // 三个平板/大屏布局：同样接收共享会话。
        listOf(tablet, large, cinema).forEach { source ->
            assertFalse(
                source.contains("rememberTabletDanmakuChromeState(bvid)"),
                "tablet layouts must not acquire a session from the route bvid",
            )
            assertTrue(source.contains("rememberTabletDanmakuChromeState(danmakuManager)"))
            assertTrue(source.contains("danmakuManager: DanmakuManager,"))
        }

        // 平板 chrome 状态改为接收 manager 而非 bvid。
        val chrome = appSource("feature/video/screen/TabletDanmakuChromeState.kt")
        assertFalse(chrome.contains("rememberDanmakuManager("))
        assertTrue(chrome.contains("internal fun rememberTabletDanmakuChromeState(\n    danmakuManager: DanmakuManager,\n)"))
    }

    @Test
    fun stateHolderPassesOneSessionKeyedBytheLiveBvid() {
        val holder = appSource("feature/video/screen/VideoDetailScreenStateHolder.kt")

        // 唯一 canonical 会话：以 currentBvid（同页切集会更新）建立。
        assertTrue(holder.contains("val sharedDanmakuManager = rememberDanmakuManager(currentBvid)"))
        // 三个消费点都传共享会话，不再各自建立。
        assertTrue(holder.contains("danmakuManager = sharedDanmakuManager"))
        assertTrue(
            holder.countOccurrences("danmakuManager = sharedDanmakuManager") >= 3,
            "body + tablet + large layouts must all receive the shared session",
        )
    }

    private fun appSource(relativePath: String): String = listOf(
        File("app/src/main/java/com/android/purebilibili/$relativePath"),
        File("src/main/java/com/android/purebilibili/$relativePath"),
    ).first { it.exists() }.readText()

    private fun String.countOccurrences(needle: String): Int {
        var count = 0
        var index = indexOf(needle)
        while (index >= 0) {
            count++
            index = indexOf(needle, index + needle.length)
        }
        return count
    }
}
