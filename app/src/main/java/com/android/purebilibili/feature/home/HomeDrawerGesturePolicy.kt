package com.android.purebilibili.feature.home

import androidx.compose.material3.DrawerValue

/**
 * 首页抽屉关闭时不参与全屏手势竞争，只允许头像点击发起打开。
 * 抽屉已经打开或正在打开时保留横向拖拽，供用户自然地侧滑关闭。
 */
internal fun shouldEnableHomeDrawerGestures(
    currentValue: DrawerValue,
    targetValue: DrawerValue,
): Boolean {
    return currentValue == DrawerValue.Open || targetValue == DrawerValue.Open
}
