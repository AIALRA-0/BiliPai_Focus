package com.android.purebilibili.core.ui

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class BottomSafeAreaPolicyTest {

    @Test
    fun resolveBottomSafeAreaPadding_addsNavigationInsetAndExtraSpacing() {
        val padding = resolveBottomSafeAreaPadding(
            navigationBarsBottom = 16.dp,
            extraBottomPadding = 24.dp
        )

        assertEquals(40.dp, padding)
    }

    @Test
    fun resolveBottomSafeAreaPadding_supportsInsetOnlyScreens() {
        val padding = resolveBottomSafeAreaPadding(
            navigationBarsBottom = 20.dp,
            extraBottomPadding = 0.dp
        )

        assertEquals(20.dp, padding)
    }

    @Test
    fun resolveBottomSafeAreaPadding_clampsNegativeInputs() {
        val padding = resolveBottomSafeAreaPadding(
            navigationBarsBottom = (-8).dp,
            extraBottomPadding = (-12).dp
        )

        assertEquals(0.dp, padding)
    }
}
