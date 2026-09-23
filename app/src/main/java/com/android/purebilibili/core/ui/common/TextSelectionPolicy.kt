package com.android.purebilibili.core.ui.common

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 文本选择与复制策略管理类
 */
object TextSelectionPolicy {

    /**
     * 解析文本选择底栏/对话框的标题
     */
    fun resolveTitle(customTitle: String?): String {
        return if (!customTitle.isNullOrBlank()) {
            customTitle
        } else {
            "选择文本"
        }
    }

    /**
     * 解析复制成功后的反馈文案
     */
    fun resolveCopyFeedbackMessage(label: String?): String {
        return if (!label.isNullOrBlank()) {
            "已复制 $label"
        } else {
            "已复制到剪贴板"
        }
    }

    /**
     * 格式化原地实时复制的提示文案
     */
    fun formatInPlaceCopyFeedback(characterCount: Int): String {
        return if (characterCount > 0) {
            "已复制所选内容 (${characterCount}字)"
        } else {
            "已复制所选内容"
        }
    }

    /**
     * 是否展示选择操作栏（文本非空时展示）
     */
    fun shouldShowActions(text: String?): Boolean {
        return !text.isNullOrBlank()
    }

    /**
     * 提取当前选区中的文本内容
     */
    fun extractSelectedText(fullText: String, start: Int, end: Int): String {
        if (fullText.isEmpty()) return ""
        val min = minOf(start, end).coerceIn(0, fullText.length)
        val max = maxOf(start, end).coerceIn(0, fullText.length)
        if (min >= max) return ""
        return fullText.substring(min, max)
    }

    /**
     * 当前是否为有效的部分选取
     */
    fun isPartialSelection(selectedLength: Int, totalLength: Int): Boolean {
        return selectedLength in 1 until totalLength
    }

    /**
     * 解析选区状态提示文案
     */
    fun resolveSelectionHint(selectedLength: Int, totalLength: Int): String {
        return if (isPartialSelection(selectedLength, totalLength)) {
            "已选中 $selectedLength 字 / 共 $totalLength 字，点击下方按钮直接复制"
        } else {
            "拖动水滴游标自由选择局部内容，点击下方按钮直接复制"
        }
    }

    /**
     * 解析主按钮文案（当有部分选择时，高亮显示复制选中）
     */
    fun resolveMainCopyButtonLabel(selectedLength: Int, totalLength: Int): String {
        return if (isPartialSelection(selectedLength, totalLength)) {
            "复制选中内容 (${selectedLength}字)"
        } else {
            "复制全部"
        }
    }

    /**
     * 创建系统分享 Intent
     */
    fun createShareIntent(text: String, title: String = "分享文本"): Intent {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        return Intent.createChooser(sendIntent, title)
    }

    /**
     * 格式化选区字数气泡角标文案
     */
    fun formatSelectedCountBadge(characterCount: Int): String {
        return if (characterCount > 0) "已选 ${characterCount} 字" else ""
    }

    /**
     * 从 Compose 的 TextToolbar 复制回调中安全提取当前选中的文本内容
     */
    fun resolveSelectedTextFromCopyCallback(onCopyRequested: Any?): String? {
        if (onCopyRequested == null) return null
        return try {
            var receiver = (onCopyRequested as? kotlin.jvm.internal.CallableReference)?.boundReceiver
            if (receiver == null) {
                for (field in onCopyRequested.javaClass.declaredFields) {
                    field.isAccessible = true
                    val value = field.get(onCopyRequested) ?: continue
                    if (value.javaClass.name.contains("SelectionManager")) {
                        receiver = value
                        break
                    }
                    if (field.name == "this$0") {
                        receiver = value
                    }
                }
            }
            if (receiver != null) {
                val method = receiver.javaClass.methods.firstOrNull {
                    it.name.startsWith("getSelectedText") && it.parameterCount == 0
                } ?: receiver.javaClass.declaredMethods.firstOrNull {
                    it.name.startsWith("getSelectedText") && it.parameterCount == 0
                }
                if (method != null) {
                    method.isAccessible = true
                    val result = method.invoke(receiver)
                    val text = (result as? androidx.compose.ui.text.AnnotatedString)?.text ?: result?.toString()
                    if (!text.isNullOrEmpty()) return text
                }
                for (f in receiver.javaClass.declaredFields) {
                    f.isAccessible = true
                    val obj = f.get(receiver) ?: continue
                    if (obj is androidx.compose.ui.text.AnnotatedString) {
                        return obj.text
                    }
                }
            }
            null
        } catch (_: Throwable) {
            null
        }
    }
}

/**
 * 兼容原生文本选区的轻触手势检测器。
 * 区别于系统 [androidx.compose.foundation.gestures.detectTapGestures] 会在按下 (down) 时立即消耗事件导致文本无法被划选，
 * 此检测器在 down 时绝不消耗事件，仅当手指在超时时间 ([androidx.compose.ui.platform.ViewConfiguration.longPressTimeoutMillis]) 内正常抬起时才触发点击。
 * 若用户长按超过超时阈值或拖拽划选手势，事件直接完整放行给 Compose 原生 TextSelection 划选体系与游标。
 */
suspend fun PointerInputScope.detectTapWithSelectionFriendly(
    onTap: (Offset) -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val up = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            waitForUpOrCancellation()
        }
        if (up != null && !up.isConsumed) {
            up.consume()
            onTap(up.position)
        }
    }
}
