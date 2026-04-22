package com.android.purebilibili.feature.dynamic

import org.junit.Assert.assertEquals
import org.junit.Test
import androidx.compose.ui.unit.dp

class DynamicLayoutPolicyTest {

    @Test
    fun `dynamic feed matches pili plus single-column content width`() {
        assertEquals(480.dp, resolveDynamicFeedMaxWidth())
    }

    @Test
    fun `dynamic video card keeps vertical layout on wide content`() {
        assertEquals(
            DynamicVideoCardLayoutMode.VERTICAL,
            resolveDynamicVideoCardLayoutMode(containerWidthDp = 620)
        )
    }

    @Test
    fun `dynamic video card keeps vertical layout on compact content`() {
        assertEquals(
            DynamicVideoCardLayoutMode.VERTICAL,
            resolveDynamicVideoCardLayoutMode(containerWidthDp = 540)
        )
    }

    @Test
    fun `dynamic cards use flat list spacing like pili plus`() {
        assertEquals(0.dp, resolveDynamicCardOuterPadding())
        assertEquals(12.dp, resolveDynamicCardContentPadding())
    }

    @Test
    fun `dynamic top areas tighten user list and tab spacing`() {
        assertEquals(10.dp, resolveDynamicHorizontalUserListHorizontalPadding())
        assertEquals(10.dp, resolveDynamicHorizontalUserListSpacing())
        assertEquals(14.dp, resolveDynamicTopBarHorizontalPadding())
    }

    @Test
    fun `dynamic sidebar trims width without crowding avatar affordances`() {
        assertEquals(68.dp, resolveDynamicSidebarWidth(isExpanded = true))
        assertEquals(60.dp, resolveDynamicSidebarWidth(isExpanded = false))
    }

    @Test
    fun `dynamic user live badge uses compact themed pill sizing`() {
        assertEquals(true, shouldShowDynamicUserLiveBadge(isLive = true))
        assertEquals(false, shouldShowDynamicUserLiveBadge(isLive = false))
        assertEquals("直播", resolveDynamicUserLiveBadgeLabel())
        assertEquals(16.dp, resolveDynamicUserLiveBadgeHeight())
        assertEquals(24.dp, resolveDynamicUserLiveBadgeMinWidth())
        assertEquals(8.dp, resolveDynamicUserLiveBadgeReservedSpace())
    }
}
