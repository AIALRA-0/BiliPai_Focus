package com.android.purebilibili.feature.list

import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryManagementPolicyTest {

    @Test
    fun `pause action label reflects current shadow state`() {
        assertEquals("暂停记录", resolveHistoryPauseActionLabel(isHistoryPaused = false))
        assertEquals("继续记录", resolveHistoryPauseActionLabel(isHistoryPaused = true))
    }

    @Test
    fun `pause success message describes next state`() {
        assertEquals("已暂停历史记录", resolveHistoryPauseSuccessMessage(nextPaused = true))
        assertEquals("已继续记录历史", resolveHistoryPauseSuccessMessage(nextPaused = false))
    }

    @Test
    fun `clear confirmation states account-wide destructive scope`() {
        assertEquals(
            "确认清空全部历史记录吗？当前仅显示 3 条，本操作会清空账号全部历史。",
            resolveHistoryClearConfirmText(visibleCount = 3)
        )
    }
}
