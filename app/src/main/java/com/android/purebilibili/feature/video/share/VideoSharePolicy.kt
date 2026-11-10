package com.android.purebilibili.feature.video.share

import android.content.ClipData
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri

internal const val WECHAT_PACKAGE_NAME = "com.tencent.mm"
internal const val QQ_PACKAGE_NAME = "com.tencent.mobileqq"

internal data class VideoSharePayload(
    val title: String,
    val bvid: String,
    val coverUrl: String,
    val url: String,
    val text: String,
    val upName: String = "",
    val playCountText: String = "",
    val page: Int = 1,
    val positionMs: Long = 0,
    val authorId: Long = 0,
)

internal enum class VideoShareTarget(val packageName: String?) {
    BILIBILI_FRIENDS(null),
    BILIBILI_DYNAMIC(null),
    WECHAT(WECHAT_PACKAGE_NAME),
    QQ(QQ_PACKAGE_NAME),
    COPY_LINK(null),
    MORE(null)
}

internal fun resolveVideoShareRecipientIds(
    selectedIds: Set<Long>,
    followings: List<com.android.purebilibili.data.model.response.FollowingUser>,
    selfMid: Long,
): List<Long> = followings.asSequence()
    .map { it.mid }
    .filter { it > 0L && it != selfMid && it in selectedIds }
    .distinct()
    .toList()

internal enum class VideoShareStyle {
    LINK,
    CARD,
}

internal fun buildVideoSharePayload(
    title: String,
    bvid: String,
    coverUrl: String = "",
    upName: String = "",
    playCountText: String = "",
    page: Int = 1,
    positionMs: Long = 0,
    authorId: Long = 0,
): VideoSharePayload {
    val cleanTitle = title.trim()
    val cleanBvid = bvid.trim()
    val fallbackTitle = cleanTitle.ifBlank { cleanBvid }
    val url = "https://www.bilibili.com/video/$cleanBvid" + if (page > 1) "?p=$page" else ""
    return VideoSharePayload(
        title = fallbackTitle,
        bvid = cleanBvid,
        coverUrl = coverUrl.trim(),
        url = url,
        text = "【$fallbackTitle】\n$url",
        upName = upName.trim(),
        playCountText = playCountText.trim(),
        page = page.coerceAtLeast(1),
        positionMs = positionMs.coerceAtLeast(0),
        authorId = authorId,
    )
}

internal fun resolveVideoShareCardMetaLine(payload: VideoSharePayload): String {
    val parts = buildList {
        if (payload.upName.isNotBlank()) {
            add("UP主：${payload.upName}")
        }
        if (payload.playCountText.isNotBlank()) {
            add("播放：${payload.playCountText}")
        }
    }
    return parts.joinToString("  ·  ")
}

/**
 * 宿主 App 常把 Display Name / 文件名当消息标题，因此用净化后的视频标题命名。
 */
internal fun resolveVideoShareCardFileName(payload: VideoSharePayload): String {
    val rawTitle = payload.title.ifBlank { payload.bvid }.ifBlank { "video" }
    val sanitized = rawTitle
        .map { ch ->
            if (ch.isLetterOrDigit() || ch in "._- 《》【】（）()、，。！？") ch else '_'
        }
        .joinToString("")
        .trim()
        .replace(Regex("\\s+"), "_")
        .take(40)
        .trim('_')
        .ifBlank { payload.bvid.ifBlank { "video" } }
    return "BiliPai_share_card_$sanitized.jpg"
}

internal fun buildVideoShareIntent(payload: VideoSharePayload): Intent {
    return Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, payload.title)
        putExtra(Intent.EXTRA_TEXT, payload.text)
    }
}

internal fun buildTargetedShareIntent(
    payload: VideoSharePayload,
    packageName: String,
    activityClassName: String? = null,
): Intent {
    return buildVideoShareIntent(payload).apply {
        setPackage(packageName)
        if (activityClassName != null) {
            setComponent(ComponentName(packageName, activityClassName))
        }
    }
}

/**
 * 卡片图分享：标题走 EXTRA_TITLE/SUBJECT，正文只带链接便于跳转。
 */
