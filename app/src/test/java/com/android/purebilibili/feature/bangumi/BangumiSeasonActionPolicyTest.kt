package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.data.model.response.AreaInfo
import com.android.purebilibili.data.model.response.BangumiDetail
import com.android.purebilibili.data.model.response.BangumiPayment
import com.android.purebilibili.data.model.response.BangumiPublish
import com.android.purebilibili.data.model.response.BangumiRights
import com.android.purebilibili.data.model.response.BangumiSection
import org.junit.Assert.assertEquals
import org.junit.Test

class BangumiSeasonActionPolicyTest {

    @Test
    fun `detail season id should take precedence for action`() {
        assertEquals(123L, resolveBangumiActionSeasonId(routeSeasonId = 456L, detailSeasonId = 123L))
    }

    @Test
    fun `route season id should be fallback when detail season id is missing`() {
        assertEquals(456L, resolveBangumiActionSeasonId(routeSeasonId = 456L, detailSeasonId = 0L))
    }

    @Test
    fun `detail meta chips should combine type publish area and styles`() {
        val chips = resolveBangumiDetailMetaChips(
            BangumiDetail(
                seasonTypeName = "番剧",
                publish = BangumiPublish(pubTimeShow = "2026年1月开播"),
                areas = listOf(AreaInfo(id = 2, name = "日本")),
                styles = listOf("原创", "热血")
            )
        )

        assertEquals(listOf("番剧", "2026年1月开播", "日本", "原创", "热血"), chips)
    }

    @Test
    fun `restriction labels should prefer concrete payment and rights hints`() {
        val labels = resolveBangumiRestrictionLabels(
            BangumiDetail(
                rights = BangumiRights(isPreview = 1, areaLimit = 1),
                payment = BangumiPayment(tip = "付费观看", price = "6")
            )
        )

        assertEquals(listOf("试看", "地区受限", "付费观看"), labels)
    }

    @Test
    fun `section title should fallback to stable label`() {
        assertEquals("扩展内容 2", resolveBangumiSectionTitle(BangumiSection(), fallbackIndex = 1))
    }
}
