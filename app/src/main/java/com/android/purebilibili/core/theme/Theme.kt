// 文件路径: core/theme/Theme.kt
package com.android.purebilibili.core.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.android.purebilibili.feature.settings.AppThemeMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.defaultTextStyles
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

// --- 扩展颜色定义 ---
private val LightSurfaceVariant = Color(0xFFF1F2F3)

//  [优化] 根据主题色索引生成配色方案
private fun createDarkColorScheme(primaryColor: Color) = darkColorScheme(
    primary = primaryColor,
    onPrimary = White,
    primaryContainer = primaryColor.copy(alpha = 0.3f), //  Container derived from primary
    onPrimaryContainer = primaryColor.copy(alpha = 1f), // Stronger primary for content
    secondary = primaryColor.copy(alpha = 0.85f),
    secondaryContainer = primaryColor.copy(alpha = 0.2f), //  Container derived from primary
    onSecondaryContainer = primaryColor.copy(alpha = 0.9f),
    background = DarkBackground, // iOS User Interface Black
    surface = DarkSurface, // iOS System Gray 6 (Dark)
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = DarkSurfaceElevated, // iOS System Gray 5 (Dark)
    outline = iOSSystemGray3Dark,
    outlineVariant = iOSSystemGray4Dark
)

private fun createAmoledDarkColorScheme(primaryColor: Color) = darkColorScheme(
    primary = primaryColor,
    onPrimary = White,
    primaryContainer = primaryColor.copy(alpha = 0.32f),
    onPrimaryContainer = primaryColor,
    secondary = primaryColor.copy(alpha = 0.9f),
    secondaryContainer = primaryColor.copy(alpha = 0.22f),
    onSecondaryContainer = primaryColor,
    background = Black,
    surface = Black,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF050505),
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = Color(0xFF090909),
    outline = Color(0xFF262626),
    outlineVariant = Color(0xFF1A1A1A)
)

internal fun resolveEffectiveDynamicColorEnabled(
    dynamicColorEnabled: Boolean,
    amoledDarkTheme: Boolean,
    uiPreset: UiPreset
): Boolean = dynamicColorEnabled

internal fun resolveMiuixColorSchemeMode(
    themeMode: AppThemeMode,
    dynamicColorEnabled: Boolean
): ColorSchemeMode {
    return when (themeMode) {
        AppThemeMode.FOLLOW_SYSTEM -> {
            if (dynamicColorEnabled) ColorSchemeMode.MonetSystem else ColorSchemeMode.System
        }

        AppThemeMode.LIGHT -> {
            if (dynamicColorEnabled) ColorSchemeMode.MonetLight else ColorSchemeMode.Light
        }

        AppThemeMode.DARK -> {
            if (dynamicColorEnabled) ColorSchemeMode.MonetDark else ColorSchemeMode.Dark
        }
    }
}

internal data class MiuixMaterialBridge(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val error: Color,
    val onError: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val outline: Color,
    val outlineVariant: Color
)

internal fun createMiuixMaterialBridge(colorScheme: ColorScheme): MiuixMaterialBridge {
    return MiuixMaterialBridge(
        primary = colorScheme.primary,
        onPrimary = colorScheme.onPrimary,
        primaryContainer = colorScheme.primaryContainer,
        onPrimaryContainer = colorScheme.onPrimaryContainer,
        secondary = colorScheme.secondary,
        onSecondary = colorScheme.onSecondary,
        secondaryContainer = colorScheme.secondaryContainer,
        onSecondaryContainer = colorScheme.onSecondaryContainer,
        tertiary = colorScheme.tertiary,
        onTertiary = colorScheme.onTertiary,
        tertiaryContainer = colorScheme.tertiaryContainer,
        onTertiaryContainer = colorScheme.onTertiaryContainer,
        error = colorScheme.error,
        onError = colorScheme.onError,
        background = colorScheme.background,
        onBackground = colorScheme.onBackground,
        surface = colorScheme.surface,
        onSurface = colorScheme.onSurface,
        surfaceVariant = colorScheme.surfaceVariant,
        onSurfaceVariant = colorScheme.onSurfaceVariant,
        surfaceContainer = colorScheme.surfaceContainer,
        surfaceContainerHigh = colorScheme.surfaceContainerHigh,
        outline = colorScheme.outline,
        outlineVariant = colorScheme.outlineVariant
    )
}

