package com.android.purebilibili.feature.video.ui.overlay

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.widget.Toast
import androidx.media3.common.Player
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Timer lifetime follows the playback session, not a transient settings/overlay composition. */
internal class PiliPlayerSleepTimer private constructor(player: Player, context: Context) {
    private val playerRef = WeakReference(player)
    private val appContext = context.applicationContext
    private var activityRef = WeakReference<Activity>(null)
    private val selectedMinutes = MutableStateFlow<Int?>(null)
    val minutes = selectedMinutes.asStateFlow()
    private var job: Job? = null
    private var waiting = false
    var waitForEnd: Boolean = false
        set(value) {
            field = value
            if (!value && waiting) finish()
        }
    var exitApp: Boolean = false
    private val endedListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            // 到点后暂停、空闲、错误或自然结束都不再无限等待。
            if (waiting && !shouldWaitForSleepTimerEnd(
                    waitForEnd = true,
                    playWhenReady = player.playWhenReady,
                    playbackState = player.playbackState,
                    hasError = player.playerError != null,
                )) finish()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            // 单集循环不发送 ENDED；自动换集或循环回起点同样视为本集结束。
            if (waiting && reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) finish()
        }
    }

    fun attachActivity(activity: Activity?) { activityRef = WeakReference(activity) }

    fun setTimer(minutes: Int?) {
        cancel()
        val duration = minutes?.takeIf { it > 0 } ?: return
        selectedMinutes.value = duration
        val deadline = SystemClock.elapsedRealtime() + duration * 60_000L
        job = scope.launch {
            delay((deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
            val player = playerRef.get()
            if (player == null) { cancel(); return@launch }
            if (shouldWaitForSleepTimerEnd(
                    waitForEnd = waitForEnd,
                    playWhenReady = player.playWhenReady,
                    playbackState = player.playbackState,
                    hasError = player.playerError != null,
                )) {
                waiting = true
                player.addListener(endedListener)
            } else finish()
        }
    }

    private fun cancel() {
        job?.cancel()
        job = null
        waiting = false
        playerRef.get()?.removeListener(endedListener)
        selectedMinutes.value = null
    }

    private fun finish() {
        // 先清理等待监听，再暂停，避免播放状态回调重入 finish。
        val player = playerRef.get()
        cancel()
        player?.pause()
        if (exitApp) activityRef.get()?.finishAffinity()
        else Toast.makeText(appContext, "定时结束，已暂停播放", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val sessions = WeakHashMap<Player, PiliPlayerSleepTimer>()
        fun forPlayer(player: Player, context: Context): PiliPlayerSleepTimer =
            sessions.getOrPut(player) { PiliPlayerSleepTimer(player, context) }
    }
}

/** 仅对仍请求播放且能够继续到结尾的会话等待本集结束。 */
internal fun shouldWaitForSleepTimerEnd(
    waitForEnd: Boolean,
    playWhenReady: Boolean,
    playbackState: Int,
    hasError: Boolean,
): Boolean = waitForEnd && playWhenReady && !hasError &&
    (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING)
