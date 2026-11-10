package com.android.purebilibili.core.player.waveform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AudioWaveformPolicyTest {

    @Test
    fun `analysis follows visible consumers and survives overlapping surfaces`() {
        val first = Any()
        val second = Any()
        try {
            assertTrue(!AudioWaveformSource.isAnalysisRequested())
            AudioWaveformSource.setConsumerActive(first, true)
            AudioWaveformSource.setConsumerActive(second, true)
            assertTrue(AudioWaveformSource.isAnalysisRequested())
            AudioWaveformSource.setConsumerActive(first, false)
            assertTrue(AudioWaveformSource.isAnalysisRequested())
            AudioWaveformSource.setConsumerActive(second, false)
            assertTrue(!AudioWaveformSource.isAnalysisRequested())
            AudioWaveformSource.setConsumerActive(first, true)
            assertTrue(AudioWaveformSource.isAnalysisRequested())
        } finally {
            AudioWaveformSource.setConsumerActive(first, false)
            AudioWaveformSource.setConsumerActive(second, false)
        }
    }

    @Test
    fun soundWaveHistoryCoversNearlyOneSecondWithoutShowingFutureAudio() {
        val samples = (1..100).map { AudioWaveformSample(it * 20_000L, it / 100f) }
        val history = resolveWaveformBars(samples, 1_200_000L, barCount = 48)
        assertEquals(48, history.size)
        assertEquals(.13f, history.first())
        assertEquals(.60f, history.last())
        assertTrue(history.all { it <= .60f })
        assertTrue(resolveWaveformBars(emptyList(), 1_200_000L, 48).all { it == 0f })
    }
    @Test
    fun `five bars follow audible history and never anticipate a future burst`() {
        val samples = (1..5).map { AudioWaveformSample(it * 20_000L, it / 5f) }
        assertTrue(resolveWaveformBars(samples, 0L).all { it == 0f })
        assertEquals(listOf(0.2f, 0.4f, 0.6f, 0.8f, 1f), resolveWaveformBars(samples, 100_000L).toList())
        assertTrue(resolveWaveformBars(samples, 300_000L).all { it == 0f }, "old levels must expire")
    }

    @Test
    fun `silence remains silent while loud and quiet signals remain distinct`() {
        assertEquals(0f, resolveWaveformLevel(0.0, 0.0))
        assertEquals(0f, resolveWaveformLevel(0.001, 0.002))
        assertEquals(0f, resolveWaveformLevel(Double.NaN, 0.0))
        assertTrue(resolveWaveformLevel(0.1, 0.2) < resolveWaveformLevel(0.5, 0.8))
        assertEquals(1f, resolveWaveformLevel(1.0, 1.0))
    }

    @Test
    fun `envelope timing is independent of refresh rate and pause settles to zero`() {
        fun advance(hz: Int, playing: Boolean): Float {
            var value = floatArrayOf(if (playing) 0f else 1f)
            repeat(hz / 5) { value = resolveWaveformFrame(value, floatArrayOf(1f), playing, 1000f / hz) }
            return value.single()
        }
        for (playing in listOf(true, false)) {
            assertEquals(advance(60, playing), advance(90, playing), 0.0001f)
            assertEquals(advance(60, playing), advance(120, playing), 0.0001f)
        }
        var value = floatArrayOf(1f)
        repeat(60) { value = resolveWaveformFrame(value, floatArrayOf(1f), false, 1000f / 60) }
        assertEquals(0f, value.single())
    }

    @Test
    fun `old player cannot publish or clear the new players timeline`() {
        val old = Any()
        val new = Any()
        AudioWaveformSource.claim(old)
        AudioWaveformSource.claim(new)
        AudioWaveformSource.publish(new, AudioWaveformSample(20_000L, 0.5f))
        AudioWaveformSource.publish(old, AudioWaveformSample(30_000L, 1f))
        AudioWaveformSource.release(old)
        assertEquals(0.5f, AudioWaveformSource.snapshot().samples.single().level)
        AudioWaveformSource.publish(new, AudioWaveformSample(10_000L, 0.2f))
        assertEquals(10_000L, AudioWaveformSource.snapshot().samples.single().timeUs)
        AudioWaveformSource.release(new)
        assertTrue(AudioWaveformSource.snapshot().samples.isEmpty())
    }
}
