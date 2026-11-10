package com.android.purebilibili.feature.live

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.android.purebilibili.data.model.response.splitLiveMessageEmotes

internal data class LiveRichDraftEdit(val value: TextFieldValue, val clearReply: Boolean)

/** 编辑器中的回复前缀只用于展示，不计入正文及发送长度。 */
internal fun projectLiveRichDraftEdit(
    previous: TextFieldValue,
    edited: TextFieldValue,
    replyPrefix: String,
    emoteKeys: Set<String>,
): LiveRichDraftEdit {
    val displayed = replyPrefix + previous.text
    val prefixLength = replyPrefix.length
    val prefixIntact = edited.text.startsWith(replyPrefix)
    val withoutReply = if (prefixIntact) {
        removeLiveDraftRanges(edited, listOf(0 until prefixLength))
    } else {
        removeLiveEditedTokenRemainders(displayed, edited, listOf(0 until prefixLength), includeLeadingInsertion = true)
    }
    // 输入法组合期间不重写正文，避免破坏候选词、拼音和组合范围。
    val normalized = if (withoutReply.composition != null || previous.composition != null) withoutReply else {
        var offset = 0
        val tokens = splitLiveMessageEmotes(previous.text, emoteKeys).mapNotNull { (part, emote) ->
            val start = offset
            offset += part.length
            if (emote) start until offset else null
        }
        removeLiveEditedTokenRemainders(previous.text, withoutReply, tokens)
    }
    return LiveRichDraftEdit(normalized, prefixLength > 0 && !prefixIntact)
}

/** 删除或替换一个表情的部分标记时，同时移除该标记剩余片段，保留新输入。 */
private fun removeLiveEditedTokenRemainders(
    previous: String,
    edited: TextFieldValue,
    tokens: List<IntRange>,
    includeLeadingInsertion: Boolean = false,
): TextFieldValue {
    if (previous == edited.text) return edited
    var start = 0
    while (start < minOf(previous.length, edited.text.length) && previous[start] == edited.text[start]) start++
    var suffix = 0
    while (suffix < minOf(previous.length, edited.text.length) - start &&
        previous[previous.lastIndex - suffix] == edited.text[edited.text.lastIndex - suffix]) suffix++
    val oldEnd = previous.length - suffix
    val newEnd = edited.text.length - suffix
    val delta = newEnd - oldEnd
    val leftovers = buildList {
        tokens.filter { !it.isEmpty() }.forEach { token ->
            val end = token.last + 1
            val touched = if (oldEnd == start) start >= token.first + (if (includeLeadingInsertion) 0 else 1) && start < end
                else start < end && oldEnd > token.first
            if (touched) {
                if (token.first < start) add(token.first until start)
                if (end > oldEnd) add((oldEnd + delta) until (end + delta))
            }
        }
    }
    return removeLiveDraftRanges(edited, leftovers)
}

private fun removeLiveDraftRanges(value: TextFieldValue, ranges: List<IntRange>): TextFieldValue {
    val valid = ranges.filterNot { it.isEmpty() }.sortedBy { it.first }
    if (valid.isEmpty()) return value
    fun offset(index: Int): Int = (index - valid.sumOf { range ->
        (index - range.first).coerceIn(0, range.last + 1 - range.first)
    }).coerceAtLeast(0)
    val text = buildString {
        value.text.forEachIndexed { index, char -> if (valid.none { index in it }) append(char) }
    }
    val composition = value.composition?.takeUnless { range ->
        valid.any { it.first < range.max && it.last + 1 > range.min }
    }?.let { TextRange(offset(it.start), offset(it.end)) }
    return TextFieldValue(text, TextRange(offset(value.selection.start), offset(value.selection.end)), composition)
}
