package com.android.purebilibili.feature.search

import com.android.purebilibili.core.theme.UiPreset

internal enum class SearchResultCardSurfaceStyle {
    GLASS,
    PLAIN
}

internal data class SearchVideoCardAppearance(
    val glassEnabled: Boolean,
    val blurEnabled: Boolean,
    val showCoverGlassBadges: Boolean,
    val showInfoGlassBadges: Boolean
)

internal data class SearchResultCardAppearance(
    val surfaceStyle: SearchResultCardSurfaceStyle,
    val containerAlpha: Float,
    val borderAlpha: Float,
    val tonalElevationDp: Int,
    val shadowElevationDp: Int
)

internal fun resolveSearchCardBlurEnabled(
    headerBlurEnabled: Boolean,
    bottomBarBlurEnabled: Boolean
): Boolean = headerBlurEnabled || bottomBarBlurEnabled

internal fun resolveSearchVideoCardAppearance(
    liquidGlassEnabled: Boolean,
    blurEnabled: Boolean,
    showHomeCoverGlassBadges: Boolean,
    showHomeInfoGlassBadges: Boolean
): SearchVideoCardAppearance = SearchVideoCardAppearance(
    glassEnabled = liquidGlassEnabled,
    blurEnabled = blurEnabled,
    showCoverGlassBadges = showHomeCoverGlassBadges,
    showInfoGlassBadges = showHomeInfoGlassBadges
)

internal fun resolveSearchResultCardAppearance(
    liquidGlassEnabled: Boolean,
    uiPreset: UiPreset = UiPreset.IOS
): SearchResultCardAppearance {
    return if (liquidGlassEnabled && uiPreset == UiPreset.MD3) {
        SearchResultCardAppearance(
            surfaceStyle = SearchResultCardSurfaceStyle.GLASS,
            containerAlpha = 0.96f,
            borderAlpha = 0f,
            tonalElevationDp = 1,
            shadowElevationDp = 0
        )
    } else if (liquidGlassEnabled) {
        SearchResultCardAppearance(
            surfaceStyle = SearchResultCardSurfaceStyle.GLASS,
            containerAlpha = 0.92f,
            borderAlpha = 0.12f,
            tonalElevationDp = 0,
            shadowElevationDp = 0
        )
    } else if (uiPreset == UiPreset.MD3) {
        SearchResultCardAppearance(
            surfaceStyle = SearchResultCardSurfaceStyle.PLAIN,
            containerAlpha = 1f,
            borderAlpha = 0f,
            tonalElevationDp = 1,
            shadowElevationDp = 0
        )
    } else {
        SearchResultCardAppearance(
            surfaceStyle = SearchResultCardSurfaceStyle.PLAIN,
            containerAlpha = 1f,
            borderAlpha = 0f,
            tonalElevationDp = 1,
            shadowElevationDp = 1
        )
    }
}
