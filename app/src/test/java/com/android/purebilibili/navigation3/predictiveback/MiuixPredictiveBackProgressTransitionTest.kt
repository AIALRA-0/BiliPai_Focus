package com.android.purebilibili.navigation3.predictiveback

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import top.yukonga.miuix.kmp.nav.transition.NavSettlePhase

class MiuixPredictiveBackProgressTransitionTest {

    @Test
    fun `preview maximum applies while held and commit completes continuously`() {
        assertEquals(
            0.3f,
            resolveMiuixPredictiveBackVisualProgress(
                rawProgress = 0.6f,
                gestureProgress = 0.6f,
                settlePhase = null,
                maxPreviewFraction = 0.5f,
            ),
            0.0001f,
        )
        assertEquals(
            0.3f,
            resolveMiuixPredictiveBackVisualProgress(
                rawProgress = 0.6f,
                gestureProgress = 0.6f,
                settlePhase = NavSettlePhase.Commit,
                maxPreviewFraction = 0.5f,
            ),
            0.0001f,
        )
        assertEquals(
            0.65f,
            resolveMiuixPredictiveBackVisualProgress(
                rawProgress = 0.8f,
                gestureProgress = 0.6f,
                settlePhase = NavSettlePhase.Commit,
                maxPreviewFraction = 0.5f,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            resolveMiuixPredictiveBackVisualProgress(
                rawProgress = 1f,
                gestureProgress = 0.6f,
                settlePhase = NavSettlePhase.Commit,
                maxPreviewFraction = 0.5f,
            ),
            0.0001f,
        )
    }

    @Test
    fun `default maximum keeps gesture progress one to one`() {
        assertEquals(
            0.6f,
            resolveMiuixPredictiveBackVisualProgress(
                rawProgress = 0.6f,
                gestureProgress = 0.6f,
                settlePhase = null,
                maxPreviewFraction = 1f,
            ),
            0.0001f,
        )
    }

    @Test
    fun `cancel follows limited preview and programmatic back stays complete`() {
        assertEquals(
            0.1f,
            resolveMiuixPredictiveBackVisualProgress(
                rawProgress = 0.2f,
                gestureProgress = 0.6f,
                settlePhase = NavSettlePhase.Cancel,
                maxPreviewFraction = 0.5f,
            ),
            0.0001f,
        )
        assertEquals(
            0.8f,
            resolveMiuixPredictiveBackVisualProgress(
                rawProgress = 0.8f,
                gestureProgress = null,
                settlePhase = NavSettlePhase.Programmatic,
                maxPreviewFraction = 0.5f,
            ),
            0.0001f,
        )
    }

    @Test
    fun `progress control is selected only for Miuix style`() {
        assertTrue(
            shouldUseMiuixPredictiveBackProgress(
                animation = BiliPaiPredictiveBackAnimationStyle.MIUIX,
                enabled = true,
            ),
        )
        assertFalse(
            shouldUseMiuixPredictiveBackProgress(
                animation = BiliPaiPredictiveBackAnimationStyle.MIUIX,
                enabled = false,
            ),
        )
        assertFalse(
            shouldUseMiuixPredictiveBackProgress(
                animation = BiliPaiPredictiveBackAnimationStyle.SCALE,
                enabled = true,
            ),
        )
    }

    @Test
    fun `gesture conflict routes keep their excluded or dedicated transitions`() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/navigation3/BiliPaiNavEntryProvider.kt"),
            File("src/main/java/com/android/purebilibili/navigation3/BiliPaiNavEntryProvider.kt"),
        ).first(File::exists).readText()

        listOf(
            "Search",
            "JsPluginContent",
            "ExternalMedia",
            "OfflineVideoPlayer",
            "AudioMode",
            "BangumiPlayer",
            "MusicDetail",
            "NativeMusic",
            "Live",
            "Web",
        ).forEach { keyName ->
            val entryBlock = source
                .substringAfter("entry<BiliPaiNavKey.$keyName>(")
                .substringBefore("content = content")
            assertTrue(
                entryBlock.contains("transition = predictiveBackExcludedTransition"),
                "$keyName must not use the limited predictive-back transition",
            )
        }
        assertTrue(
            source.substringAfter("entry<BiliPaiNavKey.VideoDetail>(")
                .substringBefore("content = content")
                .contains("transition = videoCardTransition"),
        )
        assertTrue(
            source.substringAfter("entry<BiliPaiNavKey.Story>(")
                .substringBefore("content = content")
                .contains("transition = fullscreenVideoCardTransition"),
        )
    }
}
