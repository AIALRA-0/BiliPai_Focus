package com.android.purebilibili.core.ui

/** 同一次同步操作最多派发一次触感；不合并独立点击、异步结果或手势刻度。 */
object AppActionHaptics {
    private class Action(var emitted: Boolean = false)
    private val current = ThreadLocal<Action?>()

    fun runAction(action: () -> Unit) {
        if (current.get() != null) {
            action()
            return
        }
        current.set(Action())
        try {
            action()
        } finally {
            current.remove()
        }
    }

    /** 手势已经派发刻度时，提交导航不再叠加页面回调的点击反馈。 */
    fun runWithoutAdditionalFeedback(action: () -> Unit) {
        val previous = current.get()
        current.set(Action(emitted = true))
        try {
            action()
        } finally {
            if (previous == null) current.remove() else current.set(previous)
        }
    }

    /** 场景已明确关闭时也占用当前操作的反馈，防止通用按钮再次补振。 */
    fun suppressCurrentActionFeedback() {
        current.get()?.emitted = true
    }

    fun dispatch(feedback: () -> Unit) {
        val action = current.get()
        if (action?.emitted == true) return
        if (action != null) action.emitted = true
        feedback()
    }
}
