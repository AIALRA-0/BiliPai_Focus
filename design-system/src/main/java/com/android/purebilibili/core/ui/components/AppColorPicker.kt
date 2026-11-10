package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import top.yukonga.miuix.kmp.basic.ColorPicker
import top.yukonga.miuix.kmp.basic.HsvHueSlider
import top.yukonga.miuix.kmp.basic.HsvSaturationSlider
import top.yukonga.miuix.kmp.basic.HsvValueSlider

/** Theme colors are opaque; alpha is exposed only for callers that store it. */
@Composable
fun AppColorPicker(
    color: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier,
    alphaEnabled: Boolean = false,
) {
    var hsv by remember {
        mutableStateOf(FloatArray(3).also { android.graphics.Color.colorToHSV(color.toArgb(), it) })
    }
    var lastEmittedArgb by remember { mutableStateOf(color.toArgb()) }
    LaunchedEffect(color) {
        if (color.toArgb() != lastEmittedArgb) {
            hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(color.toArgb(), it) }
            lastEmittedArgb = color.toArgb()
        }
    }
    val liquid = useAppLiquidValueControls()
    val miuix = LocalAppUiStyle.current == AppUiStyle.MIUIX && !liquid
    if (miuix && alphaEnabled) {
        ColorPicker(color = color, onColorChanged = onColorChanged, modifier = modifier)
        return
    }
    fun update(index: Int, value: Float) {
        val next = hsv.copyOf().also { it[index] = value }
        hsv = next
        val nextColor = Color.hsv(next[0], next[1], next[2], if (alphaEnabled) color.alpha else 1f)
        lastEmittedArgb = nextColor.toArgb()
        onColorChanged(nextColor)
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().height(36.dp).background(color))
        if (miuix) {
            AppText("色相")
            HsvHueSlider(currentHue = hsv[0], onHueChanged = { update(0, it * 360f) })
            AppText("饱和度")
            HsvSaturationSlider(
                currentHue = hsv[0], currentSaturation = hsv[1],
                onSaturationChanged = { update(1, it) },
            )
            AppText("亮度")
            HsvValueSlider(
                currentHue = hsv[0], currentSaturation = hsv[1], currentValue = hsv[2],
                onValueChanged = { update(2, it) },
            )
        } else {
            Text("色相")
            AppSlider(value = hsv[0], onValueChange = { update(0, it) }, valueRange = 0f..360f)
            Text("饱和度")
            AppSlider(value = hsv[1], onValueChange = { update(1, it) })
            Text("亮度")
            AppSlider(value = hsv[2], onValueChange = { update(2, it) })
            if (alphaEnabled) {
                Text("透明度")
                AppSlider(value = color.alpha, onValueChange = { onColorChanged(color.copy(alpha = it)) })
            }
        }
    }
}
