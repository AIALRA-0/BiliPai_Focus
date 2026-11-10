package com.android.purebilibili.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.SeedPaletteStyle
import com.android.purebilibili.core.theme.ThemePreviewConfiguration
import com.android.purebilibili.core.theme.resolveThemePreviewPalette
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

/**
 * 外观单选选项的示意图类型：决定 [ThemeOptionPreviewThumb] 用哪份预览方案着色。
 */
internal enum class ThemeOptionPreviewKind {
    /** 浅色方案。 */
    LIGHT,

    /** 普通深色方案。 */
    DARK,

    /** 跟随系统：整幅浅色上按对角线拼半幅深色。 */
    FOLLOW_SYSTEM,

    /** AMOLED 纯黑深色方案。 */
    AMOLED,
}

/**
 * 主题模式与深色主题风格选项示意图所用的三份预览方案。
 *
 * 全部经 [resolveThemePreviewPalette] 从当前种子色、色彩风格、色彩标准、对比度、
 * 种子调色板与角色覆盖推导，与页面内色板小样看到的是同一套配色结果；
 * AMOLED 单独按 `amoledDarkTheme = true` 生成，浅色与普通深色固定关闭该参数，
 * 与「深色主题风格只影响深色」的实际行为一致。
 */
@Immutable
internal data class ThemeOptionPreviewPalettes(
    val light: ColorScheme,
    val dark: ColorScheme,
    val amoled: ColorScheme,
)

/**
 * 整组生成 [ThemeOptionPreviewPalettes]：输入变化时一次性重算三份方案，
 * 单个选项行重组时不重复生成。
 */
@Composable
internal fun rememberThemeOptionPreviewPalettes(
    seed: Color,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
    colorContrast: Contrast,
    seedPaletteStyle: SeedPaletteStyle,
    configuration: ThemePreviewConfiguration,
): ThemeOptionPreviewPalettes = remember(
    seed, paletteStyle, colorSpec, colorContrast, seedPaletteStyle, configuration,
) {
    ThemeOptionPreviewPalettes(
        light = resolveThemePreviewPalette(
            seed, false, paletteStyle, colorSpec, colorContrast, seedPaletteStyle,
            configuration.copy(amoledDarkTheme = false),
        ).material,
        dark = resolveThemePreviewPalette(
            seed, true, paletteStyle, colorSpec, colorContrast, seedPaletteStyle,
            configuration.copy(amoledDarkTheme = false),
        ).material,
        amoled = resolveThemePreviewPalette(
            seed, true, paletteStyle, colorSpec, colorContrast, seedPaletteStyle,
            configuration.copy(amoledDarkTheme = true),
        ).material,
    )
}

/**
 * 主题模式 / 深色主题风格选项的迷你屏幕示意图。
 *
 * 固定 44×30dp 的圆角缩略屏：搜索条、头像位、双卡片与底部 dock，各部位取传入
 * 方案的真实角色色（surface、surfaceContainer、surfaceContainerHighest、primary、
 * primaryContainer 与文字层级），选中前即可看出该选项的实际观感；AMOLED 选项
 * 通过纯黑底面与普通深色区分，跟随系统为浅深对角拼接。
 *
 * 纯绘制、无语义也不响应触摸，选项行已有文字标签承担读屏与选中态。经单选条目的
 * optionLeading 提供时，锚定玻璃弹窗、MD3 菜单与居中弹窗都显示示意图；
 * Miuix 原生窗口菜单使用同一前导小样。
 */
@Composable
internal fun ThemeOptionPreviewThumb(
    kind: ThemeOptionPreviewKind,
    palettes: ThemeOptionPreviewPalettes,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .size(width = 44.dp, height = 30.dp)
            .clip(AppShapes.container(ContainerLevel.Chip)),
    ) {
        when (kind) {
            ThemeOptionPreviewKind.LIGHT -> drawMiniScreen(palettes.light)
            ThemeOptionPreviewKind.DARK -> drawMiniScreen(palettes.dark)
            ThemeOptionPreviewKind.AMOLED -> drawMiniScreen(palettes.amoled)
            ThemeOptionPreviewKind.FOLLOW_SYSTEM -> {
                drawMiniScreen(palettes.light)
                clipPath(followSystemSplitPath()) {
                    drawMiniScreen(palettes.dark)
                }
            }
        }
    }
}

