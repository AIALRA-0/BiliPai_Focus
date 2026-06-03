package com.android.purebilibili.feature.dynamic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import androidx.compose.ui.unit.dp

class DynamicLayoutPolicyTest {

    @Test
    fun `dynamic feed matches pili plus single-column content width`() {
        assertEquals(480.dp, resolveDynamicFeedMaxWidth())
    }

    @Test
    fun `dynamic timeline expands into dense adaptive columns on wide screens`() {
        assertEquals(1840.dp, resolveDynamicTimelineMaxWidth())
        assertEquals(360.dp, resolveDynamicTimelineMinColumnWidth())
        assertEquals(18.dp, resolveDynamicTimelineHorizontalSpacing())
        assertEquals(10.dp, resolveDynamicTimelineVerticalSpacing())
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
    fun `dynamic top tab row uses bottom bar liquid glass proportions`() {
        val spec = resolveDynamicTopBarLiquidTabSpec()

        assertEquals(0, spec.topPaddingDp)
        assertEquals(0, spec.bottomPaddingDp)
        assertEquals(52, spec.heightDp)
        assertEquals(3, spec.indicatorHeightDp)
        assertEquals(14, spec.labelFontSizeSp)
    }

    @Test
    fun `dynamic sidebar return header aligns with top tab row height`() {
        assertEquals(52, resolveDynamicTopBarHeightDp())
        assertEquals(resolveDynamicTopBarHeightDp(), resolveDynamicSidebarReturnHeaderHeightDp())
    }

    @Test
    fun `dynamic sidebar divider starts below top chrome`() {
        assertEquals(
            76.dp,
            resolveDynamicSidebarDividerTopOffset(topPadding = 24.dp)
        )
    }

    @Test
    fun `dynamic sidebar trims width without crowding avatar affordances`() {
        assertEquals(68.dp, resolveDynamicSidebarWidth(isExpanded = true))
        assertEquals(60.dp, resolveDynamicSidebarWidth(isExpanded = false))
    }

    @Test
    fun `dynamic user live badge uses themed text label`() {
        assertEquals(true, shouldShowDynamicUserLiveBadge(isLive = true))
        assertEquals(false, shouldShowDynamicUserLiveBadge(isLive = false))
        assertEquals("直播", resolveDynamicUserLiveBadgeLabel())
    }

    @Test
    fun `dynamic share and comment actions keep readable text labels`() {
        assertEquals("转发", resolveDynamicActionButtonText(label = "转发", count = 0))
        assertEquals("评论", resolveDynamicActionButtonText(label = "评论", count = 0))
        assertEquals("评论 9", resolveDynamicActionButtonText(label = "评论", count = 9))
        assertEquals("评论 1.2k", resolveDynamicActionButtonText(label = "评论", count = 1200))
        assertEquals("85", resolveDynamicActionButtonText(label = "点赞", count = 85))
        assertNull(resolveDynamicActionButtonText(label = "点赞", count = 0))
    }

    @Test
    fun `dynamic action row uses equal slots so like button is not starved by earlier labels`() {
        assertEquals(1f, resolveDynamicActionButtonSlotWeight(), 0f)
        assertEquals(8.dp, resolveDynamicActionButtonSpacing())
    }

    @Test
    fun `dynamic action text keeps comment count when slot is narrow`() {
        assertEquals(
            "评论 1.2k",
            resolveDynamicActionButtonText(label = "评论", count = 1200, slotWidthDp = 96)
        )
        assertEquals(
            "转发",
            resolveDynamicActionButtonText(label = "转发", count = 34000, slotWidthDp = 96)
        )
        assertEquals(
            "评论 1.2k",
            resolveDynamicActionButtonText(label = "评论", count = 1200, slotWidthDp = 140)
        )
    }
}
