package com.android.purebilibili.core.ui.components

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Offset

/** Shared bottom-dock press and drag-return motion for every interactive highlight. */
fun appPressHighlightPressSpec(): SpringSpec<Float> = spring(
    dampingRatio = 0.5f,
    stiffness = 300f,
    visibilityThreshold = 0.001f,
)

fun appPressHighlightPositionSpec(): SpringSpec<Offset> = spring(
    dampingRatio = 0.5f,
    stiffness = 300f,
    visibilityThreshold = Offset.VisibilityThreshold,
)
