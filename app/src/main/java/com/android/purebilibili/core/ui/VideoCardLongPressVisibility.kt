package com.android.purebilibili.core.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 视频卡片长按功能开关（设置 > 外观 > 长按视频卡片）。
 *
 * 默认关闭。开启后，长按卡片可触发视频快捷预览与操作菜单；
 * 关闭时，完全禁用长按手势监听，杜绝误触并保持原生轻快点击响应。
 */
val LocalVideoCardLongPressEnabled = staticCompositionLocalOf { false }
