package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.SeedPaletteStyle
import com.android.purebilibili.core.theme.material2025Style
import com.android.purebilibili.core.theme.resolveMaterialPaletteSpec
import com.android.purebilibili.core.theme.resolveSeedPaletteSpec
import com.android.purebilibili.core.theme.supportsMaterialSpec2025
import com.android.purebilibili.core.ui.AppIconStyle
import com.android.purebilibili.core.ui.AppListItemStyle
import com.android.purebilibili.core.ui.components.AppSegmentOption
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

internal fun resolveThemeModeSegmentOptions(
    followSystemLabel: String = AppThemeMode.FOLLOW_SYSTEM.label,
    lightLabel: String = AppThemeMode.LIGHT.label,
    darkLabel: String = AppThemeMode.DARK.label
): List<AppSegmentOption<AppThemeMode>> {
    return listOf(
        AppSegmentOption(AppThemeMode.FOLLOW_SYSTEM, followSystemLabel),
        AppSegmentOption(AppThemeMode.LIGHT, lightLabel),
        AppSegmentOption(AppThemeMode.DARK, darkLabel)
    )
}

internal fun resolveSeedPaletteStyleOptions(): List<AppSegmentOption<SeedPaletteStyle>> {
    val material = listOf(
        SeedPaletteStyle.Standard, SeedPaletteStyle.TonalSpot, SeedPaletteStyle.Neutral,
        SeedPaletteStyle.Vibrant, SeedPaletteStyle.Expressive, SeedPaletteStyle.Rainbow,
        SeedPaletteStyle.FruitSalad, SeedPaletteStyle.Monochrome, SeedPaletteStyle.Fidelity,
        SeedPaletteStyle.Content,
    )
    return (material + SeedPaletteStyle.entries.filterNot { it in material }).map { style ->
        AppSegmentOption(style, style.label, resolveSeedPaletteStyleDescription(style))
    }
}

internal fun resolveSeedPaletteStyleDescription(style: SeedPaletteStyle): String = when (style) {
    SeedPaletteStyle.Standard -> "自由搭配下方的色彩风格与对比度"
    SeedPaletteStyle.TonalSpot -> "柔和自然的默认配色，适合日常使用"
    SeedPaletteStyle.Neutral -> "以灰色为主，带一点主题色，简洁低调"
    SeedPaletteStyle.Vibrant -> "让主题色更鲜艳，搭配丰富的辅助颜色"
    SeedPaletteStyle.Expressive -> "大胆尝试不同颜色，带来更活泼的搭配"
    SeedPaletteStyle.Rainbow -> "保留你选的颜色，搭配简洁的灰色背景"
    SeedPaletteStyle.FruitSalad -> "加入清新明亮的配色，保留部分原有颜色"
    SeedPaletteStyle.Monochrome -> "只使用黑、白、灰，呈现纯粹的简约风格"
    SeedPaletteStyle.Fidelity -> "尽量保留你选的颜色，搭配反差鲜明的辅助色"
    SeedPaletteStyle.Content -> "围绕你选的颜色，搭配接近的辅助色"
    SeedPaletteStyle.Material -> "柔和耐看的经典 Material 配色"
    SeedPaletteStyle.Material3Legacy -> "经典 Material 配色，主题色鲜明、背景更素净"
    SeedPaletteStyle.Soft -> "让各处颜色更柔和，减少鲜艳感"
    SeedPaletteStyle.Vivid -> "让你选的主题色更浓郁、更醒目"
    SeedPaletteStyle.VividSurfaces -> "主题色更醒目，背景和卡片也带有更多颜色"
    SeedPaletteStyle.HighContrast -> "让文字、按钮和背景更容易区分"
    SeedPaletteStyle.UltraContrast -> "进一步拉开明暗差别，让内容更醒目"
    SeedPaletteStyle.Jolly -> "鲜艳明快的颜色搭配，整体更活泼"
    SeedPaletteStyle.VividBackground -> "背景也带有主题色，浅色更明亮、深色更深沉"
    SeedPaletteStyle.OneHue -> "围绕同一种颜色，用深浅变化区分按钮和卡片"
    SeedPaletteStyle.CandyPop -> "明亮鲜艳的糖果配色，文字会自动调整以便阅读"
    SeedPaletteStyle.Chroma -> "保留你选的颜色的鲜艳程度，突出原有特色"
}

internal fun resolveSelectedColorSpec(
    requested: ColorSpec.SpecVersion,
    style: PaletteStyle,
    seedStyle: SeedPaletteStyle,
): ColorSpec.SpecVersion = if (seedStyle.enabled) resolveSeedPaletteSpec(seedStyle, requested)
    else resolveMaterialPaletteSpec(style, requested)

internal fun shouldShowColorSpecControl(seedStyle: SeedPaletteStyle): Boolean =
    !seedStyle.enabled || seedStyle.material2025Style() != null

