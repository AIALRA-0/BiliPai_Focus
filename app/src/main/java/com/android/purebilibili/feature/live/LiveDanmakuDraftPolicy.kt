package com.android.purebilibili.feature.live

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.android.purebilibili.data.repository.LiveEmoticonItem
import com.android.purebilibili.data.repository.appendLiveTextEmoticon

/** 替换选中文本，插入后光标位于表情标记之后，取消旧的输入法组合范围。 */
internal fun insertLiveTextEmoticon(
    draft: TextFieldValue,
    item: LiveEmoticonItem,
    maxLength: Int,
): Result<TextFieldValue> {
    val start = draft.selection.min.coerceIn(0, draft.text.length)
    val end = draft.selection.max.coerceIn(start, draft.text.length)
    val remaining = draft.text.removeRange(start, end)
    return appendLiveTextEmoticon(remaining, item, maxLength).map {
        TextFieldValue(
            text = draft.text.replaceRange(start, end, item.emoji),
            selection = TextRange(start + item.emoji.length),
        )
    }
}
