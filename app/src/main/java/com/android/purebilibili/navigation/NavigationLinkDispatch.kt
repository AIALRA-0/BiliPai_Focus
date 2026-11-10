package com.android.purebilibili.navigation

import com.android.purebilibili.core.util.BilibiliNavigationTarget
import com.android.purebilibili.navigation3.BiliPaiNavKey

// 链接分发只构造目的地并调用宿主回调；返回栈、切页与视频过渡仍由导航宿主管理。
internal fun dispatchNativeLinkNavigation(
    target: BilibiliNavigationTarget,
    onVideo: (String) -> Unit,
    onSearch: (String) -> Unit,
    onKey: (BiliPaiNavKey) -> Unit,
    onRoute: (String) -> Unit,
    onPopularFeed: (BilibiliNavigationTarget.PopularFeed) -> Unit,
): Boolean {
    when (target) {
        is BilibiliNavigationTarget.Video -> {
            if (target.commentRootRpid > 0L || target.commentTargetRpid > 0L) {
                onKey(BiliPaiNavKey.VideoDetail(
                    bvid = target.videoId,
                    commentRootRpid = target.commentRootRpid,
                    commentTargetRpid = target.commentTargetRpid,
                ))
            } else {
                onVideo(target.videoId)
            }
        }
        is BilibiliNavigationTarget.Dynamic -> {
            onKey(BiliPaiNavKey.DynamicDetail(
                target.dynamicId,
                commentRootRpid = target.commentRootRpid,
                commentTargetRpid = target.commentTargetRpid,
                openComments = target.openComments,
            ))
        }
        is BilibiliNavigationTarget.CommentDetail -> {
            onKey(BiliPaiNavKey.CommentDetail(
                oid = target.oid,
                rootId = target.rootReplyId,
                targetId = target.targetReplyId,
                type = target.businessId,
                enterUri = target.enterUri,
            ))
        }
        is BilibiliNavigationTarget.Search -> onSearch(target.keyword)
        is BilibiliNavigationTarget.Space -> {
            if (target.mid <= 0L) return false
            onKey(BiliPaiNavKey.Space(target.mid))
        }
        is BilibiliNavigationTarget.Live -> {
            onKey(BiliPaiNavKey.Live(roomId = target.roomId.toString()))
        }
        is BilibiliNavigationTarget.BangumiSeason -> {
            onKey(
                BiliPaiNavKey.BangumiDetail(
                    seasonId = target.seasonId,
                    mediaId = target.mediaId
                )
            )
        }
        is BilibiliNavigationTarget.BangumiEpisode -> {
            onKey(BiliPaiNavKey.BangumiDetail(seasonId = 0L, epId = target.epId))
        }
        is BilibiliNavigationTarget.Music -> {
            onRoute(ScreenRoutes.createMusicRoute(target.musicId) ?: return false)
        }
        is BilibiliNavigationTarget.ArticleCollection -> {
            onKey(BiliPaiNavKey.ArticleCollection(target.collectionId))
        }
        is BilibiliNavigationTarget.Article -> {
            onKey(BiliPaiNavKey.ArticleDetail(target.articleId, commentRootRpid = target.commentRootRpid, commentTargetRpid = target.commentTargetRpid))
        }
        is BilibiliNavigationTarget.Match -> {
            if (target.contestId <= 0L) return false
            onKey(BiliPaiNavKey.MatchInfo(target.contestId))
        }
        is BilibiliNavigationTarget.PopularFeed -> {
            if (target.subCategoryKey == "weekly") {
                onKey(BiliPaiNavKey.WeeklySeries(target.weeklyNumber))
                return true
            }
            onPopularFeed(target)
        }
    }
    return true
}

internal fun dispatchMessageLinkNavigation(
    rawLink: String,
    currentRoute: String?,
    onVideo: (String) -> Unit,
    onVideoRoute: (route: String, sourceRoute: String?) -> Unit,
    onKey: (BiliPaiNavKey) -> Unit,
    onRoute: (String) -> Unit,
) {
    when (val action = resolveMessageLinkNavigationAction(rawLink)) {
        is MessageLinkNavigationAction.Video -> {
            onVideo(action.videoId)
        }
        is MessageLinkNavigationAction.CommentDetail -> {
            onKey(
                BiliPaiNavKey.CommentDetail(
                    oid = action.oid,
                    rootId = action.rootReplyId,
                    targetId = action.targetReplyId,
                    type = action.businessId,
                    enterUri = action.enterUri
                )
            )
        }
        is MessageLinkNavigationAction.VideoComment -> {
            onVideoRoute(
                VideoRoute.createRoute(
                    bvid = action.videoId,
                    cid = 0L,
                    coverUrl = "",
                    commentRootRpid = action.rootReplyId,
                    commentTargetRpid = action.targetReplyId
                ),
                currentRoute
            )
        }
        is MessageLinkNavigationAction.Dynamic -> {
            onKey(BiliPaiNavKey.DynamicDetail(action.dynamicId))
        }
        is MessageLinkNavigationAction.DynamicComment -> {
            onKey(
                BiliPaiNavKey.DynamicDetail(
                    dynamicId = action.dynamicId,
                    commentRootRpid = action.rootReplyId,
                    commentTargetRpid = action.targetReplyId,
                    openComments = action.openComments,
                )
            )
        }
        is MessageLinkNavigationAction.Space -> {
            onKey(BiliPaiNavKey.Space(action.mid))
        }
        is MessageLinkNavigationAction.Live -> {
            onKey(BiliPaiNavKey.Live(roomId = action.roomId.toString()))
        }
        is MessageLinkNavigationAction.BangumiSeason -> {
            onKey(
                BiliPaiNavKey.BangumiDetail(
                    seasonId = action.seasonId,
                    mediaId = action.mediaId
                )
            )
        }
        is MessageLinkNavigationAction.BangumiEpisode -> {
            onKey(BiliPaiNavKey.BangumiDetail(seasonId = 0L, epId = action.epId))
        }
        is MessageLinkNavigationAction.Music -> {
            ScreenRoutes.createMusicRoute(action.musicId)?.let { onRoute(it) }
                ?: onKey(BiliPaiNavKey.Web(rawLink))
        }
        is MessageLinkNavigationAction.ArticleCollection -> {
            onKey(BiliPaiNavKey.ArticleCollection(action.collectionId))
        }
        is MessageLinkNavigationAction.Article -> {
            onKey(BiliPaiNavKey.ArticleDetail(action.articleId, commentRootRpid = action.rootReplyId, commentTargetRpid = action.targetReplyId))
        }
        is MessageLinkNavigationAction.Web -> {
            val url = action.url.trim()
            if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
                onKey(BiliPaiNavKey.Web(url))
            }
        }
    }
}
