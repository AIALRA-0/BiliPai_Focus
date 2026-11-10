package com.android.purebilibili.feature.live

import com.android.purebilibili.data.model.response.LiveMessageEmote
import com.android.purebilibili.data.model.response.splitLiveMessageEmotes
import com.android.purebilibili.data.repository.LiveEmoticonItem
import com.android.purebilibili.data.repository.LiveEmoticonPackage

internal fun LiveEmoticonItem.messageEmote(): LiveMessageEmote? = url.takeIf { it.isNotBlank() }?.let {
    LiveMessageEmote(
        url = it,
        width = width.coerceAtLeast(1),
        height = height.coerceAtLeast(1),
        official = emoticonUnique.startsWith("official_"),
        unique = emoticonUnique,
    )
}

/** 在进入聊天记录和画面弹幕之前绑定房间资源，避免本地回显依赖异步回传或全局缓存。 */
internal fun resolveLiveMessageEmoteResources(
    message: LiveDanmakuItem,
    packages: List<LiveEmoticonPackage>,
): LiveDanmakuItem {
    if (message.isSystem || message.isSuperChat) return message
    val direct = message.emoticon?.takeIf { it.url.isNotBlank() }
        ?: message.emoticonUrl?.takeIf { it.isNotBlank() }?.let { LiveMessageEmote(it) }
        ?: if (message.dmType == 1) {
            packages.asSequence().flatMap { it.items.asSequence() }.firstOrNull {
                it.dmType == 1 && it.url.isNotBlank() &&
                    (it.displayText == message.text || it.emoticonUnique.takeIf(String::isNotBlank) == message.text)
            }?.messageEmote()
        } else null
    val roomResources = liveDraftEmoteResources(packages)
    val inline = buildMap {
        splitLiveMessageEmotes(message.text, roomResources.keys).forEach { (token, isEmote) ->
            if (isEmote) roomResources[token]?.let { put(token, it) }
        }
        // 回传的消息级资源优先，保留不在当前表情包中的内嵌图片。
        message.inlineEmotes.forEach { (key, emote) ->
            if (emote.url.isNotBlank()) put(key, emote)
        }
    }
    return message.copy(emoticon = direct, emoticonUrl = direct?.url, inlineEmotes = inline)
}
