package com.android.purebilibili.feature.live

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LiveRichDraftPolicyTest {
    private val prefix = "@主播 "
    private val emotes = setOf("[笑]", "[笑哈哈]")

    @Test
    fun `reply display is excluded from message and reversed selection`() {
        val result = projectLiveRichDraftEdit(
            TextFieldValue("正文"), TextFieldValue(prefix + "正文", TextRange(prefix.length + 2, prefix.length)), prefix, emotes,
        )
        assertEquals("正文", result.value.text)
        assertEquals(TextRange(2, 0), result.value.selection)
        assertFalse(result.clearReply)
    }

    @Test
    fun `backspace at body start removes entire reply and preserves body`() {
        val result = projectLiveRichDraftEdit(
            TextFieldValue("正文", TextRange.Zero),
            TextFieldValue(prefix.dropLast(1) + "正文", TextRange(prefix.length - 1)), prefix, emotes,
        )
        assertTrue(result.clearReply)
        assertEquals(TextFieldValue("正文", TextRange.Zero), result.value)
    }

    @Test
    fun `pasting ahead of reply retains insertion without leaking reply into body`() {
        val result = projectLiveRichDraftEdit(
            TextFieldValue("正文"), TextFieldValue("新增" + prefix + "正文", TextRange(2)), prefix, emotes,
        )
        assertTrue(result.clearReply)
        assertEquals(TextFieldValue("新增正文", TextRange(2)), result.value)
    }

    @Test
    fun `cut across reply and part of body keeps only remaining body`() {
        val result = projectLiveRichDraftEdit(
            TextFieldValue("前后"), TextFieldValue("后", TextRange.Zero), prefix, emotes,
        )
        assertTrue(result.clearReply)
        assertEquals(TextFieldValue("后", TextRange.Zero), result.value)
    }

    @Test
    fun `partial emote deletion removes whole marker and keeps suffix`() {
        val result = projectLiveRichDraftEdit(
            TextFieldValue("前[笑]后", TextRange(4)), TextFieldValue("前[笑后", TextRange(3)), "", emotes,
        )
        assertEquals(TextFieldValue("前后", TextRange(1)), result.value)
    }

    @Test
    fun `replacement through emote preserves inserted text and adjacent emote`() {
        val result = projectLiveRichDraftEdit(
            TextFieldValue("前[笑哈哈][笑]后"), TextFieldValue("前[替换哈][笑]后", TextRange(4)), "", emotes,
        )
        assertEquals(TextFieldValue("前替换[笑]后", TextRange(3)), result.value)
    }

    @Test
    fun `composition is translated to body offsets without rewriting tokens`() {
        val result = projectLiveRichDraftEdit(
            TextFieldValue("前"),
            TextFieldValue(prefix + "前ni", TextRange(prefix.length + 3), TextRange(prefix.length + 1, prefix.length + 3)),
            prefix, emotes,
        )
        assertEquals(TextFieldValue("前ni", TextRange(3), TextRange(1, 3)), result.value)
        assertFalse(result.clearReply)
    }

    @Test
    fun `unknown marker stays editable plain text`() {
        val result = projectLiveRichDraftEdit(
            TextFieldValue("[未知]"), TextFieldValue("[未]", TextRange(2)), "", emotes,
        )
        assertEquals("[未]", result.value.text)
        assertNull(result.value.composition)
    }
}
