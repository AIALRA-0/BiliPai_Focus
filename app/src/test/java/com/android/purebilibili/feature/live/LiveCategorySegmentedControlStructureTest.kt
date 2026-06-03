package com.android.purebilibili.feature.live

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveCategorySegmentedControlStructureTest {

    @Test
    fun `live home category row delegates to bottom bar segmented control`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/live/LiveListScreen.kt"
        )

        assertTrue(source.contains("BottomBarLiquidSegmentedControl("))
        assertTrue(source.contains("resolveLiveHomeCategorySegmentedControlSpec()"))
        assertTrue(source.contains(".horizontalScroll(scrollState, enabled = false)"))
        assertTrue(source.contains("resolveLiveHomeCategoryFollowScrollTarget("))
        assertTrue(source.contains("scrollState.scrollTo(targetScroll)"))
        assertTrue(source.contains("onIndicatorPositionChanged = { indicatorPosition = it }"))
        assertFalse(source.contains("dragSelectionEnabled = false"))
        assertFalse(source.contains("liquidGlassEffectsEnabled = false"))
        assertFalse(source.contains("SimpleLiquidIndicator"))
        assertFalse(source.contains("shouldUseLiveHomeCategoryLiquidIndicator("))
    }

    @Test
    fun `all tags parent row uses shared segmented control dimensions`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/live/LiveAreaScreen.kt"
        )

        assertTrue(source.contains("rememberPagerState"))
        assertTrue(source.contains("HorizontalPager("))
        assertTrue(source.contains("pagerState.animateScrollToPage"))
        assertTrue(source.contains("selectedTab = pagerState.currentPage"))
        assertTrue(source.contains("BottomBarLiquidSegmentedControl("))
        assertTrue(source.contains("resolveLiveAreaParentSegmentedControlSpec()"))
        assertTrue(source.contains(".horizontalScroll(scrollState, enabled = false)"))
        assertTrue(source.contains("resolveLiveHomeCategoryFollowScrollTarget("))
        assertTrue(source.contains("scrollState.scrollTo(targetScroll)"))
        assertTrue(source.contains("onIndicatorPositionChanged = { indicatorPosition = it }"))
        assertFalse(source.contains("dragSelectionEnabled = false"))
        assertFalse(source.contains("liquidGlassEffectsEnabled = false"))
    }

    @Test
    fun `live room interaction panel supports horizontal pager gestures`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/live/LivePlayerScreen.kt"
        )

        assertTrue(source.contains("rememberPagerState"))
        assertTrue(source.contains("HorizontalPager("))
        assertTrue(source.contains("pagerState.animateScrollToPage"))
        assertTrue(source.contains("selectedIndex = pagerState.currentPage"))
    }

    @Test
    fun `live room interaction panel is collapsed by default and gated before rendering`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/live/LivePlayerScreen.kt"
        )

        assertTrue(source.contains("defaultLiveInteractionPanelVisible()"))
        assertTrue(source.contains("isInteractionPanelVisible"))
        assertTrue(source.contains("shouldReserveLivePortraitInteractionPanel("))
        assertTrue(source.contains("if (isInteractionPanelVisible)"))
        assertFalse(source.contains("var isChatVisible by remember { mutableStateOf(true) }"))
    }

    @Test
    fun `live send sheet uses danmaku permission and lifecycle controls heartbeat`() {
        val screenSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/live/LivePlayerScreen.kt"
        )
        val sheetSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/live/components/LiveSendDanmakuSheet.kt"
        )

        assertTrue(screenSource.contains("permission = successState?.danmakuPermission"))
        assertTrue(screenSource.contains("viewModel.pauseLiveHeartbeat()"))
        assertTrue(screenSource.contains("viewModel.resumeLiveHeartbeatIfNeeded()"))
        assertTrue(sheetSource.contains("permission: LiveDanmakuPermission"))
        assertTrue(sheetSource.contains("permission.canSend"))
    }

    @Test
    fun `live player wires danmaku display area settings`() {
        val screenSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/live/LivePlayerScreen.kt"
        )
        val overlaySource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/LiveDanmakuOverlay.kt"
        )

        assertTrue(screenSource.contains("resolveDanmakuSettingsScope(isLandscape = isLandscape)"))
        assertTrue(screenSource.contains("getDanmakuSettings(context, liveDanmakuSettingsScope)"))
        assertTrue(screenSource.contains("val liveDanmakuDisplayArea = liveDanmakuSettings.displayArea"))
        assertTrue(screenSource.contains("displayArea = liveDanmakuDisplayArea"))
        assertTrue(screenSource.contains("setDanmakuArea(context, area, liveDanmakuSettingsScope)"))
        assertTrue(screenSource.contains("title = \"弹幕区域\""))
        assertTrue(screenSource.contains("\"半屏\""))
        assertTrue(screenSource.contains("\"全屏\""))
        assertTrue(overlaySource.contains("displayArea: Float = 1f"))
        assertTrue(overlaySource.contains(".fillMaxHeight(safeDisplayArea)"))
    }

    @Test
    fun `live player applies fixed aspect ratio viewport`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/live/LivePlayerScreen.kt"
        )

        assertTrue(source.contains("val viewportAspectRatio = videoAspectRatio"))
        assertTrue(source.contains("resolveVideoViewportLayout("))
        assertTrue(source.contains("containerWidth = maxWidth.roundToPx()"))
        assertTrue(source.contains("containerHeight = maxHeight.roundToPx()"))
        assertTrue(source.contains("aspectRatio = viewportAspectRatio"))
        assertTrue(source.contains("Modifier.size("))
        assertTrue(source.contains("width = viewportLayout.width.toDp()"))
        assertTrue(source.contains("height = viewportLayout.height.toDp()"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
