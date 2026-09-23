// 文件路径: core/theme/Shape.kt
package com.android.purebilibili.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp

/**
 * CompositionLocal 提供当前 Android 原生主题的圆角缩放比例。
 * 业务组件优先使用 AppShapes；这里只服务共享转场或按尺寸计算曲率的几何路径。
 */
val LocalCornerRadiusScale = staticCompositionLocalOf { 1f }

val Md3Shapes = Shapes()

val MiuixAlignedShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
