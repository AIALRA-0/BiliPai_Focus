package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortraitProgressBarPolicyTest {

    @Test
    fun dragging_state_uses_live_drag_progress() {
        assertEquals(
            0.72f,
            resolvePortraitProgressDisplayProgress(
                progress = 0.15f,
                dragProgress = 0.72f,
                isDragging = true,
                pendingSettledProgress = null
            )
        )
    }

    @Test
    fun settled_drag_progress_holds_after_release_until_external_progress_catches_up() {
        assertEquals(
            0.72f,
            resolvePortraitProgressDisplayProgress(
                progress = 0.15f,
                dragProgress = 0.72f,
                isDragging = false,
                pendingSettledProgress = 0.72f
            )
        )
    }

    @Test
    fun settled_drag_progress_clears_when_external_progress_matches_target() {
        assertFalse(
            shouldHoldPortraitSettledProgress(
                progress = 0.719f,
                pendingSettledProgress = 0.72f
            )
        )
    }

    @Test
    fun settled_drag_progress_stays_when_external_progress_is_still_stale() {
        assertTrue(
            shouldHoldPortraitSettledProgress(
                progress = 0.15f,
                pendingSettledProgress = 0.72f
            )
        )
    }
}
