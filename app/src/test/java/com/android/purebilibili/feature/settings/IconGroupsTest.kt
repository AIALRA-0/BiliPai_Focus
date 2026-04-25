package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertTrue

class IconGroupsTest {

    @Test
    fun getIconGroups_containsBiliPaiAndHeadphone_withoutRemovedOptions() {
        val keys = getIconGroups().flatMap { group -> group.icons }.map { option -> option.key }.toSet()

        assertTrue(keys.contains("icon_bilipai"))
        assertTrue(keys.contains("Headphone"))
        kotlin.test.assertFalse(keys.contains("icon_flat_material"))
        kotlin.test.assertFalse(keys.contains("icon_retro"))
    }
}
