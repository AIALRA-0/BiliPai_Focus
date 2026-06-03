package com.android.purebilibili.core.ui.common

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

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
 *  长按复制文本修饰符
 * 
 * @param text 要复制的文本内容
 * @param label 复制成功提示中显示的文本描述（可选，例如 "视频链接"）
 */
fun Modifier.copyOnLongPress(
    text: String,
    label: String? = null
): Modifier = composed {
    val copyToClipboard = rememberClipboardCopyHandler()
    if (text.isBlank()) return@composed this
    
    pointerInput(text) {
        detectTapGestures(
            onLongPress = {
                copyToClipboard(text, label)
            }
        )
    }
}

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
