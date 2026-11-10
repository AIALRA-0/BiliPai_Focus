package com.android.purebilibili.core.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputEventHandler
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.android.purebilibili.core.ui.LocalAppThemeConfig

@Composable
internal fun Modifier.globalTextTapCopy(text: String): Modifier {
    // Both native renderers share this gate; disabled copying adds no input node.
    if (text.isBlank() || !LocalAppThemeConfig.current.globalTextTapCopyEnabled) return this
    return this then GlobalTextTapCopyElement(text)
}

private data class GlobalTextTapCopyElement(val text: String) :
    ModifierNodeElement<GlobalTextTapCopyNode>() {
    override fun create() = GlobalTextTapCopyNode(text)

    override fun update(node: GlobalTextTapCopyNode) = node.updateText(text)

    override fun InspectorInfo.inspectableProperties() {
        name = "globalTextTapCopy"
        properties["text"] = text
    }
}

private class GlobalTextTapCopyNode(private var text: String) :
    DelegatingNode(), CompositionLocalConsumerModifierNode {
    override val shouldAutoInvalidate: Boolean = false

    private val input = delegate(SuspendingPointerInputModifierNode(PointerInputEventHandler {
        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Final,
            )
            val startPosition = down.position
            var gestureCanceled = down.isConsumed
            var upTimeMillis: Long? = null
            while (upTimeMillis == null && !gestureCanceled) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null) {
                    gestureCanceled = true
                    break
                }
                if (
                    change.isConsumed ||
                    (change.position - startPosition).getDistance() > viewConfiguration.touchSlop
                ) {
                    gestureCanceled = true
                }
                if (!change.pressed) upTimeMillis = change.uptimeMillis
            }
            val pressDurationMillis = (upTimeMillis ?: down.uptimeMillis) - down.uptimeMillis
            if (
                shouldCopyGlobalTextTap(
                    text = text,
                    globalCopyEnabled = true,
                    gestureCanceled = gestureCanceled,
                    pressDurationMillis = pressDurationMillis,
                    longPressTimeoutMillis = viewConfiguration.longPressTimeoutMillis,
                )
            ) {
                copyText()
            }
        }
    }))

    fun updateText(value: String) {
        if (text == value) return
        text = value
        // A recycled label must not finish a gesture that began on different content.
        input.resetPointerInputHandler()
    }

    private fun copyText() {
        val context = currentValueOf(LocalContext)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("BiliPai 文本", text.trim()))
        val config = currentValueOf(com.android.purebilibili.core.ui.LocalAppThemeConfig)
        if (config.hapticFeedbackEnabled && config.hapticSettings.isEnabled(com.android.purebilibili.core.ui.AppHapticScene.TEXT)) {
            val feedback = currentValueOf(LocalHapticFeedback)
            val textFeedback = (feedback as? com.android.purebilibili.core.ui.AppPolicyHapticFeedback)
                ?.forScene(com.android.purebilibili.core.ui.AppHapticScene.TEXT) ?: feedback
            textFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
        }
    }
}
