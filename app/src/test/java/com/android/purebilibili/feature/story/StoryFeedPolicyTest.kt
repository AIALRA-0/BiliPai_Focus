package com.android.purebilibili.feature.story

import com.android.purebilibili.data.model.response.StoryItem
import com.android.purebilibili.data.model.response.StoryOwner
import com.android.purebilibili.data.model.response.StoryPlayerArgs
import com.android.purebilibili.data.model.response.StoryStat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class StoryFeedPolicyTest {

    @Test
    fun resolveNextStoryFeedAid_keepsPreviousCursorWhenTailAidIsMissing() {
        val nextAid = resolveNextStoryFeedAid(
            previousAid = 100L,
            items = listOf(
                storyItem(id = 1L, aid = 101L),
                storyItem(id = 2L, aid = 0L)
            )
        )

        assertEquals(101L, nextAid)
    }

    @Test
    fun mergeStoryFeedItems_skipsItemsAlreadySeenByAidBvidOrId() {
        val merged = mergeStoryFeedItems(
            existingItems = listOf(
                storyItem(id = 1L, aid = 100L, bvid = "BV_EXISTING"),
                storyItem(id = 2L, aid = 101L, bvid = "")
            ),
            newItems = listOf(
                storyItem(id = 3L, aid = 100L, bvid = "BV_DUP_AID"),
                storyItem(id = 4L, aid = 102L, bvid = "BV_EXISTING"),
                storyItem(id = 2L, aid = 0L, bvid = ""),
                storyItem(id = 5L, aid = 103L, bvid = "BV_FRESH")
            )
        )

        assertEquals(listOf(1L, 2L, 5L), merged.map { it.id })
    }

    @Test
    fun buildStoryPortraitFeed_usesFirstPlayableStoryAsInitialVideo() {
        val feed = buildStoryPortraitFeed(
            listOf(
                storyItem(id = 1L, aid = 0L, cid = 0L, bvid = ""),
                storyItem(id = 2L, aid = 200L, cid = 1200L, bvid = "")
            )
        )

        val portraitFeed = assertNotNull(feed)
        assertEquals("av200", portraitFeed.initialInfo.bvid)
        assertEquals(200L, portraitFeed.initialInfo.aid)
        assertEquals(1200L, portraitFeed.initialInfo.cid)
        assertEquals(1200L, portraitFeed.initialInfo.pages.firstOrNull()?.cid)
    }

    @Test
    fun buildStoryPortraitFeed_mapsRemainingPlayableStoriesAsRecommendations() {
        val feed = buildStoryPortraitFeed(
            listOf(
                storyItem(id = 1L, aid = 100L, cid = 1100L, bvid = "BV_FIRST"),
                storyItem(id = 2L, aid = 101L, cid = 1101L, bvid = "BV_SECOND"),
                storyItem(id = 3L, aid = 0L, cid = 0L, bvid = ""),
                storyItem(id = 4L, aid = 102L, cid = 1102L, bvid = "")
            )
        )

        val portraitFeed = assertNotNull(feed)
        assertEquals("BV_FIRST", portraitFeed.initialInfo.bvid)
        assertEquals(listOf("BV_SECOND", "av102"), portraitFeed.recommendations.map { it.bvid })
        assertEquals(listOf(1101L, 1102L), portraitFeed.recommendations.map { it.cid })
    }

    private fun storyItem(
        id: Long,
        aid: Long,
        bvid: String = "BV_$aid",
        cid: Long = aid + 1000L
    ): StoryItem {
        return StoryItem(
            id = id,
            title = "story $id",
            cover = "https://example.com/$id.jpg",
            duration = 60,
            owner = StoryOwner(mid = 10L + id, name = "up $id", face = "https://example.com/$id.png"),
            stat = StoryStat(view = 100, like = 20, reply = 3, favorite = 5, coin = 2, share = 1, danmaku = 4),
            playerArgs = StoryPlayerArgs(
                aid = aid,
                cid = cid,
                bvid = bvid
            )
        )
    }
}
