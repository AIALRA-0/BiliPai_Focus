package com.android.purebilibili.core.player.waveform

import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.pow

internal const val AUDIO_WAVEFORM_BAR_COUNT = 5
internal const val AUDIO_WAVEFORM_WINDOWS_PER_SECOND = 50
internal const val AUDIO_WAVEFORM_IDLE_EPSILON = 0.004f
internal const val AUDIO_WAVEFORM_STALE_NANOS = 250_000_000L
internal const val AUDIO_WAVEFORM_HISTORY_LIMIT = 256

internal data class AudioWaveformSample(val timeUs: Long, val level: Float)

/** RMS 为主、少量峰值保留瞬态；固定 dB 范围避免安静段被自动增益放大。 */
internal fun resolveWaveformLevel(rms: Double, peak: Double): Float {
    val amplitude = rms * 0.85 + peak * 0.15
    if (!amplitude.isFinite() || amplitude <= 0.00316227766) return 0f // -50 dB
    val normalized = ((20.0 * log10(amplitude) + 50.0) / 44.0).coerceIn(0.0, 1.0)
    return normalized.pow(1.5).toFloat()
}

/** 五柱从左至右显示最近约 100ms 的声音；不显示尚未播放的解码数据。 */
internal fun resolveWaveformBars(
    samples: List<AudioWaveformSample>,
    positionUs: Long,
    barCount: Int = AUDIO_WAVEFORM_BAR_COUNT,
): FloatArray {
    if (barCount <= 0) return FloatArray(0)
    val result = FloatArray(barCount)
    if (positionUs < 0L) return result
    val stepUs = 1_000_000L / AUDIO_WAVEFORM_WINDOWS_PER_SECOND
    var index = samples.lastIndex
    for (bar in barCount - 1 downTo 0) {
        val timeUs = positionUs - (barCount - 1 - bar) * stepUs
        while (index >= 0 && samples[index].timeUs > timeUs) index--
        if (index >= 0 && timeUs - samples[index].timeUs < stepUs * 2) {
            result[bar] = samples[index].level
        }
    }
    return result
}

internal fun isWaveformSnapshotFresh(updatedAtNanos: Long, nowNanos: Long): Boolean =
    updatedAtNanos != 0L && nowNanos - updatedAtNanos in 0L..AUDIO_WAVEFORM_STALE_NANOS

/** 时间常数以毫秒定义，刷新率改变不会改变起落节奏。 */
internal fun resolveWaveformFrame(
    current: FloatArray,
    target: FloatArray,
    playing: Boolean,
    elapsedMs: Float,
): FloatArray = FloatArray(current.size) { index ->
    val from = current[index].coerceIn(0f, 1f)
    val to = if (playing) target.getOrElse(index) { 0f }.coerceIn(0f, 1f) else 0f
    val timeConstantMs = if (!playing) 70f else if (to > from) 32f else 110f
    val coefficient = 1f - exp(-elapsedMs.coerceAtLeast(0f) / timeConstantMs)
    val next = from + (to - from) * coefficient
    if (to == 0f && next < AUDIO_WAVEFORM_IDLE_EPSILON) 0f else next
}
