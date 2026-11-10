package com.android.purebilibili.core.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppTextSearchHighlightTest {
    @Test
    fun mapsNormalizedMatchesBackToOriginalText() {
        val text = "仅自动启用非 AI 字幕"
        val ranges = appTextSearchMatchRanges(text, "非ai字幕")
        assertEquals(listOf("非 AI 字幕"), ranges.map { text.substring(it) })
    }

    @Test
    fun marksEveryOccurrenceAndPreservesOtherText() {
        assertEquals(listOf(0..1, 3..4), appTextSearchMatchRanges("关闭；关闭后生效", "关闭"))
        assertTrue(appTextSearchMatchRanges("开启", "关闭").isEmpty())
        assertTrue(appTextSearchMatchRanges("开启", "  / ").isEmpty())
    }
}
