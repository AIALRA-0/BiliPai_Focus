package com.android.purebilibili.core.ui.performance

import androidx.metrics.performance.FrameData
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.adaptive.RuntimeVisualGuardDecision
import com.android.purebilibili.core.ui.adaptive.isRuntimeVisualGuardHighJankWindow
import com.android.purebilibili.core.ui.adaptive.isRuntimeVisualGuardTrackedStateKey
import com.android.purebilibili.core.ui.adaptive.mergeRuntimeVisualGuardDecisions
import com.android.purebilibili.core.ui.adaptive.resolveRuntimeVisualGuardDecision
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal const val VIDEO_CARD_TRANSITION_JANK_STATE = "VideoCardTransition"
internal const val RUNTIME_VISUAL_GUARD_MIN_FRAME_COUNT = 12

/**
 * 同时保留的信号窗口上限。`home:feed:<category>` 会随分类数增长，
 * 超过上限时淘汰最久未更新且已结算的窗口，避免 map 无限膨胀。
 */
internal const val RUNTIME_VISUAL_GUARD_MAX_TRACKED_SIGNALS = 24

internal class RuntimeVisualGuardTracker(
    baseTier: MotionTier = MotionTier.Normal,
    enabled: Boolean = true,
    private val minimumFrameCount: Int = RUNTIME_VISUAL_GUARD_MIN_FRAME_COUNT,
) {
    /** JankStats 回调固定在主线程，但 [setBaseTier] / [setEnabled] 来自组合，故加 volatile。 */
    @Volatile
    private var baseTier: MotionTier = baseTier

    @Volatile
    private var enabled: Boolean = enabled

    private val _decision = MutableStateFlow(normalDecision())
    val decision: StateFlow<RuntimeVisualGuardDecision> = _decision.asStateFlow()

    /** 每个 jank 打点一个独立窗口：竖滑与横滑同帧共存时不会互相污染分母。 */
    private val signals = LinkedHashMap<String, SignalWindow>()

    /**
     * JankStats 是 Activity 级别的，但守卫决策是进程级别的。Activity 切换时旧页面
     * 的 onStop 可能晚于新页面的 onStart，必须以会话所有权拒绝旧回调和旧清理。
     */
    private var activeSession: Any? = null

    private class SignalWindow {
        var activeValue: String? = null
        var frameCount = 0
        var jankyFrameCount = 0
        var consecutiveHighJankWindows = 0
        var lastDowngradeAtMs: Long? = null
        var downgraded = false
        var forceLowBlurBudget = false
        var lastTouchedAtMs = 0L

        val hasOpenWindow: Boolean get() = activeValue != null
    }

    /**
     * 设备基线档位。进程单例在 Activity 之前就已加载，因此不能在构造期读取
     * `WindowWidthSizeClass`；改由组合根在宽度变化时注入（折叠屏展开/分屏也会跟随）。
     */
    fun setBaseTier(tier: MotionTier) {
        if (baseTier == tier) return
        baseTier = tier
        publish(nowMs = null)
    }

    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        enabled = value
        if (!value) {
            signals.clear()
            _decision.value = normalDecision()
        } else {
            publish(nowMs = null)
        }
    }

    fun activateSession(session: Any) {
        if (activeSession === session) return
        discardActiveWindow()
        activeSession = session
    }

    fun onFrame(session: Any, frameData: FrameData, nowMs: Long) {
        if (activeSession !== session) return
        onFrame(frameData = frameData, nowMs = nowMs)
    }

    fun onFrame(frameData: FrameData, nowMs: Long) {
        if (!enabled) return
        val tracked = frameData.states.asSequence()
            .filter { isRuntimeVisualGuardTrackedStateKey(it.key) }
            .associate { it.key to it.value }

        // 本帧仍在活跃的信号累加；本帧消失但窗口还开着的信号收窗结算。
        tracked.forEach { (key, value) ->
            accumulate(key = key, stateValue = value, isJank = frameData.isJank, nowMs = nowMs)
        }
        signals.entries
            .filter { (key, window) -> window.hasOpenWindow && key !in tracked }
            .map { it.key }
            .forEach { key -> finishWindow(key, nowMs) }

        evictStaleSignals()
        publish(nowMs)
    }

    /** 测试入口：单信号简写，语义等价于卡片转场打点。 */
    internal fun onFrame(tracked: Boolean, isJank: Boolean, nowMs: Long) {
        onFrame(
            stateValue = if (tracked) "Tracked" else null,
            isJank = isJank,
            nowMs = nowMs,
        )
    }

    /** 测试入口：单信号简写，语义等价于卡片转场打点。 */
    internal fun onFrame(stateValue: String?, isJank: Boolean, nowMs: Long) {
        if (!enabled) return
        val key = VIDEO_CARD_TRANSITION_JANK_STATE
        if (stateValue.isNullOrBlank()) {
            if (signals[key]?.hasOpenWindow == true) finishWindow(key, nowMs)
        } else {
            accumulate(key = key, stateValue = stateValue, isJank = isJank, nowMs = nowMs)
        }
        publish(nowMs)
    }

    fun discardActiveWindow() {
        signals.values.forEach { window ->
            window.activeValue = null
            window.frameCount = 0
            window.jankyFrameCount = 0
        }
    }

    fun discardActiveWindow(session: Any) {
        if (activeSession !== session) return
        discardActiveWindow()
        activeSession = null
    }

    private fun accumulate(key: String, stateValue: String, isJank: Boolean, nowMs: Long) {
        val window = signals.getOrPut(key) { SignalWindow() }
        // 同一信号内 state 值切换（Opening → Returning）也算一个窗口结束。
        if (window.activeValue != null && window.activeValue != stateValue) {
            finishWindow(key, nowMs)
        }
        val refreshed = signals.getOrPut(key) { SignalWindow() }
        refreshed.activeValue = stateValue
        refreshed.frameCount += 1
        if (isJank) refreshed.jankyFrameCount += 1
        refreshed.lastTouchedAtMs = nowMs
    }

    private fun finishWindow(key: String, nowMs: Long) {
        val window = signals[key] ?: return
        val frameCount = window.frameCount
        val jankyFrameCount = window.jankyFrameCount
        window.activeValue = null
        window.frameCount = 0
        window.jankyFrameCount = 0
        window.lastTouchedAtMs = nowMs
        if (frameCount < minimumFrameCount) return

        val jankPercent = jankyFrameCount * 100f / frameCount
        if (window.downgraded) {
            val resolved = resolveRuntimeVisualGuardDecision(
                enabled = enabled,
                baseTier = baseTier,
                rollingJankPercent = jankPercent,
                consecutiveHighJankWindows = 0,
                lastDowngradeAtMs = window.lastDowngradeAtMs,
                nowMs = nowMs,
            )
            if (resolved.downgraded) {
                window.applyDowngraded(resolved)
            } else {
                window.clearDowngrade()
            }
        } else {
            window.consecutiveHighJankWindows = if (isRuntimeVisualGuardHighJankWindow(jankPercent)) {
                window.consecutiveHighJankWindows + 1
            } else {
                0
            }
            val resolved = resolveRuntimeVisualGuardDecision(
                enabled = enabled,
                baseTier = baseTier,
                rollingJankPercent = jankPercent,
                consecutiveHighJankWindows = window.consecutiveHighJankWindows,
                lastDowngradeAtMs = null,
                nowMs = nowMs,
            )
            window.lastDowngradeAtMs = resolved.nextLastDowngradeAtMs
            window.downgraded = resolved.downgraded
            window.forceLowBlurBudget = resolved.forceLowBlurBudget
        }

        recoverExpiredSignalsFromCompletedWindow(jankPercent = jankPercent, nowMs = nowMs)
    }

    private fun SignalWindow.applyDowngraded(resolved: RuntimeVisualGuardDecision) {
        downgraded = resolved.downgraded
        forceLowBlurBudget = resolved.forceLowBlurBudget
        lastDowngradeAtMs = resolved.nextLastDowngradeAtMs ?: lastDowngradeAtMs
    }

    private fun SignalWindow.clearDowngrade() {
        lastDowngradeAtMs = null
        consecutiveHighJankWindows = 0
        downgraded = false
        forceLowBlurBudget = false
    }

    /**
     * 触发按信号独立统计，但恢复证据可以来自任意同类交互。否则用户离开一次掉帧的
     * 首页分类后，那个不再出现的 key 会永久把全局档位锁在 Reduced。
     */
    private fun recoverExpiredSignalsFromCompletedWindow(jankPercent: Float, nowMs: Long) {
        signals.values.filter { it.downgraded }.forEach { candidate ->
            val resolved = resolveRuntimeVisualGuardDecision(
                enabled = enabled,
                baseTier = baseTier,
                rollingJankPercent = jankPercent,
                consecutiveHighJankWindows = 0,
                lastDowngradeAtMs = candidate.lastDowngradeAtMs,
                nowMs = nowMs,
            )
            if (!resolved.downgraded) candidate.clearDowngrade()
        }
    }

    /** 只淘汰没有开着窗口、且最久未更新的信号，避免误删正在统计的交互。 */
    private fun evictStaleSignals() {
        if (signals.size <= RUNTIME_VISUAL_GUARD_MAX_TRACKED_SIGNALS) return
        val evictable = signals.entries
            .filter { !it.value.hasOpenWindow && !it.value.downgraded }
            .sortedBy { it.value.lastTouchedAtMs }
        var overflow = signals.size - RUNTIME_VISUAL_GUARD_MAX_TRACKED_SIGNALS
        for (entry in evictable) {
            if (overflow <= 0) break
            signals.remove(entry.key)
            overflow -= 1
        }
    }

    private fun publish(nowMs: Long?) {
        if (!enabled) {
            _decision.value = normalDecision()
            return
        }
        val perSignal = signals.values.map { window ->
            RuntimeVisualGuardDecision(
                effectiveMotionTier = if (window.downgraded) MotionTier.Reduced else baseTier,
                forceLowBlurBudget = window.forceLowBlurBudget,
                downgraded = window.downgraded,
                nextLastDowngradeAtMs = window.lastDowngradeAtMs,
            )
        }
        _decision.value = mergeRuntimeVisualGuardDecisions(perSignal, baseTier)
    }

    private fun normalDecision() = RuntimeVisualGuardDecision(
        effectiveMotionTier = baseTier,
        forceLowBlurBudget = false,
        downgraded = false,
        nextLastDowngradeAtMs = null,
    )
}

internal val AppRuntimeVisualGuardTracker = RuntimeVisualGuardTracker()
