package com.android.purebilibili.core.theme

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

/**
 * 在系统 Density 之上叠加缩放系数的包装器。
 *
 * 系统在大字号下启用非线性字体缩放时，sp→px 的换算由系统曲线决定，
 * 平台提供的 LocalDensity 内部携带该曲线；用 Density(density, fontScale)
 * 重建实例会退回线性换算，大字号下文字会比系统预期更大。此包装只调整
 * 缩放系数，sp↔dp 换算继续委托原始 Density，因此保留系统曲线；系统未
 * 启用非线性缩放时原始 Density 本身就是线性实现，换算结果与重建 Density 一致。
 */
private class ScaledDensity(
    private val delegate: Density,
    densityScale: Float,
    private val fontScaleMultiplier: Float,
) : Density {
    override val density: Float = delegate.density * densityScale
    override val fontScale: Float = delegate.fontScale * fontScaleMultiplier

    // Density 默认的 sp→px 与 px→sp 换算都会经过这两个成员扩展；
    // 覆写为委托原始实现，即可让全部文字换算沿用系统曲线。
    override fun TextUnit.toDp(): Dp = with(delegate) { this@toDp.toDp() } * fontScaleMultiplier

    override fun Dp.toSp(): TextUnit = with(delegate) { (this@toSp / fontScaleMultiplier).toSp() }
}

/**
 * 等比缩放整个 UI：dp 与 sp 的像素尺寸都乘以 [scale]，系统字体缩放曲线保持不变。
 * [scale] 为 1 时原样返回，不引入包装层。
 */
fun Density.withDensityScale(scale: Float): Density =
    if (scale == 1f) this else ScaledDensity(this, scale, 1f)

/**
 * 仅缩放文字：sp 的像素尺寸乘以 [multiplier]，dp 尺寸不变，系统字体缩放曲线
 * 保持不变；[multiplier] 为 1 时原样返回，不引入包装层。
 */
fun Density.withFontScaleMultiplier(multiplier: Float): Density =
    if (multiplier == 1f) this else ScaledDensity(this, 1f, multiplier)
