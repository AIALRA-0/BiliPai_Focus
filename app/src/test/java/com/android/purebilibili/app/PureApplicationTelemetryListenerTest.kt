package com.android.purebilibili.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PureApplicationTelemetryListenerTest {

    @Test
    fun `telemetry listener should use stable named implementation`() {
        val listener = PureApplicationRuntimeConfig.createTelemetryBackgroundStateListener()

        assertEquals(
            "com.android.purebilibili.app.PureApplicationRuntimeConfig\$TelemetryBackgroundStateListener",
            listener.javaClass.name
        )
        assertFalse(listener.javaClass.isAnonymousClass)
    }
}