internal fun resolveMaterialColorSchemeFromMiuixBridge(
    bridge: MiuixMaterialBridge,
    amoledDarkTheme: Boolean
): ColorScheme {
    val baseScheme = if (bridge.background.luminance() < 0.5f) {
        darkColorScheme(
            primary = bridge.primary,
            onPrimary = bridge.onPrimary,
            primaryContainer = bridge.primaryContainer,
            onPrimaryContainer = bridge.onPrimaryContainer,
            secondary = bridge.secondary,
            onSecondary = bridge.onSecondary,
            secondaryContainer = bridge.secondaryContainer,
            onSecondaryContainer = bridge.onSecondaryContainer,
            tertiary = bridge.tertiary,
            onTertiary = bridge.onTertiary,
            tertiaryContainer = bridge.tertiaryContainer,
            onTertiaryContainer = bridge.onTertiaryContainer,
            error = bridge.error,
            onError = bridge.onError,
            background = bridge.background,
            onBackground = bridge.onBackground,
            surface = bridge.surface,
            onSurface = bridge.onSurface,
            surfaceVariant = bridge.surfaceVariant,
            onSurfaceVariant = bridge.onSurfaceVariant,
            surfaceContainer = bridge.surfaceContainer,
            surfaceContainerHigh = bridge.surfaceContainerHigh,
            outline = bridge.outline,
            outlineVariant = bridge.outlineVariant
        )
    } else {
        lightColorScheme(
            primary = bridge.primary,
            onPrimary = bridge.onPrimary,
            primaryContainer = bridge.primaryContainer,
            onPrimaryContainer = bridge.onPrimaryContainer,
            secondary = bridge.secondary,
            onSecondary = bridge.onSecondary,
            secondaryContainer = bridge.secondaryContainer,
            onSecondaryContainer = bridge.onSecondaryContainer,
            tertiary = bridge.tertiary,
            onTertiary = bridge.onTertiary,
            tertiaryContainer = bridge.tertiaryContainer,
            onTertiaryContainer = bridge.onTertiaryContainer,
            error = bridge.error,
            onError = bridge.onError,
            background = bridge.background,
            onBackground = bridge.onBackground,
            surface = bridge.surface,
            onSurface = bridge.onSurface,
            surfaceVariant = bridge.surfaceVariant,
            onSurfaceVariant = bridge.onSurfaceVariant,
            surfaceContainer = bridge.surfaceContainer,
            surfaceContainerHigh = bridge.surfaceContainerHigh,
            outline = bridge.outline,
            outlineVariant = bridge.outlineVariant
        )
    }
    return if (amoledDarkTheme) {
        applyAmoledSurfaceOverrides(baseScheme)
    } else {
        baseScheme
    }
}

internal fun resolveMiuixColorsFromMaterialBridge(
    bridge: MiuixMaterialBridge,
    darkTheme: Boolean
): top.yukonga.miuix.kmp.theme.Colors {
    val base = if (darkTheme) miuixDarkColorScheme() else miuixLightColorScheme()
    return base.copy(
        primary = bridge.primary,
        onPrimary = bridge.onPrimary,
        primaryVariant = bridge.primaryContainer,
        onPrimaryVariant = bridge.onPrimaryContainer,
        primaryContainer = bridge.primaryContainer,
        onPrimaryContainer = bridge.onPrimaryContainer,
        secondary = bridge.secondary,
        onSecondary = bridge.onSecondary,
        secondaryVariant = bridge.surfaceContainerHigh,
        onSecondaryVariant = bridge.onSurfaceVariant,
        secondaryContainer = bridge.secondaryContainer,
        onSecondaryContainer = bridge.onSecondaryContainer,
        secondaryContainerVariant = bridge.surfaceContainer,
        onSecondaryContainerVariant = bridge.onSurfaceVariant,
        tertiaryContainer = bridge.tertiaryContainer,
        onTertiaryContainer = bridge.onTertiaryContainer,
        tertiaryContainerVariant = bridge.tertiaryContainer,
        error = bridge.error,
        onError = bridge.onError,
        background = bridge.background,
        onBackground = bridge.onBackground,
        onBackgroundVariant = bridge.onSurfaceVariant,
        surface = bridge.surface,
        onSurface = bridge.onSurface,
        surfaceVariant = bridge.surfaceVariant,
        onSurfaceSecondary = bridge.onSurfaceVariant,
        onSurfaceVariantSummary = bridge.onSurfaceVariant,
        onSurfaceVariantActions = bridge.onSurfaceVariant,
        surfaceContainer = bridge.surfaceContainer,
        onSurfaceContainer = bridge.onSurface,
        onSurfaceContainerVariant = bridge.onSurfaceVariant,
        surfaceContainerHigh = bridge.surfaceContainerHigh,
        onSurfaceContainerHigh = bridge.onSurface,
        surfaceContainerHighest = bridge.surfaceContainerHigh,
        onSurfaceContainerHighest = bridge.onSurface,
        outline = bridge.outline,
        dividerLine = bridge.outlineVariant
    )
}

