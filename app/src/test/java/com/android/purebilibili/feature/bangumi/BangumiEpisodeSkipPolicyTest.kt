package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.data.model.response.EpisodeSkip
import com.android.purebilibili.data.model.response.SkipRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BangumiEpisodeSkipPolicyTest {
    private val skip = EpisodeSkip(
        op = SkipRange(start = 0, end = 90_000),
        ed = SkipRange(start = 1_200_000, end = 1_260_000)
    )

    @Test
    fun `opening range resolves one automatic seek`() {
        val action = resolveBangumiEpisodeSkipAction(
            currentPositionMs = 1_000,
            durationMs = 1_260_000,
            skip = skip,
            openingAlreadySkipped = false,
            endingAlreadySkipped = false
        )

        assertEquals(BangumiEpisodeSkipKind.OPENING, action?.kind)
        assertEquals(90_000L, action?.seekToMs)
        assertNull(
            resolveBangumiEpisodeSkipAction(
                currentPositionMs = 1_000,
                durationMs = 1_260_000,
                skip = skip,
                openingAlreadySkipped = true,
                endingAlreadySkipped = false
            )
        )
    }

    @Test
    fun `ending range clamps seek to player duration`() {
        val action = resolveBangumiEpisodeSkipAction(
            currentPositionMs = 1_220_000,
            durationMs = 1_240_000,
            skip = skip,
            openingAlreadySkipped = false,
            endingAlreadySkipped = false
        )

        assertEquals(BangumiEpisodeSkipKind.ENDING, action?.kind)
        assertEquals(1_240_000L, action?.seekToMs)
    }

    @Test
    fun `positions outside valid ranges do not seek`() {
        assertNull(
            resolveBangumiEpisodeSkipAction(
                currentPositionMs = 500_000,
                durationMs = 1_260_000,
                skip = skip,
                openingAlreadySkipped = false,
                endingAlreadySkipped = false
            )
        )
    }
}
