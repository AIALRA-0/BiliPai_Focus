package com.android.purebilibili.core.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppBottomNavigationHostPolicyTest {

    @Test
    fun `renderers require the native liquid glass switch`() {
        val policy = resolveAppBottomNavigationVisualPolicy(
            androidNativeLiquidGlassEnabled = false,
        )

        assertFalse(policy.liquidGlassEnabled)
    }

    @Test
    fun `native liquid glass switch enables liquid glass`() {
        val policy = resolveAppBottomNavigationVisualPolicy(
            androidNativeLiquidGlassEnabled = true,
        )

        assertTrue(policy.liquidGlassEnabled)
    }
}
