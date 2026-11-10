package com.android.purebilibili.feature.audio.player

import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundAudioTimeoutTest {
    @Test
    fun unansweredRemoteCommandFailsInsteadOfCancellingCaller() = runTest {
        val neverAnswered = SettableFuture.create<String>()
        val error = runCatching {
            withBackgroundAudioTimeout(5000L, "return") { neverAnswered.awaitBackgroundAudio() }
        }.exceptionOrNull()

        assertTrue(error is BackgroundAudioTimeoutException)
        assertFalse(error is CancellationException)
    }

    @Test
    fun answeredRemoteCommandReturnsItsValue() = runTest {
        val answered = SettableFuture.create<String>().apply { set("ok") }

        assertEquals("ok", withBackgroundAudioTimeout(5000L, "return") { answered.awaitBackgroundAudio() })
    }

    @Test
    fun callerCancellationStaysCancellation() = runTest {
        val neverAnswered = SettableFuture.create<String>()
        var failure: Throwable? = null
        val call = async {
            try {
                withBackgroundAudioTimeout(60_000L, "return") { neverAnswered.awaitBackgroundAudio() }
            } catch (e: Throwable) {
                failure = e
                throw e
            }
        }
        testScheduler.runCurrent()
        call.cancelAndJoin()

        assertTrue(failure is CancellationException)
        assertFalse(failure is BackgroundAudioTimeoutException)
        assertTrue(neverAnswered.isCancelled)
    }
}
