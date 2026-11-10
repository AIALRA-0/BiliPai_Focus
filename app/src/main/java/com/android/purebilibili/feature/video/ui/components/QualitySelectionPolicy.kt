package com.android.purebilibili.feature.video.ui.components

internal fun qualityUnavailableReason(
    qualityId: Int,
    playableQualityIds: List<Int>,
    isLoggedIn: Boolean,
    isVip: Boolean
): String? = when {
    qualityId > 0 && qualityId in playableQualityIds -> null
    (qualityId == 100 || qualityId >= 112) && !isVip -> "需要大会员"
    qualityId >= 80 && !isLoggedIn -> "登录后可用"
    else -> "当前视频不可切换"
}
