package com.android.purebilibili.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * BiliPai 底栏返回不跟手 seek，只在 onBackCompleted 用 animateScrollBy 切页。
 * 这里只保留与 BiliPai 一致的 duration 策略断言。
 */
class MainBottomPagerPredictiveBackPolicyTest {

    @Test
    fun `biliPai style duration scales with page distance`() {
        // BiliPai: duration = 100 * max(distance, 2) + 100
        assertEquals(300, resolveBottomPagerNavigationDurationMillis(pageDistance = 1))
        assertEquals(300, resolveBottomPagerNavigationDurationMillis(pageDistance = 2))
        assertEquals(400, resolveBottomPagerNavigationDurationMillis(pageDistance = 3))
        assertEquals(500, resolveBottomPagerNavigationDurationMillis(pageDistance = 4))
    }
}
