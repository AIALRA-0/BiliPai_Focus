package com.android.purebilibili.feature.video.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.store.PlaybackCompletionBehavior
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.data.model.response.Page
import com.android.purebilibili.data.model.response.UgcEpisode
import com.android.purebilibili.data.model.response.UgcSeason
import com.android.purebilibili.data.model.response.UgcSection
import com.android.purebilibili.data.model.response.ViewInfo
import com.android.purebilibili.feature.video.player.ExternalPlaylistSource
import com.android.purebilibili.feature.video.player.PlaylistItem
import com.android.purebilibili.feature.video.player.PlaylistManager
import com.android.purebilibili.feature.video.playback.resolver.PlayInOrderNextSource
import com.android.purebilibili.feature.video.playback.resolver.resolvePlayInOrderNextSource
import com.android.purebilibili.feature.video.playback.next.PiliPlaybackOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlayInOrderNextPolicyTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun teardown() {
        PlaylistManager.clearPlaylist()
        Dispatchers.resetMain()
    }


    @Test
    fun `sequential should prioritize page or season before playlist`() {
        assertEquals(
            PlayInOrderNextSource.PAGE_OR_SEASON,
            resolvePlayInOrderNextSource(
                isExternalPlaylist = false,
                hasNextPage = false,
                hasNextSeasonEpisode = true,
                hasNextPlaylistItem = true
            )
        )
    }

    @Test
    fun `sequential should fallback to playlist when collection has no next`() {
        assertEquals(
            PlayInOrderNextSource.PLAYLIST,
            resolvePlayInOrderNextSource(
                isExternalPlaylist = false,
                hasNextPage = false,
                hasNextSeasonEpisode = false,
                hasNextPlaylistItem = true
            )
        )
    }

    @Test
    fun `sequential should stop when neither collection nor playlist has next`() {
        assertEquals(
            PlayInOrderNextSource.NONE,
            resolvePlayInOrderNextSource(
                isExternalPlaylist = false,
                hasNextPage = false,
                hasNextSeasonEpisode = false,
                hasNextPlaylistItem = false
            )
        )
    }

    @Test
    fun `external next watch finishes A parts before B without advancing queue during lookup`() = runTest(dispatcher) {
        withViewModel { owner, setBehavior, setInfo ->
            externalSources.forEach { source ->
                continuingBehaviors.forEach { behavior ->
                    setBehavior(behavior)
                    val firstPart = collectionInfo("BV-A", 10L, hasParts = true)
                    setInfo(firstPart)
                    PlaylistManager.setExternalPlaylist(queue, startIndex = 0, source = source)

                    val nextPart = owner.resolveNextWatchSuggestion()
                    assertEquals("BV-A", nextPart?.bvid, "$source $behavior")
                    assertEquals(11L, nextPart?.cid)
                    assertEquals(1, nextPart?.pageIndex)
                    assertNull(nextPart?.playlistIndex)
                    assertEquals(0, PlaylistManager.currentIndex.value)

                    setInfo(firstPart.copy(cid = 11L))
                    val nextVideo = owner.resolveNextWatchSuggestion()
                    assertEquals("BV-B", nextVideo?.bvid, "$source $behavior must not enter season C")
                    assertEquals(20L, nextVideo?.cid)
                    assertEquals(1, nextVideo?.playlistIndex)
                    assertNull(nextVideo?.pageIndex)
                    assertEquals(0, PlaylistManager.currentIndex.value)
                }
            }
        }
    }

    @Test
    fun `external queue tail still offers remaining parts before stop or queue loop`() = runTest(dispatcher) {
        withViewModel { owner, setBehavior, setInfo ->
            externalSources.forEach { source ->
                val firstPart = collectionInfo("BV-B", 20L, hasParts = true)
                PlaylistManager.setExternalPlaylist(queue, startIndex = 1, source = source)
                continuingBehaviors.forEach { behavior ->
                    setInfo(firstPart)
                    setBehavior(behavior)
                    val nextPart = owner.resolveNextWatchSuggestion()
                    assertEquals("BV-B", nextPart?.bvid, "$source $behavior")
                    assertEquals(21L, nextPart?.cid)
                    assertEquals(1, nextPart?.pageIndex)
                    assertNull(nextPart?.playlistIndex)
                    assertEquals(1, PlaylistManager.currentIndex.value)

                    setInfo(firstPart.copy(cid = 21L))
                    val nextVideo = owner.resolveNextWatchSuggestion()
                    if (behavior == PlaybackCompletionBehavior.LOOP_PLAYLIST) {
                        assertEquals("BV-A", nextVideo?.bvid)
                        assertEquals(10L, nextVideo?.cid)
                        assertEquals(0, nextVideo?.playlistIndex)
                        assertNull(nextVideo?.pageIndex)
                    } else {
                        assertNull(nextVideo, "$source $behavior must stop, not offer season C")
                    }
                    assertEquals(1, PlaylistManager.currentIndex.value)
                }
            }
        }
    }

    @Test
    fun `external part reversal does not reverse the queue or enter the season`() = runTest(dispatcher) {
        withViewModel { owner, setBehavior, setInfo ->
            val firstPart = collectionInfo("BV-A", 10L, hasParts = true)
            PiliPlaybackOrder.setReversed(0L, "BV-A", true)
            try {
                externalSources.forEach { source ->
                    continuingBehaviors.forEach { behavior ->
                        setBehavior(behavior)
                        PlaylistManager.setExternalPlaylist(queue, startIndex = 0, source = source)
                        setInfo(firstPart.copy(cid = 11L))
                        val nextPart = owner.resolveNextWatchSuggestion()
                        assertEquals("BV-A", nextPart?.bvid)
                        assertEquals(10L, nextPart?.cid)
                        assertEquals(0, nextPart?.pageIndex)
                        assertEquals(0, PlaylistManager.currentIndex.value)

                        setInfo(firstPart)
                        val nextVideo = owner.resolveNextWatchSuggestion()
                        assertEquals("BV-B", nextVideo?.bvid)
                        assertEquals(1, nextVideo?.playlistIndex)
                        assertEquals(0, PlaylistManager.currentIndex.value)
                    }
                }
            } finally {
                PiliPlaybackOrder.setReversed(0L, "BV-A", false)
            }
        }
    }

    @Test
    fun `manual next and previous navigate parts before crossing external video boundaries`() = runTest(dispatcher) {
        withViewModel { owner, _, setInfo ->
            var requestedPage: Int? = null
            var requestedVideo: Pair<String, Long>? = null
            var requestedAutoPlay: Boolean? = null
            every { owner.switchPage(any(), any(), any()) } answers {
                requestedPage = firstArg()
                requestedAutoPlay = thirdArg()
            }
            // Only media loading is replaced; VM target selection and queue mutation are real.
            every { owner.loadVideo(any(), any(), any(), any(), any(), any(), any(), any(), any()) } answers {
                requestedVideo = firstArg<String>() to arg<Long>(7)
                requestedAutoPlay = arg(3)
            }
            externalSources.forEach { source ->
                val info = collectionInfo("BV-A", 10L, hasParts = true)
                PlaylistManager.setExternalPlaylist(queue, startIndex = 0, source = source)
                setInfo(info)
                requestedVideo = null
                requestedPage = null
                assertTrue(owner.playNextPageOrRecommended(ignoreSavedProgress = true))
                assertEquals(1, requestedPage)
                assertNull(requestedVideo)
                assertEquals(true, requestedAutoPlay)
                assertEquals(0, PlaylistManager.currentIndex.value)

                setInfo(info.copy(cid = 11L))
                requestedPage = null
                assertTrue(owner.playPreviousPageOrRecommended(ignoreSavedProgress = true))
                assertEquals(0, requestedPage)
                assertEquals(0, PlaylistManager.currentIndex.value)

                requestedPage = null
                assertTrue(owner.playNextPageOrRecommended(ignoreSavedProgress = true))
                assertNull(requestedPage)
                assertEquals("BV-B" to 20L, requestedVideo)
                assertEquals(true, requestedAutoPlay)
                assertEquals(1, PlaylistManager.currentIndex.value)

                val tail = collectionInfo("BV-B", 20L, hasParts = true)
                setInfo(tail.copy(cid = 21L))
                requestedVideo = null
                assertEquals(false, owner.playNextPageOrRecommended(ignoreSavedProgress = true))
                assertNull(requestedVideo)
                assertEquals(1, PlaylistManager.currentIndex.value)
            }
        }
    }

    private fun withViewModel(
        block: (VideoPlaybackViewModel, (PlaybackCompletionBehavior) -> Unit, (ViewInfo) -> Unit) -> Unit
    ) {
        mockkObject(SettingsManager)
        val application = mockk<Application>(relaxed = true)
        every { application.applicationContext } returns application
        every { SettingsManager.getPlaybackCdnPreference(application) } returns flowOf("base_url")
        // Entry autoplay is disabled; explicit continuation must still request playback.
        every { SettingsManager.getClickToPlaySync(application) } returns false
        var behavior = PlaybackCompletionBehavior.PLAY_IN_ORDER
        every { SettingsManager.getPlaybackCompletionBehaviorSync(application) } answers { behavior }
        val owner = spyk(VideoPlaybackViewModel(application))
        try {
            owner.javaClass.getDeclaredField("appContext").apply { isAccessible = true }.set(owner, application)
            @Suppress("UNCHECKED_CAST")
            val state = owner.javaClass.getDeclaredField("_uiState").apply { isAccessible = true }
                .get(owner) as MutableStateFlow<VideoPlaybackUiState>
            block(owner, { behavior = it }, { state.value = VideoPlaybackUiState.Success(info = it, playUrl = "") })
        } finally {
            owner.viewModelScope.cancel()
            unmockkObject(SettingsManager)
        }
    }

    private fun collectionInfo(bvid: String, cid: Long, hasParts: Boolean) = ViewInfo(
        bvid = bvid,
        cid = cid,
        pages = if (hasParts) listOf(Page(cid = cid), Page(cid = cid + 1)) else emptyList(),
        ugc_season = UgcSeason(sections = listOf(UgcSection(episodes = listOf(
            UgcEpisode(bvid = bvid, cid = cid),
            UgcEpisode(bvid = "BV-C", cid = 30L)
        ))))
    )

    private val queue = listOf(
        PlaylistItem(bvid = "BV-A", cid = 10L, title = "A", cover = "", owner = ""),
        PlaylistItem(bvid = "BV-B", cid = 20L, title = "B", cover = "", owner = "")
    )
    private val externalSources = listOf(
        ExternalPlaylistSource.WATCH_LATER,
        ExternalPlaylistSource.FAVORITE,
        ExternalPlaylistSource.SPACE
    )
    private val continuingBehaviors = listOf(
        PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC,
        PlaybackCompletionBehavior.PLAY_IN_ORDER,
        PlaybackCompletionBehavior.LOOP_PLAYLIST
    )
}
