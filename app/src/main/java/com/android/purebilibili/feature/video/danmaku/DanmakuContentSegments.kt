package com.android.purebilibili.feature.video.danmaku

internal data class DanmakuContentSegment(val content: String, val isEmoticon: Boolean)

/** 整条图片消息优先；内嵌消息资源按原始标记匹配，房间缓存只匹配方括号表情。 */
internal fun resolveDanmakuContentSegments(
    text: String,
    enableEmoticon: Boolean,
    messageEmotes: Map<String, String>,
    roomEmotes: Map<String, String>,
): List<DanmakuContentSegment> {
    if (!enableEmoticon) return listOf(DanmakuContentSegment(text, false))
    val messageKeys = messageEmotes.filter { it.key.isNotEmpty() && it.value.isNotBlank() }.keys
    if (text in messageKeys) return listOf(DanmakuContentSegment(text, true))
    val pattern = Regex((messageKeys.sortedByDescending { it.length }.map { Regex.escape(it) } +
        "\\[(.*?)\\]").joinToString("|"))
    return buildList {
        var offset = 0
        pattern.findAll(text).forEach { match ->
            if (match.range.first > offset) add(DanmakuContentSegment(text.substring(offset, match.range.first), false))
            val token = match.value
            val bare = token.removePrefix("[").removeSuffix("]")
            val key = when {
                token in messageKeys -> token
                bare in messageKeys -> bare
                !roomEmotes[token].isNullOrBlank() -> token
                !roomEmotes[bare].isNullOrBlank() -> bare
                else -> null
            }
            add(DanmakuContentSegment(key ?: token, key != null))
            offset = match.range.last + 1
        }
        if (offset < text.length) add(DanmakuContentSegment(text.substring(offset), false))
    }
}
