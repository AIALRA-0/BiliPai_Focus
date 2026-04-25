package com.android.purebilibili.feature.video.ui.pager

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.math.abs

class PortraitCommentPresentationPolicyTest {

    @Test
    fun `video sub reply expansion should stay inside embedded comment sheet`() {
        assertTrue(shouldUseEmbeddedVideoSubReplyPresentation())
    }

    @Test
    fun `video detail should not mount detached sub reply sheet when embedded path is enabled`() {
        assertFalse(shouldShowDetachedVideoSubReplySheet(useEmbeddedPresentation = true))
    }

    @Test
    fun `video comment reply composer should remain enabled`() {
        assertTrue(shouldOpenPortraitCommentReplyComposer())
    }

    @Test
    fun `video detail should route thread detail inside existing comment sheet when embedded path is enabled`() {
        assertTrue(shouldOpenPortraitCommentThreadDetail(useEmbeddedPresentation = true))
    }

    @Test
    fun `portrait thread detail keeps main sheet presentation even if parent sheet flag is false`() {
        assertTrue(
            resolvePortraitCommentHostMainSheetVisible(
                commentSheetVisible = false,
                subReplyVisible = true
            )
        )
    }

    @Test
    fun `portrait player shrinks while comment sheet is expanded`() {
        assertEquals(0.58f, resolvePortraitCommentExpandedPlayerScale(commentSheetVisible = true))
        assertEquals(1f, resolvePortraitCommentExpandedPlayerScale(commentSheetVisible = false))
        assertTrue(
            abs(
                resolvePortraitCommentExpandedPlayerScale(commentVisibilityProgress = 0.5f) - 0.79f
            ) < 0.001f
        )
    }

    @Test
    fun `comment drag progress follows sheet offset`() {
        assertEquals(1f, resolvePortraitCommentVisibilityProgress(sheetOffsetPx = 0f, sheetHeightPx = 600f))
        assertEquals(0.5f, resolvePortraitCommentVisibilityProgress(sheetOffsetPx = 300f, sheetHeightPx = 600f))
        assertEquals(0f, resolvePortraitCommentVisibilityProgress(sheetOffsetPx = 900f, sheetHeightPx = 600f))
    }

    @Test
    fun `comment drag dismiss triggers after threshold`() {
        assertFalse(
            shouldDismissPortraitCommentSheetByDrag(
                sheetOffsetPx = 100f,
                sheetHeightPx = 600f
            )
        )
        assertTrue(
            shouldDismissPortraitCommentSheetByDrag(
                sheetOffsetPx = 180f,
                sheetHeightPx = 600f
            )
        )
    }

    @Test
    fun `detached thread detail should stay below the player when top area is reserved`() {
        assertEquals(
            0.55f,
            resolveVideoSubReplySheetMaxHeightFraction(
                screenHeightPx = 1000,
                topReservedPx = 450
            )
        )
    }

    @Test
    fun `detached thread detail should stay fullscreen when no top reserve exists`() {
        assertEquals(
            1f,
            resolveVideoSubReplySheetMaxHeightFraction(
                screenHeightPx = 1000,
                topReservedPx = 0
            )
        )
    }
}
