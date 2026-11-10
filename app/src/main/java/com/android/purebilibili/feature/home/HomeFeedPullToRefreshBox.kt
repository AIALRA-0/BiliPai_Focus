package com.android.purebilibili.feature.home

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.android.purebilibili.core.ui.AdaptivePullToRefreshBox

/**
 * 顶部间距只在刷新容器的重启作用域读取，避免顶栏折叠使 Pager 页面重新执行。
 * 指示器与 Miuix 原生刷新容器共用当前间距；列表内容保持独立的稳定插槽。
 * 玻璃、MD3 与 Miuix 继续沿用各自的刷新样式和布局参数。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeFeedPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    state: PullToRefreshState,
    indicatorTopInset: State<Dp>,
    modifier: Modifier = Modifier,
    indicator: @Composable BoxScope.(Dp) -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val topInset = indicatorTopInset.value
    AdaptivePullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        indicatorTopInset = topInset,
        modifier = modifier,
        indicator = { indicator(topInset) },
        content = content,
    )
}
