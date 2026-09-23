package com.android.purebilibili.core.ui

import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AppUiStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class BottomBarContentPaddingPolicyTest {

    @Test
    fun `floating phone content includes body inset gap and navigation bars`() {
        assertEquals(
            120.dp,
            resolveBottomBarContentPadding(
                navigationBarsBottom = 24.dp,
                reserveBottomBar = true,
                isBottomBarFloating = true,
                bottomBarLabelMode = 0,
                isTablet = false,
                uiStyle = AppUiStyle.MATERIAL3,
                hasUiSkinDecoration = false,
                extraContentPadding = 8.dp,
            ),
        )
    }

    @Test
    fun `docked tablet content uses docked body without floating inset`() {
        assertEquals(
            108.dp,
            resolveBottomBarContentPadding(
                navigationBarsBottom = 24.dp,
                reserveBottomBar = true,
                isBottomBarFloating = false,
                bottomBarLabelMode = 2,
                isTablet = true,
                uiStyle = AppUiStyle.MIUIX,
                hasUiSkinDecoration = false,
                extraContentPadding = 8.dp,
            ),
        )
    }

    @Test
    fun `side navigation content only keeps system and content padding`() {
        assertEquals(
            32.dp,
            resolveBottomBarContentPadding(
                navigationBarsBottom = 24.dp,
                reserveBottomBar = false,
                isBottomBarFloating = true,
                bottomBarLabelMode = 0,
                isTablet = true,
                uiStyle = AppUiStyle.MATERIAL3,
                hasUiSkinDecoration = false,
                extraContentPadding = 8.dp,
            ),
        )
    }

    @Test
    fun `negative inputs are clamped`() {
        assertEquals(
            0.dp,
            resolveBottomBarContentPadding(
                navigationBarsBottom = (-4).dp,
                reserveBottomBar = false,
                isBottomBarFloating = false,
                bottomBarLabelMode = 1,
                isTablet = false,
                uiStyle = AppUiStyle.MATERIAL3,
                hasUiSkinDecoration = false,
                extraContentPadding = (-8).dp,
            ),
        )
    }

    @Test
    fun `floating skin reserves actual decorated shell`() {
        assertEquals(
            144.dp,
            resolveBottomBarContentPadding(
                navigationBarsBottom = 24.dp,
                reserveBottomBar = true,
                isBottomBarFloating = true,
                bottomBarLabelMode = 0,
                isTablet = false,
                uiStyle = AppUiStyle.MIUIX,
                hasUiSkinDecoration = true,
                extraContentPadding = 8.dp,
            ),
        )
    }

    @Test
    fun `material docked navigation reserves material navigation bar`() {
        assertEquals(
            124.dp,
            resolveBottomBarContentPadding(
                navigationBarsBottom = 24.dp,
                reserveBottomBar = true,
                isBottomBarFloating = false,
                bottomBarLabelMode = 0,
                isTablet = false,
                uiStyle = AppUiStyle.MATERIAL3,
                hasUiSkinDecoration = false,
                extraContentPadding = 8.dp,
            ),
        )
    }
}
