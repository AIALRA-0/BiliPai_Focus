package com.android.purebilibili.feature.video.screen

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.ui.AdaptivePullToRefreshBox

/** Isolates the refresh indicator's composition-time inset read from the comment rows. */
@Composable
internal fun VideoCommentRefreshContainer(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    enabled: Boolean,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    // 播放器展开时禁用刷新；适配器保留列表组合状态，并在恢复时新建原生刷新手势状态。
    AdaptivePullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        enabled = enabled,
        allowContentScrollWhileRefreshing = true,
        indicatorTopInset = contentPadding.calculateTopPadding(),
        modifier = modifier,
        content = content,
    )
}
