package com.android.purebilibili.feature.video.playback.next

import com.android.purebilibili.data.model.response.ViewInfo
import java.util.concurrent.ConcurrentHashMap

/** Shared by displayed episode order, lookahead, autoplay, and explicit previous/next. */
internal object PiliPlaybackOrder {
    private val reversed = ConcurrentHashMap<String, Boolean>()
    private fun key(seasonId: Long, bvid: String): String = if (seasonId > 0) "season:$seasonId" else "video:$bvid"
    fun setReversed(seasonId: Long, bvid: String, value: Boolean) {
        reversed[key(seasonId, bvid)] = value
    }
    fun isReversed(info: ViewInfo): Boolean {
        val season = info.ugc_season.takeUnless { info.pages.size > 1 }
        val id = season?.id?.takeIf { it > 0 }
            ?: season?.sections?.firstOrNull { it.season_id > 0 }?.season_id ?: 0L
        return reversed[key(id, info.bvid)] == true
    }
    fun step(info: ViewInfo): Int = if (isReversed(info)) -1 else 1
}
