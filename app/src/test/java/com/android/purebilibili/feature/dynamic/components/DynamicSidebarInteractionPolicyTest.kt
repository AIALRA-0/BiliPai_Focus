package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.core.util.HapticType
import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicSidebarInteractionPolicyTest {

    @Test
    fun userAvatarClick_triggersLightHapticBeforeFilteringUser() {
        val events = mutableListOf<String>()

        performDynamicSidebarUserAvatarClick(
            haptic = { type ->
                events += "haptic:${type.name}"
            },
            onClick = {
                events += "filter_user"
            }
        )

        assertEquals(listOf("haptic:${HapticType.LIGHT.name}", "filter_user"), events)
    }
}
