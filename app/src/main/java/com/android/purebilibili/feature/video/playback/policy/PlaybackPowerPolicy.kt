package com.android.purebilibili.feature.video.playback.policy

import androidx.media3.common.C
import com.android.purebilibili.core.store.SettingsManager

internal fun resolvePlaybackWakeMode(
    miniPlayerMode: SettingsManager.MiniPlayerMode,
    stopPlaybackOnExit: Boolean
): Int {
    if (stopPlaybackOnExit) return C.WAKE_MODE_NONE
    return when (miniPlayerMode) {
        SettingsManager.MiniPlayerMode.SYSTEM_PIP,
        SettingsManager.MiniPlayerMode.IN_APP_AND_SYSTEM_PIP -> C.WAKE_MODE_NETWORK
        SettingsManager.MiniPlayerMode.OFF,
        SettingsManager.MiniPlayerMode.IN_APP_ONLY -> C.WAKE_MODE_LOCAL
    }
}
