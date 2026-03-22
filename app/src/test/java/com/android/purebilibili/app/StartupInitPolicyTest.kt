package com.android.purebilibili.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupInitPolicyTest {

    @Test
    fun homeVisualDefaultsMigrationDoesNotBlockColdStart() {
        assertFalse(shouldBlockStartupForHomeVisualDefaultsMigration())
    }

    @Test
    fun defersPlaylistRestoreAtStartup() {
        assertTrue(shouldDeferPlaylistRestoreAtStartup())
    }

    @Test
    fun defersTelemetryInitializationAtStartup() {
        assertTrue(shouldDeferTelemetryInitAtStartup())
    }

    @Test
    fun usesStableDeferredStartupDelayWindow() {
        assertEquals(900L, deferredNonCriticalStartupDelayMs())
    }
}
