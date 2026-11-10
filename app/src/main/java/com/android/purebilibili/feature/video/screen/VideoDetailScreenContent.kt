package com.android.purebilibili.feature.video.screen

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
internal fun VideoDetailScreenContent(
    transitionState: VideoDetailTransitionState,
    routeSheetMotion: VideoDetailRouteSheetMotion,
    isFullscreenMode: Boolean,
    backgroundColor: Color,
    modifier: Modifier,
    backgroundAlphaProvider: () -> Float = { 1f },
    mainContent: @Composable BoxScope.() -> Unit,
    overlayContent: @Composable BoxScope.() -> Unit
) {
    VideoDetailRouteSheetHost(
        frameProvider = transitionState.routeSheetFrameProvider,
        motion = routeSheetMotion,
        isFullscreenMode = isFullscreenMode,
        backgroundColor = backgroundColor,
        backgroundAlphaProvider = backgroundAlphaProvider,
        modifier = modifier,
        content = mainContent,
        overlayContent = overlayContent
    )
}
