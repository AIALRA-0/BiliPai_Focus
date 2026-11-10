package com.android.purebilibili.core.ui.components

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.AppUiStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppPrimitiveRendererPolicyTest {

    @Test
    fun outlinedTextFieldKeepsMiuixWithPrefixOrSuffix() {
        assertTrue(
            shouldUseMiuixOutlinedTextField(
                uiStyle = AppUiStyle.MIUIX,
                hasPrefix = false,
                hasSuffix = false,
            )
        )
        assertTrue(
            shouldUseMiuixOutlinedTextField(
                uiStyle = AppUiStyle.MIUIX,
                hasPrefix = true,
                hasSuffix = false,
            )
        )
        assertTrue(shouldUseMiuixOutlinedTextField(AppUiStyle.MIUIX, hasPrefix = true, hasSuffix = true))
        assertFalse(shouldUseMiuixOutlinedTextField(AppUiStyle.MIUIX, hasPrefix = true, hasSuffix = false, liquidGlassEnabled = true))
        assertTrue(shouldUseMiuixOutlinedTextField(AppUiStyle.MIUIX, hasPrefix = false, hasSuffix = false, liquidGlassEnabled = true))
        assertFalse(
            shouldUseMiuixOutlinedTextField(
                uiStyle = AppUiStyle.MATERIAL3,
                hasPrefix = false,
                hasSuffix = false,
            )
        )
    }

    @Test
    fun actionPrimitivesUseMiuixWheneverUiStyleIsMiuix() {
        assertTrue(shouldUseMiuixActionPrimitive(AppUiStyle.MIUIX))
        assertFalse(shouldUseMiuixActionPrimitive(AppUiStyle.MATERIAL3))
        @Suppress("DEPRECATION")
        assertTrue(shouldUseMiuixNonGlassActionPrimitive(AppUiStyle.MIUIX, liquidGlassEnabled = true))
        @Suppress("DEPRECATION")
        assertTrue(shouldUseMiuixNonGlassActionPrimitive(AppUiStyle.MIUIX, liquidGlassEnabled = false))
        @Suppress("DEPRECATION")
        assertFalse(shouldUseMiuixNonGlassActionPrimitive(AppUiStyle.MATERIAL3, liquidGlassEnabled = true))
        @Suppress("DEPRECATION")
        assertFalse(shouldUseMiuixNonGlassActionPrimitive(AppUiStyle.MATERIAL3, liquidGlassEnabled = false))
    }

    @Test
    fun chipsUsePrimaryToneOnlyWhenSelected() {
        assertEquals(AppMiuixActionTone.PRIMARY, resolveMiuixChipActionTone(selected = true))
        assertEquals(AppMiuixActionTone.SECONDARY, resolveMiuixChipActionTone(selected = false))
    }

    @Test
    fun nonGlassChipMetricsStayCompactInsteadOfOfficialButtonSize() {
        val metrics = resolveMiuixNonGlassChipMetrics()
        assertEquals(36, metrics.minHeightDp)
        assertEquals(52, metrics.minWidthDp)
        assertEquals(10, metrics.cornerRadiusDp)
        assertEquals(12, metrics.horizontalPaddingDp)
        assertEquals(8, metrics.iconGapDp)
    }

    @Test
    fun fabUsesOfficialSixtyAndSmallStaysAtTouchMinimum() {
        assertEquals(60, resolveMiuixFabMinSizeDp(small = false))
        assertEquals(48, resolveMiuixFabMinSizeDp(small = true))
    }

    @Test
    fun fabDefaultMaterialContainerRemapsToMiuixContainer() {
        val materialDefault = Color(0xFF123456)
        val custom = Color(0xFFABCDEF)
        val miuixDefaultContainer = Color(0xFF111111)
        assertEquals(
            miuixDefaultContainer,
            resolveMiuixFabContainerColor(
                requested = materialDefault,
                defaultMaterialContainer = materialDefault,
                miuixDefaultContainer = miuixDefaultContainer,
            ),
        )
        assertEquals(
            custom,
            resolveMiuixFabContainerColor(
                requested = custom,
                defaultMaterialContainer = materialDefault,
                miuixDefaultContainer = miuixDefaultContainer,
            ),
        )
    }
}
