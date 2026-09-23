package com.android.purebilibili.core.util

import androidx.compose.ui.unit.IntRect

internal data class AppAdaptiveStrategySnapshot(
    val detectionBasis: AppFoldableDetectionBasis,
    val foldableDisplayRole: AppFoldableDisplayRole,
    val currentWindowWidthDp: Int,
    val currentWindowHeightDp: Int,
    val maximumWindowWidthDp: Int?,
    val maximumWindowHeightDp: Int?,
    val widthSizeClass: WindowWidthSizeClass,
    val heightSizeClass: WindowHeightSizeClass,
    val posture: AppFoldPosture,
    val hingeBounds: IntRect?,
    val hingeCount: Int,
    val naturalOrientation: AppDisplayNaturalOrientation,
    val isInMultiWindowMode: Boolean,
    val playerPresentation: String? = null,
)

internal fun AppWindowAdaptiveInfo.toAdaptiveStrategySnapshot(
    playerPresentation: String? = null,
): AppAdaptiveStrategySnapshot {
    return AppAdaptiveStrategySnapshot(
        detectionBasis = displayContext.detectionBasis,
        foldableDisplayRole = displayContext.foldableDisplayRole,
        currentWindowWidthDp = displayContext.currentWindowWidthDp,
        currentWindowHeightDp = displayContext.currentWindowHeightDp,
        maximumWindowWidthDp = displayContext.maximumWindowWidthDp,
        maximumWindowHeightDp = displayContext.maximumWindowHeightDp,
        widthSizeClass = windowSizeClass.widthSizeClass,
        heightSizeClass = windowSizeClass.heightSizeClass,
        posture = posture,
        hingeBounds = foldingFeature.hingeBounds,
        hingeCount = foldingFeature.hinges.size,
        naturalOrientation = displayContext.naturalOrientation,
        isInMultiWindowMode = displayContext.isInMultiWindowMode,
        playerPresentation = playerPresentation,
    )
}

internal fun formatAppAdaptiveStrategySnapshot(
    snapshot: AppAdaptiveStrategySnapshot,
): String = buildString {
    append("basis=${snapshot.detectionBasis}")
    append(", role=${snapshot.foldableDisplayRole}")
    append(", current=${snapshot.currentWindowWidthDp}x${snapshot.currentWindowHeightDp}dp")
    append(", maximum=${snapshot.maximumWindowWidthDp}x${snapshot.maximumWindowHeightDp}dp")
    append(", sizeClass=${snapshot.widthSizeClass}/${snapshot.heightSizeClass}")
    append(", posture=${snapshot.posture}")
    append(", hinge=${snapshot.hingeBounds}")
    append(", hingeCount=${snapshot.hingeCount}")
    append(", natural=${snapshot.naturalOrientation}")
    append(", multiWindow=${snapshot.isInMultiWindowMode}")
    snapshot.playerPresentation?.let { mode ->
        append(", player=$mode")
    }
}
