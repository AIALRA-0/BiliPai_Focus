package com.android.purebilibili.core.ui.performance

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View

/**
 * Raises the display rate while the user is interacting, then releases that vote once input stops.
 *
 * Idle does not request 0Hz. A lit panel has no 0Hz mode; 0 here means "no preference".
 * [View.REQUESTED_FRAME_RATE_CATEGORY_NORMAL] is about 60Hz and would hold the panel there.
 * Releasing the vote lets the system fall to the panel floor (often 1Hz or 10Hz) after frames stop.
 * [android.view.WindowManager.LayoutParams.preferredRefreshRate] alone cannot do this: since API 34
 * it is a seamless-only hint, and it is ignored once a surface votes a frame rate.
 */
internal class InteractionRefreshRateController(
    private val activity: Activity,
    private val handler: Handler = Handler(Looper.getMainLooper()),
) {
    private var originalModeId = 0
    private var originalRefreshRate = 0f
    private var active = false
    private var touching = false
    private var appliedIntent: RefreshIntent? = null
    private var activeMode: DisplayRefreshMode? = null
    private val idleRunnable = Runnable { apply(RefreshIntent.IDLE) }

    fun onResume() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || active) return
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay
        } ?: return
        val currentMode = display.mode
        activeMode = resolveActiveRefreshMode(
            currentModeId = currentMode.modeId,
            supportedModes = display.supportedModes.map { mode ->
                DisplayRefreshMode(
                    modeId = mode.modeId,
                    refreshRate = mode.refreshRate,
                    width = mode.physicalWidth,
                    height = mode.physicalHeight,
                )
            },
        )
        val canVoteFrameRate = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
        if (activeMode == null && !canVoteFrameRate) return
        val window = activity.window
        originalModeId = window.attributes.preferredDisplayModeId
        originalRefreshRate = window.attributes.preferredRefreshRate
        if (canVoteFrameRate) {
            window.setFrameRatePowerSavingsBalanced(true)
            window.setFrameRateBoostOnTouchEnabled(true)
        }
        active = true
        appliedIntent = null
        touching = false
        scheduleIdle()
    }

    fun onPause() {
        if (!active) return
        handler.removeCallbacks(idleRunnable)
        touching = false
        restoreOriginal()
        activeMode = null
        appliedIntent = null
        active = false
    }

    fun onTouchEvent(event: MotionEvent) {
        if (!active) return
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touching = true
                handler.removeCallbacks(idleRunnable)
                apply(RefreshIntent.ACTIVE)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touching = false
                scheduleIdle()
            }
        }
    }

    fun onOtherInteraction() {
        if (!active || touching) return
        apply(RefreshIntent.ACTIVE)
        scheduleIdle()
    }

    private fun scheduleIdle() {
        handler.removeCallbacks(idleRunnable)
        handler.postDelayed(idleRunnable, IDLE_DELAY_MS)
    }

    private fun apply(intent: RefreshIntent) {
        if (!active || appliedIntent == intent) return
        val window = activity.window
        val mode = if (intent == RefreshIntent.ACTIVE) activeMode else null
        window.attributes = window.attributes.apply {
            preferredDisplayModeId = mode?.modeId ?: 0
            preferredRefreshRate = mode?.refreshRate ?: 0f
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.decorView.setRequestedFrameRate(
                if (intent == RefreshIntent.ACTIVE) {
                    View.REQUESTED_FRAME_RATE_CATEGORY_HIGH
                } else {
                    View.REQUESTED_FRAME_RATE_CATEGORY_NO_PREFERENCE
                },
            )
            // The vote is applied on the next draw. One invalidate publishes it; further
            // frames are what keep the panel above its floor.
            window.decorView.invalidate()
        }
        appliedIntent = intent
    }

    private fun restoreOriginal() {
        val window = activity.window
        window.attributes = window.attributes.apply {
            preferredDisplayModeId = originalModeId
            preferredRefreshRate = originalRefreshRate
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.decorView.setRequestedFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_DEFAULT)
        }
    }

    private enum class RefreshIntent { IDLE, ACTIVE }

    private companion object {
        // Let a fling finish before releasing the high rate.
        const val IDLE_DELAY_MS = 3_000L
    }
}

internal data class DisplayRefreshMode(
    val modeId: Int,
    val refreshRate: Float,
    val width: Int,
    val height: Int,
)

/** Fastest mode at the current resolution, when a slower mode exists to return to. */
internal fun resolveActiveRefreshMode(
    currentModeId: Int,
    supportedModes: List<DisplayRefreshMode>,
): DisplayRefreshMode? {
    val current = supportedModes.firstOrNull { it.modeId == currentModeId } ?: return null
    val sameSize = supportedModes.filter { it.width == current.width && it.height == current.height }
    val fastest = sameSize.maxWithOrNull(
        compareBy<DisplayRefreshMode> { it.refreshRate }.thenByDescending { it.modeId },
    ) ?: return null
    val hasSlower = sameSize.any { it.refreshRate + 1f < fastest.refreshRate }
    if (!hasSlower) return null
    return fastest
}
