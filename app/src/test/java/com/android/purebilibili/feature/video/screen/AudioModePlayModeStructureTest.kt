package com.android.purebilibili.feature.video.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.android.purebilibili.data.model.response.Page

class AudioModePlayModeStructureTest {

    @Test
    fun audioModeControlsUsePlaylistModeNavigation() {
        val source = audioModePlayerSource()

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
        val source = audioModePlayerSource()
        val episodeClickBlock = source
            .substringAfter("onEpisodeClick = { episode ->")
            .substringBefore("}")

        assertTrue(
            episodeClickBlock.contains("autoPlay = resolveAudioModeCollectionSwitchAutoPlay()"),
            "听视频合集切换应显式自动播放，不能受点击播放设置影响"
        )
    }

    @Test
    fun audioModeQueueSelectionLoadsSelectedTrack() {
        val source = audioModePlayerSource()
        assertTrue(
            source.contains("PlaylistManager.playAt(index)?.let"),
            "队列选择应先更新 PlaylistManager 当前项"
        )
        assertTrue(
            source.contains("autoPlay = resolveAudioModePageSwitchAutoPlay()"),
            "队列选择应显式恢复播放"
        )
    }

    @Test
    fun audioModeLikePassesAidAndBvidInsteadOfEmptySubject() {
        val source = audioModePlayerSource()
        assertTrue(source.contains("engagementViewModel.toggleLike("))
        assertTrue(source.contains("aid = info.aid"))
        assertTrue(source.contains("bvid = info.bvid"))
        assertTrue(source.contains("currentlyLiked = engagementState.isLiked"))
        assertFalse(source.contains("onLikeClick = { engagementViewModel.toggleLike() }"))
    }

    @Test
    fun multiPageAudioModeUsesCurrentPartForLyricsMatching() {
        val pages = listOf(
            Page(cid = 11L, page = 1, part = "001. 海屿你 - 马也_Crabbit"),
            Page(cid = 22L, page = 2, part = "002. 如果可以 - 韦礼安")
        )

        assertTrue(
            resolveAudioModeTrackTitle(
                videoTitle = "2026网络最好听100首热门歌曲",
                currentCid = 11L,
                pages = pages
            ) == "001. 海屿你 - 马也_Crabbit"
        )
        val metadata = resolveAudioModeLyricMetadata(
            trackTitle = "001. 海屿你 - 马也_Crabbit",
            fallbackArtist = "那首你最爱的歌谣啊"
        )
        assertTrue(metadata.title == "海屿你")
        assertTrue(metadata.artist == "马也_Crabbit")
    }

    @Test
    fun multiPageAudioModeExposesPageSelectorAndAutoplaysSelection() {
        val source = audioModePlayerSource()

        assertTrue(source.contains("info.pages.size > 1"))
        assertTrue(source.contains("PagesSelector("))
        assertTrue(source.contains("cid = page.cid"))
        assertTrue(source.contains("autoPlay = resolveAudioModePageSwitchAutoPlay()"))
    }

    private fun audioModePlayerSource(): String = loadSource(
        "src/main/java/com/android/purebilibili/feature/video/screen/AudioModeMusicPlayer.kt",
        "app/src/main/java/com/android/purebilibili/feature/video/screen/AudioModeMusicPlayer.kt"
    )

    private fun playerViewModelSource(): String = loadSource(
        "src/main/java/com/android/purebilibili/feature/video/viewmodel/VideoPlaybackViewModel.kt",
        "app/src/main/java/com/android/purebilibili/feature/video/viewmodel/VideoPlaybackViewModel.kt"
    )

    private fun loadSource(vararg paths: String): String {
        val sourceFile = paths.map(::File).firstOrNull { it.exists() }
            ?: error("Cannot locate source from ${File(".").absolutePath}")
        return sourceFile.readText()
    }
}
