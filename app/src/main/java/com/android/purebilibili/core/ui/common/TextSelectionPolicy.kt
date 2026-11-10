package com.android.purebilibili.core.ui.common

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
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
}

/**
 * 兼容原生文本选区的轻触手势检测器。
 * 区别于系统 [androidx.compose.foundation.gestures.detectTapGestures] 会在按下 (down) 时立即消耗事件导致文本无法被划选，
 * 此检测器在 down 时绝不消耗事件，仅当手指在超时时间 ([androidx.compose.ui.platform.ViewConfiguration.longPressTimeoutMillis]) 内正常抬起时才触发点击。
 * 可选长按回调供带链接的富文本打开复制面板；未提供时长按与拖拽仍交给原生划选。
 */
suspend fun PointerInputScope.detectTapWithSelectionFriendly(
    onLongPress: (() -> Unit)? = null,
    onTap: ((Offset) -> Unit)? = null,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (onLongPress == null) {
            val up = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                waitForUpOrCancellation()
            }
            if (up != null && !up.isConsumed && onTap != null) {
                up.consume()
                onTap(up.position)
            }
        } else {
            // Observe the Initial pass so a link consuming the press in BasicText does not
            // cancel the long press. Movement beyond touch slop still yields to scrolling.
            var released = false
            var dragged = false
            val finishedBeforeTimeout = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (!released && !dragged) {
                    val change = awaitPointerEvent(PointerEventPass.Initial)
                        .changes.firstOrNull { it.id == down.id }
                    released = change == null || !change.pressed
                    dragged = change != null &&
                        (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                }
                true
            } == true
            if (!finishedBeforeTimeout) {
                onLongPress()
                while (true) {
                    val release = awaitPointerEvent(PointerEventPass.Initial)
                        .changes.firstOrNull { it.id == down.id }
                    if (release == null || !release.pressed) {
                        release?.consume()
                        break
                    }
                }
            } else if (released && onTap != null) {
                // Let BasicText consume a link click during Main before handling blank text.
                val up = awaitPointerEvent(PointerEventPass.Final)
                    .changes.firstOrNull { it.id == down.id }
                if (up != null && !up.isConsumed) {
                    up.consume()
                    onTap(up.position)
                }
            }
        }
    }
}
