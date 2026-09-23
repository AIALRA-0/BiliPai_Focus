package com.android.purebilibili.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.PullToRefresh as MiuixPullToRefresh
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState as rememberMiuixPullToRefreshState

/**
 * @param indicatorTopInset Distance from this box's top to where the indicator
 * should sit — height of **overlaid** chrome only. Scaffold-padded bodies use 0.
 * Custom [indicator] slots must apply the same inset themselves (e.g. home).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptivePullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    /**
     * Overlay chrome height above the visual content start. Applied to MIUIX
     * [contentPadding] and the default Material indicator.
     */
    indicatorTopInset: Dp = 0.dp,
    state: PullToRefreshState = rememberPullToRefreshState(),
    contentAlignment: Alignment = Alignment.TopStart,
    indicator: @Composable BoxScope.() -> Unit = {
        AdaptivePullToRefreshDefaultIndicator(
            isRefreshing = isRefreshing,
            state = state,
            indicatorTopInset = indicatorTopInset,
        )
    },
    content: @Composable BoxScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val mergedContentPadding = remember(contentPadding, indicatorTopInset, layoutDirection) {
        PaddingValues(
            start = contentPadding.calculateStartPadding(layoutDirection),
            top = contentPadding.calculateTopPadding() + indicatorTopInset,
            end = contentPadding.calculateEndPadding(layoutDirection),
            bottom = contentPadding.calculateBottomPadding(),
        )
    }

    when (rememberPresetPrimitiveRenderer()) {
        PresetPrimitiveRenderer.MIUIX_BRIDGED -> {
            val miuixState = rememberMiuixPullToRefreshState()
            MiuixPullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = modifier,
                pullToRefreshState = miuixState,
                contentPadding = mergedContentPadding,
                color = AppSurfaceTokens.primary(),
                refreshTexts = resolveMiuixPullToRefreshTexts(),
                content = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = contentAlignment,
                    ) {
                        content()
                    }
                },
            )
        }
        PresetPrimitiveRenderer.MATERIAL3 -> {
            ComfortablePullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = modifier,
                state = state,
                contentAlignment = contentAlignment,
                indicator = indicator,
                content = content,
            )
        }
    }
}

/**
 * Default pull-to-refresh chrome for [AdaptivePullToRefreshBox].
 *
 * - Material 3: official expressive [PullToRefreshDefaults.LoadingIndicator]
 *   (ContainedLoadingIndicator + dynamic colors).
 * - Miuix: unused — the Miuix branch mounts native [MiuixPullToRefresh] instead.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BoxScope.AdaptivePullToRefreshDefaultIndicator(
    isRefreshing: Boolean,
    state: PullToRefreshState,
    indicatorTopInset: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val indicatorModifier = modifier
        .align(Alignment.TopCenter)
        .padding(top = indicatorTopInset)
    when (rememberPresetPrimitiveRenderer()) {
        PresetPrimitiveRenderer.MATERIAL3 -> {
            PullToRefreshDefaults.LoadingIndicator(
                modifier = indicatorModifier,
                isRefreshing = isRefreshing,
                state = state,
            )
        }
        PresetPrimitiveRenderer.MIUIX_BRIDGED -> {
            PullToRefreshDefaults.Indicator(
                modifier = indicatorModifier,
                isRefreshing = isRefreshing,
                state = state,
            )
        }
    }
}
