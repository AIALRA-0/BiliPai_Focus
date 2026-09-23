package com.android.purebilibili.feature.video.ui.pager

import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.UgcSeason
import com.android.purebilibili.data.model.response.ViewInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PortraitCollectionContextPolicyTest {

    @Test
    fun collectionSelection_promotesLightweightItemAndKeepsSeason() {
        val season = UgcSeason(id = 42L, title = "合集")

        val item = buildPortraitCollectionPageItem(
            existing = RelatedVideo(bvid = "BV-target", cid = 7L, title = "第七集"),
            targetBvid = "BV-target",
            targetCid = 7L,
            collectionContext = season,
        )

        val info = item as ViewInfo
        assertEquals("第七集", info.title)
        assertEquals(7L, info.cid)
        assertEquals(42L, info.ugc_season?.id)
    }

    @Test
    fun detailInfo_usesLoadedCollectionContextForLightweightPagerItem() {
        val season = UgcSeason(id = 42L, title = "合集")
        val loaded = ViewInfo(bvid = "BV-target", ugc_season = season)

        val resolved = resolvePortraitDetailInfo(
            targetBvid = "BV-target",
            sharedInfo = ViewInfo(bvid = "BV-target", title = "完整详情"),
            loadedPageInfo = loaded,
            fallbackInfo = ViewInfo(bvid = "BV-target"),
        )

        assertEquals("完整详情", resolved?.title)
        assertEquals(42L, resolved?.ugc_season?.id)
    }

    @Test
    fun detailInfo_ignoresLoadedContextFromAnotherPage() {
        val fallback = ViewInfo(bvid = "BV-target")

        val resolved = resolvePortraitDetailInfo(
            targetBvid = "BV-target",
            sharedInfo = null,
            loadedPageInfo = ViewInfo(
                bvid = "BV-other",
                ugc_season = UgcSeason(id = 42L),
            ),
            fallbackInfo = fallback,
        )

        assertEquals("BV-target", resolved?.bvid)
        assertNull(resolved?.ugc_season)
    }
}