internal fun applyAmoledSurfaceOverrides(
    baseScheme: ColorScheme
): ColorScheme = baseScheme.copy(
    background = Black,
    surface = Black,
    surfaceVariant = Color(0xFF050505),
    surfaceContainer = Color(0xFF090909),
    outline = Color(0xFF262626),
    outlineVariant = Color(0xFF1A1A1A)
)

private fun createLightColorScheme(primaryColor: Color) = lightColorScheme(
    primary = primaryColor,
    onPrimary = White,
    primaryContainer = primaryColor.copy(alpha = 0.15f), //  Container derived from primary (ligther for light mode)
    onPrimaryContainer = primaryColor,
    secondary = primaryColor.copy(alpha = 0.8f),
    secondaryContainer = primaryColor.copy(alpha = 0.1f), //  Container derived from primary
    onSecondaryContainer = primaryColor,
    background = iOSSystemGray6, // Use iOS System Gray 6 for main background (grouped table view style)
    surface = White, // iOS cards are usually white
    onSurface = TextPrimary,
    surfaceVariant = iOSSystemGray5, // Separators / Higher groupings
    onSurfaceVariant = TextSecondary,
    surfaceContainer = iOSSystemGray5, // iOS System Gray 5 (Light)
    outline = iOSSystemGray3,
    outlineVariant = iOSSystemGray4
)

// 保留默认配色作为后备 (使用 iOS 系统蓝)
private val DarkColorScheme = createDarkColorScheme(iOSSystemBlue)
private val LightColorScheme = createLightColorScheme(iOSSystemBlue)

private data class HslColorModel(
    val hue: Float,
    val saturation: Float,
    val lightness: Float
)

private fun Color.toHslColorModel(): HslColorModel {
    val red = red
    val green = green
    val blue = blue
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    val lightness = (max + min) / 2f

    val saturation = if (delta == 0f) {
        0f
    } else {
        delta / (1f - kotlin.math.abs(2f * lightness - 1f))
    }

    val hue = when {
        delta == 0f -> 0f
        max == red -> 60f * positiveModulo((green - blue) / delta, 6f)
        max == green -> 60f * (((blue - red) / delta) + 2f)
        else -> 60f * (((red - green) / delta) + 4f)
    }

    return HslColorModel(
        hue = normalizeHue(hue),
        saturation = saturation.coerceIn(0f, 1f),
        lightness = lightness.coerceIn(0f, 1f)
    )
}

private fun normalizeHue(hue: Float): Float {
    val value = hue % 360f
    return if (value < 0f) value + 360f else value
}

private fun positiveModulo(value: Float, modulus: Float): Float {
    val result = value % modulus
    return if (result < 0f) result + modulus else result
}

