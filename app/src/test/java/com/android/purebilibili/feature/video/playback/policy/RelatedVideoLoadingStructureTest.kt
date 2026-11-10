package com.android.purebilibili.feature.video.playback.policy

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RelatedVideoLoadingStructureTest {
    private fun viewModelSource(): String = File(
        "src/main/java/com/android/purebilibili/feature/video/viewmodel/VideoPlaybackViewModel.kt"
    ).readText()

    @Test
    fun recommendationsStartWhenPlaybackStateIsPublished() {
        assertTrue(viewModelSource().contains(
            "publishSubjectSnapshot(readyState)\n                        loadRelatedVideosAfterPlayback(result.info.bvid, requestToken)"
        ))
    }

    @Test
    fun recommendationRequestIsBoundedAndDoesNotDependOnPartCid() {
        val request = viewModelSource().substringAfter("private fun loadRelatedVideosAfterPlayback(")
            .substringBefore("private fun scheduleDeferredPostLoadWork(")
        assertTrue(request.contains("withTimeoutOrNull(8_000L)"))
        assertTrue(request.contains("RelatedLoadState.FAILED"))
        assertFalse(request.contains("info.cid"))
    }

    @Test
    fun restoringPendingRecommendationsRestartsLoading() {
        val restore = viewModelSource().substringAfter("fun restoreFromCache(")
            .substringBefore("private fun publishSubjectSnapshot(")
        assertTrue(restore.contains("restoredState.relatedLoadState == VideoPlaybackUiState.RelatedLoadState.LOADING"))
        assertTrue(restore.contains("loadRelatedVideosAfterPlayback(restoredState.info.bvid, currentLoadRequestToken)"))
    }
}
