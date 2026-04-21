package com.android.purebilibili.feature.video.ui.pager

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
