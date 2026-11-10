// File: core/player/LoudnessNormalizationAudioProcessor.kt
package com.android.purebilibili.core.player

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 响度均衡（滚动 AGC）：按缓冲块估计 RMS 响度，向目标响度平滑收敛增益，
 * 增益区间受限并做削波保护。不同曲目间响度差异大时，听感音量趋于一致。
 *
 * 纯 Kotlin 实现，仅在启用「响度均衡」设置时注入音频输出链。
 */
@OptIn(UnstableApi::class)
internal class LoudnessNormalizationAudioProcessor(
    /** 目标 RMS（满幅 1.0 归一化），约等于 -20 dBFS。 */
    private val targetRms: Float = 0.10f,
    /** 增益上下限，避免对极小声片段过度放大或对高响度内容过度衰减。 */
    private val minGain: Float = 0.4f,
    private val maxGain: Float = 2.4f
) : AudioProcessor {

    private var inputAudioFormat: AudioFormat =
        AudioFormat(androidx.media3.common.Format.NO_VALUE, -1, C.ENCODING_INVALID)
    private var outputAudioFormat: AudioFormat = inputAudioFormat

    private var buffer: ByteBuffer = ByteBuffer.allocateDirect(0)
    private var inputEnded = false
    private var configured = false

    /** 平滑增益，逐缓冲收敛，避免爆音。 */
    private var gain = 1f

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        val encoding = inputAudioFormat.encoding
        if (encoding != C.ENCODING_PCM_16BIT && encoding != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        this.inputAudioFormat = inputAudioFormat
        outputAudioFormat = inputAudioFormat
        configured = true
        return outputAudioFormat
    }

    override fun isActive(): Boolean = configured

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val remaining = inputBuffer.remaining()
        ensureBufferCapacity(remaining)
        val rms = estimateRms(inputBuffer)
        if (rms > 1e-4f) {
            val desired = (targetRms / rms).coerceIn(minGain, maxGain)
            // 逐块 8% 收敛：快速跟住响度跳变，同时不产生可闻泵感
            gain += (desired - gain) * 0.08f
        }
        applyGain(inputBuffer, remaining)
    }

    override fun getOutput(): ByteBuffer = buffer

    override fun isEnded(): Boolean = inputEnded && !buffer.hasRemaining()

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun flush() {
        buffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
        inputEnded = false
        // 换曲时保留增益，减少曲目内的初始收敛时间；跨曲目响度差异由 AGC 重新跟随
    }

    override fun reset() {
        flush()
        configured = false
        gain = 1f
    }

    private fun ensureBufferCapacity(size: Int) {
        if (buffer.capacity() < size) {
            buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        } else {
            buffer.clear()
        }
    }

    private fun estimateRms(source: ByteBuffer): Float {
        val position = source.position()
        val limit = source.limit()
        if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) {
            val count = (limit - position) / 4
            if (count == 0) return 0f
            var sum = 0.0
            repeat(count) { index ->
                val sample = source.getFloat(position + index * 4)
                sum += sample.toDouble() * sample.toDouble()
            }
            return sqrt(sum / count).toFloat()
        }
        val count = (limit - position) / 2
        if (count == 0) return 0f
        var sum = 0.0
        repeat(count) { index ->
            val sample = source.getShort(position + index * 2).toInt()
            sum += (sample / 32768.0) * (sample / 32768.0)
        }
        return sqrt(sum / count).toFloat()
    }

    private fun applyGain(source: ByteBuffer, size: Int) {
        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_FLOAT -> {
                val count = size / 4
                repeat(count) { index ->
                    val sample = source.getFloat(index * 4)
                    val scaled = applyLimiter(sample * gain)
                    buffer.putFloat(scaled)
                }
            }
            else -> {
                val count = size / 2
                repeat(count) { index ->
                    val sample = source.getShort(index * 2).toInt()
                    val scaled = (sample * gain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    buffer.putShort(scaled.toShort())
                }
            }
        }
        source.position(source.limit())
        buffer.flip()
    }

    private fun applyLimiter(sample: Float): Float = max(-1f, min(1f, sample))
}
