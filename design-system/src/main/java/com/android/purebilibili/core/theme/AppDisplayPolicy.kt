package com.android.purebilibili.core.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.theme.TextStyles
import top.yukonga.miuix.kmp.theme.defaultTextStyles

private const val DISPLAY_NARROW_WIDTH_THRESHOLD_DP = 360
// 下限与设置段选项(resolveAppDpiOverrideSegmentOptions 的 90..110)对齐;
// 官方触控目标最小值是物理尺寸,过低的密度缩放会让 48dp 目标缩水到可访问性阈值以下。
private const val DISPLAY_DPI_OVERRIDE_PERCENT_MIN = 90
private const val DISPLAY_DPI_OVERRIDE_PERCENT_MAX = 115

enum class AppFontSizePreset(
    val value: Int,
    val label: String,
    val multiplier: Float
) {
    SMALLEST(5, "特小", 0.85f),
    SMALLER(0, "更小", 0.92f),
    SMALL(1, "偏小", 0.96f),
    DEFAULT(2, "默认", 1.00f),
    LARGE(3, "偏大", 1.04f),
    LARGER(4, "更大", 1.08f),
    EXTRA_LARGE(6, "特大", 1.15f),
    EXTRA_EXTRA_LARGE(7, "超大", 1.25f);

    companion object {
        fun fromValue(value: Int): AppFontSizePreset {
            return entries.find { it.value == value } ?: DEFAULT
        }
    }
}

/**
 * 全局字重档位：对全部字阶槽位做统一字重覆盖（PiliPlus 同款交互）。
 * [FOLLOW_THEME] 表示不覆盖，沿用各槽位的 Material 基线字重。
 * 细字重（Thin/Light）在中文系统字体上不可靠，仅暴露 Medium 及以上档位。
 */
enum class AppFontWeightPreset(
    val value: Int,
    val label: String,
    val fontWeight: FontWeight?
) {
    FOLLOW_THEME(-1, "跟随默认", null),
    MEDIUM(0, "中黑", FontWeight.Medium),
    SEMI_BOLD(1, "半粗", FontWeight.SemiBold),
    BOLD(2, "粗体", FontWeight.Bold);

    companion object {
        fun fromValue(value: Int): AppFontWeightPreset {
            return entries.find { it.value == value } ?: FOLLOW_THEME
        }
    }
}

enum class AppUiScalePreset(
    val value: Int,
    val label: String,
    val densityMultiplier: Float
) {
    COMPACT(0, "紧凑", 0.92f),
    STANDARD(1, "标准", 1.00f),
    COMFORTABLE(2, "舒适", 1.04f),
    LARGE(3, "更大", 1.08f);

    companion object {
        fun fromValue(value: Int): AppUiScalePreset {
            return entries.find { it.value == value } ?: STANDARD
        }
    }
}

data class DisplayMetricsSnapshot(
    val systemDensityDpi: Int,
    val systemSmallestWidthDp: Int,
    val fontSizePreset: AppFontSizePreset,
    val uiScalePreset: AppUiScalePreset,
    val dpiOverridePercent: Int?,
    val effectiveDensityMultiplier: Float,
    val effectiveDensityDpi: Int,
    val effectiveSmallestWidthDp: Int,
    val isNarrowWidth: Boolean
)

val LocalDisplayMetricsSnapshot = staticCompositionLocalOf {
    DisplayMetricsSnapshot(
        systemDensityDpi = 440,
        systemSmallestWidthDp = 360,
        fontSizePreset = AppFontSizePreset.DEFAULT,
        uiScalePreset = AppUiScalePreset.STANDARD,
        dpiOverridePercent = null,
        effectiveDensityMultiplier = 1f,
        effectiveDensityDpi = 440,
        effectiveSmallestWidthDp = 360,
        isNarrowWidth = false
    )
}

fun resolveEffectiveDensityMultiplier(
    uiScalePreset: AppUiScalePreset,
    dpiOverridePercent: Int?
): Float {
    val normalizedOverride = dpiOverridePercent
        ?.coerceIn(DISPLAY_DPI_OVERRIDE_PERCENT_MIN, DISPLAY_DPI_OVERRIDE_PERCENT_MAX)
    return normalizedOverride?.div(100f) ?: uiScalePreset.densityMultiplier
}

fun resolveEffectiveSmallestWidthDp(
    smallestScreenWidthDp: Int,
    densityMultiplier: Float
): Int {
    if (smallestScreenWidthDp <= 0) return 0
    return (smallestScreenWidthDp / densityMultiplier)
        .roundToInt()
        .coerceAtLeast(1)
}

