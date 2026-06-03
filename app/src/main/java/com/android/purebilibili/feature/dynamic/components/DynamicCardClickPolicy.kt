package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.core.util.BilibiliUrlParser
import com.android.purebilibili.core.util.BilibiliNavigationTarget
import com.android.purebilibili.core.util.BilibiliNavigationTargetParser
import com.android.purebilibili.data.model.response.ArchiveMajor
import com.android.purebilibili.data.model.response.ArticleMajor
import com.android.purebilibili.data.model.response.DrawItem
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.LiveRcmdMajor
import com.android.purebilibili.data.model.response.OpusContentBlock
import com.android.purebilibili.data.model.response.OpusLinkCard
import com.android.purebilibili.data.model.response.OpusMajor
import com.android.purebilibili.data.model.response.UgcSeasonMajor
import com.android.purebilibili.feature.dynamic.model.LiveContentInfo
import kotlinx.serialization.json.Json

internal sealed interface DynamicCardPrimaryAction {
    data class OpenVideo(val bvid: String) : DynamicCardPrimaryAction
    data class OpenBangumi(val seasonId: Long, val epId: Long) : DynamicCardPrimaryAction
    data class OpenArticle(val articleId: Long, val title: String) : DynamicCardPrimaryAction
    data class OpenDynamicDetail(val dynamicId: String) : DynamicCardPrimaryAction
    data class OpenLive(val roomId: Long, val title: String, val uname: String) : DynamicCardPrimaryAction
    data class OpenUser(val mid: Long) : DynamicCardPrimaryAction
    data object None : DynamicCardPrimaryAction
}

internal sealed interface DynamicCardMediaAction {
    data class PreviewImages(
        val images: List<String>,
        val initialIndex: Int
    ) : DynamicCardMediaAction

    data class OpenDynamicDetail(val dynamicId: String) : DynamicCardMediaAction

    data object None : DynamicCardMediaAction
}

internal sealed interface DynamicOpusLinkCardAction {
    data class OpenVideo(val videoId: String) : DynamicOpusLinkCardAction
    data class OpenDynamicDetail(val dynamicId: String) : DynamicOpusLinkCardAction
    data class OpenArticle(val articleId: Long, val title: String) : DynamicOpusLinkCardAction
    data class OpenLive(val roomId: Long) : DynamicOpusLinkCardAction
    data class OpenUser(val mid: Long) : DynamicOpusLinkCardAction
    data class OpenBangumi(val seasonId: Long, val epId: Long) : DynamicOpusLinkCardAction
    data class OpenExternalUrl(val url: String) : DynamicOpusLinkCardAction
    data object None : DynamicOpusLinkCardAction
}

internal fun resolveBvidFromRawVideoTarget(rawValue: String?): String? {
    val target = rawValue?.trim().orEmpty()
    if (target.isEmpty()) return null
    val parsed = BilibiliUrlParser.parse(target)
    val bvid = parsed.bvid?.trim()
    if (!bvid.isNullOrEmpty()) return bvid
    return parsed.aid?.takeIf { it > 0 }?.let { "av$it" }
}

internal fun resolveArchivePlayableBvid(archive: ArchiveMajor): String? {
    return resolveBvidFromRawVideoTarget(archive.bvid)
        ?: resolveBvidFromRawVideoTarget(archive.jump_url)
        ?: archive.aid.trim().toLongOrNull()?.takeIf { it > 0L }?.let { "av$it" }
}

internal fun resolveArchiveBangumiTarget(archive: ArchiveMajor): DynamicCardPrimaryAction.OpenBangumi? {
    var seasonId = archive.season_id.takeIf { it > 0L } ?: 0L
    var epId = archive.epid.takeIf { it > 0L } ?: 0L

    when (val target = BilibiliNavigationTargetParser.parse(archive.jump_url)) {
        is BilibiliNavigationTarget.BangumiSeason -> {
            if (seasonId <= 0L) seasonId = target.seasonId
        }
        is BilibiliNavigationTarget.BangumiEpisode -> {
            if (epId <= 0L) epId = target.epId
        }
        else -> Unit
    }

    return if (seasonId > 0L || epId > 0L) {
        DynamicCardPrimaryAction.OpenBangumi(
            seasonId = seasonId,
            epId = epId
        )
    } else {
        null
    }
}

internal fun resolveUgcSeasonPlayableBvid(season: UgcSeasonMajor): String? {
    return season.archive?.let(::resolveArchivePlayableBvid)
        ?: resolveBvidFromRawVideoTarget(season.jump_url)
        ?: season.aid.takeIf { it > 0L }?.let { "av$it" }
}

internal fun resolveDynamicWatchLaterAid(item: DynamicItem): Long? {
    val target = item.orig ?: item
    val major = target.modules.module_dynamic?.major ?: return null
    major.archive?.aid
        ?.trim()
        ?.toLongOrNull()
        ?.takeIf { it > 0L }
        ?.let { return it }
    major.ugc_season?.archive?.aid
        ?.trim()
        ?.toLongOrNull()
        ?.takeIf { it > 0L }
        ?.let { return it }
    return major.ugc_season?.aid?.takeIf { it > 0L }
}

