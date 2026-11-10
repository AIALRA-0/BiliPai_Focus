package com.android.purebilibili.feature.audio.lyrics.halcyon

/**
 * Halcyon lyric/player visual defaults, hardcoded to the reference app's stock values
 * so BiliPai renders like Halcyon without wiring Halcyon's SettingsManager.
 */
internal object HalcyonLyricSettings {
    /** SettingsManager.DEFAULT_APPLE_MUSIC_LYRICS_SUSTAIN_THRESHOLD_MS */
    const val appleMusicLyricsSustainThresholdMs: Int = 1_200

    /** SettingsManager.DEFAULT_PLAYER_APPLE_FLOW_SPEED */
    const val playerAppleFlowSpeed: Int = 10

    /** SettingsManager.lyricPauseCurrentOnly default */
    const val lyricPauseCurrentOnly: Boolean = true

    /** SettingsManager.lyricNonCurrentBlurPercent default */
    const val lyricNonCurrentBlurPercent: Int = 70

    /** SettingsManager.lyricWordSeekEnabled default */
    const val lyricWordSeekEnabled: Boolean = false

    /** SettingsManager.lyricTouchFeedbackEnabled default */
    const val lyricTouchFeedbackEnabled: Boolean = false

    /** SettingsManager.lyricPronunciationBelow default */
    const val lyricPronunciationBelow: Boolean = false

    /** SettingsManager.appleMusicLyricsFullTrailBreath default */
    const val appleMusicLyricsFullTrailBreath: Boolean = false

    /** lyric_hdr_highlight_enabled default */
    const val lyricHdrHighlightEnabled: Boolean = false

    /** lyric rainbow default */
    const val lyricRainbowEnabled: Boolean = false

    /** SettingsManager.LYRIC_COMPACT/WIDE_PRIMARY_TEXT_SIZE_DEFAULT_SP */
    const val primaryTextSizeSp: Float = 32f

    /** SettingsManager.LYRIC_*_SECONDARY_TEXT_SIZE_DEFAULT_SP */
    const val secondaryTextSizeSp: Float = 16f

    /** SettingsManager.lyricFontWeight default */
    const val lyricFontWeight: Int = 800
}

internal const val HALCYON_DEFAULT_SUSTAIN_THRESHOLD_MS: Int =
    HalcyonLyricSettings.appleMusicLyricsSustainThresholdMs

internal const val HALCYON_DEFAULT_APPLE_FLOW_SPEED: Int =
    HalcyonLyricSettings.playerAppleFlowSpeed

/** Halcyon `PlayerScreenHelpers.PLAYER_POSITION_BACKWARD_DRIFT_TOLERANCE_MS`. */
internal const val PLAYER_POSITION_BACKWARD_DRIFT_TOLERANCE_MS: Long = 600L
