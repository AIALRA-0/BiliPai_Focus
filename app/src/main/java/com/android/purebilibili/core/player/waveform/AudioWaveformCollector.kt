package com.android.purebilibili.core.player.waveform

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.WaveformAudioBufferSink
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

/** Media3 官方统计每个声道的振幅，合并声道能量而非信号，避免反相立体声抵消。 */
@OptIn(UnstableApi::class)
internal class AudioWaveformCollector(
    private val sampleRateHz: Int,
    private val channelCount: Int,
    private val encoding: Int,
    private val publish: (AudioWaveformSample) -> Unit,
) {
    private var waveform: WaveformAudioBufferSink? = null
    private var originUs = 0L
    private var framesPublished = 0L
    private var expectedNextTimeUs: Long? = null
    private var channelEnergy = 0.0
    private var channelPeak = 0.0
    private var floatBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    private val bytesPerSample = when (encoding) {
        C.ENCODING_PCM_16BIT -> 2
        C.ENCODING_PCM_24BIT -> 3
        C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 4
        else -> 0
    }
    val isSupported: Boolean = bytesPerSample > 0 && sampleRateHz >= AUDIO_WAVEFORM_WINDOWS_PER_SECOND && channelCount > 0

    fun handleBuffer(buffer: ByteBuffer, presentationTimeUs: Long) {
        if (!isSupported) return
        val frameBytes = bytesPerSample * channelCount
        val frameCount = buffer.remaining() / frameBytes
        if (frameCount == 0) return
        val expected = expectedNextTimeUs
        if (waveform == null || (expected != null && abs(presentationTimeUs - expected) > 50_000L)) {
            // 官方 flush 不清除已累积的半窗口；重新创建以隔离 seek、切曲和格式切换。
            originUs = presentationTimeUs
            framesPublished = 0L
            channelEnergy = 0.0
            channelPeak = 0.0
            waveform = createWaveformSink()
        }
        expectedNextTimeUs = presentationTimeUs + frameCount * 1_000_000L / sampleRateHz
        val input = buffer.asReadOnlyBuffer().order(ByteOrder.nativeOrder())
        input.limit(input.position() + frameCount * frameBytes)
        // 官方 AudioMixingUtil 只接收 16 位和 Float。高位深仅转换旁路副本，不改音频输出。
        val readable = if (encoding == C.ENCODING_PCM_24BIT || encoding == C.ENCODING_PCM_32BIT) {
            val requiredBytes = frameCount * channelCount * 4
            if (floatBuffer.capacity() < requiredBytes) {
                floatBuffer = ByteBuffer.allocateDirect(requiredBytes).order(ByteOrder.nativeOrder())
            }
            floatBuffer.apply {
                clear()
                repeat(frameCount * channelCount) {
                    val value = if (encoding == C.ENCODING_PCM_32BIT) {
                        input.int / 2_147_483_648f
                    } else {
                        val packed = (input.get().toInt() and 0xff) or
                            ((input.get().toInt() and 0xff) shl 8) or (input.get().toInt() shl 16)
                        packed / 8_388_608f
                    }
                    putFloat(value)
                }
                flip()
            }
        } else input
        waveform?.handleBuffer(readable)
    }

    private fun createWaveformSink(): WaveformAudioBufferSink =
        WaveformAudioBufferSink(AUDIO_WAVEFORM_WINDOWS_PER_SECOND, channelCount) { channel, bar ->
            channelEnergy += bar.rootMeanSquare * bar.rootMeanSquare
            channelPeak = maxOf(channelPeak, abs(bar.minSampleValue), abs(bar.maxSampleValue))
            if (channel == channelCount - 1) {
                framesPublished += bar.sampleCount
                publish(AudioWaveformSample(
                    timeUs = originUs + framesPublished * 1_000_000L / sampleRateHz,
                    level = resolveWaveformLevel(sqrt(channelEnergy / channelCount), channelPeak),
                ))
                channelEnergy = 0.0
                channelPeak = 0.0
            }
        }.also {
            it.flush(sampleRateHz, channelCount,
                if (encoding == C.ENCODING_PCM_16BIT) C.ENCODING_PCM_16BIT else C.ENCODING_PCM_FLOAT)
        }
}
