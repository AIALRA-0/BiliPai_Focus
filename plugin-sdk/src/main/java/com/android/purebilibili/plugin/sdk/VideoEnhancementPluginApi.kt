package com.android.purebilibili.plugin.sdk

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/** Persisted enhancement configuration; the plugin's global switch is managed by the host. */
@Serializable
data class Anime4KConfig(
    val algorithm: VideoEnhancementAlgorithm = VideoEnhancementAlgorithm.ANIME4K,
    val preset: Anime4KPreset = Anime4KPreset.FAST,
    /** User-facing sharpness from 0 to 1; the renderer converts it to RCAS stops. */
    val fsrSharpness: Float = DEFAULT_FSR_SHARPNESS,
    val rememberAcrossVideos: Boolean = false,
    val rememberedEnabled: Boolean = false
)

@Serializable
enum class VideoEnhancementAlgorithm {
    ANIME4K,
    FSR_1_0
}

@Serializable
enum class Anime4KPreset {
    FAST,
    QUALITY
}

const val DEFAULT_FSR_SHARPNESS: Float = 0.9f

interface VideoEnhancementPluginApi : Plugin {
    val configState: StateFlow<Anime4KConfig>
    fun setPreset(preset: Anime4KPreset)
    fun setAlgorithm(algorithm: VideoEnhancementAlgorithm)
    fun setFsrSharpness(strength: Float)
    fun setRememberAcrossVideos(enabled: Boolean, currentVideoEnabled: Boolean)
    fun rememberCurrentVideoEnabled(enabled: Boolean)
}
