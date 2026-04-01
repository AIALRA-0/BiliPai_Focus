package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.core.plugin.SkipAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerViewModelSponsorBlockPolicyTest {

    @Test
    fun showButtonAction_updatesSkipUiState() {
        val state = reduceSponsorSkipUiState(
            previous = SponsorSkipUiState(),
            action = SkipAction.ShowButton(
                skipToMs = 15_000L,
                label = "跳过恰饭",
                segmentId = "segment"
            )
        )

        assertTrue(state.visible)
        assertEquals("segment", state.segmentId)
        assertEquals(15_000L, state.skipToMs)
    }

    @Test
    fun noneAction_clearsSkipUiState() {
        val state = reduceSponsorSkipUiState(
            previous = SponsorSkipUiState(
                visible = true,
                segmentId = "segment",
                skipToMs = 15_000L,
                label = "跳过恰饭"
            ),
            action = SkipAction.None
        )

        assertFalse(state.visible)
        assertNull(state.segmentId)
    }
}
