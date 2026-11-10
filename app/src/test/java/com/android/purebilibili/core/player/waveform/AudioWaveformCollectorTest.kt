package com.android.purebilibili.core.player.waveform

import androidx.media3.common.C
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class AudioWaveformCollectorTest {
    @Test
    fun `official rms keeps out of phase stereo audible without consuming pcm`() {
        val samples = mutableListOf<AudioWaveformSample>()
        val collector = AudioWaveformCollector(44_100, 2, C.ENCODING_PCM_16BIT, samples::add)
        val pcm = ByteBuffer.allocateDirect(882 * 4).order(ByteOrder.nativeOrder()).apply {
            repeat(882) { putShort(16_000); putShort(-16_000) }
            flip()
        }
        collector.handleBuffer(pcm, 1_000_000L)
        assertEquals(0, pcm.position())
        assertEquals(1, samples.size)
        assertEquals(1_020_000L, samples.single().timeUs)
        assertTrue(samples.single().level > 0.5f, "opposite phase channels must not cancel the indicator")
    }

    @Test
    fun `partial windows combine but a timestamp jump discards the old partial window`() {
        val samples = mutableListOf<AudioWaveformSample>()
        val collector = AudioWaveformCollector(44_100, 1, C.ENCODING_PCM_16BIT, samples::add)
        collector.handleBuffer(monoPcm(441, 16_000), 0L)
        assertTrue(samples.isEmpty())
        collector.handleBuffer(monoPcm(441, 16_000), 10_000L)
        assertTrue(samples.single().level > 0f)
        collector.handleBuffer(monoPcm(441, 16_000), 20_000L)
        collector.handleBuffer(monoPcm(882, 0), 2_000_000L)
        assertEquals(0f, samples.last().level, "seek must not carry a loud partial window into silence")
        assertEquals(2_020_000L, samples.last().timeUs)
    }

    @Test
    fun `high bit depth is converted only for analysis and unsupported pcm is skipped`() {
        for (encoding in listOf(C.ENCODING_PCM_24BIT, C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT)) {
            val samples = mutableListOf<AudioWaveformSample>()
            val collector = AudioWaveformCollector(44_100, 1, encoding, samples::add)
            val pcm = ByteBuffer.allocateDirect(882 * 4).order(ByteOrder.nativeOrder()).apply {
                repeat(882) {
                    when (encoding) {
                        C.ENCODING_PCM_24BIT -> { put(0); put(0); put(64) }
                        C.ENCODING_PCM_32BIT -> putInt(1 shl 30)
                        else -> putFloat(0.5f)
                    }
                }
                flip()
            }
            collector.handleBuffer(pcm, 0L)
            assertEquals(0, pcm.position())
            assertTrue(samples.single().level > 0.5f)
        }
        val unsupported = AudioWaveformCollector(44_100, 1, C.ENCODING_PCM_8BIT) { error("unsupported input") }
        assertTrue(!unsupported.isSupported)
        unsupported.handleBuffer(monoPcm(882, 0), 0L)
    }
}

internal fun monoPcm(frames: Int, amplitude: Short): ByteBuffer =
    ByteBuffer.allocateDirect(frames * 2).order(ByteOrder.nativeOrder()).apply {
        repeat(frames) { putShort(amplitude) }
        flip()
    }
