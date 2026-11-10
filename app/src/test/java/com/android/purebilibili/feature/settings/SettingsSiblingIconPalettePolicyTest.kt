package com.android.purebilibili.feature.settings

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SettingsSiblingIconPalettePolicyTest {
    @Test
    fun siblingNavigationRequestsNeutralIconsRegardlessOfCategoryOrOffset() {
        assertEquals(List(8) { Color.Unspecified }, resolveSettingsSiblingIconTints(8, 5))
        assertEquals(Color.Unspecified, resolveSettingsSiblingIconTint(0))
        assertEquals(Color.Unspecified, resolveSettingsSiblingIconTint(15, 7))
    }

    @Test
    fun longGroupsDoNotIntroduceNewColorsOrAnArbitraryPaletteLimit() {
        assertEquals(List(32) { Color.Unspecified }, resolveSettingsSiblingIconTints(32))
        assertEquals(emptyList(), resolveSettingsSiblingIconTints(0))
        assertFailsWith<IllegalArgumentException> { resolveSettingsSiblingIconTints(-1) }
    }

    @Test
    fun miuixGroupsKeepTheColorfulPaletteForLongGroups() {
        val tints = resolveSettingsSiblingIconTints(SettingsSiblingIconPalette.size, colorful = true)
        assertEquals(SettingsSiblingIconPalette, tints)
        assertEquals(tints.size, tints.distinct().size)
        // 超出色板长度时循环取色，仍然不引入新颜色
        assertEquals(
            SettingsSiblingIconPalette.first(),
            resolveSettingsSiblingIconTint(SettingsSiblingIconPalette.size, colorful = true),
        )
        assertEquals(
            SettingsSiblingIconPalette[2],
            resolveSettingsSiblingIconTint(1, paletteOffset = 1, colorful = true),
        )
        assertEquals(emptyList(), resolveSettingsSiblingIconTints(0, colorful = true))
        assertFailsWith<IllegalArgumentException> { resolveSettingsSiblingIconTints(-1, colorful = true) }
    }
}
