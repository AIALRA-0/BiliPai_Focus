package com.android.purebilibili.feature.video.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AudioModePlayModeStructureTest {

    @Test
    fun audioModeControlsUsePlaylistModeNavigation() {
        val source = audioModeSource()

        assertTrue(
            source.contains("onPrevious = { viewModel.playPreviousAudioModeTrack() }"),
            "听视频上一首应走 PlaylistManager.playMode 链路"
        )
        assertTrue(
            source.contains("onNext = { viewModel.playNextAudioModeTrack() }"),
            "听视频下一首应走 PlaylistManager.playMode 链路"
        )
        assertFalse(
            source.contains("onNext = { viewModel.playNextPageOrRecommended() }"),
            "听视频下一首不能绕过随机/单曲循环模式"
        )
    }

    @Test
    fun playerViewModelHandlesAudioModeCompletionThroughPlaylistMode() {
        val source = playerViewModelSource()

        assertTrue(
            source.contains("handleAudioModePlaybackEnded(ignoreSavedProgress = true)"),
            "听视频播完应优先走 PlaylistManager.playMode 链路"
        )
    }

    @Test
    fun audioModeCollectionSelectionForcesPlayback() {
        val source = audioModeSource()
        val episodeClickBlock = source
            .substringAfter("onEpisodeClick = { episode ->")
            .substringBefore("}")

        assertTrue(
            episodeClickBlock.contains("autoPlay = resolveAudioModeCollectionSwitchAutoPlay()"),
            "听视频合集切换应显式自动播放，不能受点击播放设置影响"
        )
    }

    @Test
    fun audioModeCollectionSelectionSnapsPagerToTargetCover() {
        val source = audioModeSource()
        val pagerSyncBlock = source
            .substringAfter("LaunchedEffect(currentIndex, playlist, pendingCollectionSwitchBvid)")
            .substringBefore("// 当用户滑动 Pager 时，直接加载对应视频")
        val episodeClickBlock = source
            .substringAfter("onEpisodeClick = { episode ->")
            .substringBefore("}")

        assertTrue(
            episodeClickBlock.contains("pendingCollectionSwitchBvid = episode.bvid"),
            "合集点击应标记目标 bvid，避免后续按普通滑动动画同步封面"
        )
        assertTrue(
            pagerSyncBlock.contains("pagerState.scrollToPage(currentIndex)"),
            "合集点击后的目标封面应直接同步，不应 animate 滑过去"
        )
        assertTrue(
            pagerSyncBlock.contains("pagerState.animateScrollToPage(currentIndex)"),
            "普通队列索引变化仍保留动画同步"
        )
    }

    private fun audioModeSource(): String = loadSource(
        "src/main/java/com/android/purebilibili/feature/video/screen/AudioModeScreen.kt",
        "app/src/main/java/com/android/purebilibili/feature/video/screen/AudioModeScreen.kt"
    )

    private fun playerViewModelSource(): String = loadSource(
        "src/main/java/com/android/purebilibili/feature/video/viewmodel/PlayerViewModel.kt",
        "app/src/main/java/com/android/purebilibili/feature/video/viewmodel/PlayerViewModel.kt"
    )

    private fun loadSource(vararg paths: String): String {
        val sourceFile = paths.map(::File).firstOrNull { it.exists() }
            ?: error("Cannot locate source from ${File(".").absolutePath}")
        return sourceFile.readText()
    }
}
