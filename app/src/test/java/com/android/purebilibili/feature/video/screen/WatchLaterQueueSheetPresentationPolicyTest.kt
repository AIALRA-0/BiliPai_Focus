package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals

class WatchLaterQueueSheetPresentationPolicyTest {

    @Test
    fun useInlineSheetWhenRealtimeHazeRequired() {
        assertEquals(
            ExternalPlaylistQueueSheetPresentation.INLINE_HAZE,
            resolveExternalPlaylistQueueSheetPresentation(requireRealtimeHaze = true)
        )
    }

    @Test
    fun canFallbackToModalWhenRealtimeHazeNotRequired() {
        assertEquals(
            ExternalPlaylistQueueSheetPresentation.MODAL,
            resolveExternalPlaylistQueueSheetPresentation(requireRealtimeHaze = false)
        )
    }
}
