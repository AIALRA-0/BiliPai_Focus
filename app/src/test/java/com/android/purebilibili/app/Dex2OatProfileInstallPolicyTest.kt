package com.android.purebilibili.app

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class Dex2OatProfileInstallPolicyTest {

    @Test
    fun profileInstall_enabledOnAndroidNougatAndAbove() {
        assertFalse(shouldRequestDex2OatProfileInstall(sdkInt = 23))
        assertTrue(shouldRequestDex2OatProfileInstall(sdkInt = 24))
        assertTrue(shouldRequestDex2OatProfileInstall(sdkInt = 35))
    }

    @Test
    fun profileInstallDelay_usesConservativePostStartupWindow() {
        assertEquals(2_500L, dex2OatProfileInstallDelayMs())
    }
}
