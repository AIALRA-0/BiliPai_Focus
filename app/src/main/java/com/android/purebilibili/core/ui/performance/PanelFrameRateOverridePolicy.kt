package com.android.purebilibili.core.ui.performance

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * 面板实际刷新率的标签策略与采样钩子（诊断用）。
 *
 * LTPO 设备的系统覆盖监听（OnFrameRateOverrideListener）不在公开 SDK 内，
 * 这里退而采样 [android.view.Display.getMode] 的当前刷新率：它反映的是
 * SurfaceFlinger 实际切到的档位，足以区分"停在 60"与"已上 120"。
 * 轮询仅在组合存活（调试浮层可见）期间进行。
 */
internal fun resolvePanelFrameRateOverrideLabel(refreshRate: Float?): String {
    val rate = refreshRate ?: return ""
    if (rate <= 0f) return ""
    val rounded = rate.roundToInt().toFloat()
    val rateText = if (abs(rate - rounded) < 0.05f) {
        rounded.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", rate)
    }
    return "$rateText Hz（面板）"
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private const val PANEL_FRAME_RATE_SAMPLE_INTERVAL_MS = 500L

@Composable
internal fun rememberPanelFrameRateLabel(active: Boolean): String {
    val activity = LocalContext.current.findActivity()
    var refreshRate by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(activity, active) {
        if (!active) return@LaunchedEffect
        while (true) {
            refreshRate = resolvePanelDisplayRefreshRate(activity)
            delay(PANEL_FRAME_RATE_SAMPLE_INTERVAL_MS)
        }
    }
    return resolvePanelFrameRateOverrideLabel(refreshRate.takeIf { it > 0f })
}

private fun resolvePanelDisplayRefreshRate(activity: Activity?): Float {
    if (activity == null) return 0f
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        activity.display?.mode?.refreshRate ?: 0f
    } else {
        @Suppress("DEPRECATION")
        activity.windowManager.defaultDisplay?.mode?.refreshRate ?: 0f
    }
}