internal fun resolveUgcSeasonArchiveFallback(season: UgcSeasonMajor): ArchiveMajor? {
    season.archive?.let { return it }
    val hasRenderableContent = season.title.isNotBlank() || season.cover.isNotBlank()
    if (!hasRenderableContent) return null
    val bvid = resolveUgcSeasonPlayableBvid(season).orEmpty()
    return ArchiveMajor(
        aid = season.aid.takeIf { it > 0 }?.toString().orEmpty(),
        bvid = bvid,
        title = season.title,
        cover = season.cover,
        desc = season.desc.ifBlank { season.intro },
        duration_text = season.duration_text,
        stat = com.android.purebilibili.data.model.response.ArchiveStat(
            play = season.stat.play,
            danmaku = season.stat.danmaku
        ),
        jump_url = season.jump_url
    )
}

internal fun resolveArticleCoverUrls(article: ArticleMajor): List<String> {
    return article.covers
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

internal fun resolveArticleCoverDrawItems(article: ArticleMajor): List<DrawItem> {
    return resolveArticleCoverUrls(article).map { cover ->
        DrawItem(src = cover)
    }
}

internal fun resolveDynamicOpusPresentationBlocks(
    opus: OpusMajor,
    isDetail: Boolean
): List<OpusContentBlock> {
    return if (isDetail) opus.contentBlocks else emptyList()
}

internal fun resolveDynamicOpusPreviewImageLimit(isDetail: Boolean): Int? {
    return if (isDetail) null else 9
}

internal fun resolveDynamicOpusLinkCardAction(card: OpusLinkCard): DynamicOpusLinkCardAction {
    val url = card.jumpUrl.trim()
    if (url.isBlank()) return DynamicOpusLinkCardAction.None
    return when (val target = BilibiliNavigationTargetParser.parse(url)) {
        is BilibiliNavigationTarget.Video -> DynamicOpusLinkCardAction.OpenVideo(target.videoId)
        is BilibiliNavigationTarget.Dynamic -> DynamicOpusLinkCardAction.OpenDynamicDetail(target.dynamicId)
        is BilibiliNavigationTarget.Article -> DynamicOpusLinkCardAction.OpenArticle(
            articleId = target.articleId,
            title = card.title
        )
        is BilibiliNavigationTarget.Live -> DynamicOpusLinkCardAction.OpenLive(target.roomId)
        is BilibiliNavigationTarget.Space -> DynamicOpusLinkCardAction.OpenUser(target.mid)
        is BilibiliNavigationTarget.BangumiSeason -> DynamicOpusLinkCardAction.OpenBangumi(
            seasonId = target.seasonId,
            epId = 0L
        )
        is BilibiliNavigationTarget.BangumiEpisode -> DynamicOpusLinkCardAction.OpenBangumi(
            seasonId = 0L,
            epId = target.epId
        )
        is BilibiliNavigationTarget.Music,
        is BilibiliNavigationTarget.Search,
        null -> DynamicOpusLinkCardAction.OpenExternalUrl(url)
    }
}

internal fun resolveDynamicCardPrimaryAction(item: DynamicItem): DynamicCardPrimaryAction {
    val target = item.orig ?: item
    val authorMid = target.modules.module_author?.mid ?: 0L
    val major = target.modules.module_dynamic?.major
    major?.pgc?.let(::resolveArchiveBangumiTarget)?.let { return it }
    val bvid = major?.archive?.let(::resolveArchivePlayableBvid)
        ?: major?.ugc_season?.let(::resolveUgcSeasonPlayableBvid)
    if (bvid != null) {
        return DynamicCardPrimaryAction.OpenVideo(bvid)
    }

    major?.article
        ?.takeIf { it.id > 0L }
        ?.let { article ->
            when (val target = BilibiliNavigationTargetParser.parse(article.jump_url)) {
                is BilibiliNavigationTarget.Dynamic -> {
                    return DynamicCardPrimaryAction.OpenDynamicDetail(target.dynamicId)
                }
                is BilibiliNavigationTarget.Article -> {
                    return DynamicCardPrimaryAction.OpenArticle(
                        articleId = target.articleId,
                        title = article.title.ifBlank { article.desc }
                    )
                }
                else -> Unit
            }
            return DynamicCardPrimaryAction.OpenArticle(
                articleId = article.id,
                title = article.title.ifBlank { article.desc }
            )
        }

    major?.live_rcmd?.let { live ->
        resolveLivePrimaryAction(
            liveRcmd = live,
            fallbackName = target.modules.module_author?.name.orEmpty()
        )?.let { return it }
    }

    val dynamicId = target.id_str.trim().takeIf { it.isNotEmpty() }
    if (dynamicId != null) {
        return DynamicCardPrimaryAction.OpenDynamicDetail(dynamicId)
    }

    if (authorMid > 0) {
        return DynamicCardPrimaryAction.OpenUser(authorMid)
    }

    return DynamicCardPrimaryAction.None
}

internal fun resolveDynamicCardMediaAction(
    item: DynamicItem,
    clickedIndex: Int,
    isDetail: Boolean = true
): DynamicCardMediaAction {
    val target = item.orig ?: item
    val major = target.modules.module_dynamic?.major
    if (major == null) return DynamicCardMediaAction.None
    if (!isDetail && major.opus != null) {
        val dynamicId = target.id_str.trim()
        return if (dynamicId.isNotEmpty()) {
            DynamicCardMediaAction.OpenDynamicDetail(dynamicId)
        } else {
            DynamicCardMediaAction.None
        }
    }
    val images = when {
        major.draw != null && major.draw.items.isNotEmpty() -> major.draw.items.map { it.src }
        major.opus != null && major.opus.pics.isNotEmpty() -> major.opus.pics.map { it.url }
        major.article != null -> resolveArticleCoverUrls(major.article)
        else -> emptyList()
    }
    if (clickedIndex !in images.indices) return DynamicCardMediaAction.None
    return DynamicCardMediaAction.PreviewImages(
        images = images,
        initialIndex = clickedIndex
    )
}

internal fun dispatchDynamicCardPrimaryAction(
    action: DynamicCardPrimaryAction,
    onVideoClick: (String) -> Unit,
    onBangumiClick: (Long, Long) -> Unit,
    onArticleClick: ((Long, String) -> Unit)? = null,
    onDynamicDetailClick: ((String) -> Unit)?,
    onUserClick: (Long) -> Unit,
    onLiveClick: (Long, String, String) -> Unit
) {
    when (action) {
        is DynamicCardPrimaryAction.OpenVideo -> onVideoClick(action.bvid)
        is DynamicCardPrimaryAction.OpenBangumi -> onBangumiClick(action.seasonId, action.epId)
        is DynamicCardPrimaryAction.OpenArticle -> onArticleClick?.invoke(action.articleId, action.title)
        is DynamicCardPrimaryAction.OpenDynamicDetail -> onDynamicDetailClick?.invoke(action.dynamicId)
        is DynamicCardPrimaryAction.OpenLive -> onLiveClick(action.roomId, action.title, action.uname)
        is DynamicCardPrimaryAction.OpenUser -> onUserClick(action.mid)
        DynamicCardPrimaryAction.None -> Unit
    }
}

internal fun shouldEnableDynamicCardPrimaryClick(
    action: DynamicCardPrimaryAction,
    hasArticleClick: Boolean,
    hasDynamicDetailClick: Boolean,
    hasPrimaryClickOverride: Boolean
): Boolean {
    if (hasPrimaryClickOverride) return true
    return when (action) {
        is DynamicCardPrimaryAction.OpenArticle -> hasArticleClick
        is DynamicCardPrimaryAction.OpenDynamicDetail -> hasDynamicDetailClick
        DynamicCardPrimaryAction.None -> false
        else -> true
    }
}

internal fun dispatchDynamicCardPrimaryClick(
    item: DynamicItem,
    action: DynamicCardPrimaryAction,
    onPrimaryClickOverride: ((DynamicItem) -> Unit)?,
    onVideoClick: (String) -> Unit,
    onBangumiClick: (Long, Long) -> Unit,
    onArticleClick: ((Long, String) -> Unit)? = null,
    onDynamicDetailClick: ((String) -> Unit)?,
    onUserClick: (Long) -> Unit,
    onLiveClick: (Long, String, String) -> Unit
) {
    if (onPrimaryClickOverride != null) {
        onPrimaryClickOverride(item)
        return
    }
    dispatchDynamicCardPrimaryAction(
        action = action,
        onVideoClick = onVideoClick,
        onBangumiClick = onBangumiClick,
        onArticleClick = onArticleClick,
        onDynamicDetailClick = onDynamicDetailClick,
        onUserClick = onUserClick,
        onLiveClick = onLiveClick
    )
}

private val dynamicLiveJson = Json { ignoreUnknownKeys = true }

private fun resolveLivePrimaryAction(
    liveRcmd: LiveRcmdMajor,
    fallbackName: String = ""
): DynamicCardPrimaryAction.OpenLive? {
    val payload = runCatching {
        dynamicLiveJson.decodeFromString<LiveContentInfo>(liveRcmd.content)
    }.getOrNull()

    val liveInfo = payload?.live_play_info ?: return null
    val roomId = liveInfo.room_id.takeIf { it > 0 } ?: return null
    val title = liveInfo.title.ifBlank { liveInfo.link }
    val uname = fallbackName.ifBlank { liveInfo.uid.takeIf { it > 0 }?.toString().orEmpty() }
    return DynamicCardPrimaryAction.OpenLive(
        roomId = roomId,
        title = title.ifBlank { "直播间" },
        uname = uname
    )
}
