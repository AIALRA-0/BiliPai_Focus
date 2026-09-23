package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import kotlin.math.max

data class RoundedControlVisualGeometry(
    val height: Dp,
    val cornerRadius: Dp,
)

/** Text metrics are already converted through Density, including the user's font scale. */
fun resolveMiuixNonGlassControlGeometry(
    compact: Boolean,
    textHeight: Dp,
): RoundedControlVisualGeometry = RoundedControlVisualGeometry(
    height = maxOf(
        (if (compact) AppChromeSizeTokens.MiuixNativeCompactControlHeightDp
        else AppChromeSizeTokens.MiuixNativeTabHeightDp).dp,
        textHeight + 16.dp,
    ),
    cornerRadius = (if (compact) AppChromeSizeTokens.MiuixNativeCompactCornerRadiusDp
    else AppChromeSizeTokens.MiuixNativeTabCornerRadiusDp).dp,
)

/**
 * Resolves visual geometry without treating the accessibility touch target as component height.
 *
 * [nativeMinimumHeight] comes from the selected renderer's native component. If the semantic
 * [preferredCornerRadius] would exceed [maxCornerRatio], the visible control grows just enough to
 * preserve the corner. Touch expansion is intentionally outside this policy.
 */
fun resolveRoundedControlVisualGeometry(
    preferredCornerRadius: Dp,
    nativeMinimumHeight: Dp,
    maxCornerRatio: Float = 0.3f,
): RoundedControlVisualGeometry {
    val safeCorner = preferredCornerRadius.coerceAtLeast(0.dp)
    val safeMinimumHeight = nativeMinimumHeight.coerceAtLeast(0.dp)
    val safeRatio = maxCornerRatio.coerceIn(0.15f, 0.45f)
    val radiusDrivenHeight = (safeCorner.value / safeRatio).dp
    return RoundedControlVisualGeometry(
        height = max(safeMinimumHeight.value, radiusDrivenHeight.value).dp,
        cornerRadius = safeCorner,
    )
}

data class AppSegmentedControlPolicy(
    val usesEmphasizedTitle: Boolean,
    val usesMaterialFallback: Boolean,
    val usesNativeTabRow: Boolean,
    val usesMaterialColorTokens: Boolean,
    /** Semantic item corner; each native renderer resolves compatible visual geometry. */
    val preferredCornerRadius: Dp,
)

internal fun resolveAppSegmentedControlPolicy(
    uiStyle: AppUiStyle,
): AppSegmentedControlPolicy {
    // Prefer Card-level corners, never the full Pill token. Native renderers keep this corner and
    // derive any required visual height from it instead of forcing a shared 48dp container.
    val preferred = AppShapes.resolveContainerCornerDp(
        level = ContainerLevel.Card,
        uiStyle = uiStyle,
    )
    return when (uiStyle) {
        AppUiStyle.MIUIX -> AppSegmentedControlPolicy(
            usesEmphasizedTitle = true,
            usesMaterialFallback = true,
            usesNativeTabRow = true,
            usesMaterialColorTokens = false,
            preferredCornerRadius = preferred,
        )
        AppUiStyle.MATERIAL3 -> AppSegmentedControlPolicy(
            usesEmphasizedTitle = true,
            usesMaterialFallback = true,
            usesNativeTabRow = false,
            usesMaterialColorTokens = true,
            preferredCornerRadius = preferred,
        )
    }
}

@Composable
fun rememberAppSegmentedControlPolicy(): AppSegmentedControlPolicy {
    val uiStyle = LocalAppUiStyle.current
    return remember(uiStyle) {
        resolveAppSegmentedControlPolicy(uiStyle)
    }
}
