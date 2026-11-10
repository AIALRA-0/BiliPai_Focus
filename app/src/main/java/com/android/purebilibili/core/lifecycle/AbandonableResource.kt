package com.android.purebilibili.core.lifecycle

import androidx.compose.runtime.RememberObserver

/**
 * 为 remember 中创建的资源补齐组合中止路径。
 * 成功提交后由调用方的 DisposableEffect 接管正常释放，不改变资源转移规则。
 */
internal class AbandonableResource<T>(
    val value: T,
    private var onAbandon: ((T) -> Unit)?,
) : RememberObserver {
    override fun onRemembered() {
        onAbandon = null
    }

    override fun onForgotten() = Unit

    override fun onAbandoned() {
        val release = onAbandon
        onAbandon = null
        release?.invoke(value)
    }
}
