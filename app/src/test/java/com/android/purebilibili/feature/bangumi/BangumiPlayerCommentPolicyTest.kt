package com.android.purebilibili.feature.bangumi

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class BangumiPlayerCommentPolicyTest {

    @Test
    fun `bangumi player content exposes intro and comment as first level tabs`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/bangumi/ui/player/BangumiPlayerContent.kt"
        )

        assertTrue(source.contains("else listOf(\"简介\", commentTabTitle)"))
        assertTrue(source.contains("HorizontalPager("))
        assertTrue(source.contains("VideoContentTabBar("))
        assertTrue(source.contains("BangumiCommentPanel("))
        assertTrue(source.contains("key(currentEpisode.id)"))
        assertTrue(source.contains("getShowVideoDetailCommentCount(context)"))
        assertTrue(source.contains("listState = commentListState"))
        assertFalse(source.contains("VideoCommentMainList("))

        val panel = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/bangumi/ui/player/BangumiCommentPanel.kt"
        )
        assertTrue(panel.contains("VideoCommentTab("))
        assertTrue(panel.contains("VideoCommentFloatingHeader("))
        assertTrue(panel.contains("floatingHeaderContentPaddingPx = if (floatingHeader) commentHeaderHeightProvider else null"))
        assertTrue(panel.contains("commentHeaderHeightPx.floatValue = size.height.toFloat()"))
        assertTrue(panel.contains("BottomInputBar("))
        assertTrue(panel.contains("showActionButtons = false"))
        assertTrue(panel.contains("onCheckCommentFraud = viewModel::checkCommentFraud"))
        assertFalse(panel.contains("直接评论 UP 主和大家"))
    }

    @Test
    fun `bangumi player screen initializes comments for current episode aid`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/bangumi/BangumiPlayerScreen.kt"
        )

        assertTrue(source.contains("commentViewModel.init("))
        assertTrue(source.contains("val targetOid = if (isPugv) (successState?.currentEpisode?.id ?: currentEpisodeIdForDebug) else currentAid"))
        assertTrue(source.contains("aid = targetOid"))
        assertTrue(source.contains("val targetType = if (isPugv) 33 else 1"))
        assertTrue(source.contains("commentType = targetType"))
        assertTrue(source.contains("expectedReplyCount = successState?.seasonDetail?.stat?.reply?.toInt() ?: 0"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
