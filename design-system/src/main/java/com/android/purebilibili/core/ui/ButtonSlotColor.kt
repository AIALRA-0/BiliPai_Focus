package com.android.purebilibili.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer

/** Scoped to a button so explicit page colors cannot make its label disappear. */
internal data class AppButtonSlotPalette(
    val containerColor: Color,
    val contentColor: Color,
    val enabled: Boolean,
)

internal val LocalAppButtonSlotPalette = compositionLocalOf<AppButtonSlotPalette?> { null }

/** Media surfaces already supply colors matched to their own artwork backdrop. */
val LocalAppButtonSlotColorCorrectionEnabled = compositionLocalOf { true }

/** Called inside a native control's slot, after it supplies its enabled/disabled foreground. */
@Composable
internal fun ProvideAppButtonSlotPalette(
    containerColor: Color,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    // A decorator owns this foreground; ordinary surface correction stays out of its slot.
    if (com.android.purebilibili.core.ui.components.LocalAppIconButtonGlassSurfaceProvided.current) {
        content()
        return
    }
    val foreground = androidx.compose.material3.LocalContentColor.current
    CompositionLocalProvider(
        LocalAppButtonSlotPalette provides AppButtonSlotPalette(containerColor, foreground, enabled),
        top.yukonga.miuix.kmp.theme.LocalContentColor provides foreground,
        content = content,
    )
}

@Composable
internal fun resolveAppButtonSlotColor(requested: Color): Color {
    if (!LocalAppButtonSlotColorCorrectionEnabled.current) return requested
    val palette = LocalAppButtonSlotPalette.current ?: return requested
    return resolveButtonSlotColor(requested, palette, MaterialTheme.colorScheme)
}

@Composable
internal fun resolveAppButtonSlotColor(requested: ColorProducer): ColorProducer {
    if (!LocalAppButtonSlotColorCorrectionEnabled.current) return requested
    val palette = LocalAppButtonSlotPalette.current ?: return requested
    val scheme = MaterialTheme.colorScheme
    return ColorProducer { resolveButtonSlotColor(requested(), palette, scheme) }
}

internal fun resolveButtonSlotColor(
    requested: Color,
    palette: AppButtonSlotPalette,
    scheme: ColorScheme,
): Color {
    if (requested == Color.Unspecified || requested.alpha == 0f) return requested
    if (!palette.enabled) return palette.contentColor
    return resolveButtonContentColor(palette.containerColor, requested, scheme)
}
