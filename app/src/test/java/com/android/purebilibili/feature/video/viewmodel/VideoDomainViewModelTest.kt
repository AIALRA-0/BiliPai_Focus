package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.feature.video.usecase.TripleActionResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VideoDomainViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `engagement keeps transient state for same generation and resets for next video`() {
        val viewModel = VideoEngagementViewModel()
        val first = subject("BV1", generation = 1L)
        viewModel.bindSubject(first, VideoEngagementSeed(isLiked = false))
        viewModel.setCoinDialogVisible(true)

        viewModel.bindSubject(first, VideoEngagementSeed(isLiked = true))
        assertTrue(viewModel.uiState.value.coinDialogVisible)
        assertTrue(viewModel.uiState.value.isLiked)

        viewModel.bindSubject(subject("BV2", generation = 2L), VideoEngagementSeed())
        assertFalse(viewModel.uiState.value.coinDialogVisible)
        assertFalse(viewModel.uiState.value.isLiked)
    }

    @Test
    fun `engagement keeps successful local result when playback seed is stale`() = runTest(dispatcher) {
        val viewModel = VideoEngagementViewModel(actions = FakeEngagementActions())
        val first = subject("BV1", generation = 1L)
        viewModel.bindSubject(first, VideoEngagementSeed(isLiked = false, likeCount = 10))

        viewModel.toggleLike()
        runCurrent()
        viewModel.bindSubject(first, VideoEngagementSeed(isLiked = false, likeCount = 10))

        assertTrue(viewModel.uiState.value.isLiked)
        assertEquals(11, viewModel.uiState.value.likeCount)

        viewModel.bindSubject(subject("BV2", generation = 2L), VideoEngagementSeed(likeCount = 3))
        assertFalse(viewModel.uiState.value.isLiked)
        assertEquals(3, viewModel.uiState.value.likeCount)
    }

    @Test
    fun `engagement discards an old request after subject generation changes`() = runTest(dispatcher) {
        val pendingLike = CompletableDeferred<Result<Boolean>>()
        val viewModel = VideoEngagementViewModel(
            actions = FakeEngagementActions(pendingLike = pendingLike)
        )
        viewModel.bindSubject(subject("BV1", generation = 1L), VideoEngagementSeed(likeCount = 10))
        viewModel.toggleLike()
        runCurrent()

        viewModel.bindSubject(subject("BV2", generation = 2L), VideoEngagementSeed(likeCount = 3))
        pendingLike.complete(Result.success(true))
        runCurrent()

        assertFalse(viewModel.uiState.value.isLiked)
        assertEquals(3, viewModel.uiState.value.likeCount)
    }

    @Test
    fun `coin entry loads balance in engagement state`() = runTest(dispatcher) {
        val viewModel = VideoEngagementViewModel(
            actions = FakeEngagementActions(),
            coinBalanceLoader = VideoCoinBalanceLoader { 8.5 }
        )
        viewModel.bindSubject(subject("BV1", generation = 1L), VideoEngagementSeed())

        viewModel.openCoinDialog()
        runCurrent()

        assertTrue(viewModel.uiState.value.coinDialogVisible)
        assertEquals(8.5, viewModel.uiState.value.userCoinBalance ?: 0.0, 0.0)
    }

    @Test
    fun `composer drops drafts when subject generation changes`() {
        val viewModel = VideoComposerViewModel()
        val first = subject("BV1", generation = 1L)
        viewModel.bindSubject(first)
        viewModel.updateCommentDraft("draft")
        viewModel.bindSubject(first)
        assertEquals("draft", viewModel.uiState.value.commentDraft)

        viewModel.bindSubject(subject("BV2", generation = 2L))
        assertEquals("", viewModel.uiState.value.commentDraft)
    }

    @Test
    fun `supplement updates payload without replacing subject generation`() {
        val viewModel = VideoSupplementViewModel()
        val subject = subject("BV1", generation = 1L)
        viewModel.bindSubject(subject, VideoSupplementSeed(onlineCount = "1"))
        viewModel.bindSubject(subject, VideoSupplementSeed(onlineCount = "2"))

        assertEquals(subject, viewModel.uiState.value.subject)
        assertEquals("2", viewModel.uiState.value.onlineCount)
    }

    @Test
    fun `supplement discards deferred result after generation changes`() = runTest(dispatcher) {
        val viewModel = VideoSupplementViewModel(
            loader = VideoSupplementLoader { snapshot ->
                VideoSupplementSeed(onlineCount = snapshot.bvid)
            },
            startDelayMs = 100L
        )
        viewModel.bindSubject(subject("BV1", generation = 1L), VideoSupplementSeed())
        viewModel.bindSubject(subject("BV2", generation = 2L), VideoSupplementSeed())

        advanceTimeBy(100L)
        runCurrent()

        assertEquals("BV2", viewModel.uiState.value.onlineCount)
        assertEquals(2L, viewModel.uiState.value.subject?.generation)
    }

    @Test
    fun `supplement cancels deferred task while page is invisible`() = runTest(dispatcher) {
        var loads = 0
        val viewModel = VideoSupplementViewModel(
            loader = VideoSupplementLoader {
                loads += 1
                VideoSupplementSeed(onlineCount = "loaded")
            },
            startDelayMs = 100L
        )
        viewModel.bindSubject(subject("BV1", generation = 1L), VideoSupplementSeed())
        viewModel.setVisible(false)

        advanceTimeBy(100L)
        runCurrent()

        assertEquals(0, loads)
        assertEquals("", viewModel.uiState.value.onlineCount)
    }

    @Test
    fun `supplement default deferred loader preserves playback seed`() = runTest(dispatcher) {
        val viewModel = VideoSupplementViewModel(startDelayMs = 100L)
        viewModel.bindSubject(
            subject("BV1", generation = 1L),
            VideoSupplementSeed(onlineCount = "seeded", ownerFollowerCount = 12)
        )

        advanceTimeBy(100L)
        runCurrent()

        assertEquals("seeded", viewModel.uiState.value.onlineCount)
        assertEquals(12, viewModel.uiState.value.ownerFollowerCount)
    }

    @Test
    fun `composer buffered event survives a temporary collector stop`() = runTest(dispatcher) {
        val viewModel = VideoComposerViewModel()

        viewModel.notifyCommentSent()

        assertEquals(VideoComposerEvent.CommentSent, viewModel.events.first())
    }

    private fun subject(bvid: String, generation: Long) = VideoSubjectSnapshot(
        bvid = bvid,
        cid = generation,
        aid = generation,
        ownerMid = 1L,
        title = bvid,
        coverUrl = "",
        durationMs = 1_000L,
        generation = generation
    )

    private class FakeEngagementActions(
        private val pendingLike: CompletableDeferred<Result<Boolean>>? = null
    ) : VideoEngagementActions {
        override suspend fun toggleFollow(mid: Long, currentlyFollowing: Boolean) =
            Result.success(!currentlyFollowing)

        override suspend fun toggleLike(aid: Long, currentlyLiked: Boolean, bvid: String) =
            pendingLike?.await() ?: Result.success(!currentlyLiked)

        override suspend fun toggleFavorite(aid: Long, currentlyFavorited: Boolean, bvid: String) =
            Result.success(!currentlyFavorited)

        override suspend fun toggleWatchLater(
            aid: Long,
            currentlyInWatchLater: Boolean,
            bvid: String
        ) = Result.success(!currentlyInWatchLater)

        override suspend fun doCoin(aid: Long, count: Int, alsoLike: Boolean, bvid: String) =
            Result.success(true)

        override suspend fun doTripleAction(aid: Long) = Result.success(
            TripleActionResult(
                likeSuccess = true,
                coinSuccess = true,
                coinMessage = null,
                favoriteSuccess = true
            )
        )
    }
}
