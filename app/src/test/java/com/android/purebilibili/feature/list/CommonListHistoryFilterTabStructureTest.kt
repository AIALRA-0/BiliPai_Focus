package com.android.purebilibili.feature.list

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommonListHistoryFilterTabStructureTest {
    @Test
    fun historySearch_reusesMatchingLiquidDockAsSeparateRow() {
        val source = loadSource("src/main/java/com/android/purebilibili/feature/list/CommonListScreen.kt")
        val searchSection = source
            .substringAfter("val searchPlaceholder = when")
            .substringBefore("if (favoriteViewModel != null)")

        assertTrue(searchSection.contains("AppLiquidAwareSearchField("))
        assertTrue(searchSection.contains("backdrop = commonListChromeBackdrop"))
        assertTrue(
            searchSection.contains("primaryGridState.isScrollInProgress"),
            "搜索 Dock 的材质动态应跟随历史列表滚动"
        )
    }

    @Test
    fun historyFilterRow_usesThemeAdaptiveTabsInsteadOfFilterChips() {
        val source = loadSource("src/main/java/com/android/purebilibili/feature/list/CommonListScreen.kt")
        val historyFilterSection = source
            .substringAfter("val historyFilterOptions = remember")
            .substringBefore("if (favoriteViewModel != null) {")

        assertTrue(
            source.contains("resolveHistoryFilterTabChromeSpec"),
            "历史筛选行必须走统一的 tab chrome 策略"
        )
        assertTrue(
            historyFilterSection.contains("AppThemeAdaptiveTabRow("),
            "历史筛选必须走主题控件分流：关液态玻璃时 MD3 下划线、开玻璃时液态胶囊"
        )
        assertTrue(historyFilterSection.contains("dragSelectionEnabled = historyFilterChrome.dragSelectionEnabled"))
        assertTrue(historyFilterSection.contains("tapPressRefractionEnabled = true"))
        assertTrue(
            historyFilterSection.contains("scrollable = historyFilterChrome.itemWidthDp != null"),
            "关闭液态玻璃后应允许原生标签行横向滚动，避免窄屏强制均分导致文字省略"
        )
        assertTrue(
            historyFilterSection.contains(
                "minTabWidth = historyFilterChrome.itemWidthDp?.dp ?: Dp.Unspecified"
            ),
            "原生标签行使用策略宽度，液态标签行必须保留 beta.21 的未指定宽度语义"
        )
        assertFalse(historyFilterSection.contains("itemWidthDp ?: 0"))
        assertTrue(historyFilterSection.contains("height = historyFilterChrome.heightDp.dp"))
        assertTrue(historyFilterSection.contains("indicatorHeight = historyFilterChrome.indicatorHeightDp.dp"))
        assertTrue(
            historyFilterSection.contains("miuixBackdrop = commonListChromeBackdrop"),
            "液态路径必须绑定与顶栏一致的 backdrop 源"
        )
        assertFalse(
            historyFilterSection.contains("AppFilterChip("),
            "关闭液态玻璃后不得回退到胶囊 FilterChip"
        )
        assertFalse(
            historyFilterSection.contains("BottomBarLiquidSegmentedControl("),
            "历史筛选不得绕过 AppThemeAdaptiveTabRow 直调液态分段控件"
        )
        assertFalse(
            historyFilterSection.contains("forceLiquidChrome"),
            "液态 dock 应由共享组件统一读取全局开关"
        )
        assertFalse(
            historyFilterSection.contains("LazyRow("),
            "历史筛选不应依赖横向 FilterChip 列表"
        )
        assertFalse(
            historyFilterSection.contains("FlowRow("),
            "历史筛选不应再把主筛选放进 FlowRow 胶囊"
        )
    }

    @Test
    fun historyFilterRow_supportsHorizontalPagerSwipeToSwitchCategories() {
        val source = loadSource("src/main/java/com/android/purebilibili/feature/list/CommonListScreen.kt")

        assertTrue(
            source.contains("val historyPagerState = rememberPagerState("),
            "历史分类必须声明 historyPagerState"
        )
        assertTrue(
            source.contains("indicatorPositionProvider = {"),
            "历史筛选 Tab 栏指示器必须跟随 Pager 滑动手势平滑位移"
        )
        assertTrue(
            source.contains("historyPagerState.currentPage + historyPagerState.currentPageOffsetFraction"),
            "指示器位置需由 historyPagerState 的 currentPage 与 offsetFraction 驱动"
        )
        assertTrue(
            source.contains("isScrollInProgressProvider = { historyPagerState.isScrollInProgress }"),
            "Tab 栏应监听 historyPagerState 的滑动状态"
        )
        assertTrue(
            source.contains("userScrollEnabled = !isHistoryBatchMode"),
            "历史页面 Pager 在普通模式下允许左右滑动手势，批量选择模式下禁用以防手势冲突"
        )
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath),
            File("app/$normalizedPath")
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
