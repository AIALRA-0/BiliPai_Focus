package com.android.purebilibili.feature.plugin

import com.android.purebilibili.core.store.CreatorSignalSnapshot
import com.android.purebilibili.core.store.TodayWatchDislikedVideoSnapshot
import com.android.purebilibili.core.store.TodayWatchFeedbackSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TodayWatchTasteInsightPolicyTest {

    @Test
    fun tasteInsightState_exposesModeFocusAndRecentNegativeFeedback() {
        val state = buildTodayWatchTasteInsightState(
            mode = TodayWatchPluginMode.RELAX,
            feedbackSnapshot = TodayWatchFeedbackSnapshot(
                dislikedBvids = setOf("BV1"),
                dislikedCreatorMids = setOf(42L),
                dislikedKeywords = setOf("吵闹", "整活"),
                recentDislikedVideos = listOf(
                    TodayWatchDislikedVideoSnapshot(
                        bvid = "BV1",
                        title = "吵闹整活合集",
                        creatorName = "UP-X",
                        creatorMid = 42L,
                        dislikedAtMillis = 1_700_010_000_000L
                    )
                )
            ),
            creatorSignals = listOf(
                CreatorSignalSnapshot(mid = 7L, name = "UP-A", score = 5.2, watchCount = 4)
            )
        )

        assertTrue(state.modeSummary.contains("短时长"))
        assertEquals("吵闹整活合集", state.recentDislikedVideos.single().title)
        assertEquals("UP-X", state.recentDislikedVideos.single().subtitle)
        assertTrue(state.negativeSignals.any { it.label == "吵闹" })
        assertTrue(state.preferredCreators.any { it.label == "UP-A" })
    }
}
