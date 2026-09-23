package com.android.purebilibili.core.ui.common

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.hypot

/** 列表场景长按复制默认超时：高于系统 ~400ms，降低滑动停留误触。 */
internal const val LIST_LONG_PRESS_COPY_TIMEOUT_MS = 700L

/**
 * 长按复制期间手指位移超过该倍数的 [touchSlop] 则取消复制（交给滚动）。
 */
internal const val LONG_PRESS_COPY_SLOP_MULTIPLIER = 1f

/**
 * 解析长按复制超时。
 * [explicitTimeoutMs] 优先；否则用系统 longPressTimeout，并可用 [minTimeoutMs] 抬高下限。
 */
internal fun resolveLongPressCopyTimeoutMs(
    systemLongPressTimeoutMs: Long,
    explicitTimeoutMs: Long? = null,
    minTimeoutMs: Long = 0L,
): Long {
    val base = explicitTimeoutMs ?: systemLongPressTimeoutMs.coerceAtLeast(1L)
    return base.coerceAtLeast(minTimeoutMs.coerceAtLeast(0L))
}

/**
 * 按下期间位移是否已超出长按复制允许范围。
 */
internal fun hasExceededLongPressCopySlop(
    start: Offset,
    current: Offset,
    touchSlopPx: Float,
    slopMultiplier: Float = LONG_PRESS_COPY_SLOP_MULTIPLIER,
): Boolean {
    val dx = current.x - start.x
    val dy = current.y - start.y
    val limit = touchSlopPx * slopMultiplier.coerceAtLeast(0f)
    return hypot(dx.toDouble(), dy.toDouble()) > limit
}

@Composable
fun rememberClipboardCopyHandler(): (String, String?) -> Unit {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    return remember(context, haptic) {
        { rawText: String, label: String? ->
            val text = rawText.trim()
            if (text.isNotEmpty()) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                copyPlainTextToClipboard(context, text, label ?: "BiliPai")
                val toastMsg = if (label != null) "已复制 $label" else "已复制到剪贴板"
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                } else if (label != null) {
                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

/**
 * 文本长按划选修饰符。
 * 全局已由 [ProvideAppTextSelectionHost] 与 [androidx.compose.foundation.text.selection.SelectionContainer]
 * 统一接管原生水滴游标原地划选及实时悬浮工具条。
 * 此处保留方法签名以兼容各业务组件与架构测试规范，不再挂载拦截性 pointerInput，完全放行手势给原生划选。
 */
fun Modifier.copyOnLongPress(
    text: String,
    label: String? = null,
    longPressTimeoutMillis: Long? = null,
): Modifier = this

/**
 * 单击复制文本修饰符
 *
 * @param text 要复制的文本内容
 * @param label 复制成功提示中显示的文本描述（可选）
 */
fun Modifier.copyOnClick(
    text: String,
    label: String? = null
): Modifier = composed {
    val copyToClipboard = rememberClipboardCopyHandler()
    if (text.isBlank()) return@composed this

    clickable {
        copyToClipboard(text, label)
    }
}
