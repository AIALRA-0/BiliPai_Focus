package com.android.purebilibili.feature.video.screen

import java.io.File
import com.android.purebilibili.core.store.TabletSecondaryDefaultTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TabletVideoLayoutPolicyTest {

    @Test
    fun secondaryPaneDefaultFindsTheRequestedTabInEitherOrder() {
        assertEquals(
            0,
            resolveTabletSecondaryDefaultTabIndex(
                tabs = listOf(TabletSecondaryTab.RELATED, TabletSecondaryTab.COMMENTS),
                preferRelated = true,
            ),
        )
        assertEquals(
            1,
            resolveTabletSecondaryDefaultTabIndex(
                tabs = listOf(TabletSecondaryTab.COMMENTS, TabletSecondaryTab.RELATED),
                preferRelated = true,
            ),
        )
        assertEquals(
            1,
            resolveTabletSecondaryDefaultTabIndex(
                tabs = listOf(TabletSecondaryTab.RELATED, TabletSecondaryTab.COMMENTS),
                preferRelated = false,
            ),
        )
        assertEquals(1, resolveTabletCinemaDefaultTab(TabletSecondaryDefaultTab.RELATED))
        assertEquals(0, resolveTabletCinemaDefaultTab(TabletSecondaryDefaultTab.COMMENTS))
        assertEquals(
            1,
            resolveTabletCommentTabIndex(
                listOf(TabletSecondaryTab.RELATED, TabletSecondaryTab.COMMENTS),
            ),
        )
        assertEquals(TabletSecondaryDefaultTab.RELATED, TabletSecondaryDefaultTab.fromValue(99))
        assertTrue(shouldShowTabletSecondaryDanmakuActions())
    }

    @Test
    fun tabletSecondaryTabsOptIntoMiuixNonGlassEqualLabelWidths() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/video/screen/TabletVideoLayout.kt"
        ).readText()
        val tabRow = source
            .substringAfter("internal fun TabletSecondaryLiquidTabRow(")
            .substringBefore("/**\n * 🖥️ 平板端视频详情页布局")

        assertTrue(tabRow.contains("equalizeMiuixNonGlassItemWidths = false"))
        assertTrue(tabRow.contains("allowNativeLabelOverflow = true"))
        assertFalse(tabRow.contains("108.dp"))
    }

    @Test
    fun secondaryPaneHostsDanmakuSendAndToggle() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/video/screen/TabletVideoLayout.kt"
        ).readText()

        assertTrue(source.contains("TabletSecondaryDanmakuActions("))
        assertTrue(source.contains("onDanmakuInputClick = playbackActions.showDanmakuSendDialog"))
        assertTrue(source.contains("fun TabletSecondaryDanmakuActions("))
        assertTrue(source.contains("showPaneModeControls: Boolean = true"))
        assertTrue(source.contains("applyStatusBarPadding: Boolean = true"))
        assertTrue(source.contains("includeRelatedTab: Boolean = true"))
        assertTrue(source.contains("relatedTabFirst: Boolean = false"))
        assertTrue(source.contains("showRelatedVideos: Boolean = true"))
        assertTrue(source.contains("fixedTab == null && tabs.size > 1"))
        assertTrue(source.contains("text = if (isExpanded) \"收起\" else \"展开\""))
        assertTrue(source.contains("padding(horizontal = 16.dp, vertical = 12.dp)"))
        assertTrue(source.contains("NativeDanmakuToggleButton("))
        assertTrue(source.contains("shouldShowTabletSecondaryDanmakuActions()"))
        assertTrue(source.contains("trailingContent = ownerTrailingContent"))
    }

    @Test
    fun expandedTablet_prioritizesPrimaryPaneWidth() {
        val policy = resolveTabletVideoLayoutPolicy(widthDp = 1280)

        assertEquals(0.72f, policy.primaryRatio)
        assertEquals(1080, policy.playerMaxWidthDp)
        assertEquals(1000, policy.infoMaxWidthDp)
    }

    @Test
    fun ultraWideTablet_balancesPaneRatioAndPlayerCap() {
        val policy = resolveTabletVideoLayoutPolicy(widthDp = 1920)

        assertEquals(0.66f, policy.primaryRatio)
        assertTrue(policy.playerMaxWidthDp >= 1240)
        assertTrue(policy.infoMaxWidthDp >= 1160)
    }

    @Test
    fun ultraWidePolicy_keepsLargePrimaryPane() {
        val policy = resolveTabletVideoLayoutPolicy(widthDp = 1920)

        assertEquals(0.66f, policy.primaryRatio)
        assertEquals(1240, policy.playerMaxWidthDp)
        assertEquals(1160, policy.infoMaxWidthDp)
    }
}
