package com.android.purebilibili.core.store

/** 小窗、音频和画质编辑共用一次存储快照；仅保存真实配置，不作为加载占位。 */
internal data class PlaybackPreferencesSettings(
    val miniPlayerMode: SettingsManager.MiniPlayerMode,
    val stopPlaybackOnExit: Boolean,
    val backgroundPlaybackEnabled: Boolean,
    val audioFocusEnabled: Boolean,
    val audioModeAutoPipEnabled: Boolean,
    val audioNowPlayingBarEnabled: Boolean,
    val audioNowPlayingBarImmersiveEnabled: Boolean,
    val audioNowPlayingBarOpensAudioMode: Boolean,
    val musicLyricsUiStyle: SettingsManager.MusicLyricsUiStyle,
    val loudnessNormalizationEnabled: Boolean,
    val audioSpectrumEnabled: Boolean,
    val startupAutoPlayEnabled: Boolean,
    val wifiQuality: Int,
    val mobileQuality: Int,
    val autoHighestQualityEnabled: Boolean,
    val directedTrafficEnabled: Boolean,
    val videoCodecPreference: String,
    val videoSecondCodecPreference: String,
    val defaultAudioQuality: Int,
    val dataSaverMode: SettingsManager.DataSaverMode,
)
