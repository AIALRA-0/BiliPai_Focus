package com.android.purebilibili.core.player.waveform

internal data class AudioWaveformSnapshot(
    val samples: List<AudioWaveformSample> = emptyList(),
    val positionUs: Long = -1L,
    val updatedAtNanos: Long = 0L,
)

/** 音频线程发布不可变快照，界面只读。所有权隔离防止旧播放器释放时清空新会话。 */
internal object AudioWaveformSource {
    private var owner: Any? = null
    private val consumers = mutableSetOf<Any>()
    @Volatile private var analysisRequested = false
    @Volatile private var current = AudioWaveformSnapshot()

    @Synchronized
    fun setConsumerActive(token: Any, active: Boolean) {
        if (active) consumers.add(token) else consumers.remove(token)
        analysisRequested = consumers.isNotEmpty()
        if (!analysisRequested) current = AudioWaveformSnapshot()
    }

    fun isAnalysisRequested(): Boolean = analysisRequested

    @Synchronized
    fun claim(token: Any) {
        owner = token
        current = AudioWaveformSnapshot()
    }

    @Synchronized
    fun publish(token: Any, sample: AudioWaveformSample) {
        if (owner !== token) return
        val history = if (current.samples.lastOrNull()?.timeUs?.let { it >= sample.timeUs } == true) {
            // 时间戳倒退说明进入了新片段；不能把 seek 前后的声音拼在一起。
            current = AudioWaveformSnapshot()
            emptyList()
        } else current.samples
        current = current.copy(samples = (history + sample).takeLast(AUDIO_WAVEFORM_HISTORY_LIMIT))
    }

    @Synchronized
    fun updatePosition(token: Any, positionUs: Long) {
        if (owner !== token || positionUs < 0L) return
        current = current.copy(positionUs = positionUs, updatedAtNanos = System.nanoTime())
    }

    @Synchronized
    fun release(token: Any) {
        if (owner === token) reset()
    }

    @Synchronized
    fun reset() {
        owner = null
        current = AudioWaveformSnapshot()
    }

    fun snapshot(): AudioWaveformSnapshot = current
}
