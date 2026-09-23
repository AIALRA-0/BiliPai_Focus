package com.android.purebilibili.core.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * UP 主认证徽章与头像的全局可见性(设置 > 外观 > 首页与列表)。
 *
 * 首页卡片已由 HomeScreen 显式传入设置值;相关推荐、搜索、分区等其它
 * 视频卡片通过本 CompositionLocal 默认跟随同一开关,实现全局生效。
 */
data class UpBadgeVisibility(
    val showBadges: Boolean,
    val showAvatars: Boolean,
) {
    companion object {
        val Default = UpBadgeVisibility(showBadges = false, showAvatars = false)
    }
}

val LocalUpBadgeVisibility = staticCompositionLocalOf {
    UpBadgeVisibility.Default
}
