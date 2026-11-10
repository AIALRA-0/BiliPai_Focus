package com.android.purebilibili.feature.profile

import com.android.purebilibili.core.util.WindowWidthSizeClass

data class ProfileLayoutTokens(
    val heroHeightFraction: Float = 0.36f,
    val heroMinHeightDp: Int = 336,
    val heroMaxHeightDp: Int = 360,
    val contentSheetTopOverlapDp: Int = 0,
    val contentSheetTopPaddingDp: Int = 8,
    val contentSheetBottomPaddingDp: Int = 24,
    val heroBottomInsetDp: Int = 16,
    val sectionSpacingDp: Int = 20,
    val tabletWallpaperRevealHeightDp: Int = 224
)

data class ProfileCardTokens(
    val widthDp: Int = 148,
    val coverAspectRatio: Float = 3f / 4f,
    val metadataHeightDp: Int = 56,
    val gapDp: Int = 12
)

fun resolveProfileLayoutTokens(): ProfileLayoutTokens = ProfileLayoutTokens()

fun resolveProfileCardTokens(): ProfileCardTokens = ProfileCardTokens()

fun resolveProfileHeroHeightDp(
    screenHeightDp: Int,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact
): Float {
    val tokens = resolveProfileLayoutTokens()
    val widthScale = when (widthSizeClass) {
        WindowWidthSizeClass.Expanded,
        WindowWidthSizeClass.Large,
        WindowWidthSizeClass.ExtraLarge,
        WindowWidthSizeClass.Medium -> 0.92f
        WindowWidthSizeClass.Compact -> 1f
    }
    return (screenHeightDp * tokens.heroHeightFraction * widthScale)
        .coerceIn(tokens.heroMinHeightDp.toFloat(), tokens.heroMaxHeightDp.toFloat())
}

fun resolveProfileTopBannerHeightDp(
    widthSizeClass: WindowWidthSizeClass,
    screenHeightDp: Int = referenceProfileScreenHeightDp(widthSizeClass)
): Float {
    return resolveProfileHeroHeightDp(screenHeightDp = screenHeightDp, widthSizeClass = widthSizeClass)
}

fun resolveProfileTabletWallpaperRevealHeightDp(
    useSplitLayout: Boolean,
    hasWallpaper: Boolean,
    tokens: ProfileLayoutTokens = resolveProfileLayoutTokens()
): Int = if (useSplitLayout && hasWallpaper) tokens.tabletWallpaperRevealHeightDp else 0

internal fun referenceProfileScreenHeightDp(widthSizeClass: WindowWidthSizeClass): Int {
    return when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> 800
        WindowWidthSizeClass.Medium -> 900
        WindowWidthSizeClass.Expanded -> 1000
        WindowWidthSizeClass.Large -> 1000
        WindowWidthSizeClass.ExtraLarge -> 1000
    }
}

fun resolveProfileCardCoverHeightDp(cardTokens: ProfileCardTokens = resolveProfileCardTokens()): Float {
    return cardTokens.widthDp / cardTokens.coverAspectRatio
}

fun resolveProfileCardHeightDp(cardTokens: ProfileCardTokens = resolveProfileCardTokens()): Float {
    return resolveProfileCardCoverHeightDp(cardTokens) + cardTokens.metadataHeightDp
}

// Shared geometry repair for both native and liquid-glass profiles, not a material override.
internal const val PROFILE_EXPANDED_AVATAR_SIZE_DP = 96f
internal const val PROFILE_EXPANDED_AVATAR_TOP_DP = 35f
internal const val PROFILE_EXPANDED_NAME_TOP_DP = 147.3f
internal const val PROFILE_EXPANDED_NAME_SCALE = 1.12f

internal fun resolveProfileAvatarSizePx(
    density: Float,
    expandedFraction: Float,
    circularCutoutWidthPx: Float,
): Float {
    val endSizePx = if (circularCutoutWidthPx > 0f) circularCutoutWidthPx * 0.5f else 24f * density
    return endSizePx + (PROFILE_EXPANDED_AVATAR_SIZE_DP * density - endSizePx) *
        expandedFraction.coerceIn(0f, 1f)
}

internal fun resolveProfileIdentityStatsTopPx(
    statusBarTopPx: Float,
    density: Float,
    nameHeightPx: Int,
    uidHeightPx: Int,
): Float = statusBarTopPx + maxOf(
    215f * density,
    PROFILE_EXPANDED_NAME_TOP_DP * density +
        nameHeightPx * PROFILE_EXPANDED_NAME_SCALE + uidHeightPx + 30f * density,
)
