package com.android.purebilibili.core.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/** 全局控件与需要独立光学采样的菜单共用形变计算。 */
data class AppElasticTransform(
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val translation: Offset = Offset.Zero,
    val baseScale: Float = 1f,
)

/** 弹窗尺度的形变扩展比例：固定 4dp 扩展在大面板上不可感知，改按短边比例放大。 */
const val APP_POPUP_ELASTIC_EXPANSION_RATIO = 0.10f

/** 与常见返回控件相同的拖动尺度，避免大弹窗稀释同一段手指位移。 */
const val APP_POPUP_ELASTIC_DRAG_REFERENCE_DP = 40f

/** 以调用方的基础扩展为下限，按短边比例放大弹窗的按压缩放增益。 */
fun appPopupElasticExpansionPx(minDimensionPx: Float, baseExpansionPx: Float): Float =
    maxOf(baseExpansionPx, minDimensionPx * APP_POPUP_ELASTIC_EXPANSION_RATIO)

/** 控件与玻璃弹窗共用的整体跟手位移；这里只计算平移，不改变各自的缩放策略。 */
fun resolveAppElasticDragTranslation(size: Size, drag: Offset, feedbackStrength: Float = 1f): Offset {
    val shortSide = minOf(size.width, size.height)
    if (shortSide <= 0f) return Offset.Zero
    return Offset(
        shortSide * tanh(0.05f * drag.x / shortSide) * feedbackStrength,
        shortSide * tanh(0.05f * drag.y / shortSide) * feedbackStrength,
    )
}

fun resolveAppElasticTransform(
    size: Size,
    press: Float,
    drag: Offset,
    expansionPx: Float,
    feedbackStrength: Float = 1f,
    // 仅大面积玻璃弹窗选择短边基准；普通按钮和原生组件保留原有增益。
    normalizeToShortSide: Boolean = false,
    dragReferencePx: Float? = null,
): AppElasticTransform {
    val width = size.width
    val height = size.height
    if (width <= 0f || height <= 0f) return AppElasticTransform()
    val minDimension = minOf(width, height)
    val maxDimension = maxOf(width, height)
    val dragScale = expansionPx / if (normalizeToShortSide) minDimension else height
    val dragDimension = if (normalizeToShortSide) dragReferencePx?.takeIf { it > 0f } ?: minDimension else maxDimension
    val dragX = if (normalizeToShortSide) (drag.x / dragDimension).coerceIn(-4f, 4f) else drag.x / dragDimension
    val dragY = if (normalizeToShortSide) (drag.y / dragDimension).coerceIn(-4f, 4f) else drag.y / dragDimension
    val baseScale = 1f + dragScale * press
    val angle = atan2(drag.y, drag.x)
    val translation = resolveAppElasticDragTranslation(size, drag, feedbackStrength)
    val translationLimit = minDimension * 0.25f
    return AppElasticTransform(
        scaleX = baseScale + dragScale * abs(cos(angle) * dragX) *
            (if (normalizeToShortSide) 1f else (width / height).coerceAtMost(1f)),
        scaleY = baseScale + dragScale * abs(sin(angle) * dragY) *
            (if (normalizeToShortSide) 1f else (height / width).coerceAtMost(1f)),
        translation = if (normalizeToShortSide) Offset(
            translation.x.coerceIn(-translationLimit, translationLimit),
            translation.y.coerceIn(-translationLimit, translationLimit),
        ) else translation,
        baseScale = baseScale,
    )
}
