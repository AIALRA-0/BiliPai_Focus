package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.util.HapticType
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class TopBarInteractionPolicyTest {

    @Test
    fun homeTopBarTap_triggersLightHapticBeforeAction() {
        val events = mutableListOf<String>()

        performHomeTopBarTap(
            haptic = { type ->
                events += "haptic:${type.name}"
            },
            onClick = {
                events += "action"
            }
        )

        assertEquals(listOf("haptic:${HapticType.LIGHT.name}", "action"), events)
    }

    @Test
    fun topTabViewportPadding_keepsEdgeTabsInsideScreenBounds() {
        assertEquals(12.dp, resolveTopTabViewportPaddingDp(isFloatingStyle = false, edgeToEdge = true))
        assertEquals(8.dp, resolveTopTabViewportPaddingDp(isFloatingStyle = true, edgeToEdge = false))
        assertEquals(0.dp, resolveTopTabViewportPaddingDp(isFloatingStyle = false, edgeToEdge = false))
    }
}
