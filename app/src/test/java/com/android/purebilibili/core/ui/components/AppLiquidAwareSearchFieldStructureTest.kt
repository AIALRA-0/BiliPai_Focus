package com.android.purebilibili.core.ui.components

import com.android.purebilibili.testutil.readProjectSource
import kotlin.test.Test
import kotlin.test.assertTrue

class AppLiquidAwareSearchFieldStructureTest {
    @Test
    fun `liquid search keeps native input above the shared glass surface`() {
        val source = readProjectSource(
            "app/src/main/java/com/android/purebilibili/core/ui/components/AppLiquidAwareSearchField.kt"
        )

        assertTrue(source.contains("AppCompactInputField("))
        assertTrue(source.contains("BottomBarMatchedReusableLiquidDock("))
        assertTrue(source.contains("height = liquidHeight"))
        assertTrue(source.contains("Color.Transparent"))
        assertTrue(source.contains("leadingIconHorizontalOffset: Dp = 0.dp"))
        assertTrue(source.contains("leadingIconHorizontalOffset = leadingIconHorizontalOffset"))
    }

    @Test
    fun `comment search uses dock indicators and one shared popup surface`() {
        val source = readProjectSource(
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/CommentSearchSheet.kt"
        )

        assertTrue(source.contains("AppSearchField("))
        assertTrue(source.contains("leadingIconHorizontalOffset = 8.dp"))
        assertTrue(source.contains("AppModalBottomSheet("))
        assertTrue(source.contains("BottomBarLiquidSegmentedControl("))
        assertTrue(source.contains("items = listOf(\"全部评论\", \"只看UP主\")"))
        assertTrue(source.contains("items = CommentSearchSortMode.entries.map"))
        assertTrue(source.contains("val stackControls = maxWidth < 420.dp"))
        assertTrue(source.contains("height = 48.dp"))
        assertTrue(!source.substringBefore("private fun CommentSearchResultRow(")
            .contains("biliPaiFloatingDockShell("))
    }

    @Test
    fun `popup renderer uses standard surface for every popup type`() {
        val navigation = readProjectSource(
            "app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt"
        )
        val renderer = readProjectSource(
            "app/src/main/java/com/android/purebilibili/core/ui/components/" +
                "BiliPaiPopupSurfaceRenderer.kt"
        )

        assertTrue(navigation.contains("LocalAppPopupSurfaceRenderer provides"))
        assertTrue(navigation.contains("LocalFloatingChromeBackdrop provides"))
        assertTrue(navigation.contains("bottomBarBackdrop"))
        assertTrue(renderer.contains("Surface("))
        assertTrue(!renderer.contains("BottomBarMatchedReusableLiquidDock("))
    }
}
