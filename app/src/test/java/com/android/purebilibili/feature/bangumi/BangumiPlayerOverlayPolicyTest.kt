package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.data.model.response.BangumiDetail
import com.android.purebilibili.data.model.response.BangumiEpisode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BangumiPlayerOverlayPolicyTest {

    @Test
    fun resolveBangumiOverlayQualityLabel_returnsMatchedLabel() {
        val label = resolveBangumiOverlayQualityLabel(
            currentQuality = 80,
            acceptQuality = listOf(120, 80, 64),
            acceptDescription = listOf("4K", "1080P", "720P")
        )

        assertEquals("1080P", label)
    }

    @Test
    fun resolveBangumiOverlayQualityLabel_fallsBackToAutoWhenQualityMissing() {
        val label = resolveBangumiOverlayQualityLabel(
            currentQuality = 32,
            acceptQuality = listOf(120, 80, 64),
            acceptDescription = listOf("4K", "1080P", "720P")
        )

        assertEquals("自动", label)
    }

    @Test
    fun buildBangumiOverlayPages_convertsEpisodesInDisplayOrder() {
        val pages = buildBangumiOverlayPages(
            listOf(
                BangumiEpisode(
                    id = 101L,
                    cid = 1001L,
                    title = "第1话",
                    longTitle = "相遇",
                    duration = 1_420_000L
                ),
                BangumiEpisode(
                    id = 102L,
                    cid = 1002L,
                    title = "第2话",
                    longTitle = "启程",
                    duration = 1_380_000L
                )
            )
        )

        assertEquals(2, pages.size)
        assertEquals(1001L, pages[0].cid)
        assertEquals(1, pages[0].page)
        assertEquals("第1话 相遇", pages[0].part)
        assertEquals(1420L, pages[0].duration)
        assertEquals(1002L, pages[1].cid)
        assertEquals(2, pages[1].page)
        assertEquals("第2话 启程", pages[1].part)
        assertEquals(1380L, pages[1].duration)
    }

    @Test
    fun resolveBangumiOverlayCurrentPageIndex_matchesCurrentEpisode() {
        val episodes = listOf(
            BangumiEpisode(id = 101L, cid = 1001L, title = "第1话"),
            BangumiEpisode(id = 102L, cid = 1002L, title = "第2话"),
            BangumiEpisode(id = 103L, cid = 1003L, title = "第3话")
        )

        val pageIndex = resolveBangumiOverlayCurrentPageIndex(
            episodes = episodes,
            currentEpisodeId = 102L
        )

        assertEquals(1, pageIndex)
    }

    @Test
    fun resolveBangumiEpisodeForPageSelection_returnsEpisodeAtIndex() {
        val episodes = listOf(
            BangumiEpisode(id = 101L, cid = 1001L, title = "第1话"),
            BangumiEpisode(id = 102L, cid = 1002L, title = "第2话")
        )

        val selectedEpisode = resolveBangumiEpisodeForPageSelection(
            episodes = episodes,
            selectedPageIndex = 1
        )

        assertEquals(102L, selectedEpisode?.id)
    }

    @Test
    fun resolveBangumiUnsupportedOverlayActionMessage_returnsFriendlyHint() {
        assertEquals(
            "番剧暂不支持点赞操作",
            resolveBangumiUnsupportedOverlayActionMessage(BangumiOverlayUnsupportedAction.LIKE)
        )
        assertEquals(
            "番剧暂不支持点踩操作",
            resolveBangumiUnsupportedOverlayActionMessage(BangumiOverlayUnsupportedAction.DISLIKE)
        )
        assertEquals(
            "番剧暂不支持投币操作",
            resolveBangumiUnsupportedOverlayActionMessage(BangumiOverlayUnsupportedAction.COIN)
        )
    }

    @Test
    fun resolveBangumiOverlayShareTitle_prefersEpisodeSubtitle() {
        val shareTitle = resolveBangumiOverlayShareTitle(
            title = "名侦探柯南",
            subtitle = "第1话 云霄飞车杀人事件"
        )

        assertEquals("名侦探柯南 第1话 云霄飞车杀人事件", shareTitle)
    }

    @Test
    fun shouldShowBangumiOverlayDislikeAction_isAlwaysFalse() {
        assertFalse(shouldShowBangumiOverlayDislikeAction())
    }

    @Test
    fun updateBangumiSuccessInteractionState_replacesInteractionFlags() {
        val updatedState = updateBangumiSuccessInteractionState(
            state = sampleSuccessState(isLiked = false, coinCount = 0),
            isLiked = true,
            coinCount = 3
        )

        assertTrue(updatedState.isLiked)
        assertEquals(2, updatedState.coinCount)
    }

    @Test
    fun applyBangumiCoinResult_marksEpisodeCoinedAndLikedWhenRequested() {
        val updatedState = applyBangumiCoinResult(
            state = sampleSuccessState(isLiked = false, coinCount = 0),
            coinDelta = 1,
            alsoLike = true
        )

        assertTrue(updatedState.isLiked)
        assertEquals(1, updatedState.coinCount)
    }

    private fun sampleSuccessState(
        isLiked: Boolean,
        coinCount: Int
    ): BangumiPlayerState.Success {
        return BangumiPlayerState.Success(
            seasonDetail = BangumiDetail(
                seasonId = 2026L,
                title = "名侦探柯南"
            ),
            currentEpisode = BangumiEpisode(
                id = 101L,
                aid = 10001L,
                cid = 1001L,
                title = "第1话",
                longTitle = "云霄飞车杀人事件"
            ),
            currentEpisodeIndex = 0,
            playUrl = "https://example.com/video.mpd",
            audioUrl = "https://example.com/audio.m4a",
            quality = 80,
            acceptQuality = listOf(80, 64),
            acceptDescription = listOf("1080P", "720P"),
            isLiked = isLiked,
            coinCount = coinCount
        )
    }
}
