package com.android.purebilibili.feature.live

import com.android.purebilibili.data.model.response.LiveMessageEmote
import com.android.purebilibili.data.model.response.splitLiveMessageEmotes
import com.android.purebilibili.data.repository.LiveEmoticonPackage

/** 只预览会作为文字标记发送的表情，独立大表情继续使用直接发送流程。 */
internal fun liveDraftEmoteResources(packages: List<LiveEmoticonPackage>): Map<String, LiveMessageEmote> =
    buildMap {
        packages.forEach { pack ->
            pack.items.forEach { item ->
                if (item.dmType == 0 && item.emoji.isNotBlank() && item.url.isNotBlank()) {
                    put(item.emoji, LiveMessageEmote(item.url, item.width, item.height))
                }
            }
        }
    }

internal fun liveDraftPreviewSegments(text: String, resources: Map<String, LiveMessageEmote>): List<Pair<String, Boolean>> =
    splitLiveMessageEmotes(text, resources.keys)
