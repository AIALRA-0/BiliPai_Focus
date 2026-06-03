package com.android.purebilibili.feature.video.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoInteractionIconPolicyTest {

    @Test
    fun `landscape right sidebar uses app semantic interaction icons`() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/ui/overlay/LandscapeRightSidebar.kt")
            .readText()

        listOf(
            "rememberAppShareIcon",
            "rememberAppBookmarkIcon",
            "rememberAppCoinIcon",
            "rememberAppLikeIcon",
            "rememberAppLikeFilledIcon",
            "rememberAppMoreIcon"
        ).forEach { helper ->
            assertTrue(source.contains(helper), "Landscape sidebar should use $helper for preset-aware icons.")
        }
        assertFalse(source.contains("CupertinoIcons.Default.SquareAndArrowUp"))
        assertFalse(source.contains("CupertinoIcons.Default.Bookmark"))
        assertFalse(source.contains("CupertinoIcons.Default.HandThumbsup"))
        assertFalse(source.contains("private val CoinIcon"))
    }

    @Test
    fun `legacy video action row uses app semantic interaction icons`() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/player/VideoPlayerComponents.kt")
            .readText()

        listOf(
            "rememberAppBookmarkIcon",
            "rememberAppCoinIcon",
            "rememberAppLikeIcon",
            "rememberAppLikeFilledIcon"
        ).forEach { helper ->
            assertTrue(source.contains(helper), "Legacy video action row should use $helper for preset-aware icons.")
        }
        assertFalse(source.contains("com.android.purebilibili.core.ui.AppIcons.BiliCoin"))
        assertFalse(source.contains("if (isFavorited) CupertinoIcons.Filled.Bookmark else CupertinoIcons.Default.Bookmark"))
    }

    @Test
    fun `audio mode interaction row uses app semantic interaction icons`() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/AudioModeScreen.kt")
            .readText()

        listOf(
            "rememberAppBookmarkIcon",
            "rememberAppCoinIcon",
            "rememberAppLikeIcon",
            "rememberAppLikeFilledIcon"
        ).forEach { helper ->
            assertTrue(source.contains(helper), "Audio mode interaction row should use $helper for preset-aware icons.")
        }
        assertFalse(source.contains("AppIcons.BiliCoin"))
        assertFalse(source.contains("CupertinoIcons.Filled.HandThumbsup else CupertinoIcons.Default.HandThumbsup"))
        assertFalse(source.contains("CupertinoIcons.Filled.Bookmark else CupertinoIcons.Default.Bookmark"))
    }

    @Test
    fun `fullscreen overlay controls use app semantic interaction icons`() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/ui/overlay/VideoPlayerOverlay.kt")
            .readText()

        listOf(
            "rememberAppBookmarkIcon",
            "rememberAppCoinIcon",
            "rememberAppLikeIcon",
            "rememberAppLikeFilledIcon",
            "rememberAppMoreIcon",
            "rememberAppShareIcon"
        ).forEach { helper ->
            assertTrue(source.contains(helper), "Fullscreen overlay should use $helper for preset-aware icons.")
        }
        assertFalse(source.contains("CupertinoIcons.Default.Ellipsis"))
        assertFalse(source.contains("CupertinoIcons.Default.SquareAndArrowUp"))
        assertFalse(source.contains("CupertinoIcons.Filled.HandThumbsup else CupertinoIcons.Outlined.HandThumbsup"))
        assertFalse(source.contains("AppIcons.BiliCoin"))
        assertFalse(source.contains("CupertinoIcons.Filled.Star else CupertinoIcons.Default.Star"))
    }

    @Test
    fun `home video preview dialog uses app semantic menu icons`() {
        val source = File("src/main/java/com/android/purebilibili/feature/home/components/VideoPreviewDialog.kt")
            .readText()

        listOf(
            "rememberAppClearIcon",
            "rememberAppPhotoIcon",
            "rememberAppPlayIcon",
            "rememberAppShareIcon",
            "rememberAppWatchLaterIcon"
        ).forEach { helper ->
            assertTrue(source.contains(helper), "Preview dialog should use $helper for preset-aware icons.")
        }
        assertFalse(source.contains("CupertinoIcons.Filled.Play"))
        assertFalse(source.contains("CupertinoIcons.Default.Play"))
        assertFalse(source.contains("CupertinoIcons.Default.Clock"))
        assertFalse(source.contains("CupertinoIcons.Default.Photo"))
        assertFalse(source.contains("Icons.Rounded.Share"))
        assertFalse(source.contains("CupertinoIcons.Outlined.Xmark"))
    }

    @Test
    fun `landscape top control bar uses app semantic interaction icons`() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/ui/overlay/LandscapeTopControlBar.kt")
            .readText()

        listOf(
            "rememberAppCoinIcon",
            "rememberAppLikeFilledIcon",
            "rememberAppLikeIcon",
            "rememberAppMoreIcon",
            "rememberAppShareIcon"
        ).forEach { helper ->
            assertTrue(source.contains(helper), "Landscape top control bar should use $helper for preset-aware icons.")
        }
        assertFalse(source.contains("CupertinoIcons.Default.HandThumbsup"))
        assertFalse(source.contains("CupertinoIcons.Default.SquareAndArrowUp"))
        assertFalse(source.contains("CupertinoIcons.Default.Ellipsis"))
        assertFalse(source.contains("private val CoinIcon"))
    }

    @Test
    fun `bottom input bar uses app semantic action icons`() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/ui/components/BottomInputBar.kt")
            .readText()

        listOf(
            "rememberAppBookmarkIcon",
            "rememberAppCoinIcon",
            "rememberAppLikeFilledIcon",
            "rememberAppLikeIcon",
            "rememberAppShareIcon"
        ).forEach { helper ->
            assertTrue(source.contains(helper), "Bottom input bar should use $helper for preset-aware icons.")
        }
        assertFalse(source.contains("CupertinoIcons.Filled.HandThumbsup else CupertinoIcons.Outlined.HandThumbsup"))
        assertFalse(source.contains("CupertinoIcons.Filled.Star else CupertinoIcons.Outlined.Star"))
        assertFalse(source.contains("CupertinoIcons.Filled.SquareAndArrowUp"))
        assertFalse(source.contains("Text(\"币\""))
    }

    @Test
    fun `reply rows use app semantic like icons`() {
        val replySource = File("src/main/java/com/android/purebilibili/feature/video/ui/components/ReplyComponents.kt")
            .readText()
        val subReplySource = File("src/main/java/com/android/purebilibili/feature/video/ui/components/SubReplyDetailComponents.kt")
            .readText()

        listOf(replySource, subReplySource).forEach { source ->
            assertTrue(source.contains("rememberAppLikeFilledIcon"))
            assertTrue(source.contains("rememberAppLikeIcon"))
            assertFalse(source.contains("CupertinoIcons.Filled.HandThumbsup else CupertinoIcons"))
        }
    }

    @Test
    fun `collection row uses app semantic share icon`() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/ui/components/CollectionRow.kt")
            .readText()

        assertTrue(source.contains("rememberAppShareIcon"))
        assertFalse(source.contains("CupertinoIcons.Default.SquareAndArrowUp"))
    }

    @Test
    fun `common video close buttons use app clear icon`() {
        val paths = listOf(
            "src/main/java/com/android/purebilibili/feature/video/ui/overlay/MiniPlayerOverlay.kt",
            "src/main/java/com/android/purebilibili/feature/video/ui/components/CommentInputBar.kt",
            "src/main/java/com/android/purebilibili/feature/video/ui/components/SponsorSkipUI.kt",
            "src/main/java/com/android/purebilibili/feature/video/ui/components/CollectionSheet.kt"
        )

        paths.forEach { path ->
            val source = File(path).readText()
            assertTrue(source.contains("rememberAppClearIcon"), "$path should use rememberAppClearIcon for close actions.")
            assertFalse(source.contains("CupertinoIcons.Default.Xmark"))
            assertFalse(source.contains("CupertinoIcons.Default.XmarkCircle"))
        }
    }
}
