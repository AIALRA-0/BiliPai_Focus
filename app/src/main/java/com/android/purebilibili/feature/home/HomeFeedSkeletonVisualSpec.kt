package com.android.purebilibili.feature.home

import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.android.purebilibili.core.ui.AppShapes

internal fun resolveHomeSkeletonCoverShape(cornerRadius: Dp): Shape {
    return AppShapes.topRounded(cornerRadius)
}

internal fun resolveHomeSkeletonInfoShape(cornerRadius: Dp): Shape {
    return AppShapes.bottomRounded(cornerRadius)
}
