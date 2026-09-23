package com.android.purebilibili.feature.list

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

internal fun <T> commonListSharedBoundsMotionSpec(): SpringSpec<T> = spring(
    dampingRatio = 0.82f,
    stiffness = 260f,
)
