package com.android.purebilibili.navigation3

import com.android.purebilibili.core.ui.transition.MiuixVideoCardTransitionState

/** 离场详情仍可能与新详情同时存在；来源页面相同不代表属于同一次内容转场。 */
internal fun resolveVideoCardEntryTransitionState(
    key: BiliPaiNavKey,
    source: BiliPaiNavSourceMetadata,
    current: MiuixVideoCardTransitionState,
    retained: MiuixVideoCardTransitionState,
): MiuixVideoCardTransitionState {
    if (key !is BiliPaiNavKey.VideoDetail) return current
    val ownsSource = source.sourceRoute == key.sourceRoute &&
        source.sourceKey == "${key.sourceRoute}:${key.bvid}"
    return if (ownsSource) current else retained
}
