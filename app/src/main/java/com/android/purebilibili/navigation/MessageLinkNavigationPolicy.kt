package com.android.purebilibili.navigation

import com.android.purebilibili.core.util.BilibiliNavigationTarget
import com.android.purebilibili.core.util.BilibiliNavigationTargetParser

internal sealed interface MessageLinkNavigationAction {
    data class Video(val videoId: String) : MessageLinkNavigationAction
    data class VideoComment(val videoId: String, val rootReplyId: Long) : MessageLinkNavigationAction
    data class Dynamic(val dynamicId: String) : MessageLinkNavigationAction
    data class DynamicComment(val dynamicId: String) : MessageLinkNavigationAction
    data class Space(val mid: Long) : MessageLinkNavigationAction
    data class Live(val roomId: Long) : MessageLinkNavigationAction
    data class BangumiSeason(val seasonId: Long) : MessageLinkNavigationAction
    data class BangumiEpisode(val epId: Long) : MessageLinkNavigationAction
    data class Music(val musicId: String) : MessageLinkNavigationAction
    data class Web(val url: String) : MessageLinkNavigationAction
}

internal fun resolveMessageLinkNavigationAction(rawLink: String): MessageLinkNavigationAction {
    resolveMessageCommentNavigationAction(rawLink)?.let { return it }

    return when (val target = BilibiliNavigationTargetParser.parse(rawLink)) {
        is BilibiliNavigationTarget.Video -> MessageLinkNavigationAction.Video(target.videoId)
        is BilibiliNavigationTarget.Dynamic -> MessageLinkNavigationAction.Dynamic(target.dynamicId)
        is BilibiliNavigationTarget.Space -> MessageLinkNavigationAction.Space(target.mid)
        is BilibiliNavigationTarget.Live -> MessageLinkNavigationAction.Live(target.roomId)
        is BilibiliNavigationTarget.BangumiSeason -> MessageLinkNavigationAction.BangumiSeason(target.seasonId)
        is BilibiliNavigationTarget.BangumiEpisode -> MessageLinkNavigationAction.BangumiEpisode(target.epId)
        is BilibiliNavigationTarget.Music -> MessageLinkNavigationAction.Music(target.musicId)
        else -> MessageLinkNavigationAction.Web(rawLink)
    }
}

private fun resolveMessageCommentNavigationAction(rawLink: String): MessageLinkNavigationAction? {
    val uri = runCatching { java.net.URI(rawLink) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase().orEmpty()
    val host = uri.host?.lowercase().orEmpty()
    if (scheme !in setOf("bili", "bilibili") || host != "comment") return null

    val segments = uri.path
        ?.split("/")
        ?.filter { it.isNotBlank() }
        .orEmpty()
    if (segments.size < 4) return null
    if (segments.firstOrNull() !in setOf("detail", "msg_fold")) return null

    val businessId = segments.getOrNull(1)?.toIntOrNull() ?: return null
    val oid = segments.getOrNull(2)?.toLongOrNull() ?: return null
    val rootReplyId = segments.getOrNull(3)?.toLongOrNull() ?: 0L
    val queryMap = uri.rawQuery
        ?.split("&")
        ?.mapNotNull { part ->
            if (part.isBlank()) return@mapNotNull null
            val pair = part.split("=", limit = 2)
            val key = java.net.URLDecoder.decode(pair[0], java.nio.charset.StandardCharsets.UTF_8)
            val value = java.net.URLDecoder.decode(pair.getOrElse(1) { "" }, java.nio.charset.StandardCharsets.UTF_8)
            key to value
        }
        ?.toMap()
        .orEmpty()

    val fallbackLink = queryMap["enterUri"].orEmpty().ifBlank {
        when (businessId) {
            11, 16, 17 -> "bilibili://following/detail/$oid"
            else -> "bilibili://video/$oid"
        }
    }

    return when (val target = BilibiliNavigationTargetParser.parse(fallbackLink)) {
        is BilibiliNavigationTarget.Video -> MessageLinkNavigationAction.VideoComment(
            videoId = target.videoId,
            rootReplyId = rootReplyId
        )
        is BilibiliNavigationTarget.Dynamic -> MessageLinkNavigationAction.DynamicComment(
            dynamicId = target.dynamicId
        )
        is BilibiliNavigationTarget.Space -> MessageLinkNavigationAction.Space(target.mid)
        is BilibiliNavigationTarget.Live -> MessageLinkNavigationAction.Live(target.roomId)
        is BilibiliNavigationTarget.BangumiSeason -> MessageLinkNavigationAction.BangumiSeason(target.seasonId)
        is BilibiliNavigationTarget.BangumiEpisode -> MessageLinkNavigationAction.BangumiEpisode(target.epId)
        is BilibiliNavigationTarget.Music -> MessageLinkNavigationAction.Music(target.musicId)
        else -> null
    }
}
