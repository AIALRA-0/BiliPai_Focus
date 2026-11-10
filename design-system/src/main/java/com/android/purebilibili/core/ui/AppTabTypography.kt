package com.android.purebilibili.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified

internal fun resolveAppTabLabelFontSize(
    requested: TextUnit,
    nativeDefault: TextUnit,
    liquidGlassEnabled: Boolean,
): TextUnit {
    // Material selection does not change the caller's or active theme's text size.
    return if (requested.isSpecified) requested else nativeDefault
}

/** Tab labels retain the active theme/caller typography for both materials. */
@Composable
fun appTabLabelFontSize(requested: TextUnit = TextUnit.Unspecified): TextUnit = resolveAppTabLabelFontSize(
    requested = requested,
    nativeDefault = MaterialTheme.typography.labelMedium.fontSize,
    liquidGlassEnabled = LocalAppThemeConfig.current.liquidGlassEnabled,
)
