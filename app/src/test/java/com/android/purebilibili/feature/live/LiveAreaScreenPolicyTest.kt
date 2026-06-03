package com.android.purebilibili.feature.live

import com.android.purebilibili.data.model.response.LiveAreaChild
import com.android.purebilibili.data.model.response.LiveFavoriteTagEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveAreaScreenPolicyTest {

    @Test
    fun liveAreaChildrenPreferHotTagsWhileKeepingApiOrderWithinGroups() {
        val children = listOf(
            liveAreaChild(id = "1", name = "普通一", hotStatus = 0),
            liveAreaChild(id = "2", name = "热门一", hotStatus = 1),
            liveAreaChild(id = "3", name = "热门二", hotStatus = 1),
            liveAreaChild(id = "4", name = "普通二", hotStatus = 0)
        )

        val sorted = sortLiveAreaChildrenForDisplay(children)

        assertEquals(listOf("热门一", "热门二", "普通一", "普通二"), sorted.map { it.name })
    }

    @Test
    fun addingFavoriteKeepsMostRecentMetadataAndCapsTheList() {
        val current = (1..8).map {
            LiveFavoriteTagEntry(
                parentAreaId = 2,
                areaId = it,
                title = "旧标签$it",
                coverUrl = "old$it.png",
                parentTitle = "网游"
            )
        }

        val next = toggleLiveFavoriteTag(
            current = current,
            entry = LiveFavoriteTagEntry(
                parentAreaId = 3,
                areaId = 99,
                title = "王者荣耀",
                coverUrl = "new.png",
                parentTitle = "手游"
            ),
            maxSize = 8
        )

        assertEquals(8, next.size)
        assertEquals("王者荣耀", next.last().title)
        assertEquals("new.png", next.last().coverUrl)
        assertEquals("手游", next.last().parentTitle)
        assertEquals(listOf(2, 3, 4, 5, 6, 7, 8, 99), next.map { it.areaId })
    }

    @Test
    fun togglingExistingFavoriteRemovesIt() {
        val current = listOf(
            LiveFavoriteTagEntry(parentAreaId = 2, areaId = 86, title = "英雄联盟"),
            LiveFavoriteTagEntry(parentAreaId = 3, areaId = 35, title = "王者荣耀")
        )

        val next = toggleLiveFavoriteTag(
            current = current,
            entry = LiveFavoriteTagEntry(parentAreaId = 2, areaId = 86, title = "英雄联盟")
        )

        assertEquals(listOf(35), next.map { it.areaId })
    }

    @Test
    fun parentTabWidthFitsFourItemsAndKeepsOverflowScrollable() {
        assertEquals(
            100f,
            resolveLiveAreaParentTabItemWidthDp(containerWidthDp = 400f, itemCount = 4)
        )
        assertEquals(
            112f,
            resolveLiveAreaParentTabItemWidthDp(containerWidthDp = 400f, itemCount = 6)
        )
    }

    @Test
    fun parentTabLiquidIndicatorFollowsBottomBarLiquidGlassSetting() {
        assertTrue(shouldUseLiveAreaParentLiquidIndicator(itemCount = 4, bottomBarLiquidGlassEnabled = true))
        assertFalse(shouldUseLiveAreaParentLiquidIndicator(itemCount = 1, bottomBarLiquidGlassEnabled = true))
        assertFalse(shouldUseLiveAreaParentLiquidIndicator(itemCount = 4, bottomBarLiquidGlassEnabled = false))
    }

    private fun liveAreaChild(
        id: String,
        name: String,
        hotStatus: Int
    ): LiveAreaChild = LiveAreaChild(
        id = id,
        parent_id = "2",
        name = name,
        hot_status = hotStatus,
        pic = "$id.png",
        parent_name = "网游"
    )
}
