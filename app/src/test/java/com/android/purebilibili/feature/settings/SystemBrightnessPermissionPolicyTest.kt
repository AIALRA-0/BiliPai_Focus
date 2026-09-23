package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SystemBrightnessPermissionPolicyTest {

    @Test
    fun `enable request persists only when write settings is granted`() {
        assertEquals(
            SystemBrightnessToggleAction.ENABLE,
            resolveSystemBrightnessToggleAction(
                requestedEnabled = true,
                canWriteSystemSettings = true
            )
        )
    }

    @Test
    fun `enable request opens permission flow when write settings is missing`() {
        assertEquals(
            SystemBrightnessToggleAction.REQUEST_PERMISSION,
            resolveSystemBrightnessToggleAction(
                requestedEnabled = true,
                canWriteSystemSettings = false
            )
        )
    }

    @Test
    fun `disable request never requires write settings permission`() {
        assertEquals(
            SystemBrightnessToggleAction.DISABLE,
            resolveSystemBrightnessToggleAction(
                requestedEnabled = false,
                canWriteSystemSettings = false
            )
        )
    }

    @Test
    fun `stored setting is cleared after permission is revoked`() {
        assertFalse(
            normalizeSystemBrightnessSetting(
                storedEnabled = true,
                canWriteSystemSettings = false
            )
        )
        assertTrue(
            normalizeSystemBrightnessSetting(
                storedEnabled = true,
                canWriteSystemSettings = true
            )
        )
    }
}
