package com.android.purebilibili.feature.video.ui.overlay

import com.android.purebilibili.feature.video.subtitle.SubtitleDisplayMode
import com.android.purebilibili.feature.video.subtitle.SubtitleTrackOption

data class SubtitleControlUiState(
    val trackAvailable: Boolean = false,
    val primaryAvailable: Boolean = false,
    val secondaryAvailable: Boolean = false,
    val enabled: Boolean = true,
    val displayMode: SubtitleDisplayMode = SubtitleDisplayMode.OFF,
    val primaryLabel: String = "中文",
    val secondaryLabel: String = "英文",
    val trackOptions: List<SubtitleTrackOption> = emptyList(),
    val largeTextEnabled: Boolean = false
)

data class SubtitleControlCallbacks(
    val onDisplayModeChange: (SubtitleDisplayMode) -> Unit = {},
    val onEnabledChange: (Boolean) -> Unit = {},
    val onTrackSelected: (String) -> Unit = {},
    val onLargeTextChange: (Boolean) -> Unit = {}
)
