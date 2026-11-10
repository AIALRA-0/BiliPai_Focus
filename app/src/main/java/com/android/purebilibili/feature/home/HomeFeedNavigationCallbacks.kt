package com.android.purebilibili.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

internal class HomeFeedNavigationCallbacks(
    val onVideoClick: (HomeVideoClickRequest) -> Unit,
    val onUpClick: (Long) -> Unit,
    val onLiveClick: (Long, String, String) -> Unit,
)

/** 宿主导航状态更新只替换调用目标，不为整批首页卡片创建新的回调身份。 */
@Composable
internal fun rememberHomeFeedNavigationCallbacks(
    onVideoClick: (HomeVideoClickRequest) -> Unit,
    onUpClick: (Long) -> Unit,
    onLiveClick: (Long, String, String) -> Unit,
): HomeFeedNavigationCallbacks {
    val video = rememberUpdatedState(onVideoClick)
    val up = rememberUpdatedState(onUpClick)
    val live = rememberUpdatedState(onLiveClick)
    return remember(video, up, live) {
        HomeFeedNavigationCallbacks(
            onVideoClick = { request -> video.value(request) },
            onUpClick = { mid -> up.value(mid) },
            onLiveClick = { roomId, title, uname -> live.value(roomId, title, uname) },
        )
    }
}
