package com.android.purebilibili.feature.home

import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.Stat
import com.android.purebilibili.data.model.response.VideoItem

internal fun resolveDynamicArchiveAid(
    archiveAid: String,
    fallbackId: Long
): Long {
    return archiveAid.toLongOrNull()?.takeIf { it > 0 } ?: fallbackId
}

internal fun shouldIncludeHomeFollowDynamicInVideoFeed(
    archiveBvid: String
): Boolean {
    return archiveBvid.isNotBlank()
}

internal fun mapHomeFollowDynamicItemsToVideoItems(
    items: List<DynamicItem>
): List<VideoItem> {
    return items.mapNotNull { item ->
        val archive = item.modules.module_dynamic?.major?.archive ?: return@mapNotNull null
        if (!shouldIncludeHomeFollowDynamicInVideoFeed(archive.bvid)) {
            return@mapNotNull null
        }

        val resolvedAid = resolveDynamicArchiveAid(
            archiveAid = archive.aid,
            fallbackId = 0L
        )

        VideoItem(
            id = resolvedAid,
            bvid = archive.bvid,
            dynamicId = item.id_str.trim(),
            aid = resolvedAid,
            title = archive.title,
            pic = archive.cover,
            pubdate = item.modules.module_author?.pub_ts ?: 0L,
            duration = parseHomeFollowDurationText(archive.duration_text),
            owner = Owner(
                mid = item.modules.module_author?.mid ?: 0L,
                name = item.modules.module_author?.name ?: "",
                face = item.modules.module_author?.face ?: ""
            ),
            stat = Stat(
                view = parseHomeFollowStatText(archive.stat.play),
                danmaku = parseHomeFollowStatText(archive.stat.danmaku)
            )
        )
    }
}

private fun parseHomeFollowDurationText(text: String): Int {
    val parts = text.split(":")
    return try {
        when (parts.size) {
            2 -> parts[0].toInt() * 60 + parts[1].toInt()
            3 -> parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toInt()
            else -> 0
        }
    } catch (_: Exception) {
        0
    }
}

private fun parseHomeFollowStatText(text: String): Int {
    return try {
        when {
            text.contains("万") -> (text.replace("万", "").toFloat() * 10_000).toInt()
            text.contains("亿") -> (text.replace("亿", "").toFloat() * 100_000_000).toInt()
            else -> text.toIntOrNull() ?: 0
        }
    } catch (_: Exception) {
        0
    }
}
