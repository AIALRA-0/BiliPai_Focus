package com.android.purebilibili.navigation3

import androidx.compose.animation.core.LinearEasing
import com.android.purebilibili.feature.settings.isSettingsSubtreeRoute
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.navGraphicsTransition

/**
 * 平板双栏设置内切换面板时不播放整屏转场：列表壳在每个设置目的地内自渲染，
 * 栈顶变化只应表现为右栏内容瞬时替换（issue #862）。手机单栏不使用此转场。
 */
internal val SettingsPaneTransition = navGraphicsTransition(
    motion = NavMotion(
        commit = NavSettleSpec.Tween(0, LinearEasing),
        cancel = NavSettleSpec.Tween(0, LinearEasing),
        programmatic = NavSettleSpec.Tween(0, LinearEasing),
    ),
    scrim = { 0f },
) { }

/**
 * 当前一步导航是否属于设置双栏内的面板切换：两端都在设置子树内、
 * 且当前窗口为持久双栏（平板/非折叠悬停）。手机单栏恒为 false，保留常规转场。
 */
internal fun isSettingsPaneNavigation(
    persistentPanes: Boolean,
    fromKey: BiliPaiNavKey?,
    toKey: BiliPaiNavKey?,
    activeMainHostRoute: String?,
): Boolean {
    if (!persistentPanes || toKey == null || !isSettingsSubtreeRoute(toKey.routeBase)) return false
    val fromRoute = if (fromKey == BiliPaiNavKey.MainHost) activeMainHostRoute else fromKey?.routeBase
    return isSettingsSubtreeRoute(fromRoute)
}
