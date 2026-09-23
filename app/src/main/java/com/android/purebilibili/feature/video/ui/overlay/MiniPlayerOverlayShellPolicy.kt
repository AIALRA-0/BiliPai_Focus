package com.android.purebilibili.feature.video.ui.overlay

import com.android.purebilibili.core.ui.AppPlayerChromeProfile
import kotlin.math.roundToInt

/**
 * MIUIX shell tuning for the floating mini player: flatter elevation, token-aligned corners,
 * and theme primary accents for transport controls.
 */
data class MiniPlayerOverlayShellVisual(
    val cardCornerRadiusDp: Int,
    val cardElevationDp: Int,
    val cardShadowDp: Int,
    val seekHintCornerRadiusDp: Int,
    val useThemePrimaryAccent: Boolean
)

fun resolveMiniPlayerOverlayShellVisual(
    layout: MiniPlayerOverlayLayoutPolicy,
    chromeProfile: AppPlayerChromeProfile,
): MiniPlayerOverlayShellVisual {
    if (!chromeProfile.effects.usesTonalContainerTreatment) {
        return MiniPlayerOverlayShellVisual(
            cardCornerRadiusDp = layout.cardCornerRadiusDp,
            cardElevationDp = layout.cardElevationDp,
            cardShadowDp = layout.cardShadowDp,
            seekHintCornerRadiusDp = layout.seekHintCornerRadiusDp,
            useThemePrimaryAccent = false
        )
    }
    val tokenCardCorner = chromeProfile.compactChromeSpec.secondaryButtonCornerRadiusDp
    val tokenFloatingCorner = chromeProfile.compactChromeSpec.primaryCornerRadiusDp
    // Floating mini-player chrome sits between card and floating token radii.
    val cardCorner = ((tokenCardCorner + tokenFloatingCorner) / 2)
        .coerceAtLeast(layout.cardCornerRadiusDp)
    val chipCorner = chromeProfile.compactChromeSpec.chipCornerRadiusDp
        .coerceAtLeast(layout.seekHintCornerRadiusDp)
    return MiniPlayerOverlayShellVisual(
        cardCornerRadiusDp = cardCorner,
        cardElevationDp = 0,
        cardShadowDp = (layout.cardShadowDp * 0.55f).roundToInt().coerceAtLeast(6),
        seekHintCornerRadiusDp = chipCorner,
        useThemePrimaryAccent = true
    )
}
