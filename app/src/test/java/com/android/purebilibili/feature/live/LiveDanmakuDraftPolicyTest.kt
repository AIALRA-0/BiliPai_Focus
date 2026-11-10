package com.android.purebilibili.feature.live

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.android.purebilibili.data.repository.LiveEmoticonItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiveDanmakuDraftPolicyTest {
    private val emote = LiveEmoticonItem(emoji = "[笑]", url = "", dmType = 0)

    @Test
    fun insertingInMiddlePreservesSuffixAndMovesCursor() {
        val result = insertLiveTextEmoticon(TextFieldValue("前后", TextRange(1)), emote, 40).getOrThrow()
        assertEquals("前[笑]后", result.text)
        assertEquals(TextRange(4), result.selection)
    }

    @Test
    fun replacingSelectionValidatesResultRatherThanOriginalLength() {
        val result = insertLiveTextEmoticon(TextFieldValue("前abcdef后", TextRange(7, 1)), emote, 5).getOrThrow()
        assertEquals("前[笑]后", result.text)
        assertEquals(TextRange(4), result.selection)
        assertTrue(insertLiveTextEmoticon(TextFieldValue("前后", TextRange(1)), emote, 4).isFailure)
    }

    @Test
    fun exclusiveImagesAreNotInsertedAsPlainMarkers() {
        assertTrue(insertLiveTextEmoticon(TextFieldValue(""), emote.copy(dmType = 1), 40).isFailure)
    }
}
