package com.android.purebilibili.data.model.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LikedVideosModelsTest {
    @Test
    fun likedVideo_mapsStandardArchiveFieldsToVideoItem() {
        val video = LikedVideoData(
            aid = 123L,
            bvid = "BV1TEST",
            cid = 456L,
            title = "点赞视频",
            pic = "cover",
            duration = 90,
            pubdate = 1_700_000_000L,
            tid = 21,
            tname = "日常",
            owner = Owner(mid = 7L, name = "UP主"),
            stat = Stat(view = 100, like = 8),
            dimension = Dimension(width = 1080, height = 1920)
        ).toVideoItem()

        assertEquals(123L, video.aid)
        assertEquals("BV1TEST", video.bvid)
        assertEquals(456L, video.cid)
        assertEquals("UP主", video.owner.name)
        assertEquals(8, video.stat.like)
        assertTrue(video.isVertical)
    }

    @Test
    fun aggregateArchiveItem_derivesBvidFromParamAid() {
        val aid = 379321839L
        val expectedBvid = com.android.purebilibili.core.util.IdUtils.av2bv(aid)
        val item = SpaceAggregateArchiveItem(
            param = aid.toString(),
            title = "元气少女缘结神◎",
            cover = "https://cover.jpg",
            author = "哔哩哔哩番剧",
            play = 4026012,
            danmaku = 58964,
            duration = 1456,
            ctime = 1421087700L
        )

        val resolvedAid = if (item.aid > 0L) item.aid else item.param.toLongOrNull() ?: 0L
        val resolvedBvid = item.bvid.ifBlank {
            if (item.param.startsWith("BV", ignoreCase = true)) item.param
            else if (resolvedAid > 0L) com.android.purebilibili.core.util.IdUtils.av2bv(resolvedAid)
            else ""
        }

        assertEquals(379321839L, resolvedAid)
        assertEquals(expectedBvid, resolvedBvid)
        assertEquals("BV1bf4y1M7EF", resolvedBvid)
    }
}