private fun colorFromHsl(
    hue: Float,
    saturation: Float,
    lightness: Float
): Color {
    val normalizedHue = normalizeHue(hue)
    val normalizedSaturation = saturation.coerceIn(0f, 1f)
    val normalizedLightness = lightness.coerceIn(0f, 1f)
    val chroma = (1f - kotlin.math.abs(2f * normalizedLightness - 1f)) * normalizedSaturation
    val huePrime = normalizedHue / 60f
    val secondComponent = chroma * (1f - kotlin.math.abs(positiveModulo(huePrime, 2f) - 1f))
    val match = normalizedLightness - chroma / 2f

    val (redPrime, greenPrime, bluePrime) = when {
        huePrime < 1f -> Triple(chroma, secondComponent, 0f)
        huePrime < 2f -> Triple(secondComponent, chroma, 0f)
        huePrime < 3f -> Triple(0f, chroma, secondComponent)
        huePrime < 4f -> Triple(0f, secondComponent, chroma)
        huePrime < 5f -> Triple(secondComponent, 0f, chroma)
        else -> Triple(chroma, 0f, secondComponent)
    }

    return Color(
        redPrime + match,
        greenPrime + match,
        bluePrime + match,
        1f,
        ColorSpaces.Srgb
    )
}

private fun blendColors(
    background: Color,
    foreground: Color,
    foregroundRatio: Float
): Color {
    val ratio = foregroundRatio.coerceIn(0f, 1f)
    val inverse = 1f - ratio
    return Color(
        background.red * inverse + foreground.red * ratio,
        background.green * inverse + foreground.green * ratio,
        background.blue * inverse + foreground.blue * ratio,
        background.alpha * inverse + foreground.alpha * ratio,
        ColorSpaces.Srgb
    )
}

private fun chooseReadableOnColor(background: Color): Color {
    return if (calculateContrastRatio(White, background) >= calculateContrastRatio(Black, background)) {
        White
    } else {
        Black
    }
}

private fun deriveNeutralSurfaceColor(
    source: HslColorModel,
    lightness: Float,
    maxSaturation: Float
): Color {
    return colorFromHsl(
        hue = source.hue,
        saturation = minOf(source.saturation * 0.16f, maxSaturation),
        lightness = lightness
    )
}

private fun deriveAccentColor(
    source: HslColorModel,
    hueShift: Float,
    saturationScale: Float,
    lightness: Float,
    minimumSaturation: Float = 0.18f
): Color {
    return colorFromHsl(
        hue = source.hue + hueShift,
        saturation = maxOf(minimumSaturation, source.saturation * saturationScale),
        lightness = lightness
    )
}

