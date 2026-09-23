package com.android.purebilibili.feature.dynamic.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicCommentImagePreviewStructureTest {

    @Test
    fun imagePreview_returnGestureDrivesTheExistingDismissTimeline() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/ImagePreviewDialog.kt"
        ).readText()

        assertTrue(source.contains("NavigationBackHandler("))
        assertTrue(source.contains("LocalPredictiveBackGestureEnabled.current"))
        assertTrue(source.contains("?.latestEvent"))
        assertTrue(source.contains("?.progress"))
        assertTrue(source.contains("1f - backProgress"))
        assertTrue(source.contains("ImagePreviewBlurEffectCache"))
        assertTrue(source.contains("renderEffect = blurEffectCache.resolve(visualFrame.blurRadiusPx)"))
        assertTrue(source.contains("transitionEnabled = true"))
        assertFalse(source.contains("LaunchedEffect(backProgress, isDismissing)"))
        assertFalse(source.contains("BackHandler(enabled = !isDismissing)"))
    }

    @Test
    fun dynamicCommentPictures_openInAppPreviewInsteadOfBrowser() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCommentSheet.kt"
        ).readText()

        assertFalse(source.contains("Intent.ACTION_VIEW"))
        assertFalse(source.contains("Uri.parse("))
        assertTrue(source.contains("ImagePreviewDialog("))
        assertTrue(source.contains("resolveReplyPreviewTextContent(reply)"))
        assertTrue(source.contains("onImagePreview = { images, index, rect, textContent ->"))
    }

    @Test
    fun dynamicInlineSubReplies_showPicturesBeforeOpeningThread() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCommentSheet.kt"
        ).readText()
        val inlineSubReplyBlock = source
            .substringAfter("visibleSubReplies.forEach { subReply ->")
            .substringBefore("if (showInlineToggle)")

        assertTrue(inlineSubReplyBlock.contains("subReply.content.pictures"))
        assertTrue(inlineSubReplyBlock.contains("CommentPictures("))
        assertTrue(inlineSubReplyBlock.contains("resolveReplyPreviewTextContent(subReply)"))
    }
}
