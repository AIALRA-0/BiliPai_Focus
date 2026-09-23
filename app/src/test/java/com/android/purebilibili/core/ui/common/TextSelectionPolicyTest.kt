package com.android.purebilibili.core.ui.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextSelectionPolicyTest {

    @Test
    fun resolveTitle_returnsCustomOrFallback() {
        assertEquals("选择评论内容", TextSelectionPolicy.resolveTitle("选择评论内容"))
        assertEquals("选择文本", TextSelectionPolicy.resolveTitle(null))
        assertEquals("选择文本", TextSelectionPolicy.resolveTitle("  "))
    }

    @Test
    fun resolveCopyFeedbackMessage_returnsFormattedToast() {
        assertEquals("已复制 评论内容", TextSelectionPolicy.resolveCopyFeedbackMessage("评论内容"))
        assertEquals("已复制到剪贴板", TextSelectionPolicy.resolveCopyFeedbackMessage(null))
        assertEquals("已复制到剪贴板", TextSelectionPolicy.resolveCopyFeedbackMessage(""))
    }

    @Test
    fun formatInPlaceCopyFeedback_formatsWithCount() {
        assertEquals("已复制所选内容 (8字)", TextSelectionPolicy.formatInPlaceCopyFeedback(8))
        assertEquals("已复制所选内容", TextSelectionPolicy.formatInPlaceCopyFeedback(0))
        assertEquals("已复制所选内容", TextSelectionPolicy.formatInPlaceCopyFeedback(-1))
    }

    @Test
    fun shouldShowActions_checksNonBlank() {
        assertTrue(TextSelectionPolicy.shouldShowActions("有内容"))
        assertFalse(TextSelectionPolicy.shouldShowActions(""))
        assertFalse(TextSelectionPolicy.shouldShowActions("   "))
        assertFalse(TextSelectionPolicy.shouldShowActions(null))
    }

    @Test
    fun extractSelectedText_handlesRangesSafely() {
        val text = "欢迎来到我的频道"
        assertEquals("欢迎", TextSelectionPolicy.extractSelectedText(text, 0, 2))
        assertEquals("欢迎", TextSelectionPolicy.extractSelectedText(text, 2, 0)) // 倒序游标
        assertEquals("我的频道", TextSelectionPolicy.extractSelectedText(text, 4, 8))
        assertEquals("", TextSelectionPolicy.extractSelectedText(text, 3, 3)) // 折叠光标
        assertEquals("欢迎来到我的频道", TextSelectionPolicy.extractSelectedText(text, -5, 20)) // 越界防护
    }

    @Test
    fun resolveMainCopyButtonLabel_adaptsToSelection() {
        val text = "欢迎来到我的频道"
        assertEquals("复制选中内容 (4字)", TextSelectionPolicy.resolveMainCopyButtonLabel(4, text.length))
        assertEquals("复制全部", TextSelectionPolicy.resolveMainCopyButtonLabel(text.length, text.length))
        assertEquals("复制全部", TextSelectionPolicy.resolveMainCopyButtonLabel(0, text.length))
    }

    @Test
    fun resolveSelectionHint_adaptsToSelection() {
        val text = "欢迎来到我的频道"
        assertEquals("已选中 4 字 / 共 8 字，点击下方按钮直接复制", TextSelectionPolicy.resolveSelectionHint(4, text.length))
        assertEquals("拖动水滴游标自由选择局部内容，点击下方按钮直接复制", TextSelectionPolicy.resolveSelectionHint(text.length, text.length))
        assertEquals("拖动水滴游标自由选择局部内容，点击下方按钮直接复制", TextSelectionPolicy.resolveSelectionHint(0, text.length))
    }

    @Test
    fun formatSelectedCountBadge_formatsCorrectly() {
        assertEquals("已选 4 字", TextSelectionPolicy.formatSelectedCountBadge(4))
        assertEquals("已选 10 字", TextSelectionPolicy.formatSelectedCountBadge(10))
        assertEquals("", TextSelectionPolicy.formatSelectedCountBadge(0))
        assertEquals("", TextSelectionPolicy.formatSelectedCountBadge(-1))
    }

    @Test
    fun resolveSelectedTextFromCopyCallback_handlesNullSafely() {
        assertEquals(null, TextSelectionPolicy.resolveSelectedTextFromCopyCallback(null))
        assertEquals(null, TextSelectionPolicy.resolveSelectedTextFromCopyCallback("not_a_callback"))
    }
}