internal fun createStaticMd3ColorScheme(
    primaryColor: Color,
    darkTheme: Boolean,
    amoledDarkTheme: Boolean
): ColorScheme {
    val source = primaryColor.toHslColorModel()

    val scheme = if (darkTheme) {
        val primary = deriveAccentColor(
            source = source,
            hueShift = 0f,
            saturationScale = 0.90f,
            lightness = maxOf(source.lightness, 0.78f),
            minimumSaturation = 0.22f
        )
        val secondary = deriveAccentColor(
            source = source,
            hueShift = 10f,
            saturationScale = 0.42f,
            lightness = 0.76f,
            minimumSaturation = 0.16f
        )
        val tertiary = deriveAccentColor(
            source = source,
            hueShift = 56f,
            saturationScale = 0.52f,
            lightness = 0.78f,
            minimumSaturation = 0.20f
        )
        val background = deriveNeutralSurfaceColor(source, lightness = 0.075f, maxSaturation = 0.05f)
        val surface = deriveNeutralSurfaceColor(source, lightness = 0.10f, maxSaturation = 0.06f)
        val surfaceVariant = deriveNeutralSurfaceColor(source, lightness = 0.18f, maxSaturation = 0.09f)
        val surfaceContainer = deriveNeutralSurfaceColor(source, lightness = 0.14f, maxSaturation = 0.07f)
        val surfaceContainerHigh = deriveNeutralSurfaceColor(source, lightness = 0.17f, maxSaturation = 0.08f)
        val outline = deriveNeutralSurfaceColor(source, lightness = 0.54f, maxSaturation = 0.08f)
        val outlineVariant = deriveNeutralSurfaceColor(source, lightness = 0.33f, maxSaturation = 0.07f)
        val primaryContainer = blendColors(background = background, foreground = primary, foregroundRatio = 0.34f)
        val secondaryContainer = blendColors(background = background, foreground = secondary, foregroundRatio = 0.28f)
        val tertiaryContainer = blendColors(background = background, foreground = tertiary, foregroundRatio = 0.28f)

        darkColorScheme(
            primary = primary,
            onPrimary = chooseReadableOnColor(primary),
            primaryContainer = primaryContainer,
            onPrimaryContainer = chooseReadableOnColor(primaryContainer),
            secondary = secondary,
            onSecondary = chooseReadableOnColor(secondary),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = chooseReadableOnColor(secondaryContainer),
            tertiary = tertiary,
            onTertiary = chooseReadableOnColor(tertiary),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = chooseReadableOnColor(tertiaryContainer),
            background = background,
            onBackground = chooseReadableOnColor(background),
            surface = surface,
            onSurface = chooseReadableOnColor(surface),
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = resolveReadableTextColor(
                candidate = deriveNeutralSurfaceColor(source, lightness = 0.78f, maxSaturation = 0.08f),
                background = surfaceVariant,
                fallback = chooseReadableOnColor(surfaceVariant),
                minimumContrast = 3.0f
            ),
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            outline = outline,
            outlineVariant = outlineVariant
        )
    } else {
        val primary = primaryColor
        val secondary = deriveAccentColor(
            source = source,
            hueShift = 10f,
            saturationScale = 0.42f,
            lightness = source.lightness.coerceIn(0.34f, 0.46f),
            minimumSaturation = 0.15f
        )
        val tertiary = deriveAccentColor(
            source = source,
            hueShift = 56f,
            saturationScale = 0.55f,
            lightness = 0.42f,
            minimumSaturation = 0.18f
        )
        val background = deriveNeutralSurfaceColor(source, lightness = 0.98f, maxSaturation = 0.12f)
        val surface = deriveNeutralSurfaceColor(source, lightness = 0.99f, maxSaturation = 0.04f)
        val surfaceVariant = deriveNeutralSurfaceColor(source, lightness = 0.90f, maxSaturation = 0.08f)
        val surfaceContainer = deriveNeutralSurfaceColor(source, lightness = 0.95f, maxSaturation = 0.06f)
        val surfaceContainerHigh = deriveNeutralSurfaceColor(source, lightness = 0.92f, maxSaturation = 0.07f)
        val outline = deriveNeutralSurfaceColor(source, lightness = 0.55f, maxSaturation = 0.08f)
        val outlineVariant = deriveNeutralSurfaceColor(source, lightness = 0.82f, maxSaturation = 0.06f)
        val primaryContainer = blendColors(background = background, foreground = primary, foregroundRatio = 0.18f)
        val secondaryContainer = blendColors(background = background, foreground = secondary, foregroundRatio = 0.16f)
        val tertiaryContainer = blendColors(background = background, foreground = tertiary, foregroundRatio = 0.16f)

        lightColorScheme(
            primary = primary,
            onPrimary = chooseReadableOnColor(primary),
            primaryContainer = primaryContainer,
            onPrimaryContainer = chooseReadableOnColor(primaryContainer),
            secondary = secondary,
            onSecondary = chooseReadableOnColor(secondary),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = chooseReadableOnColor(secondaryContainer),
            tertiary = tertiary,
            onTertiary = chooseReadableOnColor(tertiary),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = chooseReadableOnColor(tertiaryContainer),
            background = background,
            onBackground = chooseReadableOnColor(background),
            surface = surface,
            onSurface = chooseReadableOnColor(surface),
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = resolveReadableTextColor(
                candidate = deriveNeutralSurfaceColor(source, lightness = 0.36f, maxSaturation = 0.08f),
                background = surfaceVariant,
                fallback = chooseReadableOnColor(surfaceVariant),
                minimumContrast = 3.0f
            ),
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            outline = outline,
            outlineVariant = outlineVariant
        )
    }

    return if (darkTheme && amoledDarkTheme) {
        applyAmoledSurfaceOverrides(scheme)
    } else {
        scheme
    }
}

private fun createMd3DarkColorScheme(primaryColor: Color) = createStaticMd3ColorScheme(
    primaryColor = primaryColor,
    darkTheme = true,
    amoledDarkTheme = false
)

private fun createMd3LightColorScheme(primaryColor: Color) = createStaticMd3ColorScheme(
    primaryColor = primaryColor,
    darkTheme = false,
    amoledDarkTheme = false
)

