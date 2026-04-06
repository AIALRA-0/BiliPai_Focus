package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.SpaceAggregateArchive
import com.android.purebilibili.data.model.response.SpaceAggregateArchiveItem
import com.android.purebilibili.data.model.response.SpaceAggregateArticleSection
import com.android.purebilibili.data.model.response.SpaceAggregateAudioSection
import com.android.purebilibili.data.model.response.SpaceAggregateCard
import com.android.purebilibili.data.model.response.SpaceAggregateData
import com.android.purebilibili.data.model.response.SpaceAggregateImages
import com.android.purebilibili.data.model.response.SpaceAggregateLevelInfo
import com.android.purebilibili.data.model.response.SpaceAggregateLikes
import com.android.purebilibili.data.model.response.SpaceAggregateRelation
import com.android.purebilibili.data.model.response.SpaceArticleAuthor
import com.android.purebilibili.data.model.response.SpaceArticleCategory
import com.android.purebilibili.data.model.response.SpaceArticleItem
import com.android.purebilibili.data.model.response.SpaceAudioItem
import com.android.purebilibili.data.model.response.SpaceLiveRoom
import com.android.purebilibili.data.model.response.SpaceOfficial
import com.android.purebilibili.data.model.response.SpaceVip
import com.android.purebilibili.data.model.response.SpaceVipLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpaceAggregatedModelMappingTest {

    @Test
    fun `resolveSpaceInitialSeedFromAggregate maps core header and seeded content`() {
        val seed = resolveSpaceInitialSeedFromAggregate(
            data = SpaceAggregateData(
                defaultTab = "video",
                card = SpaceAggregateCard(
                    mid = "42",
                    name = "不不不加点糖",
                    face = "https://i0.hdslb.com/bfs/face/demo.jpg",
                    sign = "这里是签名",
                    sex = "女",
                    attention = 28,
                    fans = 46,
                    officialVerify = SpaceOfficial(type = 0, title = "认证", desc = "官方认证"),
                    vip = SpaceVip(status = 1, type = 2, label = SpaceVipLabel(text = "年度大会员")),
                    levelInfo = SpaceAggregateLevelInfo(currentLevel = 5),
                    likes = SpaceAggregateLikes(likeNum = 422L),
                    relation = SpaceAggregateRelation(status = 1, isFollow = 1)
                ),
                images = SpaceAggregateImages(
                    imgUrl = "https://i0.hdslb.com/bfs/space/cover.jpg",
                    nightImgUrl = "https://i0.hdslb.com/bfs/space/night.jpg"
                ),
                live = SpaceLiveRoom(
                    roomStatus = 1,
                    liveStatus = 1,
                    title = "直播中",
                    url = "https://live.bilibili.com/6",
                    roomId = 6L
                ),
                archive = SpaceAggregateArchive(
                    count = 2,
                    item = listOf(
                        SpaceAggregateArchiveItem(
                            aid = 1001,
                            bvid = "BV1xx411c7mD",
                            title = "第一条视频",
                            cover = "https://i0.hdslb.com/bfs/archive/a.jpg",
                            author = "UP",
                            length = "03:21",
                            play = 114,
                            reply = 12,
                            ctime = 1_710_000_000,
                            tname = "动画"
                        ),
                        SpaceAggregateArchiveItem(
                            aid = 1002,
                            bvid = "BV1xx411c7mE",
                            title = "第二条视频",
                            cover = "https://i0.hdslb.com/bfs/archive/b.jpg",
                            author = "UP",
                            length = "05:43",
                            play = 514,
                            reply = 21,
                            ctime = 1_710_000_100,
                            tname = "动画"
                        )
                    )
                ),
                article = SpaceAggregateArticleSection(
                    count = 1,
                    item = listOf(
                        SpaceArticleItem(
                            id = 7L,
                            title = "一篇专栏",
                            summary = "摘要",
                            image_urls = listOf("https://i0.hdslb.com/bfs/article/a.jpg"),
                            category = SpaceArticleCategory(name = "专栏"),
                            author = SpaceArticleAuthor(name = "UP")
                        )
                    )
                ),
                audios = SpaceAggregateAudioSection(
                    count = 1,
                    item = listOf(
                        SpaceAudioItem(
                            id = 8L,
                            title = "一段音频",
                            cover = "https://i0.hdslb.com/bfs/audio/a.jpg",
                            bvid = "BV1audio",
                            duration = 189
                        )
                    )
                )
            )
        )

        assertNotNull(seed)
        assertEquals(42L, seed.userInfo.mid)
        assertEquals("不不不加点糖", seed.userInfo.name)
        assertEquals("https://i0.hdslb.com/bfs/space/cover.jpg", seed.userInfo.topPhoto)
        assertTrue(seed.userInfo.isFollowed)
        assertEquals(28, seed.relationStat?.following)
        assertEquals(46, seed.relationStat?.follower)
        assertEquals(422L, seed.upStat?.likes)
        assertEquals(listOf("BV1xx411c7mD", "BV1xx411c7mE"), seed.videos.map { it.bvid })
        assertEquals(2, seed.totalVideos)
        assertEquals("一篇专栏", seed.articles.single().title)
        assertEquals("一段音频", seed.audios.single().title)
        assertEquals(SpaceMainTab.CONTRIBUTION, seed.defaultMainTab)
        assertEquals(SpaceSubTab.VIDEO, seed.defaultSubTab)
    }

    @Test
    fun `resolveSpaceInitialSeedFromAggregate falls back to provided top photos`() {
        val seed = resolveSpaceInitialSeedFromAggregate(
            data = SpaceAggregateData(
                card = SpaceAggregateCard(
                    mid = "24",
                    name = "fallback",
                    face = "face",
                    levelInfo = SpaceAggregateLevelInfo(currentLevel = 3)
                ),
                images = SpaceAggregateImages(imgUrl = " ", nightImgUrl = "")
            ),
            cardLargePhoto = "https://i0.hdslb.com/bfs/space/card-large.jpg",
            cardSmallPhoto = "https://i0.hdslb.com/bfs/space/card-small.jpg"
        )

        assertEquals("https://i0.hdslb.com/bfs/space/card-large.jpg", seed?.userInfo?.topPhoto)
    }

    @Test
    fun `resolveSpaceInitialSeedFromAggregate returns null when card identity is missing`() {
        val seed = resolveSpaceInitialSeedFromAggregate(
            data = SpaceAggregateData(
                card = SpaceAggregateCard(mid = "", name = "", face = "")
            )
        )

        assertNull(seed)
    }
}
