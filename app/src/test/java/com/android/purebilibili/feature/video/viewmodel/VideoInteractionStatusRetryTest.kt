package com.android.purebilibili.feature.video.viewmodel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoInteractionStatusRetryTest {
    @Test
    fun failedReadRetriesAndAcceptsKnownFalse() = runTest {
        var calls = 0
        val result = queryVideoInteractionStatusWithRetry({ true }) {
            calls++
            if (calls == 1) Result.failure(IllegalStateException("offline")) else Result.success(false)
        }
        assertEquals(2, calls)
        assertEquals(false, result.getOrThrow())
    }

    @Test
    fun persistentFailureStaysUnknownAndStopsAfterThreeAttempts() = runTest {
        var calls = 0
        val result = queryVideoInteractionStatusWithRetry({ true }) {
            calls++
            Result.failure(IllegalStateException("offline"))
        }
        assertEquals(3, calls)
        assertTrue(result.isFailure)
    }

    @Test
    fun videoOrAccountChangeStopsRetry() = runTest {
        var current = true
        var calls = 0
        val result = queryVideoInteractionStatusWithRetry({ current }) {
            calls++
            current = false
            Result.failure(IllegalStateException("offline"))
        }
        assertEquals(1, calls)
        assertTrue(result.isFailure)
    }

    @Test
    fun timedOutReadCanRecoverOnNextAttempt() = runTest {
        var calls = 0
        val result = queryVideoInteractionStatusWithRetry({ true }) {
            calls++
            if (calls == 1) delay(10_000L)
            Result.success(true)
        }
        assertEquals(2, calls)
        assertTrue(result.getOrThrow())
    }

    @Test
    fun cancellationIsNotRetried() = runTest {
        var calls = 0
        var cancelled = false
        try {
            queryVideoInteractionStatusWithRetry({ true }) {
                calls++
                Result.failure(CancellationException("cancelled"))
            }
        } catch (_: CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
        assertEquals(1, calls)
    }
}
