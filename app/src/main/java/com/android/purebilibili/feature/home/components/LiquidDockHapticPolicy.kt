package com.android.purebilibili.feature.home.components

import kotlin.math.abs
import kotlin.math.roundToInt

/** 只接受真实手势坐标；动画、分页跟随与回弹不得送入此策略。 */
internal class LiquidDockHapticPolicy {
    private var count = 0
    private var slot = 0
    private var origin = 0f
    private var slop = 0f
    private var active = false
    private var lastFeedbackMs: Long? = null
    private var emitted = false

    fun start(position: Float, itemCount: Int, touchSlopSlots: Float) {
        count = itemCount.coerceAtLeast(0)
        origin = if (position.isFinite()) position.coerceIn(0f, (count - 1).coerceAtLeast(0).toFloat()) else 0f
        slot = origin.roundToInt()
        slop = touchSlopSlots.coerceAtLeast(0f)
        active = false
        emitted = false
        lastFeedbackMs = null
    }

    fun update(position: Float, nowMs: Long): Boolean {
        if (count <= 1 || !position.isFinite()) return false
        val value = position.coerceIn(0f, (count - 1).toFloat())
        val starting = !active && abs(value - origin) > slop
        if (!active && !starting) return false
        active = true
        // 在半槽边界两侧各保留 8% 槽宽的滞回，抑制指尖抖动反复跳档。
        val next = when {
            value > slot + 0.58f || value < slot - 0.58f -> value.roundToInt().coerceIn(0, count - 1)
            else -> slot
        }
        val crossed = next != slot
        slot = next
        if (!starting && !crossed) return false
        // 跳过的中间槽位不补发；保持当前槽位基线，停手后也不追补震动。
        if (lastFeedbackMs?.let { nowMs - it < 50L } == true) return false
        lastFeedbackMs = nowMs
        emitted = true
        return true
    }

    fun finish(): Boolean {
        val hadFeedback = emitted
        count = 0
        active = false
        emitted = false
        lastFeedbackMs = null
        return hadFeedback
    }
}
