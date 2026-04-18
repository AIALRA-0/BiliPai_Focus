package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AppearanceSettingsNavigationPolicyTest {

    @Test
    fun openTopTabManagement_submitsBottomBarTopTabFocusBeforeNavigation() {
        var navigated = false
        SettingsSearchFocusController.clear()

        openTopTabManagement {
            navigated = true
        }

        val request = SettingsSearchFocusController.request.value
        assertNotNull(request)
        assertEquals(SettingsSearchTarget.BOTTOM_BAR, request.target)
        assertEquals(SettingsSearchFocusIds.BOTTOM_BAR_TOP_TABS, request.focusId)
        assertEquals(true, navigated)

        SettingsSearchFocusController.clear(request.token)
    }
}
