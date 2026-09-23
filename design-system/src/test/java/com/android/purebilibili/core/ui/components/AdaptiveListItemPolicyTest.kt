package com.android.purebilibili.core.ui.components

import com.android.purebilibili.core.theme.AppUiStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdaptiveListItemPolicyTest {

    @Test
    fun `miuix clickable item with chevron routes to arrow preference`() {
        assertEquals(
            AppClickableItemRenderer.MIUIX_ARROW,
            resolveAppClickableItemRenderer(
                uiStyle = AppUiStyle.MIUIX,
                onClick = {},
                showChevron = true,
                centered = false
            )
        )
        assertTrue(
            shouldRouteClickableItemToMiuixArrowPreference(
                uiStyle = AppUiStyle.MIUIX,
                onClick = {},
                showChevron = true,
                centered = false
            )
        )
    }

    @Test
    fun `miuix clickable item without chevron routes to basic component`() {
        assertEquals(
            AppClickableItemRenderer.MIUIX_BASIC,
            resolveAppClickableItemRenderer(
                uiStyle = AppUiStyle.MIUIX,
                onClick = {},
                showChevron = false,
                centered = false
            )
        )
        assertFalse(
            shouldRouteClickableItemToMiuixArrowPreference(
                uiStyle = AppUiStyle.MIUIX,
                onClick = {},
                showChevron = false,
                centered = false
            )
        )
    }

    @Test
    fun `material3 clickable item routes to basic component`() {
        assertEquals(
            AppClickableItemRenderer.MD3_BASIC,
            resolveAppClickableItemRenderer(
                uiStyle = AppUiStyle.MATERIAL3,
                onClick = {},
                showChevron = true,
                centered = false
            )
        )
        assertFalse(
            shouldRouteClickableItemToMiuixArrowPreference(
                uiStyle = AppUiStyle.MATERIAL3,
                onClick = {},
                showChevron = true,
                centered = false
            )
        )
    }

    @Test
    fun `centered clickable item keeps legacy cupertino row renderer`() {
        assertEquals(
            AppClickableItemRenderer.CUPERTINO,
            resolveAppClickableItemRenderer(
                uiStyle = AppUiStyle.MIUIX,
                onClick = {},
                showChevron = true,
                centered = true
            )
        )
    }

    @Test
    fun `miuix switch item routes to switch preference`() {
        assertTrue(
            shouldRouteSwitchItemToMiuixSwitchPreference(
                uiStyle = AppUiStyle.MIUIX
            )
        )
        assertFalse(
            shouldRouteSwitchItemToMiuixSwitchPreference(
                uiStyle = AppUiStyle.MATERIAL3
            )
        )
    }

    @Test
    fun `miuix slider preference routes to official slider preference`() {
        assertTrue(
            shouldRouteSliderPreferenceToMiuixSliderPreference(
                uiStyle = AppUiStyle.MIUIX
            )
        )
        assertFalse(
            shouldRouteSliderPreferenceToMiuixSliderPreference(
                uiStyle = AppUiStyle.MATERIAL3
            )
        )
    }
}
