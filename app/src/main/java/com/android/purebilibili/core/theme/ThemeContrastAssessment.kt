package com.android.purebilibili.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.android.purebilibili.core.store.ThemeModeRoleOverrides

internal data class ThemeContrastCheck(
    val label: String,
    val ratio: Float,
    val required: Float,
) {
    val passes: Boolean get() = ratio >= required
}

/** Inspect opaque final roles, after custom edits, glass selection and native bridging.
 * Disabled controls and decorative outlineVariant are intentionally excluded. */
internal fun assessThemeContrast(scheme: ColorScheme): List<ThemeContrastCheck> {
    fun check(label: String, foreground: Color, background: Color, required: Float = 4.5f) =
        ThemeContrastCheck(label, calculateContrastRatio(opaqueCompositeOver(foreground, background), background), required)
    val surfaces = listOf(
        "背景" to scheme.background, "页面" to scheme.surface,
        "容器" to scheme.surfaceContainer, "高层容器" to scheme.surfaceContainerHigh,
        "最高层容器" to scheme.surfaceContainerHighest,
        "最低层容器" to scheme.surfaceContainerLowest, "低层容器" to scheme.surfaceContainerLow,
        "明亮表面" to scheme.surfaceBright, "暗淡表面" to scheme.surfaceDim,
        "变体表面" to scheme.surfaceVariant,
    )
    return buildList {
        add(check("背景文字／背景", scheme.onBackground, scheme.background))
        surfaces.forEach { (label, background) ->
            add(check("主要文字／$label", scheme.onSurface, background))
            add(check("次要文字／$label", scheme.onSurfaceVariant, background))
        }
        listOf(
            Triple("主色", scheme.onPrimary, scheme.primary),
            Triple("主色选中容器", scheme.onPrimaryContainer, scheme.primaryContainer),
            Triple("副色", scheme.onSecondary, scheme.secondary),
            Triple("副色选中容器", scheme.onSecondaryContainer, scheme.secondaryContainer),
            Triple("第三色", scheme.onTertiary, scheme.tertiary),
            Triple("第三色容器", scheme.onTertiaryContainer, scheme.tertiaryContainer),
            Triple("错误提示", scheme.onError, scheme.error),
            Triple("错误容器", scheme.onErrorContainer, scheme.errorContainer),
            Triple("反色文字", scheme.inverseOnSurface, scheme.inverseSurface),
        ).forEach { (label, foreground, background) -> add(check(label, foreground, background)) }
        listOf(
            Triple("主色固定", scheme.onPrimaryFixed, scheme.primaryFixed),
            Triple("主色固定暗色", scheme.onPrimaryFixed, scheme.primaryFixedDim),
            Triple("主色固定次要", scheme.onPrimaryFixedVariant, scheme.primaryFixed),
            Triple("主色固定暗色次要", scheme.onPrimaryFixedVariant, scheme.primaryFixedDim),
            Triple("副色固定", scheme.onSecondaryFixed, scheme.secondaryFixed),
            Triple("副色固定暗色", scheme.onSecondaryFixed, scheme.secondaryFixedDim),
            Triple("副色固定次要", scheme.onSecondaryFixedVariant, scheme.secondaryFixed),
            Triple("副色固定暗色次要", scheme.onSecondaryFixedVariant, scheme.secondaryFixedDim),
            Triple("第三色固定", scheme.onTertiaryFixed, scheme.tertiaryFixed),
            Triple("第三色固定暗色", scheme.onTertiaryFixed, scheme.tertiaryFixedDim),
            Triple("第三色固定次要", scheme.onTertiaryFixedVariant, scheme.tertiaryFixed),
            Triple("第三色固定暗色次要", scheme.onTertiaryFixedVariant, scheme.tertiaryFixedDim),
        ).forEach { (label, foreground, background) -> add(check(label, foreground, background)) }
        add(check("控件边界／页面", scheme.outline, scheme.surface, 3f))
        add(check("控件边界／容器", scheme.outline, scheme.surfaceContainer, 3f))
    }
}

/** An explicit user action: preserve backgrounds and accent seeds, adjust only saved text.
 * Surfaces depend on primary text, so recompute their geometry before repairing secondary text. */
internal fun repairThemeRoleTextContrast(roles: ThemeModeRoleOverrides, darkTheme: Boolean): ThemeModeRoleOverrides {
    val background = parseMd3CustomColorHex(roles.backgroundHex)
    fun surfaces(primary: Color) = listOf(background) + listOf(
        if (darkTheme) 0.08f else 0.035f,
        if (darkTheme) 0.12f else 0.055f,
        if (darkTheme) 0.18f else 0.09f,
        if (darkTheme) 0.24f else 0.13f,
    ).map { lerp(background, primary, it) }
    var primary = parseMd3CustomColorHex(roles.primaryTextHex)
    repeat(8) { primary = resolveSeedReadableForeground(primary, primary, surfaces(primary)) }
    val secondary = parseMd3CustomColorHex(roles.secondaryTextHex)
    return roles.copy(
        primaryTextHex = formatMd3CustomColorHex(primary),
        secondaryTextHex = formatMd3CustomColorHex(resolveSeedReadableForeground(secondary, secondary, surfaces(primary))),
    )
}
