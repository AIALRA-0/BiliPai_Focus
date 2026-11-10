package com.android.purebilibili.feature.video.ui.overlay

import com.android.purebilibili.core.store.PlaybackCompletionBehavior
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackOrderSelectionSheetPolicyTest {
    @Test
    fun `native order retains all real behaviors rather than persisted enum order`() {
        assertEquals(listOf("播完暂停", "顺序播放", "单个循环", "列表循环", "自动连播"),
            playerPlaybackOrderOptions.map { it.label })
        assertEquals(PlaybackCompletionBehavior.entries.toSet(), playerPlaybackOrderOptions.toSet())
        assertEquals(playerPlaybackOrderOptions.size, playerPlaybackOrderOptions.distinct().size)
    }
}
