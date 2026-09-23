package com.android.purebilibili.navigation

import com.android.purebilibili.core.util.BilibiliNavigationTarget
import com.android.purebilibili.core.util.BilibiliNavigationTargetParser
import com.android.purebilibili.core.util.decodeUrlComponentCompat

internal sealed interface MessageLinkNavigationAction {
    data class Video(val videoId: String) : MessageLinkNavigationAction
    data class VideoComment(
        val videoId: String,
        val rootReplyId: Long,
        val targetReplyId: Long = 0L
    ) : MessageLinkNavigationAction
    data class Dynamic(val dynamicId: String) : MessageLinkNavigationAction
    data class DynamicComment(
        val dynamicId: String,
        val rootReplyId: Long,
        val targetReplyId: Long = 0L
    ) : MessageLinkNavigationAction
    data class CommentDetail(
        val oid: Long,
        val rootReplyId: Long,
        val targetReplyId: Long = 0L,
        val businessId: Int = 1,
        val enterUri: String = ""
    ) : MessageLinkNavigationAction
    data class Space(val mid: Long) : MessageLinkNavigationAction
    data class Live(val roomId: Long) : MessageLinkNavigationAction
    data class BangumiSeason(val seasonId: Long, val mediaId: Long = 0L) : MessageLinkNavigationAction
    data class BangumiEpisode(val epId: Long) : MessageLinkNavigationAction
    data class Music(val musicId: String) : MessageLinkNavigationAction
    data class Article(val articleId: Long) : MessageLinkNavigationAction
    data class Web(val url: String) : MessageLinkNavigationAction
}

internal fun resolveMessageLinkNavigationAction(rawLink: String): MessageLinkNavigationAction {
    resolveMessageCommentNavigationAction(rawLink)?.let { return it }

    val commentLocation = resolveMessageCommentLocation(rawLink)
    return when (val target = BilibiliNavigationTargetParser.parse(rawLink)) {
        is BilibiliNavigationTarget.Video -> {
            if (commentLocation != null) {
                val aid = target.videoId.removePrefix("av").removePrefix("AV").toLongOrNull()
                if (aid != null && aid > 0L) {
                    MessageLinkNavigationAction.CommentDetail(
                        oid = aid,
                        rootReplyId = commentLocation.rootReplyId,
                        targetReplyId = commentLocation.targetReplyId,
                        businessId = 1,
                        enterUri = "bilibili://video/$aid"
                    )
                } else {
                    MessageLinkNavigationAction.VideoComment(
                        videoId = target.videoId,
                        rootReplyId = commentLocation.rootReplyId,
                        targetReplyId = commentLocation.targetReplyId
                    )
                }
            } else {
                MessageLinkNavigationAction.Video(target.videoId)
            }
        }
        is BilibiliNavigationTarget.Dynamic -> {
            if (commentLocation != null) {
                val dynId = target.dynamicId.toLongOrNull()
                if (dynId != null && dynId > 0L) {
                    MessageLinkNavigationAction.CommentDetail(
                        oid = dynId,
                        rootReplyId = commentLocation.rootReplyId,
                        targetReplyId = commentLocation.targetReplyId,
                        businessId = 17,
                        enterUri = "bilibili://following/detail/$dynId"
                    )
                } else {
                    MessageLinkNavigationAction.DynamicComment(
                        dynamicId = target.dynamicId,
                        rootReplyId = commentLocation.rootReplyId,
                        targetReplyId = commentLocation.targetReplyId
                    )
                }
            } else {
                MessageLinkNavigationAction.Dynamic(target.dynamicId)
            }
        }
        is BilibiliNavigationTarget.Space -> MessageLinkNavigationAction.Space(target.mid)
        is BilibiliNavigationTarget.Live -> MessageLinkNavigationAction.Live(target.roomId)
        is BilibiliNavigationTarget.BangumiSeason -> MessageLinkNavigationAction.BangumiSeason(
            seasonId = target.seasonId,
            mediaId = target.mediaId
        )
        is BilibiliNavigationTarget.BangumiEpisode -> MessageLinkNavigationAction.BangumiEpisode(target.epId)
        is BilibiliNavigationTarget.Music -> MessageLinkNavigationAction.Music(target.musicId)
        is BilibiliNavigationTarget.Article -> MessageLinkNavigationAction.Article(target.articleId)
        else -> {
            val trimmed = rawLink.trim()
            if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
                MessageLinkNavigationAction.Web(trimmed)
            } else {
                MessageLinkNavigationAction.Web("")
            }
        }
    }
}

private data class MessageCommentLocation(
    val rootReplyId: Long,
    val targetReplyId: Long
)

