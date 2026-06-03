package com.android.purebilibili.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupInitPolicyTest {

    @Test
    fun homeVisualDefaultsMigrationDoesNotBlockColdStart() {
        assertFalse(PureApplicationRuntimeConfig.shouldBlockStartupForHomeVisualDefaultsMigration())
    }

    @Test
    fun defersPlaylistRestoreAtStartup() {
        assertTrue(PureApplicationRuntimeConfig.shouldDeferPlaylistRestoreAtStartup())
    }

    @Test
    fun defersTelemetryInitializationAtStartup() {
        assertTrue(PureApplicationRuntimeConfig.shouldDeferTelemetryInitAtStartup())
    }

    @Test
    fun usesStableDeferredStartupDelayWindow() {
        assertEquals(900L, PureApplicationRuntimeConfig.deferredNonCriticalStartupDelayMs())
    }
}
