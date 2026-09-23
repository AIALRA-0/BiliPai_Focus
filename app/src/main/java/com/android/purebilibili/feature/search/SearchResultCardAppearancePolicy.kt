package com.android.purebilibili.feature.search

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
    effectiveLiquidGlassEnabled: Boolean,
    blurEnabled: Boolean,
    showHomeCoverGlassBadges: Boolean,
    showHomeInfoGlassBadges: Boolean,
): SearchVideoCardAppearance {
    return SearchVideoCardAppearance(
        glassEnabled = effectiveLiquidGlassEnabled,
        blurEnabled = blurEnabled,
        showCoverGlassBadges = false,
        showInfoGlassBadges = false
    )
}

internal fun resolveSearchResultCardAppearance(
    effectiveLiquidGlassEnabled: Boolean,
    supportsIndependentLiquidGlass: Boolean,
    tonalElevationDp: Int,
): SearchResultCardAppearance {
    return if (effectiveLiquidGlassEnabled && !supportsIndependentLiquidGlass) {
        SearchResultCardAppearance(
            surfaceStyle = SearchResultCardSurfaceStyle.GLASS,
            containerAlpha = 0.96f,
            borderAlpha = 0f,
            tonalElevationDp = tonalElevationDp,
            shadowElevationDp = 0
        )
    } else if (effectiveLiquidGlassEnabled) {
        SearchResultCardAppearance(
            surfaceStyle = SearchResultCardSurfaceStyle.GLASS,
            containerAlpha = 0.92f,
            borderAlpha = 0.12f,
            tonalElevationDp = 0,
            shadowElevationDp = 0
        )
    } else if (!supportsIndependentLiquidGlass) {
        SearchResultCardAppearance(
            surfaceStyle = SearchResultCardSurfaceStyle.PLAIN,
            containerAlpha = 1f,
            borderAlpha = 0f,
            tonalElevationDp = tonalElevationDp,
            shadowElevationDp = 0
        )
    } else {
        SearchResultCardAppearance(
            surfaceStyle = SearchResultCardSurfaceStyle.PLAIN,
            containerAlpha = 1f,
            borderAlpha = 0f,
            tonalElevationDp = tonalElevationDp,
            shadowElevationDp = 1
        )
    }
}
