package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.data.model.response.BangumiType
import com.android.purebilibili.data.model.response.BangumiItem
import com.android.purebilibili.data.model.response.BangumiSearchItem
import com.android.purebilibili.data.model.response.FollowBangumiItem
import com.android.purebilibili.data.model.response.TimelineEpisode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MyFollowPolicyTest {

    @Test
    fun `anime and guochuang should map to bangumi follow type`() {
        assertEquals(MY_FOLLOW_TYPE_BANGUMI, defaultMyFollowTypeForSeasonType(BangumiType.ANIME.value))
        assertEquals(MY_FOLLOW_TYPE_BANGUMI, defaultMyFollowTypeForSeasonType(BangumiType.GUOCHUANG.value))
    }

    @Test
    fun `movie tv documentary and variety should map to cinema follow type`() {
        assertEquals(MY_FOLLOW_TYPE_CINEMA, defaultMyFollowTypeForSeasonType(BangumiType.MOVIE.value))
        assertEquals(MY_FOLLOW_TYPE_CINEMA, defaultMyFollowTypeForSeasonType(BangumiType.TV_SHOW.value))
        assertEquals(MY_FOLLOW_TYPE_CINEMA, defaultMyFollowTypeForSeasonType(BangumiType.DOCUMENTARY.value))
        assertEquals(MY_FOLLOW_TYPE_CINEMA, defaultMyFollowTypeForSeasonType(BangumiType.VARIETY.value))
    }

    @Test
    fun `explicit request type should override current type`() {
        assertEquals(MY_FOLLOW_TYPE_BANGUMI, resolveMyFollowRequestType(1, currentType = 2))
        assertEquals(MY_FOLLOW_TYPE_CINEMA, resolveMyFollowRequestType(2, currentType = 1))
    }

    @Test
    fun `null request type should keep current type`() {
        assertEquals(MY_FOLLOW_TYPE_BANGUMI, resolveMyFollowRequestType(null, currentType = 1))
        assertEquals(MY_FOLLOW_TYPE_CINEMA, resolveMyFollowRequestType(null, currentType = 2))
    }

    @Test
    fun `follow item lazy keys stay unique when api returns duplicate zero season id`() {
        val first = resolveMyFollowItemLazyKey(
            index = 0,
            item = FollowBangumiItem(seasonId = 0L, mediaId = 0L, firstEp = 0L, title = "条目A")
        )
        val second = resolveMyFollowItemLazyKey(
            index = 1,
            item = FollowBangumiItem(seasonId = 0L, mediaId = 0L, firstEp = 0L, title = "条目B")
        )

        assertNotEquals(first, second)
    }

    @Test
    fun `follow item lazy key keeps stable business id when season id exists`() {
        assertEquals(
            "my_follow_season_123_0",
            resolveMyFollowItemLazyKey(
                index = 0,
                item = FollowBangumiItem(seasonId = 123L, mediaId = 456L)
            )
        )
    }

    @Test
    fun `index item lazy keys stay unique when api returns duplicate zero season id`() {
        val first = resolveBangumiIndexItemLazyKey(
            index = 0,
            item = BangumiItem(seasonId = 0L, mediaId = 0L, title = "条目A")
        )
        val second = resolveBangumiIndexItemLazyKey(
            index = 1,
            item = BangumiItem(seasonId = 0L, mediaId = 0L, title = "条目B")
        )

        assertNotEquals(first, second)
    }

    @Test
    fun `index item lazy key keeps stable business id when season id exists`() {
        assertEquals(
            "bangumi_index_season_123_0",
            resolveBangumiIndexItemLazyKey(
                index = 0,
                item = BangumiItem(seasonId = 123L, mediaId = 456L)
            )
        )
    }

    @Test
    fun `search item lazy keys stay unique when api returns duplicate zero season id`() {
        val first = resolveBangumiSearchItemLazyKey(
            index = 0,
            item = BangumiSearchItem(seasonId = 0L, mediaId = 0L, title = "搜索A")
        )
        val second = resolveBangumiSearchItemLazyKey(
            index = 1,
            item = BangumiSearchItem(seasonId = 0L, mediaId = 0L, title = "搜索B")
        )

        assertNotEquals(first, second)
    }

    @Test
    fun `search item lazy key keeps stable business id when season id exists`() {
        assertEquals(
            "bangumi_search_season_123_0",
            resolveBangumiSearchItemLazyKey(
                index = 0,
                item = BangumiSearchItem(seasonId = 123L, mediaId = 456L)
            )
        )
    }

    @Test
    fun `timeline episode lazy keys stay unique when api returns duplicate zero episode id`() {
        val first = resolveTimelineEpisodeLazyKey(
            index = 0,
            episode = TimelineEpisode(episodeId = 0L, seasonId = 0L, title = "时间表A")
        )
        val second = resolveTimelineEpisodeLazyKey(
            index = 1,
            episode = TimelineEpisode(episodeId = 0L, seasonId = 0L, title = "时间表B")
        )

        assertNotEquals(first, second)
    }

    @Test
    fun `timeline episode lazy key keeps stable business id when episode id exists`() {
        assertEquals(
            "bangumi_timeline_episode_456_0",
            resolveTimelineEpisodeLazyKey(
                index = 0,
                episode = TimelineEpisode(episodeId = 456L, seasonId = 123L)
            )
        )
    }
}
