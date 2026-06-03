package com.android.purebilibili.feature.plugin.googlecast

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleCastPlaybackControllerTest {

    private val maxAttempts = 20

    @Test
    fun `shouldKeepPolling when remote client exists and attempts below max`() {
        assertTrue(
            GoogleCastPlaybackController.shouldKeepPollingForMediaStatus(
                remoteClientExists = true,
                missingMediaAttempts = 0,
                maxAttempts = maxAttempts
            )
        )
    }

    @Test
    fun `shouldKeepPolling at last allowed attempt`() {
        assertTrue(
            GoogleCastPlaybackController.shouldKeepPollingForMediaStatus(
                remoteClientExists = true,
                missingMediaAttempts = maxAttempts - 1,
                maxAttempts = maxAttempts
            )
        )
    }

    @Test
    fun `shouldStopPolling when attempts reach max`() {
        assertFalse(
            GoogleCastPlaybackController.shouldKeepPollingForMediaStatus(
                remoteClientExists = true,
                missingMediaAttempts = maxAttempts,
                maxAttempts = maxAttempts
            )
        )
    }

    @Test
    fun `shouldStopPolling when attempts exceed max`() {
        assertFalse(
            GoogleCastPlaybackController.shouldKeepPollingForMediaStatus(
                remoteClientExists = true,
                missingMediaAttempts = maxAttempts + 5,
                maxAttempts = maxAttempts
            )
        )
    }

    @Test
    fun `shouldStopPolling when remote client is null regardless of attempts`() {
        assertFalse(
            GoogleCastPlaybackController.shouldKeepPollingForMediaStatus(
                remoteClientExists = false,
                missingMediaAttempts = 0,
                maxAttempts = maxAttempts
            )
        )
    }

    @Test
    fun `shouldStopPolling when remote client is null even with low attempts`() {
        assertFalse(
            GoogleCastPlaybackController.shouldKeepPollingForMediaStatus(
                remoteClientExists = false,
                missingMediaAttempts = 5,
                maxAttempts = maxAttempts
            )
        )
    }
}
