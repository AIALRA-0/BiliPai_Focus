package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppChromeSizeTokens
import com.android.purebilibili.feature.video.viewmodel.CommentSortMode
import top.yukonga.miuix.kmp.blur.Backdrop

/**
 * 普通视频与番剧共用的评论操作栏，不接管列表滚动显隐。
 * 标题与排序在空间不足时换行；宿主测量整栏高度供列表占位和背景使用。
 * 玻璃只影响材质，原生排序控件保留自身尺寸及光学偏移。
 */
@Composable
internal fun VideoCommentFloatingHeader(
    count: Int,
    sortMode: CommentSortMode,
    onSortModeChange: (CommentSortMode) -> Unit,
    onSearchClick: () -> Unit,
    backdrop: Backdrop?,
    liquidGlassEnabled: Boolean,
    /** 详情页可在玻璃关闭时也使用紧凑标题；番剧等宿主保持原生默认。 */
    compactTitleTypography: Boolean = false,
    /** 标题文字随评论列表滚动隐藏的进度 0..1，0 = 完全可见；排序控件不跟随隐藏。 */
    titleCollapseProgress: () -> Float = { 0f },
    modifier: Modifier = Modifier,
) {
    val nativeSortDockLiftDp = remember {
        resolveCommentSortDockViewportOverflowDp(
            containerHeightDp = AppChromeSizeTokens.BottomBarMatchedSegmentedControlHeightDp,
            indicatorHeightDp = AppChromeSizeTokens.BottomBarMatchedSegmentedIndicatorHeightDp,
        )
    }
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        CommentListTitle(
            count = count,
            title = "${sortMode.label}评论",
            liquidGlassEnabled = liquidGlassEnabled,
            compactTypography = compactTitleTypography,
            modifier = Modifier.graphicsLayer {
                val progress = titleCollapseProgress().coerceIn(0f, 1f)
                alpha = 1f - progress
                translationY = -12.dp.toPx() * progress
            },
        )
        CommentSortFilterBar(
            sortMode = sortMode,
            onSortModeChange = onSortModeChange,
            modifier = if (liquidGlassEnabled) Modifier else
                Modifier.offset(y = (-nativeSortDockLiftDp).dp),
            miuixBackdrop = if (liquidGlassEnabled) backdrop else null,
            liquidGlassEffectsEnabled = liquidGlassEnabled,
            onSearchClick = onSearchClick,
        )
    }
}
