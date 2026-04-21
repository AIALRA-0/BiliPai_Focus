package com.android.purebilibili.feature.video.playback.session

import androidx.media3.common.Player
import com.android.purebilibili.core.store.PlayerSettingsCache
import com.android.purebilibili.feature.video.ui.overlay.PlaybackUserActionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.WeakHashMap

data class PendingPlaybackUserAction(
    val type: PlaybackUserActionType,
    val requestedAtMs: Long,
    val initialPlaybackState: Int,
    val initialPlayWhenReady: Boolean,
    val initialIsPlaying: Boolean,
    val initialPositionMs: Long
)

internal fun hasPlaybackUserActionReceivedResponse(
    action: PendingPlaybackUserAction,
    playbackState: Int,
    playWhenReady: Boolean,
    isPlaying: Boolean,
    currentPositionMs: Long
): Boolean {
    return when (action.type) {
        PlaybackUserActionType.PLAY -> {
            playWhenReady ||
                isPlaying ||
                playbackState == Player.STATE_BUFFERING ||
                playbackState != action.initialPlaybackState
        }

        PlaybackUserActionType.PAUSE -> {
            !playWhenReady ||
                !isPlaying ||
                currentPositionMs != action.initialPositionMs
        }
    }
}

internal object PlaybackUserActionTracker {
    private val lock = Any()
    private val pendingActions = WeakHashMap<Player, MutableStateFlow<PendingPlaybackUserAction?>>()

    fun stateFor(player: Player): StateFlow<PendingPlaybackUserAction?> {
        synchronized(lock) {
            return pendingActions.getOrPut(player) { MutableStateFlow(null) }
        }
    }

    fun recordAction(
        player: Player,
        type: PlaybackUserActionType,
        nowMs: Long = System.currentTimeMillis()
    ) {
        if (!PlayerSettingsCache.isPlayerDiagnosticLoggingEnabled()) {
            synchronized(lock) {
                pendingActions[player]?.value = null
            }
            return
        }
        synchronized(lock) {
            val state = pendingActions.getOrPut(player) { MutableStateFlow(null) }
            if (hasPlayerAlreadyReachedUserIntent(player = player, type = type)) {
                state.value = null
                return
            }
            state.value = PendingPlaybackUserAction(
                type = type,
                requestedAtMs = nowMs,
                initialPlaybackState = player.playbackState,
                initialPlayWhenReady = player.playWhenReady,
                initialIsPlaying = player.isPlaying,
                initialPositionMs = player.currentPosition
            )
        }
    }

    fun resolveIfResponded(
        player: Player,
        playbackState: Int,
        playWhenReady: Boolean,
        isPlaying: Boolean,
        currentPositionMs: Long
    ) {
        if (!PlayerSettingsCache.isPlayerDiagnosticLoggingEnabled()) {
            synchronized(lock) {
                pendingActions[player]?.value = null
            }
            return
        }
        synchronized(lock) {
            val state = pendingActions[player] ?: return
            val current = state.value ?: return
            if (hasPlaybackUserActionReceivedResponse(
                    action = current,
                    playbackState = playbackState,
                    playWhenReady = playWhenReady,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs
                )
            ) {
                state.value = null
            }
        }
    }

    fun clear(player: Player) {
        synchronized(lock) {
            pendingActions[player]?.value = null
            pendingActions.remove(player)
        }
    }
}

internal fun hasPlayerAlreadyReachedUserIntent(
    player: Player,
    type: PlaybackUserActionType
): Boolean {
    return hasPlaybackAlreadyReachedUserIntent(
        playbackState = player.playbackState,
        playWhenReady = player.playWhenReady,
        isPlaying = player.isPlaying,
        type = type
    )
}

internal fun hasPlaybackAlreadyReachedUserIntent(
    playbackState: Int,
    playWhenReady: Boolean,
    isPlaying: Boolean,
    type: PlaybackUserActionType
): Boolean {
    return when (type) {
        PlaybackUserActionType.PLAY -> {
            isPlaying ||
                (playWhenReady && playbackState == Player.STATE_BUFFERING)
        }

        PlaybackUserActionType.PAUSE -> {
            !playWhenReady && !isPlaying
        }
    }
}
