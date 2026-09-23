package com.android.purebilibili.core.store

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsSearchHistoryPolicyTest {
    @Test
    fun ignoresBlankQueries() {
        assertEquals(listOf("动画"), updatedSettingsSearchHistory(listOf("动画"), "  "))
    }

    @Test
    fun trimsAndMovesExistingQueryToFront() {
        assertEquals(listOf("动画", "壁纸"), updatedSettingsSearchHistory(listOf("壁纸", "动画"), " 动画 "))
        assertEquals(listOf("MD3", "壁纸"), updatedSettingsSearchHistory(listOf("md3", "壁纸"), "MD3"))
    }

    @Test
    fun keepsTwentyMostRecentQueries() {
        val history = (1..20).map { "设置$it" }
        assertEquals(listOf("新设置") + history.take(19), updatedSettingsSearchHistory(history, "新设置"))
    }
}