@Composable
fun PureBiliBiliTheme(
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
    themeMode: AppThemeMode = AppThemeMode.FOLLOW_SYSTEM,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    amoledDarkTheme: Boolean = false,
    themeColorIndex: Int = 0, //  默认 0 = iOS 蓝色
    fontSizePreset: AppFontSizePreset = AppFontSizePreset.DEFAULT,
    content: @Composable () -> Unit
) {
    //  🚀 [修复] 强制监听配置变化 (如更换壁纸触发的资源刷新)
    // 即使 Activity 不重建，Configuration 也会变化，触发重组从而获取最新的 dynamicColorScheme
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val context = LocalContext.current
    
    //  获取自定义主题色 (默认 iOS 蓝)
    val customPrimaryColor = ThemeColors.getOrElse(themeColorIndex) { iOSSystemBlue }

    val renderingProfile = resolveUiRenderingProfile(uiPreset)
    val isDynamicColorActive = resolveEffectiveDynamicColorEnabled(
        dynamicColorEnabled = dynamicColor,
        amoledDarkTheme = amoledDarkTheme,
        uiPreset = uiPreset
    ) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val shapes = resolveMaterialShapes(uiPreset, androidNativeVariant)
    val materialTypography = resolveMaterialTypography(
        uiPreset = uiPreset,
        androidNativeVariant = androidNativeVariant
    ).scaled(fontSizePreset.multiplier)
    val miuixTextStyles = remember(fontSizePreset) {
        defaultTextStyles().scaled(fontSizePreset.multiplier)
    }
    val lightMaterialScheme = enforceDynamicLightTextContrast(
        if (renderingProfile.useMaterialChrome) {
            createMd3LightColorScheme(customPrimaryColor)
        } else {
            createLightColorScheme(customPrimaryColor)
        }
    )
    val darkMaterialScheme = if (amoledDarkTheme) {
        createAmoledDarkColorScheme(customPrimaryColor)
    } else if (renderingProfile.useMaterialChrome) {
        createMd3DarkColorScheme(customPrimaryColor)
    } else {
        createDarkColorScheme(customPrimaryColor)
    }

    val staticMaterialScheme = if (darkTheme) darkMaterialScheme else lightMaterialScheme
    val miuixLightColors = remember(lightMaterialScheme) {
        resolveMiuixColorsFromMaterialBridge(
            bridge = createMiuixMaterialBridge(lightMaterialScheme),
            darkTheme = false
        )
    }
    val miuixDarkColors = remember(darkMaterialScheme) {
        resolveMiuixColorsFromMaterialBridge(
            bridge = createMiuixMaterialBridge(darkMaterialScheme),
            darkTheme = true
        )
    }
    val controller = remember(
        themeMode,
        dynamicColor,
        darkTheme,
        miuixLightColors,
        miuixDarkColors
    ) {
        ThemeController(
            colorSchemeMode = resolveMiuixColorSchemeMode(
                themeMode = themeMode,
                dynamicColorEnabled = dynamicColor
            ),
            lightColors = miuixLightColors,
            darkColors = miuixDarkColors,
            isDark = darkTheme
        )
    }
    val materialColorScheme = if (isDynamicColorActive) {
        if (darkTheme) {
            val dynamicDark = dynamicDarkColorScheme(context)
            if (amoledDarkTheme) applyAmoledSurfaceOverrides(dynamicDark) else dynamicDark
        } else {
            enforceDynamicLightTextContrast(dynamicLightColorScheme(context))
        }
    } else {
        staticMaterialScheme
    }

    //  [新增] 动态设置状态栏图标颜色
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 设置状态栏图标颜色：
            // - 深色模式：使用浅色图标 (isAppearanceLightStatusBars = false)
            // - 浅色模式：使用深色图标 (isAppearanceLightStatusBars = true)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalUiPreset provides uiPreset,
        LocalAndroidNativeVariant provides androidNativeVariant,
        LocalDynamicColorActive provides isDynamicColorActive,
        LocalCornerRadiusScale provides resolveCornerRadiusScale(
            uiPreset = uiPreset,
            androidNativeVariant = androidNativeVariant
        )
    ) {
        MiuixTheme(
            controller = controller,
            textStyles = miuixTextStyles
        ) {
            MaterialTheme(
                colorScheme = materialColorScheme,
                typography = materialTypography,
                shapes = shapes,
                content = content
            )
        }
    }
}
