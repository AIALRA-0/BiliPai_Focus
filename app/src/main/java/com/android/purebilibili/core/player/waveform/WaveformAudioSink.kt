package com.android.purebilibili.core.player.waveform

import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import java.nio.ByteBuffer

/**
 * 在所有 Hi-Res 播放器共用的输出入口旁路读取 PCM，复用、转入听视频时不漏接。
 * 不向处理链注入常驻 AudioProcessor，不改变关闭开关时的音频格式支持或输出路径。
 * 开关按缓冲读取，已有 PCM 会话可立即开启／关闭；压缩直通数据绝不当作 PCM 分析。
 */
@OptIn(UnstableApi::class)
internal class WaveformAudioSink(
    delegate: AudioSink,
    private val enabled: () -> Boolean,
) : ForwardingAudioSink(delegate) {
    private var format: Format? = null
    private var playing = false
    private var collector: AudioWaveformCollector? = null
    private val token = Any()
    private var lastBuffer: ByteBuffer? = null
    private var lastPresentationTimeUs = Long.MIN_VALUE

    override fun configure(audioSinkConfig: AudioSink.AudioSinkConfig) {
        // Media3 1.11 的旧三参数重载已 final，并会转发到此入口。
        // 原样转发完整配置，保留声道映射、缓冲与播放时间线。
        super.configure(audioSinkConfig)
        format = audioSinkConfig.format
        clearAnalysis()
    }

    override fun play() {
        super.play()
        playing = true
    }

    override fun pause() {
        super.pause()
        playing = false
        clearAnalysis()
    }

    override fun handleBuffer(buffer: ByteBuffer, presentationTimeUs: Long, encodedAccessUnitCount: Int): Boolean {
        val inputFormat = format
        val shouldAnalyze = playing && enabled() && inputFormat?.sampleMimeType == MimeTypes.AUDIO_RAW
        if (shouldAnalyze && inputFormat != null) {
            if (collector == null) {
                collector = AudioWaveformCollector(
                    inputFormat.sampleRate, inputFormat.channelCount, inputFormat.pcmEncoding,
                ) { sample -> AudioWaveformSource.publish(token, sample) }
                AudioWaveformSource.claim(token)
            }
            // AudioSink 可分多次消费同一缓冲，每个缓冲只统计一次。
            if (lastBuffer !== buffer || lastPresentationTimeUs != presentationTimeUs) {
                collector?.handleBuffer(buffer, presentationTimeUs)
                lastBuffer = buffer
                lastPresentationTimeUs = presentationTimeUs
            }
        } else if (collector != null) {
            clearAnalysis()
        }
        val consumed = super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
        if (consumed) lastBuffer = null
        return consumed
    }

    override fun getCurrentPositionUs(sourceEnded: Boolean): Long {
        val positionUs = super.getCurrentPositionUs(sourceEnded)
        if (collector != null) AudioWaveformSource.updatePosition(token, positionUs)
        return positionUs
    }

    override fun handleDiscontinuity() {
        super.handleDiscontinuity()
        clearAnalysis()
    }

    override fun flush() {
        super.flush()
        clearAnalysis()
    }

    override fun reset() {
        super.reset()
        playing = false
        clearAnalysis()
        format = null
    }

    override fun release() {
        super.release()
        playing = false
        clearAnalysis()
        format = null
    }

    private fun clearAnalysis() {
        AudioWaveformSource.release(token)
        collector = null
        lastBuffer = null
        lastPresentationTimeUs = Long.MIN_VALUE
    }
}
