package com.android.purebilibili.core.player

import androidx.media3.common.Player

object PlayerVolumeController {
    // 系统媒体流是唯一的用户音量来源，播放器自身只承担临时静音与恢复。
    fun preferredVolumeSync(): Float = 1f

    fun applyPreferredVolume(player: Player) {
        player.volume = preferredVolumeSync()
    }
}
