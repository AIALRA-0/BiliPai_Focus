package com.android.purebilibili.core.ui

import androidx.compose.ui.graphics.Color

/** Fixed-contrast colors for content drawn directly over photos or video. */
object MediaContrastPalette {
    val Foreground = Color.White
    val Scrim = Color.Black
    val SecondaryForeground = Foreground.copy(alpha = 0.72f)
    val ControlSurface = Color(0xFF242424)
    val ControlScrim = Scrim.copy(alpha = 0.48f)
    val Track = Foreground.copy(alpha = 0.24f)
    val Success = Color(0xFF81C784)
    val Warning = Color(0xFFFFCC80)
    val Error = Color(0xFFFFB4AB)
    val Rating = Warning
}