internal fun resolveSelectedColorSpecSubtitle(
    requested: ColorSpec.SpecVersion,
    style: PaletteStyle,
    seedStyle: SeedPaletteStyle,
): String {
    val effective = resolveSelectedColorSpec(requested, style, seedStyle)
    val supported = if (seedStyle.enabled) seedStyle.material2025Style() != null else supportsMaterialSpec2025(style)
    val name = if (seedStyle.enabled) seedStyle.label else style.name
    return if (!seedStyle.enabled && style is PaletteStyle.Cmf) {
        "当前 SPEC_2026：CMF 的新配色方案，其他风格的设置会保留"
    } else if (effective != requested && requested == ColorSpec.SpecVersion.SPEC_2026 && supported) {
        "当前 ${effective.name}：$name 按官方规则使用 2025 标准；SPEC_2026 适用于 CMF"
    } else if (!supported) {
        "当前 ${effective.name}：$name 仅支持此标准" +
            if (requested != effective) "；已保留 ${requested.name} 偏好，切换至兼容风格后恢复" else ""
    } else resolveColorSpecSubtitle(effective)
}

internal fun resolveColorStyleOptions(): List<AppSegmentOption<PaletteStyle>> {
    return (listOf(PaletteStyle.TonalSpot) + PaletteStyle.KnownStyles.filterNot { it == PaletteStyle.TonalSpot })
        .map { style ->
            AppSegmentOption(
                value = style,
                label = style.name,
                description = resolveColorStyleDescription(style),
            )
        }
}

/** 色彩风格在下拉里的补充说明，口径与 MaterialKolor / MCU 的语义一致。 */
internal fun resolveColorStyleDescription(style: PaletteStyle): String = when (style) {
    PaletteStyle.TonalSpot -> "柔和自然的默认配色，适合日常使用"
    PaletteStyle.Neutral -> "以灰色为主，带一点主题色，简洁低调"
    PaletteStyle.Vibrant -> "让主题色更鲜艳，搭配丰富的辅助颜色"
    PaletteStyle.Expressive -> "大胆尝试不同颜色，带来更活泼的搭配"
    PaletteStyle.Rainbow -> "保留你选的颜色，搭配简洁的灰色背景"
    PaletteStyle.FruitSalad -> "加入清新明亮的配色，带来不同的颜色搭配"
    PaletteStyle.Monochrome -> "只使用黑、白、灰，呈现纯粹的简约风格"
    PaletteStyle.Fidelity -> "尽量保留你选的颜色，也适合鲜艳的主题色"
    PaletteStyle.Content -> "围绕你选的颜色，搭配接近的辅助色"
    is PaletteStyle.Cmf -> "保留你选的颜色，让背景、按钮和文字搭配更协调"
    else -> "使用 ${style.name} 配色风格"
}

/** 色彩风格在当前设置项上的显示：枚举名 + 一行含义，不展开也能看明白。 */
internal fun resolveColorStyleSubtitle(style: PaletteStyle): String =
    "当前 ${style.name}：${resolveColorStyleDescription(style)}"

internal fun resolveColorSpecOptions(
    style: PaletteStyle = PaletteStyle.TonalSpot,
    seedStyle: SeedPaletteStyle = SeedPaletteStyle.Standard,
): List<AppSegmentOption<ColorSpec.SpecVersion>> {
    if (!seedStyle.enabled && style is PaletteStyle.Cmf) {
        return listOf(AppSegmentOption(
            ColorSpec.SpecVersion.SPEC_2026, "SPEC_2026",
            resolveColorSpecDescription(ColorSpec.SpecVersion.SPEC_2026),
        ))
    }
    val supports2025 = if (seedStyle.enabled) seedStyle.material2025Style() != null else supportsMaterialSpec2025(style)
    return listOf(
        ColorSpec.SpecVersion.SPEC_2021,
        ColorSpec.SpecVersion.SPEC_2025
    ).filter { it != ColorSpec.SpecVersion.SPEC_2025 || supports2025 }.map { spec ->
        AppSegmentOption(
            value = spec,
            label = spec.name,
            description = resolveColorSpecDescription(spec),
        )
    }
}

internal fun resolveColorSpecDescription(spec: ColorSpec.SpecVersion): String = when (spec) {
    ColorSpec.SpecVersion.SPEC_2021 -> "旧版 Material 3 配色规则，兼容传统色彩风格"
    ColorSpec.SpecVersion.SPEC_2025 -> "较新的 Material 配色规则，让背景、按钮和文字的搭配更协调"
    ColorSpec.SpecVersion.SPEC_2026 -> "用于 CMF 新配色，让背景、按钮与文字搭配更协调"
    else -> "使用 ${spec.name} 配色方案"
}

