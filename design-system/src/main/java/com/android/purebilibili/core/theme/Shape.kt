// 文件路径: core/theme/Shape.kt
package com.android.purebilibili.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppShapeTokens

/**
 * CompositionLocal 提供当前 Android 原生主题的圆角缩放比例。
 * 业务组件优先使用 AppShapes；这里只服务共享转场或按尺寸计算曲率的几何路径。
 */
val LocalCornerRadiusScale = staticCompositionLocalOf { 1f }

/**
 * Material 3 形状槽位,与 [resolveAndroidNativeChromeTokens] 的容器 24dp / 胶囊 28dp 对齐:
 * - large 16→24:内容卡与 chrome 容器共用同一套圆角语言;
 * - medium 12→16:中层容器(视频详情语义形状、代码块等)跟随 expressive 尺度;
 * - small/extraSmall 保持官方默认,避免输入框、芯片等紧凑控件观感回退;
 * - extraLarge 28 与胶囊 token 一致。
 */
val Md3Shapes = Shapes(
    extraSmall = RoundedCornerShape(AppShapeTokens.MaterialExtraSmall),
    small = RoundedCornerShape(AppShapeTokens.MaterialSmall),
    medium = RoundedCornerShape(AppShapeTokens.MaterialMedium),
    large = RoundedCornerShape(AppShapeTokens.MaterialLarge),
    extraLarge = RoundedCornerShape(AppShapeTokens.MaterialExtraLarge)
)

val MiuixAlignedShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
