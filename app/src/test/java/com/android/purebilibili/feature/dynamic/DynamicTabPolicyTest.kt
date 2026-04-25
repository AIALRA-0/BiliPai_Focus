package com.android.purebilibili.feature.dynamic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicTabPolicyTest {

    @Test
    fun `visible dynamic tabs follow fixed app order and ignore unknown ids`() {
        val result = resolveDynamicVisibleTabs(
            setOf("article", "all", "unknown", "up")
        )

        assertEquals(
            listOf("all", "article", "up"),
            result.map { it.id }
        )
    }

    @Test
    fun `visible dynamic tabs fall back to all when all tabs are hidden`() {
        val result = resolveDynamicVisibleTabs(emptySet())

        assertEquals(listOf("all"), result.map { it.id })
    }

    @Test
    fun `selected dynamic tab falls back to first visible tab when current tab is hidden`() {
        val visibleTabs = resolveDynamicVisibleTabs(setOf("all", "article"))

        assertEquals(
            0,
            resolveDynamicSelectedTabWithinVisibleTabs(
                selectedTab = 4,
                visibleTabs = visibleTabs
            )
        )
        assertEquals(
            0,
            resolveDynamicSelectedVisibleTabIndex(
                selectedTab = 4,
                visibleTabs = visibleTabs
            )
        )
    }

    @Test
    fun `toggling dynamic tab visibility never hides the last remaining tab`() {
        val visibleOnlyAll = setOf("all")

        assertFalse(
            shouldAllowDynamicTabVisibilityToggleOff(
                currentVisibleTabIds = visibleOnlyAll,
                targetTabId = "all"
            )
        )
        assertEquals(
            visibleOnlyAll,
            resolveDynamicVisibleTabIdsAfterToggle(
                currentVisibleTabIds = visibleOnlyAll,
                targetTabId = "all"
            )
        )
    }

    @Test
    fun `toggling dynamic tab visibility adds and removes tabs from the visible set`() {
        val hiddenArticle = resolveDynamicVisibleTabIdsAfterToggle(
            currentVisibleTabIds = defaultDynamicTabVisibleIds,
            targetTabId = "article"
        )
        val restoredArticle = resolveDynamicVisibleTabIdsAfterToggle(
            currentVisibleTabIds = hiddenArticle,
            targetTabId = "article"
        )

        assertFalse("article" in hiddenArticle)
        assertTrue("article" in restoredArticle)
    }

    @Test
    fun `up tab visibility follows the current visible tab list`() {
        assertTrue(isDynamicUserTabVisible(resolveDynamicVisibleTabs(defaultDynamicTabVisibleIds)))
        assertFalse(isDynamicUserTabVisible(resolveDynamicVisibleTabs(setOf("all", "video", "pgc", "article"))))
    }
}