/** 色彩标准在当前设置项上的显示：枚举名 + 一行含义。 */
internal fun resolveColorSpecSubtitle(spec: ColorSpec.SpecVersion): String =
    "当前 ${spec.name}：${resolveColorSpecDescription(spec)}"

/**
 * 对比度档。对齐 PiliPlus 的 highContrast / ultraContrast 需求，但走 MaterialKolor
 * 原生的 Contrast（相对默认对比度的加减），不引入 flex_seed_scheme 的 FlexTones。
 */
internal fun resolveColorContrastOptions(): List<AppSegmentOption<Contrast>> {
    return listOf(
        Contrast.Reduced,
        Contrast.Default,
        Contrast.Medium,
        Contrast.High,
    ).map { contrast ->
        AppSegmentOption(
            value = contrast,
            label = contrast.name,
            description = resolveColorContrastDescription(contrast),
        )
    }
}

internal fun resolveColorContrastDescription(contrast: Contrast): String = when (contrast) {
    Contrast.Reduced -> "低于默认对比，配色更柔"
    Contrast.Default -> "Material 3 默认对比度"
    Contrast.Medium -> "高于默认对比，文字更清晰"
    Contrast.High -> "最高对比，接近无障碍高对比主题"
    else -> "使用 ${contrast.name} 对比度"
}

/** 对比度在当前设置项上的显示：枚举名 + 一行含义。 */
internal fun resolveColorContrastSubtitle(contrast: Contrast): String =
    "当前 ${contrast.name}：${resolveColorContrastDescription(contrast)}"

internal fun shouldShowMd3CustomColorControls(source: Md3ColorSource): Boolean =
    source == Md3ColorSource.CUSTOM

internal fun shouldShowMd3PaletteStyleControls(source: Md3ColorSource): Boolean =
    source != Md3ColorSource.FOLLOW_WALLPAPER

internal fun resolveAdvancedPaletteSubtitle(uiStyle: AppUiStyle): String =
    if (uiStyle == AppUiStyle.MIUIX) {
        "自定义明暗模式的背景、文字与控件色，卡片层次随背景调整"
    } else {
        "自定义明暗模式的背景、文字与控件色"
    }

internal fun resolveMd3ColorSourceOptions(): List<AppSegmentOption<Md3ColorSource>> {
    return listOf(
        AppSegmentOption(Md3ColorSource.FOLLOW_WALLPAPER, Md3ColorSource.FOLLOW_WALLPAPER.label),
        AppSegmentOption(Md3ColorSource.WALLPAPER_RETONED, Md3ColorSource.WALLPAPER_RETONED.label),
        AppSegmentOption(Md3ColorSource.CUSTOM, Md3ColorSource.CUSTOM.label)
    )
}

internal fun resolveAppIconStyleOptions(): List<AppSegmentOption<AppIconStyle>> {
    return listOf(
        AppSegmentOption(AppIconStyle.AUTO, "跟随预设"),
        AppSegmentOption(AppIconStyle.SOFT_COLORFUL, "柔和彩色"),
        AppSegmentOption(AppIconStyle.VIVID_COLORFUL, "鲜明彩色"),
        AppSegmentOption(AppIconStyle.THEME_CONTAINER, "主题色容器"),
        AppSegmentOption(AppIconStyle.MD3_STANDARD, "单色线性（MD3）")
    )
}

internal fun resolveAppListItemStyleOptions(): List<AppSegmentOption<AppListItemStyle>> {
    return listOf(
        AppSegmentOption(AppListItemStyle.AUTO, "跟随预设"),
        AppSegmentOption(AppListItemStyle.CUSTOM, "自定义条目"),
        AppSegmentOption(AppListItemStyle.NATIVE, "原生组件")
    )
}

internal fun resolveDarkThemeStyleSegmentOptions(
    defaultLabel: String = DarkThemeStyle.DEFAULT.label,
    amoledLabel: String = DarkThemeStyle.AMOLED.label
): List<AppSegmentOption<DarkThemeStyle>> {
    return listOf(
        AppSegmentOption(DarkThemeStyle.DEFAULT, defaultLabel),
        AppSegmentOption(DarkThemeStyle.AMOLED, amoledLabel)
    )
}

internal fun resolveAppLanguageSegmentOptions(
    followSystemLabel: String = "跟随系统",
    simplifiedChineseLabel: String = "简体中文",
    traditionalChineseLabel: String = "繁體中文",
    englishLabel: String = "英语"
): List<AppSegmentOption<AppLanguage>> {
    return listOf(
        AppSegmentOption(AppLanguage.FOLLOW_SYSTEM, followSystemLabel),
        AppSegmentOption(AppLanguage.SIMPLIFIED_CHINESE, simplifiedChineseLabel),
        AppSegmentOption(AppLanguage.TRADITIONAL_CHINESE_TAIWAN, traditionalChineseLabel),
        AppSegmentOption(AppLanguage.ENGLISH, englishLabel)
    )
}
