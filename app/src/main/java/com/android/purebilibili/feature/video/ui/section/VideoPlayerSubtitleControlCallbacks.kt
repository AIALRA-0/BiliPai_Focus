package com.android.purebilibili.feature.video.ui.section

import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.feature.video.subtitle.SubtitleDisplayMode
import com.android.purebilibili.feature.video.subtitle.resolveDefaultSubtitleDisplayMode
import com.android.purebilibili.feature.video.ui.overlay.SubtitleControlCallbacks

// 只组装字幕控制事件；显示偏好与字号状态仍由播放器持有，保存由宿主启动。
internal fun createPlayerSubtitleControlCallbacks(
    primarySelectable: Boolean,
    secondarySelectable: Boolean,
    onModePreferenceChange: (SubtitleDisplayMode) -> Unit,
    onTrackSelected: (String) -> Unit,
    onLargeTextChange: (Boolean) -> Unit,
    onPositionLockedChange: (Boolean) -> Unit,
): SubtitleControlCallbacks = SubtitleControlCallbacks(
    onDisplayModeChange = { mode ->
        Logger.d(
            "VideoPlayerSection",
            "字幕显示模式切换: mode=$mode"
        )
        onModePreferenceChange(mode)
    },
    onEnabledChange = { enabled ->
        Logger.d(
            "VideoPlayerSection",
            "字幕总开关切换: enabled=$enabled"
        )
        val nextMode = if (enabled) {
            resolveDefaultSubtitleDisplayMode(
                hasPrimaryTrack = primarySelectable,
                hasSecondaryTrack = secondarySelectable
            )
        } else {
            SubtitleDisplayMode.OFF
        }
        onModePreferenceChange(nextMode)
    },
    onTrackSelected = { trackKey ->
        onTrackSelected(trackKey)
    },
    onLargeTextChange = { enabled ->
        Logger.d(
            "VideoPlayerSection",
            "字幕大字号切换: enabled=$enabled"
        )
        onLargeTextChange(enabled)
    },
    onPositionLockedChange = { locked ->
        onPositionLockedChange(locked)
    }
)
