package com.android.purebilibili.feature.settings

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class AppearanceSettingsLayoutPolicyTest {

    @Test
    fun resolveAppearanceBottomPadding_addsExtraSpaceForExpandableSections() {
        val result = resolveAppearanceBottomPadding(
            navigationBarsBottom = 20.dp,
            expandableSectionEnabled = true
        )

        assertEquals(116.dp, result)
    }

    @Test
    fun resolveAppearanceBottomPadding_keepsCompactSpaceWhenNoExpandableSection() {
        val result = resolveAppearanceBottomPadding(
            navigationBarsBottom = 20.dp,
            expandableSectionEnabled = false
        )

        assertEquals(44.dp, result)
    }
}
