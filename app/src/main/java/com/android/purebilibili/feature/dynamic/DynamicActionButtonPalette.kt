package com.android.purebilibili.feature.dynamic

import androidx.compose.ui.graphics.Color

/** Fixed neutral colors used by the dynamic feed's action row. */
internal object DynamicActionButtonPalette {
    fun content(isDark: Boolean): Color = if (isDark) DarkContent else LightContent

    fun disabledContent(isDark: Boolean): Color = if (isDark) DarkDisabledContent else LightDisabledContent

    fun container(isDark: Boolean): Color = if (isDark) DarkContainer else LightContainer

    fun disabledContainer(isDark: Boolean): Color = if (isDark) DarkDisabledContainer else LightDisabledContainer

    private val DarkContent = Color(0xFFDDDDDD)
    private val LightContent = Color(0xFF444444)
    private val DarkDisabledContent = Color(0xFF666666)
    private val LightDisabledContent = Color(0xFF999999)
    private val DarkContainer = Color(0xFF242424)
    private val LightContainer = Color(0xFFF2F2F2)
    private val DarkDisabledContainer = Color(0x10FFFFFF)
    private val LightDisabledContainer = Color(0x05000000)
}
