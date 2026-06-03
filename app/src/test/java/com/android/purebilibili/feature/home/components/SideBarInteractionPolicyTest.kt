package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.util.HapticType
import kotlin.test.Test
import kotlin.test.assertEquals

class SideBarInteractionPolicyTest {

    @Test
    fun homeSideBarItemTap_triggersLightHapticBeforeNavigation() {
        val events = mutableListOf<String>()

        performHomeSideBarItemTap(
            haptic = { type ->
                events += "haptic:${type.name}"
            },
            onClick = {
                events += "navigate"
            }
        )

        assertEquals(listOf("haptic:${HapticType.LIGHT.name}", "navigate"), events)
    }
}
