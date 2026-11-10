package com.android.purebilibili.feature.video.danmaku

/** Temporary pause belongs to a render target, not to the saved danmaku preference. */
internal class DanmakuRenderPauseState<T : Any> {
    private val pausedTargets = LinkedHashSet<T>()

    fun setPaused(target: T, paused: Boolean): Boolean =
        if (paused) pausedTargets.add(target) else pausedTargets.remove(target)

    fun isRenderingEnabled(userEnabled: Boolean, currentTarget: T?): Boolean =
        userEnabled && (currentTarget == null || currentTarget !in pausedTargets)

    fun detach(target: T) { pausedTargets.remove(target) }
    fun clear() { pausedTargets.clear() }
}
