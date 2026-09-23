package com.android.purebilibili.core.ui.components

import com.android.purebilibili.testutil.readProjectSource
import kotlin.test.Test
import kotlin.test.assertTrue

class AppLiquidAwareTabRowConstraintPolicyTest {
    @Test
    fun scrollableLiquidTabsBoundWidthBeforeApplyingHorizontalScroll() {
        val source = readProjectSource(
            "app/src/main/java/com/android/purebilibili/core/ui/components/AppLiquidAwareTabRow.kt",
        )
        val viewport = readProjectSource(
            "app/src/main/java/com/android/purebilibili/core/ui/components/LiquidDockViewport.kt",
        )

        assertTrue(source.contains("val viewportMaxWidth = LocalConfiguration.current.screenWidthDp.dp"))
        assertTrue(source.contains(".widthIn(max = viewportMaxWidth)"))
        assertTrue(source.contains(".liquidDockViewport()"))
        assertTrue(viewport.contains("return this.clip(shape)"))
        assertTrue(viewport.contains("val shape = if (liquidGlassEnabled)"))
        assertTrue(source.contains("onIndicatorPositionChanged = { position ->"))
        assertTrue(source.contains("resolveScrollableTabIndicatorFollowDeltaPx("))
        assertTrue(source.contains("scrollState.dispatchRawDelta("))
    }
}
