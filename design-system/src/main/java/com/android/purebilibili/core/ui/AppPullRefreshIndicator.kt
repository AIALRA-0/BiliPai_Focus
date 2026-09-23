package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import com.android.purebilibili.core.theme.AppUiStyle

internal enum class AppPullRefreshIndicatorRenderer {
    MATERIAL3,
    MIUIX,
}

enum class AppPullRefreshMotionStyle { CUPERTINO, PLATFORM }

enum class AppPullRefreshIndicatorStyle {
    CUPERTINO,
    MATERIAL_DEFAULT,
    MATERIAL_SCREENSHOT_HANDLE,
    MIUIX_NATIVE,
}

data class AppPullRefreshProfile(
    val motionStyle: AppPullRefreshMotionStyle,
    val indicatorStyle: AppPullRefreshIndicatorStyle,
)

fun resolveAppPullRefreshProfile(
    renderer: PresetPrimitiveRenderer,
): AppPullRefreshProfile = when (renderer) {
    PresetPrimitiveRenderer.MIUIX_BRIDGED -> AppPullRefreshProfile(
        AppPullRefreshMotionStyle.PLATFORM,
        AppPullRefreshIndicatorStyle.MIUIX_NATIVE,
    )
    PresetPrimitiveRenderer.MATERIAL3 -> AppPullRefreshProfile(
        AppPullRefreshMotionStyle.PLATFORM,
        AppPullRefreshIndicatorStyle.MATERIAL_DEFAULT,
    )
}

@Composable
fun rememberAppPullRefreshProfile(): AppPullRefreshProfile =
    resolveAppPullRefreshProfile(rememberPresetPrimitiveRenderer())

internal fun resolveAppPullRefreshIndicatorRenderer(
    uiStyle: AppUiStyle,
): AppPullRefreshIndicatorRenderer = when (uiStyle) {
    AppUiStyle.MIUIX -> AppPullRefreshIndicatorRenderer.MIUIX
    AppUiStyle.MATERIAL3 -> AppPullRefreshIndicatorRenderer.MATERIAL3
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppPullRefreshLoadingIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    when (rememberPresetPrimitiveRenderer()) {
        PresetPrimitiveRenderer.MATERIAL3 -> PullToRefreshDefaults.LoadingIndicator(
            state = state,
            isRefreshing = isRefreshing,
            modifier = modifier,
        )

        PresetPrimitiveRenderer.MIUIX_BRIDGED -> {
            if (isRefreshing) {
                AppPullRefreshLoadingIndicator(
                    modifier = modifier,
                )
            } else if (state.distanceFraction > 0f) {
                PullToRefreshDefaults.Indicator(
                    state = state,
                    isRefreshing = false,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
fun AppPullRefreshLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = AppSurfaceTokens.primary(),
) {
    when (rememberPresetPrimitiveRenderer()) {
        PresetPrimitiveRenderer.MIUIX_BRIDGED -> AdaptiveLoadingIndicator(
            modifier = modifier,
            size = AppSpacingTokens.ExtraLarge - AppSpacingTokens.Micro,
            color = color,
            strokeWidth = AppSpacingTokens.Micro,
        )
        PresetPrimitiveRenderer.MATERIAL3 -> AdaptiveLoadingIndicator(
            modifier = modifier,
            size = AppSpacingTokens.DoubleExtraLarge + AppSpacingTokens.ExtraSmall,
            color = color,
            density = AdaptiveLoadingDensity.PAGE,
        )
    }
}
