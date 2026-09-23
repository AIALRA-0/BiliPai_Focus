package com.android.purebilibili.feature.home.components.miuix

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Offset

internal fun interactiveHighlightPressSpec(): SpringSpec<Float> = spring(
    dampingRatio = 0.5f,
    stiffness = 300f,
    visibilityThreshold = 0.001f,
)

internal fun interactiveHighlightPositionSpec(): SpringSpec<Offset> = spring(
    dampingRatio = 0.5f,
    stiffness = 300f,
    visibilityThreshold = Offset.VisibilityThreshold,
)
