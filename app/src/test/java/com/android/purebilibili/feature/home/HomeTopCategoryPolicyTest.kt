package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.FocusSettings
import com.android.purebilibili.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeTopCategoryPolicyTest {

    @Test
    fun `top categories should not contain anime`() {
        assertFalse(resolveHomeTopCategories().contains(HomeCategory.ANIME))
    }

    @Test
    fun `top categories keep stable primary order`() {
        assertEquals(
            listOf(
                HomeCategory.RECOMMEND,
                HomeCategory.FOLLOW,
                HomeCategory.POPULAR,
                HomeCategory.LIVE,
                HomeCategory.GAME
            ),
            resolveHomeTopCategories()
        )
    }

    @Test
    fun `top categories should keep compact count for header readability`() {
        assertEquals(5, resolveHomeTopCategories().size)
    }

    @Test
    fun `tab index and category mapping should be consistent`() {
        val categories = resolveHomeTopCategories()
        categories.forEachIndexed { index, category ->
            assertEquals(index, resolveHomeTopTabIndex(category))
            assertEquals(category, resolveHomeCategoryForTopTab(index))
        }
    }

    @Test
    fun `custom order and visibility should be applied without forcing recommend`() {
        val categories = resolveHomeTopCategories(
            customOrderIds = listOf("LIVE", "TECH", "RECOMMEND", "FOLLOW"),
            visibleIds = setOf("LIVE", "TECH", "FOLLOW")
        )

        assertEquals(
            listOf(
                HomeCategory.LIVE,
                HomeCategory.TECH,
                HomeCategory.FOLLOW
            ),
            categories
        )
    }

    @Test
    fun `invalid custom ids should fallback to default set`() {
        val categories = resolveHomeTopCategories(
            customOrderIds = listOf("UNKNOWN", "INVALID"),
            visibleIds = setOf("???")
        )

        assertTrue(categories.contains(HomeCategory.RECOMMEND))
        assertEquals(resolveHomeTopCategories(), categories)
    }

    @Test
    fun `safe category resolve should not crash on out of range index`() {
        val categories = listOf(
            HomeCategory.RECOMMEND,
            HomeCategory.FOLLOW,
            HomeCategory.POPULAR
        )

        assertEquals(HomeCategory.FOLLOW, resolveHomeTopCategoryOrNull(categories, 1))
        assertEquals(null, resolveHomeTopCategoryOrNull(categories, 5))
    }

    @Test
    fun `safe key resolve should fallback to index when out of range`() {
        val categories = listOf(
            HomeCategory.RECOMMEND,
            HomeCategory.FOLLOW
        )

        assertEquals(HomeCategory.RECOMMEND.ordinal, resolveHomeTopCategoryKey(categories, 0))
        assertEquals(5, resolveHomeTopCategoryKey(categories, 5))
    }

    @Test
    fun `focus filters configured home tabs and keeps follow by default`() {
        val categories = applyFocusHomeTopCategories(
            categories = listOf(
                HomeCategory.RECOMMEND,
                HomeCategory.FOLLOW,
                HomeCategory.POPULAR,
                HomeCategory.LIVE,
                HomeCategory.GAME
            ),
            settings = FocusSettings()
        )

        assertEquals(listOf(HomeCategory.FOLLOW), categories)
    }

    @Test
    fun `focus fallback keeps home non empty when everything else is hidden`() {
        val categories = applyFocusHomeTopCategories(
            categories = listOf(HomeCategory.RECOMMEND, HomeCategory.POPULAR, HomeCategory.LIVE, HomeCategory.GAME),
            settings = FocusSettings(),
            fallbackCategory = HomeCategory.FOLLOW
        )

        assertEquals(listOf(HomeCategory.FOLLOW), categories)
    }

    @Test
    fun `focus can selectively restore recommend and popular`() {
        val categories = applyFocusHomeTopCategories(
            categories = listOf(
                HomeCategory.RECOMMEND,
                HomeCategory.FOLLOW,
                HomeCategory.POPULAR,
                HomeCategory.LIVE
            ),
            settings = FocusSettings(
                showHomeRecommendTab = true,
                showHomePopularTab = true
            )
        )

        assertEquals(
            listOf(
                HomeCategory.RECOMMEND,
                HomeCategory.FOLLOW,
                HomeCategory.POPULAR
            ),
            categories
        )
    }

    @Test
    fun `home top categories should map to localized string resources`() {
        assertEquals(R.string.home_category_recommend, resolveHomeCategoryLabelRes(HomeCategory.RECOMMEND))
        assertEquals(R.string.home_category_follow, resolveHomeCategoryLabelRes(HomeCategory.FOLLOW))
        assertEquals(R.string.home_category_popular, resolveHomeCategoryLabelRes(HomeCategory.POPULAR))
        assertEquals(R.string.home_category_live, resolveHomeCategoryLabelRes(HomeCategory.LIVE))
        assertEquals(R.string.home_category_game, resolveHomeCategoryLabelRes(HomeCategory.GAME))
    }
}

