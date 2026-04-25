package com.android.purebilibili.feature.story

import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.Page
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.Stat
import com.android.purebilibili.data.model.response.StoryItem
import com.android.purebilibili.data.model.response.ViewInfo

internal data class StoryPortraitFeed(
    val initialInfo: ViewInfo,
    val recommendations: List<RelatedVideo>
)

internal fun resolveNextStoryFeedAid(
    previousAid: Long,
    items: List<StoryItem>
): Long {
    return items.asReversed()
        .firstNotNullOfOrNull { item -> item.playerArgs?.aid?.takeIf { it > 0L } }
        ?: previousAid
}

internal fun mergeStoryFeedItems(
    existingItems: List<StoryItem>,
    newItems: List<StoryItem>
): List<StoryItem> {
    val seenAids = existingItems.mapNotNull { it.playerArgs?.aid?.takeIf { aid -> aid > 0L } }.toMutableSet()
    val seenBvids = existingItems.mapNotNull { it.playerArgs?.bvid?.trim()?.takeIf { bvid -> bvid.isNotEmpty() } }.toMutableSet()
    val seenIds = existingItems.mapNotNull { it.id.takeIf { id -> id > 0L } }.toMutableSet()
    val merged = existingItems.toMutableList()

    newItems.forEach { item ->
        val aid = item.playerArgs?.aid ?: 0L
        val bvid = item.playerArgs?.bvid?.trim().orEmpty()
        val id = item.id
        val alreadySeen = (aid > 0L && aid in seenAids) ||
            (bvid.isNotEmpty() && bvid in seenBvids) ||
            (id > 0L && id in seenIds)

        if (!alreadySeen) {
            merged += item
            if (aid > 0L) seenAids += aid
            if (bvid.isNotEmpty()) seenBvids += bvid
            if (id > 0L) seenIds += id
        }
    }

    return merged
}

internal fun buildStoryPortraitFeed(items: List<StoryItem>): StoryPortraitFeed? {
    val playableItems = items.mapNotNull(::toPlayableStoryItem)
    val initial = playableItems.firstOrNull() ?: return null
    return StoryPortraitFeed(
        initialInfo = initial.toViewInfo(),
        recommendations = playableItems.drop(1).map { it.toRelatedVideo() }
    )
}

private data class PlayableStoryItem(
    val source: StoryItem,
    val aid: Long,
    val cid: Long,
    val playbackId: String
) {
    fun toViewInfo(): ViewInfo {
        return ViewInfo(
            bvid = playbackId,
            aid = aid,
            cid = cid,
            title = source.title,
            desc = source.desc,
            pic = source.cover,
            owner = source.toOwner(),
            stat = source.toStat(),
            pages = listOf(
                Page(
                    cid = cid,
                    page = 1,
                    part = source.title,
                    duration = source.duration.toLong().coerceAtLeast(0L)
                )
            )
        )
    }

    fun toRelatedVideo(): RelatedVideo {
        return RelatedVideo(
            aid = aid,
            bvid = playbackId,
            cid = cid,
            title = source.title,
            pic = source.cover,
            owner = source.toOwner(),
            stat = source.toStat(),
            duration = source.duration.coerceAtLeast(0)
        )
    }
}

private fun toPlayableStoryItem(item: StoryItem): PlayableStoryItem? {
    val args = item.playerArgs ?: return null
    val aid = args.aid.takeIf { it > 0L } ?: return null
    val cid = args.cid.takeIf { it > 0L } ?: return null
    val playbackId = args.bvid.trim().ifBlank { "av$aid" }
    return PlayableStoryItem(
        source = item,
        aid = aid,
        cid = cid,
        playbackId = playbackId
    )
}

private fun StoryItem.toOwner(): Owner {
    return Owner(
        mid = owner?.mid ?: 0L,
        name = owner?.name.orEmpty(),
        face = owner?.face.orEmpty()
    )
}

private fun StoryItem.toStat(): Stat {
    return Stat(
        view = stat?.view ?: 0,
        danmaku = stat?.danmaku ?: 0,
        reply = stat?.reply ?: 0,
        like = stat?.like ?: 0,
        coin = stat?.coin ?: 0,
        favorite = stat?.favorite ?: 0,
        share = stat?.share ?: 0
    )
}
