package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SettingsSiblingIconPalettePolicyTest {

    @Test
    fun visibleSettingsGroupGetsUniqueIconColors() {
        val colors = resolveSettingsSiblingIconTints(SettingsSiblingIconPalette.size)

        assertEquals(colors.size, colors.toSet().size)
    }

    @Test
    fun paletteOffsetKeepsSiblingColorsUnique() {
        val colors = resolveSettingsSiblingIconTints(siblingCount = 8, paletteOffset = 5)

        assertEquals(colors.size, colors.toSet().size)
    }

    @Test
    fun rootSettingsDirectoriesGetDistinctColors() {
        val rootDirectories = resolveSettingsRootCategoryOrder()
        val colors = rootDirectories.indices.map(::resolveSettingsSiblingIconTint)

        assertEquals(rootDirectories.size, colors.toSet().size)
    }

    @Test
    fun oversizedGroupMustBeSplitInsteadOfRepeatingAColor() {
        assertFailsWith<IllegalArgumentException> {
            resolveSettingsSiblingIconTints(SettingsSiblingIconPalette.size + 1)
        }
    }
}
