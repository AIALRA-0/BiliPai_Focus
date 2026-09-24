package com.android.purebilibili.core.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame

class CachedAsyncBooleanProbeTest {

    @Test
    fun `dolby capability distinguishes platform and software decoding`() {
        val hardwareOnly = resolveDolbyAudioCapabilities(
            platformSupported = true,
            softwareSupported = false
        )
        val softwareOnly = resolveDolbyAudioCapabilities(
            platformSupported = false,
            softwareSupported = true
        )
        val unsupported = resolveDolbyAudioCapabilities(
            platformSupported = false,
            softwareSupported = false
        )

        assertEquals(true, hardwareOnly.isDolbyAudioSupported)
        assertEquals(false, hardwareOnly.isDolbyAudioSoftwareDecoded)
        assertEquals(true, softwareOnly.isDolbyAudioSupported)
        assertEquals(true, softwareOnly.isDolbyAudioSoftwareDecoded)
        assertEquals(false, unsupported.isDolbyAudioSupported)
        assertEquals(false, unsupported.isDolbyAudioSoftwareDecoded)
    }

    @Test
    fun `concurrent awaits share one probe executed off caller thread`() = runTest {
        val calls = AtomicInteger()
        val probeThread = AtomicReference<Thread>()
        val callerThread = Thread.currentThread()
        val probe = CachedAsyncBooleanProbe(Dispatchers.IO) {
            calls.incrementAndGet()
            probeThread.set(Thread.currentThread())
            true
        }

        val results = coroutineScope {
            listOf(
                async { probe.await() },
                async { probe.await() }
            ).awaitAll()
        }

        assertEquals(listOf(true, true), results)
        assertEquals(1, calls.get())
        val workerThread = assertNotNull(probeThread.get())
        assertNotSame(callerThread, workerThread)
    }
}
