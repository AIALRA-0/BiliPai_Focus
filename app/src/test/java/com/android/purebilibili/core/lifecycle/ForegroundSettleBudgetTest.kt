package com.android.purebilibili.core.lifecycle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ForegroundSettleBudgetTest {

    @Test
    fun coldStartAndShortForegroundBounce_doNotArmTheSettleWindow() {
        // 冷启动：没有前置的后台停留，不该被推迟（启动另有编排）。
        assertFalse(shouldArmForegroundSettleBudget(backgroundElapsedMs = 0L))
        // 权限弹窗 / 系统对话框 / 下拉通知栏这类短暂失焦不会打断既有工作。
        assertFalse(shouldArmForegroundSettleBudget(backgroundElapsedMs = 800L))
        assertFalse(
            shouldArmForegroundSettleBudget(
                backgroundElapsedMs = FOREGROUND_SETTLE_MIN_BACKGROUND_MILLIS - 1
            )
        )
    }

    @Test
    fun realBackgroundTrip_armsTheSettleWindow() {
        assertTrue(
            shouldArmForegroundSettleBudget(
                backgroundElapsedMs = FOREGROUND_SETTLE_MIN_BACKGROUND_MILLIS
            )
        )
        assertTrue(shouldArmForegroundSettleBudget(backgroundElapsedMs = 60_000L))
        assertTrue(shouldArmForegroundSettleBudget(backgroundElapsedMs = 3_600_000L))
    }

    @Test
    fun settleWindowIsShortEnoughToNotStarveBackgroundWork() {
        // 回归护栏：窗口必须短于用户感知「卡了一下」的量级，否则封面预取与调色板重建
        // 会被整体推迟到体验之外。
        assertTrue(FOREGROUND_SETTLE_WINDOW_MILLIS in 500L..2_000L)
        assertTrue(FOREGROUND_SETTLE_MIN_BACKGROUND_MILLIS >= FOREGROUND_SETTLE_WINDOW_MILLIS)
    }

    @Test
    fun resetClearsSettlingState() {
        ForegroundSettleBudget.reset()
        assertEquals(false, ForegroundSettleBudget.isSettling.value)
    }
}
