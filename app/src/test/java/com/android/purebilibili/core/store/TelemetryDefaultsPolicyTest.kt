package com.android.purebilibili.core.store

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TelemetryDefaultsPolicyTest {

    @Test
    fun analyticsIsEnabledByDefaultAlongsideCrashAndReleasePlayerDiagnostics() {
        assertTrue(DEFAULT_CRASH_TRACKING_ENABLED)
        assertTrue(DEFAULT_ANALYTICS_ENABLED)
        assertTrue(resolveDefaultPlayerDiagnosticLoggingEnabled(isDebugBuild = false))
    }

    @Test
    fun playerDiagnosticsAreDisabledByDefaultForDebugSmoothness() {
        assertFalse(resolveDefaultPlayerDiagnosticLoggingEnabled(isDebugBuild = true))
    }
}