private fun resolveMessageCommentNavigationAction(rawLink: String): MessageLinkNavigationAction? {
    val uri = runCatching { java.net.URI(rawLink) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase().orEmpty()
    val host = uri.host?.lowercase().orEmpty()
    val path = uri.path.orEmpty()
    val queryMap = decodeQueryMap(uri.rawQuery)

    // 1. Handle bilibili://browser/?url=...
    if (scheme in setOf("bili", "bilibili") && host == "browser") {
        val innerUrl = queryMap["url"]?.trim().orEmpty()
        if (innerUrl.isNotBlank()) {
            return resolveMessageLinkNavigationAction(innerUrl)
        }
        return null
    }

    // 2. Handle H5 / Web comment links:
    // https://www.bilibili.com/h5/comment/sub?oid=...&pageType=...&root=...&comment_secondary_id=...
    if ((scheme == "http" || scheme == "https") &&
        host.contains("bilibili.com") &&
        (path.contains("comment/sub") || path.contains("h5/comment"))
    ) {
        val oid = queryMap["oid"]?.toLongOrNull() ?: return null
        val rootReplyId = queryMap["root"]?.toLongOrNull() ?: return null
        val pageType = queryMap["pageType"]?.toIntOrNull() ?: 1
        val targetReplyId = queryMap.firstPositiveLong(
            "comment_secondary_id",
            "comment_id",
            "reply_id",
            "rpid",
            "target_id",
            "anchor",
            "source_id"
        )
        val effectivePageType = if (oid >= 100_000_000_000_000_000L && pageType == 1) 17 else pageType
        val enterUri = when (effectivePageType) {
            1 -> "bilibili://video/$oid"
            11, 16, 17 -> "bilibili://following/detail/$oid"
            12 -> "bilibili://read/cv$oid"
            else -> "bilibili://video/$oid"
        }
        return MessageLinkNavigationAction.CommentDetail(
            oid = oid,
            rootReplyId = rootReplyId,
            targetReplyId = targetReplyId,
            businessId = effectivePageType,
            enterUri = enterUri
        )
    }

    // 3. Handle bilibili://comment/detail/... and bilibili://comment/msg_fold/...
    if (scheme !in setOf("bili", "bilibili") || host != "comment") return null

    val segments = path
        .split("/")
        .filter { it.isNotBlank() }
    if (segments.size < 4) return null
    if (segments.firstOrNull() !in setOf("detail", "msg_fold")) return null

    val businessId = segments.getOrNull(1)?.toIntOrNull() ?: return null
    val oid = segments.getOrNull(2)?.toLongOrNull() ?: return null
    val rootReplyId = segments.getOrNull(3)?.toLongOrNull() ?: 0L

    val enterUri = queryMap["enterUri"]?.trim().orEmpty()
    val parsedTargetReplyId = queryMap.firstPositiveLong(
        "comment_secondary_id",
        "comment_id",
        "reply_id",
        "rpid",
        "target_id",
        "anchor",
        "source_id"
    ).takeIf { it > 0L } ?: segments.getOrNull(4)?.toLongOrNull()?.takeIf { it > 0L } ?: 0L

    val effectiveBusinessId = if (oid >= 100_000_000_000_000_000L && businessId == 1) 17 else businessId
    val resolvedEnterUri = enterUri.ifBlank {
        when (effectiveBusinessId) {
            11, 16, 17 -> "bilibili://following/detail/$oid"
            12 -> "bilibili://read/cv$oid"
            else -> "bilibili://video/$oid"
        }
    }

    return MessageLinkNavigationAction.CommentDetail(
        oid = oid,
        rootReplyId = rootReplyId,
        targetReplyId = parsedTargetReplyId,
        businessId = effectiveBusinessId,
        enterUri = resolvedEnterUri
    )
}

private fun resolveMessageCommentLocation(rawLink: String): MessageCommentLocation? {
    val uri = runCatching { java.net.URI(rawLink) }.getOrNull() ?: return null
    val queryMap = decodeQueryMap(uri.rawQuery)
    val rootReplyId = queryMap.firstPositiveLong(
        "comment_root_id",
        "root_reply_id",
        "root_id"
    )
    val targetReplyId = queryMap.firstPositiveLong(
        "comment_secondary_id",
        "comment_id",
        "reply_id",
        "rpid",
        "target_id",
        "anchor",
        "source_id"
    ).takeIf { it > 0L } ?: resolveReplyIdFromFragment(uri.rawFragment)
    val resolvedRootReplyId = when {
        rootReplyId > 0L -> rootReplyId
        targetReplyId > 0L -> targetReplyId
        else -> 0L
    }
    if (resolvedRootReplyId <= 0L) return null
    return MessageCommentLocation(
        rootReplyId = resolvedRootReplyId,
        targetReplyId = targetReplyId.takeIf { it != resolvedRootReplyId } ?: 0L
    )
}

private fun decodeQueryMap(rawQuery: String?): Map<String, String> {
    return rawQuery
        ?.split("&")
        ?.mapNotNull { part ->
            if (part.isBlank()) return@mapNotNull null
            val pair = part.split("=", limit = 2)
            val key = decodeUrlComponentCompat(pair[0])
            val value = decodeUrlComponentCompat(pair.getOrElse(1) { "" })
            key to value
        }
        ?.toMap()
        .orEmpty()
}

private fun Map<String, String>.firstPositiveLong(vararg keys: String): Long {
    return keys.firstNotNullOfOrNull { key ->
        this[key]?.toLongOrNull()?.takeIf { it > 0L }
    } ?: 0L
}

private fun resolveReplyIdFromFragment(rawFragment: String?): Long {
    val fragment = rawFragment.orEmpty()
    if (fragment.isBlank()) return 0L
    return Regex("""reply(\d+)""", RegexOption.IGNORE_CASE)
        .find(fragment)
        ?.groupValues
        ?.getOrNull(1)
        ?.toLongOrNull()
        ?: 0L
}