fun buildDisplayMetricsSnapshot(
    systemDensityDpi: Int,
    smallestScreenWidthDp: Int,
    uiScalePreset: AppUiScalePreset,
    fontSizePreset: AppFontSizePreset,
    dpiOverridePercent: Int?
): DisplayMetricsSnapshot {
    val effectiveDensityMultiplier = resolveEffectiveDensityMultiplier(
        uiScalePreset = uiScalePreset,
        dpiOverridePercent = dpiOverridePercent
    )
    val effectiveSmallestWidthDp = resolveEffectiveSmallestWidthDp(
        smallestScreenWidthDp = smallestScreenWidthDp,
        densityMultiplier = effectiveDensityMultiplier
    )
    return DisplayMetricsSnapshot(
        systemDensityDpi = systemDensityDpi,
        systemSmallestWidthDp = smallestScreenWidthDp,
        fontSizePreset = fontSizePreset,
        uiScalePreset = uiScalePreset,
        dpiOverridePercent = dpiOverridePercent,
        effectiveDensityMultiplier = effectiveDensityMultiplier,
        effectiveDensityDpi = (systemDensityDpi * effectiveDensityMultiplier).roundToInt(),
        effectiveSmallestWidthDp = effectiveSmallestWidthDp,
        isNarrowWidth = effectiveSmallestWidthDp < DISPLAY_NARROW_WIDTH_THRESHOLD_DP
    )
}

private fun TextStyle.scaled(multiplier: Float): TextStyle {
    return copy(
        fontSize = fontSize.scaled(multiplier),
        lineHeight = lineHeight.scaled(multiplier),
        letterSpacing = letterSpacing.scaled(multiplier)
    )
}

private fun TextUnit.scaled(multiplier: Float): TextUnit {
    return if (isSpecified) this * multiplier else this
}

fun Typography.scaled(multiplier: Float): Typography {
    if (multiplier == 1f) return this
    return copy(
        displayLarge = displayLarge.scaled(multiplier),
        displayMedium = displayMedium.scaled(multiplier),
        displaySmall = displaySmall.scaled(multiplier),
        headlineLarge = headlineLarge.scaled(multiplier),
        headlineMedium = headlineMedium.scaled(multiplier),
        headlineSmall = headlineSmall.scaled(multiplier),
        titleLarge = titleLarge.scaled(multiplier),
        titleMedium = titleMedium.scaled(multiplier),
        titleSmall = titleSmall.scaled(multiplier),
        bodyLarge = bodyLarge.scaled(multiplier),
        bodyMedium = bodyMedium.scaled(multiplier),
        bodySmall = bodySmall.scaled(multiplier),
        labelLarge = labelLarge.scaled(multiplier),
        labelMedium = labelMedium.scaled(multiplier),
        labelSmall = labelSmall.scaled(multiplier)
    )
}

private fun TextStyle.withFontFamily(fontFamily: FontFamily?): TextStyle {
    return if (fontFamily == null) this else copy(fontFamily = fontFamily)
}

fun Typography.withFontFamily(fontFamily: FontFamily?): Typography {
    if (fontFamily == null) return this
    return copy(
        displayLarge = displayLarge.withFontFamily(fontFamily),
        displayMedium = displayMedium.withFontFamily(fontFamily),
        displaySmall = displaySmall.withFontFamily(fontFamily),
        headlineLarge = headlineLarge.withFontFamily(fontFamily),
        headlineMedium = headlineMedium.withFontFamily(fontFamily),
        headlineSmall = headlineSmall.withFontFamily(fontFamily),
        titleLarge = titleLarge.withFontFamily(fontFamily),
        titleMedium = titleMedium.withFontFamily(fontFamily),
        titleSmall = titleSmall.withFontFamily(fontFamily),
        bodyLarge = bodyLarge.withFontFamily(fontFamily),
        bodyMedium = bodyMedium.withFontFamily(fontFamily),
        bodySmall = bodySmall.withFontFamily(fontFamily),
        labelLarge = labelLarge.withFontFamily(fontFamily),
        labelMedium = labelMedium.withFontFamily(fontFamily),
        labelSmall = labelSmall.withFontFamily(fontFamily)
    )
}

private fun TextStyle.withFontWeight(fontWeight: FontWeight?): TextStyle {
    return if (fontWeight == null) this else copy(fontWeight = fontWeight)
}