/** 右侧深色的对角分界：上下端点各向中间收窄 34%/66%，保证两份方案都完整可见。 */
private fun DrawScope.followSystemSplitPath(): Path = Path().apply {
    moveTo(size.width * 0.66f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height)
    lineTo(size.width * 0.34f, size.height)
    close()
}

/**
 * 迷你屏幕草稿。以宽度的 1/22 为单位定位，深浅两套方案下各角色色均有层级差，
 * 不额外叠加描边或阴影。
 */
private fun DrawScope.drawMiniScreen(scheme: ColorScheme) {
    drawRect(color = scheme.surface)
    val unit = size.width / 22f
    val margin = unit

    // 搜索条 + 头像位
    val searchHeight = unit * 1.6f
    drawRoundRect(
        color = scheme.surfaceContainer,
        topLeft = Offset(margin, unit * 0.9f),
        size = Size(size.width * 0.62f, searchHeight),
        cornerRadius = CornerRadius(searchHeight / 2f),
    )
    drawCircle(
        color = scheme.primaryContainer,
        radius = unit * 1.6f,
        center = Offset(size.width - margin - unit * 1.6f, unit * 0.9f + searchHeight / 2f),
    )

    // 双卡片：封面区留空为卡片底色，下沿两行示意标题与信息
    val cardTop = unit * 3.4f
    val cardHeight = unit * 4f
    val cardGap = unit * 1.2f
    val cardWidth = (size.width - margin * 2f - cardGap) / 2f
    val cardRadius = CornerRadius(unit * 0.8f)
    for (index in 0..1) {
        val cardLeft = margin + index * (cardWidth + cardGap)
        drawRoundRect(
            color = scheme.surfaceContainer,
            topLeft = Offset(cardLeft, cardTop),
            size = Size(cardWidth, cardHeight),
            cornerRadius = cardRadius,
        )
        drawRoundRect(
            color = scheme.onSurfaceVariant.copy(alpha = 0.5f),
            topLeft = Offset(cardLeft + unit * 0.9f, cardTop + unit * 2.6f),
            size = Size(cardWidth * 0.62f, unit * 0.7f),
            cornerRadius = CornerRadius(unit * 0.35f),
        )
        drawRoundRect(
            color = scheme.onSurfaceVariant.copy(alpha = 0.28f),
            topLeft = Offset(cardLeft + unit * 0.9f, cardTop + unit * 3.6f),
            size = Size(cardWidth * 0.42f, unit * 0.55f),
            cornerRadius = CornerRadius(unit * 0.28f),
        )
    }

    // 底部 dock：首位选中胶囊（primary 底 + onPrimary 圆点），其余为弱化圆点
    val dockHeight = unit * 5f
    val dockTop = size.height - margin - dockHeight
    drawRoundRect(
        color = scheme.surfaceContainerHighest,
        topLeft = Offset(margin, dockTop),
        size = Size(size.width - margin * 2f, dockHeight),
        cornerRadius = CornerRadius(dockHeight / 2f),
    )
    val dockWidth = size.width - margin * 2f
    val dockCenterY = dockTop + dockHeight / 2f
    for (index in 0..3) {
        val centerX = margin + dockWidth * (index * 2f + 1f) / 8f
        if (index == 0) {
            drawCircle(color = scheme.primary, radius = unit * 1.7f, center = Offset(centerX, dockCenterY))
            drawCircle(color = scheme.onPrimary, radius = unit * 0.6f, center = Offset(centerX, dockCenterY))
        } else {
            drawCircle(
                color = scheme.onSurfaceVariant.copy(alpha = 0.55f),
                radius = unit * 0.75f,
                center = Offset(centerX, dockCenterY),
            )
        }
    }
}
