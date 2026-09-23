package com.android.purebilibili.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppContentDialogLayoutPolicyTest {

    @Test
    fun contentDialogLayout_disablesPlatformDefaultWidthAndCapsMaxWidth() {
        val compact = resolveAppCompactContentDialogLayoutPolicy()
        val standard = resolveAppContentDialogLayoutPolicy()
        val expanded = resolveAppExpandedContentDialogLayoutPolicy()

        assertFalse(compact.usePlatformDefaultWidth)
        assertFalse(standard.usePlatformDefaultWidth)
        assertFalse(expanded.usePlatformDefaultWidth)
        assertEquals(360, compact.maxWidthDp)
        assertEquals(420, standard.maxWidthDp)
        assertEquals(560, expanded.maxWidthDp)
        assertTrue(compact.maxWidthDp <= standard.maxWidthDp)
        assertTrue(standard.maxWidthDp <= expanded.maxWidthDp)
    }

    @Test
    fun contentDialogProperties_forceTabletSafeWidthFlag() {
        val properties = resolveAppContentDialogProperties(usePlatformDefaultWidth = false)
        assertFalse(properties.usePlatformDefaultWidth)
    }

    @Test
    fun contentDialogLayout_clampsInvalidMaxBelowMin() {
        val policy = resolveAppContentDialogLayoutPolicy(maxWidthDp = 100, minWidthDp = 280)
        assertEquals(280, policy.maxWidthDp)
        assertEquals(280, policy.minWidthDp)
    }
}
