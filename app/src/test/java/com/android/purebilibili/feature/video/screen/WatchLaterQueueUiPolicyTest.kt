package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.feature.video.player.ExternalPlaylistSource
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WatchLaterQueueUiPolicyTest {

    @Test
    fun showQueueBarWhenExternalSourceIsWatchLaterAndPlaylistNotEmpty() {
        assertTrue(
            shouldShowExternalPlaylistQueueBarByPolicy(
                isExternalPlaylist = true,
                externalPlaylistSource = ExternalPlaylistSource.WATCH_LATER,
                playlistSize = 8
            )
        )
    }

    @Test
    fun showQueueBarWhenExternalSourceIsSpaceAndPlaylistNotEmpty() {
        assertTrue(
            shouldShowExternalPlaylistQueueBarByPolicy(
                isExternalPlaylist = true,
                externalPlaylistSource = ExternalPlaylistSource.SPACE,
                playlistSize = 8
            )
        )
    }

    @Test
    fun showQueueBarWhenExternalSourceIsFavoriteAndPlaylistNotEmpty() {
        assertTrue(
            shouldShowExternalPlaylistQueueBarByPolicy(
                isExternalPlaylist = true,
                externalPlaylistSource = ExternalPlaylistSource.FAVORITE,
                playlistSize = 8
            )
        )
    }

    @Test
    fun hideQueueBarWhenExternalSourceIsUnknown() {
        assertFalse(
            shouldShowExternalPlaylistQueueBarByPolicy(
                isExternalPlaylist = true,
                externalPlaylistSource = ExternalPlaylistSource.UNKNOWN,
                playlistSize = 8
            )
        )
    }

    @Test
    fun hideQueueBarWhenNotExternalPlaylist() {
        assertFalse(
            shouldShowExternalPlaylistQueueBarByPolicy(
                isExternalPlaylist = false,
                externalPlaylistSource = ExternalPlaylistSource.WATCH_LATER,
                playlistSize = 8
            )
        )
    }

    @Test
    fun hideQueueBarWhenPlaylistEmpty() {
        assertFalse(
            shouldShowExternalPlaylistQueueBarByPolicy(
                isExternalPlaylist = true,
                externalPlaylistSource = ExternalPlaylistSource.WATCH_LATER,
                playlistSize = 0
            )
        )
    }

    @Test
    fun externalPlaylistQueueTitleMatchesSource() {
        assertEquals(
            "稍后再看",
            resolveExternalPlaylistQueueTitle(ExternalPlaylistSource.WATCH_LATER)
        )
        assertEquals(
            "收藏夹",
            resolveExternalPlaylistQueueTitle(ExternalPlaylistSource.FAVORITE)
        )
        assertEquals(
            "UP主视频",
            resolveExternalPlaylistQueueTitle(ExternalPlaylistSource.SPACE)
        )
    }

    @Test
    fun queueBarYieldsToCommentComposerOnCommentTab() {
        assertFalse(
            shouldShowExternalPlaylistQueueBarOnContentTab(
                queueAvailable = true,
                selectedTabIndex = VIDEO_CONTENT_COMMENT_TAB_INDEX
            )
        )
        assertTrue(
            shouldShowExternalPlaylistQueueBarOnContentTab(
                queueAvailable = true,
                selectedTabIndex = 0
            )
        )
    }

    @Test
    fun collapsedQueueBarClipsShapeBeforeHazeToAvoidSquareBlurCorners() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailPhoneContent.kt"),
            File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailPhoneContent.kt"),
        ).first { it.exists() }.readText()
        val barBlock = source
            .substringAfter("private fun ExternalPlaylistQueueCollapsedBar(")
            .substringBefore("\n}\n\n")

        assertTrue(barBlock.contains(".clip(shape)"))
        assertTrue(barBlock.contains(".height(64.dp)"))
        assertTrue(barBlock.contains("hazeEffectCompat("))
        assertTrue(
            barBlock.indexOf(".clip(shape)") < barBlock.indexOf("hazeEffectCompat("),
            "clip must precede haze so blur follows rounded corners",
        )
    }
}
