package com.android.purebilibili.core.ui.components

import com.android.purebilibili.core.ui.AppChromeSizeTokens
import android.os.Build
import androidx.compose.runtime.Composable
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AppUiStyle

/** Authored liquid controls keep their capsule identity in either native theme. */
internal object AppLiquidControlGeometry {
    val thumb = DpSize(40.dp, 24.dp)
    val switchTrack = DpSize(64.dp, 28.dp)
    val controlHeight = AppChromeSizeTokens.CompactActionHeight
    val switchInset = 2.dp
    val sliderTrackHeight = 6.dp
    val sliderInset = thumb.width / 2
}

/** Native resting geometry, shared before choosing the value control's material. */
@Immutable
internal data class AppSwitchGeometry(val track: DpSize, val thumb: Dp) {
    val inset: Dp get() = (track.height - thumb) / 2
}
private val materialSwitchWithContent = AppSwitchGeometry(DpSize(52.dp, 32.dp), 24.dp)
private val materialSwitchWithoutContent = AppSwitchGeometry(DpSize(52.dp, 32.dp), 16.dp)
private val miuixSwitch = AppSwitchGeometry(DpSize(49.dp, 28.dp), 20.dp)

internal fun appSwitchGeometry(
    style: AppUiStyle,
    checked: Boolean,
    hasThumbContent: Boolean,
): AppSwitchGeometry = when (style) {
    AppUiStyle.MATERIAL3 -> if (checked || hasThumbContent) {
        materialSwitchWithContent
    } else {
        materialSwitchWithoutContent
    }
    AppUiStyle.MIUIX -> miuixSwitch
}

@Immutable
internal data class AppSliderGeometry(
    val thumb: DpSize,
    val trackHeight: Dp,
    val horizontalInset: Dp,
    val trackHorizontalInset: Dp,
    val discreteThumbInset: Dp,
) {
    val height: Dp get() = maxOf(thumb.height, trackHeight)
}
private val materialSlider = AppSliderGeometry(DpSize(4.dp, 44.dp), 16.dp, 2.dp, 2.dp, 8.dp)
private val miuixSlider = AppSliderGeometry(DpSize(20.16.dp, 20.16.dp), 28.dp, 14.dp, 0.dp, 0.dp)

internal fun appSliderGeometry(style: AppUiStyle): AppSliderGeometry = when (style) {
    // Material3 SliderDefaults: 4x44dp handle and 16dp track.
    AppUiStyle.MATERIAL3 -> materialSlider
    // Miuix SliderDefaults.MinHeight track; resting knob diameter is 72% of it.
    AppUiStyle.MIUIX -> miuixSlider
}

/** Pointer travel follows the visible thumb endpoints, including RTL and collapsed tracks. */
internal fun resolveAppSliderPointerFraction(
    positionPx: Float,
    widthPx: Float,
    insetPx: Float,
    isLtr: Boolean,
): Float {
    val travel = (widthPx - insetPx * 2f).coerceAtLeast(0f)
    if (travel == 0f) return 0f
    val visual = ((positionPx - insetPx) / travel).coerceIn(0f, 1f)
    return if (isLtr) visual else 1f - visual
}

/** Appearance is independent of native theme, enabled state and reduced-motion policy. */
@Composable
internal fun useAppLiquidValueControls(): Boolean =
    LocalAppThemeConfig.current.liquidGlassEnabled && Build.VERSION.SDK_INT >= 33
