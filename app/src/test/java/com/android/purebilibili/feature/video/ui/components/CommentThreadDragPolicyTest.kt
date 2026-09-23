package com.android.purebilibili.feature.video.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommentThreadDragPolicyTest {
    @Test
    fun `up and down require the same dismissal distance`() {
        assertFalse(shouldDismissCommentThreadByDrag(100f, 1000f))
        assertFalse(shouldDismissCommentThreadByDrag(-100f, 1000f))
        assertTrue(shouldDismissCommentThreadByDrag(230f, 1000f))
        assertTrue(shouldDismissCommentThreadByDrag(-230f, 1000f))
        assertFalse(shouldDismissCommentThreadByDrag(230f, 0f))
    }

    @Test
    fun `reversing a drag stops at rest and gives remaining scroll back to list`() {
        assertEquals(-20f, consumeCommentThreadReverseDrag(50f, -20f))
        assertEquals(-50f, consumeCommentThreadReverseDrag(50f, -80f))
        assertEquals(50f, consumeCommentThreadReverseDrag(-50f, 80f))
        assertEquals(0f, consumeCommentThreadReverseDrag(0f, -80f))
        assertEquals(0f, consumeCommentThreadReverseDrag(50f, 20f))
    }

    @Test
    fun `both hosts move the panel background with the thread and preserve vertical blur reveal`() {
        listOf(
            "feature/video/ui/components/VideoCommentSheetHost.kt",
            "feature/dynamic/components/DynamicCommentSheet.kt",
        ).forEach { path ->
            val source = source(path)
            val overlay = source.substringAfter(".then(threadDrag.containerModifier)")
                .substringBefore("SubReplyDetailContent(")
            assertTrue(overlay.indexOf(".graphicsLayer") >= 0)
            assertTrue(overlay.indexOf(".background(") > overlay.indexOf(".graphicsLayer"))
            assertTrue(overlay.contains("translationY = threadDrag.offsetPx.value +"))
            assertTrue(source.contains("headerDragModifier = threadDrag.headerModifier"))
            assertTrue(source.contains("resolveCommentThreadCoveredBlurProgress(maxOf(threadBackProgress, threadDrag.revealProgress))"))
            assertTrue(source.contains("slideInVertically"))
            assertTrue(source.contains("slideOutVertically"))
            assertTrue(source.contains("if (threadBackCompleted) androidx.compose.animation.ExitTransition.None"))
        }
    }

    @Test
    fun `thread does not drag the underlying main panel`() {
        val source = source("feature/video/ui/components/VideoCommentSheetHost.kt")
        val handler = source.substringAfter(".pointerInput(mainSheetVisible, hostContent, mainSheetMeasuredHeightPx)")
            .substringBefore("detectVerticalDragGestures")
        assertTrue(handler.contains("hostContent != VideoCommentSheetHostContent.MAIN_LIST"))
        assertTrue(source.contains("listState = mainCommentListState"))
    }

    private fun source(path: String): String = listOf(
        File("app/src/main/java/com/android/purebilibili/$path"),
        File("src/main/java/com/android/purebilibili/$path"),
    ).first { it.exists() }.readText()
}
