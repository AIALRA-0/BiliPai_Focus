package com.android.purebilibili.feature.home

/**
 * 首页只在自己是当前顶层目的地时才接管系统栏。
 *
 * 共享元素转场会保留首页以提供源卡片；此时详情页才是系统栏的所有者，首页不能
 * 再次 show(statusBars)，否则会覆盖“播放页隐藏状态栏”的用户选择。
 */
internal fun shouldApplyHomeSystemBars(isTopLevelActive: Boolean): Boolean = isTopLevelActive

internal fun resolveHomeStatusBarDarkIcons(
    hasTopSkinArtwork: Boolean,
    skinColorMode: String?,
    topSkinTintIsLight: Boolean,
    defaultBackgroundIsLight: Boolean,
): Boolean {
    if (!hasTopSkinArtwork) return defaultBackgroundIsLight
    return when (skinColorMode?.trim()?.lowercase()) {
        "dark" -> false
        "light" -> true
        else -> topSkinTintIsLight
    }
}
