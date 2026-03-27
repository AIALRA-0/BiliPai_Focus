package com.android.purebilibili.navigation

import com.android.purebilibili.core.util.BilibiliNavigationTarget
import com.android.purebilibili.core.util.BilibiliNavigationTargetParser

internal sealed interface MessageLinkNavigationAction {
    data class Video(val videoId: String) : MessageLinkNavigationAction
    data class Dynamic(val dynamicId: String) : MessageLinkNavigationAction
    data class Space(val mid: Long) : MessageLinkNavigationAction
    data class Live(val roomId: Long) : MessageLinkNavigationAction
    data class BangumiSeason(val seasonId: Long) : MessageLinkNavigationAction
    data class BangumiEpisode(val epId: Long) : MessageLinkNavigationAction
    data class Music(val musicId: String) : MessageLinkNavigationAction
    data class Web(val url: String) : MessageLinkNavigationAction
}

internal fun resolveMessageLinkNavigationAction(rawLink: String): MessageLinkNavigationAction {
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
