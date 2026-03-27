package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.util.HapticType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomBarInteractionPolicyTest {

    @Test
    fun materialBottomBarTap_triggersLightHapticBeforeNavigation() {
        val events = mutableListOf<String>()

        performMaterialBottomBarTap(
            haptic = { type: HapticType ->
                events += "haptic:${type.name}"
            },
            onClick = {
                events += "navigate"
            }
        )

        assertEquals(listOf("haptic:${HapticType.LIGHT.name}", "navigate"), events)
    }

    @Test
    fun primaryTap_navigate_triggersLightHapticBeforeNavigation() {
        val events = mutableListOf<String>()

        performBottomBarPrimaryTap(
            item = BottomNavItem.DYNAMIC,
            isSelected = false,
            haptic = { type ->
                events += "haptic:${type.name}"
            },
            onNavigate = {
                events += "navigate"
            },
            onHomeReselect = {
                events += "home_reselect"
            }
        )

        assertEquals(listOf("haptic:${HapticType.LIGHT.name}", "navigate"), events)
    }

    @Test
    fun primaryTap_homeReselect_triggersLightHapticBeforeRefreshAction() {
        val events = mutableListOf<String>()

        performBottomBarPrimaryTap(
            item = BottomNavItem.HOME,
            isSelected = true,
            haptic = { type ->
                events += "haptic:${type.name}"
            },
            onNavigate = {
                events += "navigate"
            },
            onHomeReselect = {
                events += "home_reselect"
            }
        )

        assertEquals(listOf("haptic:${HapticType.LIGHT.name}", "home_reselect"), events)
    }

    @Test
    fun bottomBarDebounce_allowsDifferentItemsWithinDebounceWindow() {
        assertTrue(
            shouldAcceptBottomBarTap(
                tappedItem = BottomNavItem.DYNAMIC,
                lastTappedItem = BottomNavItem.HOME,
                currentTimeMillis = 1_000L,
                lastTapTimeMillis = 900L,
                debounceWindowMillis = 200L
            )
        )
    }

    @Test
    fun bottomBarDebounce_blocksSameItemWithinDebounceWindow() {
        assertFalse(
            shouldAcceptBottomBarTap(
                tappedItem = BottomNavItem.DYNAMIC,
                lastTappedItem = BottomNavItem.DYNAMIC,
                currentTimeMillis = 1_000L,
                lastTapTimeMillis = 900L,
                debounceWindowMillis = 200L
            )
        )
    }

    @Test
    fun bottomBarDebounce_allowsSameItemAfterDebounceWindow() {
        assertTrue(
            shouldAcceptBottomBarTap(
                tappedItem = BottomNavItem.DYNAMIC,
                lastTappedItem = BottomNavItem.DYNAMIC,
                currentTimeMillis = 1_200L,
                lastTapTimeMillis = 900L,
                debounceWindowMillis = 200L
            )
        )
    }
}
