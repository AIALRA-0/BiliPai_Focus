package com.android.purebilibili.core.ui.transition

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil

/** 明显模糊时缩小效果纹理；清晰交接仍使用原始分辨率，半径始终以屏幕像素定义。 */
internal fun resolveVideoCardBlurRasterScale(blurRadiusPx: Float): Float =
    if (blurRadiusPx.isFinite() && blurRadiusPx > 6f) 0.5f else 1f

internal fun resolveVideoCardBlurRasterSize(size: Size, scale: Float): IntSize = IntSize(
    width = ceil(size.width * scale).toInt().coerceAtLeast(1),
    height = ceil(size.height * scale).toInt().coerceAtLeast(1),
)

/** 图层补偿缩放的轴心仍须位于原页面坐标，不能随纹理分辨率漂移。 */
internal fun resolveVideoCardBlurRasterTranslation(pivot: Offset, rasterScale: Float): Offset =
    pivot * (1f - rasterScale)