/** 统一覆盖全部字阶槽位的字重；[fontWeight] 为 null 时不覆盖。 */
fun Typography.withFontWeight(fontWeight: FontWeight?): Typography {
    if (fontWeight == null) return this
    return copy(
        displayLarge = displayLarge.withFontWeight(fontWeight),
        displayMedium = displayMedium.withFontWeight(fontWeight),
        displaySmall = displaySmall.withFontWeight(fontWeight),
        headlineLarge = headlineLarge.withFontWeight(fontWeight),
        headlineMedium = headlineMedium.withFontWeight(fontWeight),
        headlineSmall = headlineSmall.withFontWeight(fontWeight),
        titleLarge = titleLarge.withFontWeight(fontWeight),
        titleMedium = titleMedium.withFontWeight(fontWeight),
        titleSmall = titleSmall.withFontWeight(fontWeight),
        bodyLarge = bodyLarge.withFontWeight(fontWeight),
        bodyMedium = bodyMedium.withFontWeight(fontWeight),
        bodySmall = bodySmall.withFontWeight(fontWeight),
        labelLarge = labelLarge.withFontWeight(fontWeight),
        labelMedium = labelMedium.withFontWeight(fontWeight),
        labelSmall = labelSmall.withFontWeight(fontWeight)
    )
}

/**
 * Maps Miuix-native component roles onto the app's Material typography contract.
 * This keeps native Miuix controls visually consistent with neighboring MD3-backed content.
 * Native title roles use a compact 32/24/20/18sp scale, independent of Material display roles.
 * Apply user font scaling/family/weight to the resulting TextStyles after this conversion.
 */
fun Typography.toMiuixTextStyles(): TextStyles = defaultTextStyles(
    main = bodyLarge,
    paragraph = bodyLarge,
    body1 = bodyMedium,
    body2 = bodySmall,
    button = labelLarge,
    footnote1 = labelMedium,
    footnote2 = labelSmall,
    headline1 = titleMedium,
    headline2 = titleSmall,
    subtitle = labelLarge.copy(fontWeight = FontWeight.Bold),
    title1 = headlineLarge.copy(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Medium),
    title2 = headlineSmall.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Medium),
    title3 = titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium),
    title4 = titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
)

fun TextStyles.scaled(multiplier: Float): TextStyles {
    if (multiplier == 1f) return this
    return copy(
        main = main.scaled(multiplier),
        paragraph = paragraph.scaled(multiplier),
        body1 = body1.scaled(multiplier),
        body2 = body2.scaled(multiplier),
        button = button.scaled(multiplier),
        footnote1 = footnote1.scaled(multiplier),
        footnote2 = footnote2.scaled(multiplier),
        headline1 = headline1.scaled(multiplier),
        headline2 = headline2.scaled(multiplier),
        subtitle = subtitle.scaled(multiplier),
        title1 = title1.scaled(multiplier),
        title2 = title2.scaled(multiplier),
        title3 = title3.scaled(multiplier),
        title4 = title4.scaled(multiplier)
    )
}

fun TextStyles.withFontFamily(fontFamily: FontFamily?): TextStyles {
    if (fontFamily == null) return this
    return copy(
        main = main.withFontFamily(fontFamily),
        paragraph = paragraph.withFontFamily(fontFamily),
        body1 = body1.withFontFamily(fontFamily),
        body2 = body2.withFontFamily(fontFamily),
        button = button.withFontFamily(fontFamily),
        footnote1 = footnote1.withFontFamily(fontFamily),
        footnote2 = footnote2.withFontFamily(fontFamily),
        headline1 = headline1.withFontFamily(fontFamily),
        headline2 = headline2.withFontFamily(fontFamily),
        subtitle = subtitle.withFontFamily(fontFamily),
        title1 = title1.withFontFamily(fontFamily),
        title2 = title2.withFontFamily(fontFamily),
        title3 = title3.withFontFamily(fontFamily),
        title4 = title4.withFontFamily(fontFamily)
    )
}

fun TextStyles.withFontWeight(fontWeight: FontWeight?): TextStyles {
    if (fontWeight == null) return this
    return copy(
        main = main.withFontWeight(fontWeight),
        paragraph = paragraph.withFontWeight(fontWeight),
        body1 = body1.withFontWeight(fontWeight),
        body2 = body2.withFontWeight(fontWeight),
        button = button.withFontWeight(fontWeight),
        footnote1 = footnote1.withFontWeight(fontWeight),
        footnote2 = footnote2.withFontWeight(fontWeight),
        headline1 = headline1.withFontWeight(fontWeight),
        headline2 = headline2.withFontWeight(fontWeight),
        subtitle = subtitle.withFontWeight(fontWeight),
        title1 = title1.withFontWeight(fontWeight),
        title2 = title2.withFontWeight(fontWeight),
        title3 = title3.withFontWeight(fontWeight),
        title4 = title4.withFontWeight(fontWeight)
    )
}