internal fun buildVideoCoverShareIntent(
    payload: VideoSharePayload,
    coverUri: Uri,
    mimeType: String,
    packageName: String? = null,
    activityClassName: String? = null,
    contentResolver: ContentResolver? = null
): Intent {
    return Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_SUBJECT, payload.title)
        putExtra(Intent.EXTRA_TITLE, payload.title)
        putExtra(Intent.EXTRA_TEXT, payload.url)
        putExtra(Intent.EXTRA_STREAM, coverUri)
        clipData = ClipData.newUri(contentResolver, payload.title, coverUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        packageName?.let { setPackage(it) }
        if (packageName != null && activityClassName != null) {
            setComponent(ComponentName(packageName, activityClassName))
        }
    }
}

internal fun resolveVideoShareChooserTitle(payload: VideoSharePayload): String {
    return "分享「${payload.title}」"
}

/**
 * 同一分享包下可响应 ACTION_SEND 的 Activity 候选。
 */
internal data class ShareActivityCandidate(
    val packageName: String,
    val className: String,
    val label: String,
)

private val SHARE_EXCLUDE_LABEL_TOKENS = listOf(
    "收藏",
    "电脑",
    "闪传",
    "传输",
    "空间",
    "朋友圈",
    "时间线",
    "卡包",
    "表情",
    "钱包",
    "Favorite",
    "Timeline",
    "TimeLine",
    "Flash",
    "Wallet",
)

private val SHARE_EXCLUDE_CLASS_TOKENS = listOf(
    "AddFavorite",
    "Favorite",
    "Fav",
    "Timeline",
    "TimeLine",
    "FlashTransfer",
    "QfileJump",
    "MyComputer",
    "Wallet",
    "QZone",
    "Qzone",
)

private val SHARE_FRIEND_LABEL_TOKENS = listOf(
    "发送给",
    "发给",
    "朋友",
    "好友",
    "聊天",
    "SendTo",
    "Send to",
    "Friend",
    "Chat",
)

private val SHARE_FRIEND_CLASS_TOKENS = listOf(
    "SendToFriend",
    "ShareToFriend",
    "ShareImgUI",
    "SendToFriendUI",
    "JumpActivity",
    "ShareUI",
)

private fun containsAnyToken(value: String, tokens: List<String>): Boolean {
    return tokens.any { token -> value.contains(token, ignoreCase = true) }
}

/**
 * 排除收藏 / 闪传 / 我的电脑等非「发给好友」入口，避免系统 Resolver 二次选择。
 */
internal fun isExcludedShareActivity(candidate: ShareActivityCandidate): Boolean {
    return containsAnyToken(candidate.label, SHARE_EXCLUDE_LABEL_TOKENS) ||
        containsAnyToken(candidate.className, SHARE_EXCLUDE_CLASS_TOKENS)
}

/**
 * 为分享入口打分：好友聊天入口优先；被排除的入口返回 0。
 */
internal fun scoreShareActivityCandidate(candidate: ShareActivityCandidate): Int {
    if (isExcludedShareActivity(candidate)) return 0
    var score = 1
    if (containsAnyToken(candidate.label, SHARE_FRIEND_LABEL_TOKENS)) {
        score += 12
    }
    val simpleName = candidate.className.substringAfterLast('.')
    if (containsAnyToken(candidate.className, SHARE_FRIEND_CLASS_TOKENS) ||
        containsAnyToken(simpleName, SHARE_FRIEND_CLASS_TOKENS)
    ) {
        score += 10
    }
    if (simpleName.length <= 20) {
        score += 1
    }
    return score
}

/**
 * 从候选中选出最接近「发送给好友 / 选一个聊天」的 Activity；全部不达标时返回 null，回退包级分享。
 */
internal fun resolvePreferredShareActivity(
    candidates: List<ShareActivityCandidate>,
): ShareActivityCandidate? {
    return candidates
        .mapNotNull { candidate ->
            val score = scoreShareActivityCandidate(candidate)
            if (score <= 0) null else candidate to score
        }
        .maxByOrNull { it.second }
        ?.first
}

internal fun VideoSharePayload.asContentSharePayload() = com.android.purebilibili.feature.share.ContentSharePayload(
    key = "video:$bvid:$page", title = title, url = url, cover = coverUrl,
    author = upName, authorId = authorId, bvid = bvid, dynamicType = 8, positionMs = positionMs,
)

internal fun VideoSharePayload.withPosition(enabled: Boolean): VideoSharePayload {
    if (!enabled || positionMs <= 0) return this
    val preciseUrl = com.android.purebilibili.feature.share.buildPositionShareUrl(url, positionMs)
    return copy(url = preciseUrl, text = "【$title】\n$preciseUrl")
}
