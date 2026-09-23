package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.style.TextOverflow

/**
 * 视频卡片标题完整展示(设置 > 外观 > 首页与列表 > 完整卡片展示)。
 *
 * 关闭时标题最多两行并省略；开启时完整展开。播放、弹幕等卡片信息始终完整显示。
 * 首页卡片与分区、搜索、相关推荐等其它视频卡片通过本 CompositionLocal 跟随同一开关。
 */
val LocalFullVideoCardContentVisible = staticCompositionLocalOf { false }

const val VIDEO_CARD_TRUNCATED_TITLE_MAX_LINES = 2

fun resolveVideoCardTitleMaxLines(
    showFullCardContent: Boolean,
    truncatedMaxLines: Int = VIDEO_CARD_TRUNCATED_TITLE_MAX_LINES,
): Int = if (showFullCardContent) Int.MAX_VALUE else truncatedMaxLines

fun resolveVideoCardTitleOverflow(
    showFullCardContent: Boolean,
): TextOverflow = if (showFullCardContent) TextOverflow.Visible else TextOverflow.Ellipsis

@Composable
@ReadOnlyComposable
fun videoCardTitleMaxLines(
    truncatedMaxLines: Int = VIDEO_CARD_TRUNCATED_TITLE_MAX_LINES,
): Int = resolveVideoCardTitleMaxLines(
    showFullCardContent = LocalFullVideoCardContentVisible.current,
    truncatedMaxLines = truncatedMaxLines,
)

@Composable
@ReadOnlyComposable
fun videoCardTitleOverflow(): TextOverflow {
    return resolveVideoCardTitleOverflow(LocalFullVideoCardContentVisible.current)
}
