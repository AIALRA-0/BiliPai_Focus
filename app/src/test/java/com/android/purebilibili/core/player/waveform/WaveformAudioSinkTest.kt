package com.android.purebilibili.core.player.waveform

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.audio.AudioSink
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class WaveformAudioSinkTest {
    @Test
    @Suppress("DEPRECATION")
    fun `legacy configure bridges to the new entry without dropping channel mapping`() {
        var forwarded: AudioSink.AudioSinkConfig? = null
        val delegate = Proxy.newProxyInstance(
            AudioSink::class.java.classLoader, arrayOf(AudioSink::class.java),
        ) { _, method, args ->
            if (method.name == "configure") forwarded = args?.first() as AudioSink.AudioSinkConfig
            null
        } as AudioSink
        val sink = WaveformAudioSink(delegate, enabled = { false })
        sink.configure(pcmFormat(), 4096, intArrayOf(0))
        assertEquals(pcmFormat(), forwarded?.format)
        assertEquals(4096, forwarded?.preferredBufferSizeOverride)
        assertEquals(0, forwarded?.outputChannelMapping?.get(0))
        sink.release()
    }

    @Test
    fun `existing session switches on and follows the output clock without duplicating retries`() {
        var enabled = false
        var outputPositionUs = 0L
        val sink = WaveformAudioSink(fakeAudioSink { outputPositionUs }, enabled = { enabled })
        AudioWaveformSource.reset()
        sink.configure(AudioSink.AudioSinkConfig.Builder(pcmFormat()).build())
        sink.play()
        val pcm = monoPcm(4410, 16_000)
        sink.handleBuffer(pcm, 1_000_000L, 1)
        assertTrue(AudioWaveformSource.snapshot().samples.isEmpty())
        enabled = true
        sink.handleBuffer(pcm, 1_000_000L, 1)
        assertEquals(5, AudioWaveformSource.snapshot().samples.size)
        assertEquals(0, pcm.position())
        sink.handleBuffer(pcm, 1_000_000L, 1)
        assertEquals(5, AudioWaveformSource.snapshot().samples.size, "retried buffers must be counted only once")
        sink.getCurrentPositionUs(false)
        assertTrue(AudioWaveformSource.snapshot().let { resolveWaveformBars(it.samples, it.positionUs).all { value -> value == 0f } })
        outputPositionUs = 1_100_000L
        sink.getCurrentPositionUs(false)
        assertTrue(AudioWaveformSource.snapshot().let { resolveWaveformBars(it.samples, it.positionUs).all { value -> value > 0f } })
        enabled = false
        sink.handleBuffer(monoPcm(882, 16_000), 1_100_000L, 1)
        assertTrue(AudioWaveformSource.snapshot().samples.isEmpty())
        sink.release()
    }

    @Test
    fun `pause and discontinuity clear history while compressed passthrough remains untouched`() {
        val sink = WaveformAudioSink(fakeAudioSink { 0L }, enabled = { true })
        AudioWaveformSource.reset()
        sink.configure(AudioSink.AudioSinkConfig.Builder(pcmFormat()).build())
        sink.play()
        sink.handleBuffer(monoPcm(882, 16_000), 0L, 1)
        assertTrue(AudioWaveformSource.snapshot().samples.isNotEmpty())
        sink.pause()
        assertTrue(AudioWaveformSource.snapshot().samples.isEmpty())
        sink.play()
        sink.handleBuffer(monoPcm(882, 16_000), 20_000L, 1)
        sink.handleDiscontinuity()
        assertTrue(AudioWaveformSource.snapshot().samples.isEmpty())
        sink.configure(AudioSink.AudioSinkConfig.Builder(
            pcmFormat().buildUpon().setSampleMimeType(MimeTypes.AUDIO_E_AC3).build(),
        ).build())
        val encoded = monoPcm(882, 16_000)
        sink.handleBuffer(encoded, 0L, 1)
        assertTrue(AudioWaveformSource.snapshot().samples.isEmpty())
        assertEquals(0, encoded.position())
        sink.release()
    }
}

private fun pcmFormat(): Format = Format.Builder()
    .setSampleMimeType(MimeTypes.AUDIO_RAW)
    .setSampleRate(44_100)
    .setChannelCount(1)
    .setPcmEncoding(C.ENCODING_PCM_16BIT)
    .build()

private fun fakeAudioSink(position: () -> Long): AudioSink = Proxy.newProxyInstance(
    AudioSink::class.java.classLoader,
    arrayOf(AudioSink::class.java),
) { _, method, _ ->
    when (method.name) {
        "handleBuffer" -> false
        "getCurrentPositionUs" -> position()
        else -> null
    }
} as AudioSink
