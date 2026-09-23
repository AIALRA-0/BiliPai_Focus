package com.android.purebilibili.feature.dynamic

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicScreenStructureTest {

    @Test
    fun `dynamic staggered grid provides stable content types`() {
        val source = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt")
            .readText()
        val gridSource = source
            .substringAfter("FeedVerticalStaggeredGrid(")
            .substringBefore("@Composable\nprivate fun OldContentDivider")

        assertTrue(gridSource.contains("contentType = \"dynamic_empty_state\""))
        assertTrue(gridSource.contains("contentType = { \"dynamic_card\" }"))
        assertTrue(gridSource.contains("contentType = \"dynamic_old_content_divider\""))
        assertTrue(gridSource.contains("contentType = \"dynamic_loading_footer\""))
        assertTrue(gridSource.contains("contentType = \"dynamic_no_more_footer\""))
        assertTrue(source.contains("visibleItemsInfo.maxOfOrNull { it.index }"))
        assertTrue(source.contains("shouldLoadMoreDynamicFeed("))
    }

    @Test
    fun `dynamic screen supports tab reselect and up panel shortcuts`() {
        val source = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt")
            .readText()

        assertTrue(source.contains("resolveDynamicTabReselectAction("))
        assertTrue(source.contains("DynamicTabReselectAction.SCROLL_TO_TOP"))
        assertTrue(source.contains("pagerState.scrollToPage(page = visibleIndex)"))
        assertTrue(source.contains("resolveDynamicUpPanelUsers("))
        assertTrue(source.contains("isDynamicUpPanelAllShortcut(clickedUserId)"))
        assertTrue(source.contains("onTabSelected = onDynamicTabSelected"))
        assertTrue(source.contains("onArticleClick = onArticleClick"))
        assertTrue(source.contains("DynamicSelectedUserFeedHeader("))
        assertTrue(source.contains("DynamicUserContentFilter.entries"))
        assertTrue(source.contains("DynamicAdaptiveSegmentedControl("))
        assertTrue(source.contains("shouldAutoLoadMoreForUserContentFilter("))
        assertTrue(source.contains("已停止自动翻页"))
        assertTrue(source.contains(".background(AppSurfaceTokens.background())"))
        assertTrue(source.contains("animateScale = false"))
        assertTrue(!source.contains("TopReadabilityChrome("))
        assertTrue(!source.contains("dynamicFeedBackdrop"))
        assertTrue(source.contains("!(activePresentation.isLoading && activePresentation.items.isEmpty()) && it.isReady"))
        assertTrue(source.contains(".layerBackdrop(dynamicDockBackdrop)"))
        assertTrue(source.contains("hazeSourceCompat(state = dynamicTopBarHazeState)"))
        assertTrue(source.contains("rememberRecoverableHazeState("))
        val topBarSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicTopBar.kt"
        ).readText()
        assertTrue(!topBarSource.contains("forceLiquidChrome"))
        assertTrue(topBarSource.contains("liquidGlassEffectsEnabled = liquidGlassEnabled"))
        assertTrue(topBarSource.contains("if (liquidGlassEnabled)"))
        assertTrue(topBarSource.contains("Modifier.background(dockColor, dockShape)"))
        assertTrue(topBarSource.contains("miuixBackdrop = dockBackdrop"))
        assertTrue(topBarSource.contains("biliPaiFloatingDockShell("))
        assertTrue(topBarSource.contains("liquidGlassTuningOverride = liquidGlassTuning"))
        assertTrue(topBarSource.contains("homeSettings.liquidGlassProgress"))
        assertTrue(topBarSource.contains("homeSettings.liquidGlassAdvancedSettings"))
        assertTrue(topBarSource.contains("homeSettings.liquidGlassReadabilityMode"))
        assertTrue(!topBarSource.contains("resolveLiquidGlassTuning(progress = 0f)"))
        assertTrue(!topBarSource.contains("copy(backdropBlurRadius = 0f)"))
        assertTrue(topBarSource.contains("unifiedBlur("))
        assertTrue(!source.contains("activeListState?.isScrollInProgress == true ||"))
        assertTrue(topBarSource.contains("isScrollInProgressProvider = { false }"))
        val segmentedControlSource = File(
            "src/main/java/com/android/purebilibili/feature/home/components/BottomBarFloatingSegmentedControl.kt"
        ).readText()
        assertTrue(
            segmentedControlSource.contains("if (liquidGlassEnabled && miuixBackdrop != null)")
        )
        assertTrue(!source.contains("text = \"全\""))
        val sidebarSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicSidebar.kt"
        ).readText()
        assertTrue(!sidebarSource.contains("text = \"全\""))
        assertTrue(!sidebarSource.contains("isAllShortcut"))
        assertTrue(sidebarSource.contains("text = \"Live(\${liveUsers.size})\""))
        assertTrue(sidebarSource.contains("autoSize = TextAutoSize.StepBased("))
        assertTrue(sidebarSource.contains("horizontal = AppSpacingTokens.None"))
        assertTrue(sidebarSource.contains("textAlign = TextAlign.Center"))
        assertTrue(sidebarSource.contains("maxLines = 1"))
        assertTrue(sidebarSource.contains("softWrap = false"))
    }

    @Test
    fun `report dialog and additional cards use shared native components`() {
        val screenSource = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt")
            .readText()
        val cardSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCard.kt"
        ).readText()
        val sidebarSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicSidebar.kt"
        ).readText()

        assertTrue(screenSource.contains("AppListItem("))
        assertTrue(screenSource.contains("AppRadioButton("))
        assertTrue(cardSource.contains("AppContentCard("))
        assertTrue(sidebarSource.contains("AppTextButton("))
    }

    @Test
    fun `dynamic top action dock has a persistent collapse switch`() {
        val screenSource = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt")
            .readText()
        val topBarSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicTopBar.kt"
        ).readText()

        assertTrue(screenSource.contains("dynamicTopActionsCollapsed by rememberSaveable"))
        assertTrue(screenSource.contains("actionDockCollapsed = dynamicTopActionsCollapsed"))
        assertTrue(topBarSource.contains("visible = !actionDockCollapsed"))
        assertTrue(topBarSource.contains("onActionDockCollapsedChange(!actionDockCollapsed)"))
        assertTrue(topBarSource.contains("\"折叠顶部操作\""))
        assertTrue(topBarSource.contains("\"展开顶部操作\""))
    }

    @Test
    fun `fold bar unfolds related dynamics in place instead of opening detail`() {
        val screenSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt"
        ).readText()
        val cardSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicCard.kt"
        ).readText()
        val foldBlock = cardSource
            .substringAfter("PiliPlus: fold InkWell")
            .substringBefore("if (!isDetail) {\n            AppHorizontalDivider")

        assertTrue(screenSource.contains("onUnfoldRelatedClick = viewModel::unfoldRelatedDynamics"))
        assertTrue(foldBlock.contains("onUnfoldRelatedClick?.invoke(item.id_str)"))
        assertFalse(foldBlock.contains("openDynamicDetail"))
    }
}
