package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.ui.components.resolveAppLiquidSegmentedControlSpec
import com.android.purebilibili.core.ui.components.resolveAppSegmentedLabelFontSizeSp
import com.android.purebilibili.core.ui.components.resolveAppSegmentedLiquidGlassRequest
import com.android.purebilibili.core.ui.components.shouldFillMaxWidthAppSegmentedControl
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppSegmentedControlPolicyTest {

    @Test
    fun defaultSettingsIndicatorRenderPolicyMatchesCommentSortIndicator() {
        val policy = resolveAppLiquidSegmentedControlSpec(
            itemCount = 2,
            hasExternalBackdrop = false,
        )

        assertEquals(66, policy.itemWidthDp)
        assertEquals(44, policy.heightDp)
        assertEquals(33, policy.indicatorHeightDp)
        assertEquals(15, policy.labelFontSizeSp)
        assertFalse(policy.liquidGlassEffectsEnabled)
        assertFalse(policy.tapPressRefractionEnabled)
    }

    @Test
    fun denseSegmentLabelsShrinkFontAndPreferFillMaxWidth() {
        assertEquals(12f, resolveAppSegmentedLabelFontSizeSp(4, 4))
        assertEquals(13f, resolveAppSegmentedLabelFontSizeSp(3, 4))
        assertTrue(shouldFillMaxWidthAppSegmentedControl(4, 4))
        assertEquals(
            12,
            resolveAppLiquidSegmentedControlSpec(
                itemCount = 4,
                hasExternalBackdrop = false,
                longestLabelLength = 4,
            ).labelFontSizeSp,
        )
    }

    @Test
    fun liquidHostPreservesCustomSizingAndBottomBarInjection() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/settings/AppSegmentedControl.kt",
        )

        assertTrue(source.contains("itemWidth = null"))
        assertTrue(source.contains("modifier = modifier"))
        assertTrue(source.contains("height = resolvedHeight"))
        assertTrue(source.contains("indicatorHeight = resolvedIndicatorHeight"))
        assertTrue(source.contains("labelFontSize = resolvedLabelFontSize"))
        assertTrue(source.contains("liquidGlassEffectsEnabled = spec.liquidGlassEffectsEnabled"))
        assertTrue(source.contains("tapPressRefractionEnabled = resolvedTapPressRefractionEnabled"))
        assertTrue(source.contains("indicatorIdleSurfaceColorOverride = indicatorIdleSurfaceColorOverride"))
        assertTrue(source.contains("containerColorOverride = containerColorOverride"))
    }

    @Test
    fun liquidGlassRequestOnlyForForcedControlWithBackdrop() {
        assertNull(resolveAppSegmentedLiquidGlassRequest(true, false))
        assertEquals(true, resolveAppSegmentedLiquidGlassRequest(true, true))
        assertNull(resolveAppSegmentedLiquidGlassRequest(false, true))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        return listOf(File(path), File(normalizedPath))
            .firstOrNull(File::exists)
            ?.readText()
            ?: error("Cannot locate $path from ${File(".").absolutePath}")
    }
}
