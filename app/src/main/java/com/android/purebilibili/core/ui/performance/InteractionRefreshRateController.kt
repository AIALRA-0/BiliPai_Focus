package com.android.purebilibili.core.ui.performance

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import kotlin.math.abs

/** Keeps the window responsive during input without pinning a high display mode while idle. */
internal class InteractionRefreshRateController(
    private val activity: Activity,
    private val handler: Handler = Handler(Looper.getMainLooper()),
) {
    private var originalRefreshRate = 0f
    private var active = false
    private var touching = false
    private var requestedRefreshRate: Float? = null
    private var rates: InteractionRefreshRates? = null
    private val idleRunnable = Runnable { requestRate(rates?.idleRate) }

    fun onResume() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || active) return
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay
        } ?: return
        val currentMode = display.mode
        rates = resolveInteractionRefreshRates(
            display.supportedModes
                .filter { it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight }
                .map { it.refreshRate },
        )
        if (rates == null) return
        originalRefreshRate = activity.window.attributes.preferredRefreshRate
        active = true
        requestedRefreshRate = null
        touching = false
        scheduleIdle()
    }

    fun onPause() {
        if (!active) return
        handler.removeCallbacks(idleRunnable)
        touching = false
        requestRate(originalRefreshRate)
        rates = null
        requestedRefreshRate = null
        active = false
    }

    fun onTouchEvent(event: MotionEvent) {
        if (!active) return
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touching = true
                handler.removeCallbacks(idleRunnable)
                requestRate(rates?.activeRate)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touching = false
                scheduleIdle()
            }
        }
    }

    fun onOtherInteraction() {
        if (!active || touching) return
        requestRate(rates?.activeRate)
        scheduleIdle()
    }

    private fun scheduleIdle() {
        handler.removeCallbacks(idleRunnable)
        handler.postDelayed(idleRunnable, IDLE_DELAY_MS)
    }

    private fun requestRate(rate: Float?) {
        if (!active || rate == null || requestedRefreshRate == rate) return
        activity.window.attributes = activity.window.attributes.apply { preferredRefreshRate = rate }
        requestedRefreshRate = rate
    }

    private companion object {
        // Let a fling finish before requesting the lower rate.
        const val IDLE_DELAY_MS = 3_000L
    }
}

internal data class InteractionRefreshRates(val idleRate: Float, val activeRate: Float)

internal fun resolveInteractionRefreshRates(supportedRates: List<Float>): InteractionRefreshRates? {
    val idle = supportedRates.minByOrNull { abs(it - 60f) }
        ?.takeIf { abs(it - 60f) <= 1f } ?: return null
    val active = supportedRates.filter { it > idle + 1f }.maxOrNull() ?: return null
    return InteractionRefreshRates(idleRate = idle, activeRate = active)
}
