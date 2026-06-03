package com.android.purebilibili.feature.home.components

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import java.io.File
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopTabStylePolicyTest {

    @Test
    fun `floating plus liquid uses liquid glass`() {
        val state = resolveTopTabStyle(
            isBottomBarFloating = true,
            isBottomBarBlurEnabled = true,
            isLiquidGlassEnabled = true
        )

        assertEquals(true, state.floating)
        assertEquals(TopTabMaterialMode.LIQUID_GLASS, state.materialMode)
    }

    @Test
    fun `floating without liquid but blur enabled uses blur`() {
        val state = resolveTopTabStyle(
            isBottomBarFloating = true,
            isBottomBarBlurEnabled = true,
            isLiquidGlassEnabled = false
        )

        assertEquals(true, state.floating)
        assertEquals(TopTabMaterialMode.BLUR, state.materialMode)
    }

    @Test
    fun `floating without blur and liquid uses plain`() {
        val state = resolveTopTabStyle(
            isBottomBarFloating = true,
            isBottomBarBlurEnabled = false,
            isLiquidGlassEnabled = false
        )

        assertEquals(true, state.floating)
        assertEquals(TopTabMaterialMode.PLAIN, state.materialMode)
    }

    @Test
    fun `docked with blur uses blur`() {
        val state = resolveTopTabStyle(
            isBottomBarFloating = false,
            isBottomBarBlurEnabled = true,
            isLiquidGlassEnabled = false
        )

        assertEquals(false, state.floating)
        assertEquals(TopTabMaterialMode.BLUR, state.materialMode)
    }

    @Test
    fun `docked with liquid uses liquid glass when blur enabled`() {
        val state = resolveTopTabStyle(
            isBottomBarFloating = false,
            isBottomBarBlurEnabled = true,
            isLiquidGlassEnabled = true
        )

        assertEquals(false, state.floating)
        assertEquals(TopTabMaterialMode.LIQUID_GLASS, state.materialMode)
    }

    @Test
    fun `docked with liquid uses liquid glass without blur`() {
        val state = resolveTopTabStyle(
            isBottomBarFloating = false,
            isBottomBarBlurEnabled = false,
            isLiquidGlassEnabled = true
        )

        assertEquals(false, state.floating)
        assertEquals(TopTabMaterialMode.LIQUID_GLASS, state.materialMode)
    }

    @Test
    fun `reduced interaction budget keeps liquid glass home header tab material mode`() {
        assertEquals(
            TopTabMaterialMode.LIQUID_GLASS,
            resolveEffectiveHomeHeaderTabMaterialMode(
                materialMode = TopTabMaterialMode.LIQUID_GLASS,
                interactionBudget = HomeInteractionMotionBudget.REDUCED
            )
        )
        assertEquals(
            TopTabMaterialMode.BLUR,
            resolveEffectiveHomeHeaderTabMaterialMode(
                materialMode = TopTabMaterialMode.BLUR,
                interactionBudget = HomeInteractionMotionBudget.REDUCED
            )
        )
    }

    @Test
    fun `top tab liquid glass follows requested state regardless of interaction budget`() {
        assertTrue(
            resolveEffectiveTopTabLiquidGlassEnabled(
                isLiquidGlassEnabled = true,
                interactionBudget = HomeInteractionMotionBudget.FULL
            )
        )
        assertTrue(
            resolveEffectiveTopTabLiquidGlassEnabled(
                isLiquidGlassEnabled = true,
                interactionBudget = HomeInteractionMotionBudget.REDUCED
            )
        )
        assertFalse(
            resolveEffectiveTopTabLiquidGlassEnabled(
                isLiquidGlassEnabled = false,
                interactionBudget = HomeInteractionMotionBudget.FULL
            )
        )
    }

    @Test
    fun `home top tab material mode only keeps blur or plain`() {
        assertEquals(TopTabMaterialMode.BLUR, resolveHomeTopTabMaterialMode(headerBlurEnabled = true))
        assertEquals(TopTabMaterialMode.PLAIN, resolveHomeTopTabMaterialMode(headerBlurEnabled = false))
    }

    @Test
    fun `home top tab renderer routes by preset and native variant`() {
        assertEquals(
            HomeTopTabRenderer.IOS,
            resolveHomeTopTabRenderer(
                uiPreset = UiPreset.IOS,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3,
                labelMode = 2
            )
        )
        assertEquals(
            HomeTopTabRenderer.MD3,
            resolveHomeTopTabRenderer(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3,
                labelMode = 2
            )
        )
        assertEquals(
            HomeTopTabRenderer.MIUIX,
            resolveHomeTopTabRenderer(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX,
                labelMode = 2
            )
        )
        assertEquals(
            HomeTopTabRenderer.MD3,
            resolveHomeTopTabRenderer(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX,
                labelMode = 0
            )
        )
    }

    @Test
    fun `home top preset style separates ios material3 and miuix text tabs`() {
        val ios = resolveHomeTopPresetStyle(
            uiPreset = UiPreset.IOS,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3,
            labelMode = 2
        )
        val material3 = resolveHomeTopPresetStyle(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3,
            labelMode = 2
        )
        val miuix = resolveHomeTopPresetStyle(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX,
            labelMode = 2
        )

        assertNotEquals(ios.preset, material3.preset)
        assertNotEquals(material3.preset, miuix.preset)
        assertEquals(HomeTopPreset.IOS, ios.preset)
        assertEquals(HomeTopPreset.MATERIAL3, material3.preset)
        assertEquals(HomeTopPreset.MIUIX, miuix.preset)
        assertEquals(HomeTopTabRenderer.IOS, ios.renderer)
        assertEquals(HomeTopTabRenderer.MD3, material3.renderer)
        assertEquals(HomeTopTabRenderer.MIUIX, miuix.renderer)
        assertEquals(TopTabIndicatorStyle.CAPSULE, ios.indicatorStyle)
        assertEquals(TopTabIndicatorStyle.MATERIAL, material3.indicatorStyle)
        assertEquals(TopTabIndicatorStyle.CAPSULE, miuix.indicatorStyle)
    }

    @Test
    fun `miuix icon modes keep miuix dimensions while falling back to shared md3 renderer`() {
        val iconAndText = resolveHomeTopPresetStyle(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX,
            labelMode = 0
        )

        assertEquals(HomeTopPreset.MIUIX, iconAndText.preset)
        assertEquals(HomeTopTabRenderer.MD3, iconAndText.renderer)
        assertEquals(56.dp, iconAndText.tabRowHeightDocked)
        assertEquals(60.dp, iconAndText.tabRowHeightFloating)
        assertEquals(30.dp, iconAndText.md3VisualSpec.selectedCapsuleHeight)
        assertEquals(44.dp, iconAndText.actionButtonSizeDocked)
    }

    @Test
    fun `miuix top panel reserves extra content gap below category tabs`() {
        val ios = resolveHomeTopPresetStyle(
            uiPreset = UiPreset.IOS,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3,
            labelMode = 2
        )
        val material3 = resolveHomeTopPresetStyle(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3,
            labelMode = 2
        )
        val miuix = resolveHomeTopPresetStyle(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX,
            labelMode = 2
        )

        assertEquals(5.dp, ios.reservedContentBottomGap)
        assertEquals(5.dp, material3.reservedContentBottomGap)
        assertEquals(12.dp, miuix.reservedContentBottomGap)
        assertEquals(
            12.dp,
            resolveHomeTopReservedContentBottomGap(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
    }

    @Test
    fun `miuix top settings button follows action button metrics while other presets keep existing size`() {
        assertEquals(
            40.dp,
            resolveHomeTopSettingsButtonSize(
                uiPreset = UiPreset.IOS,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3
            )
        )
        assertEquals(
            40.dp,
            resolveHomeTopSettingsButtonSize(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3
            )
        )
        assertEquals(
            44.dp,
            resolveHomeTopSettingsButtonSize(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
        assertEquals(
            20.dp,
            resolveHomeTopSettingsIconSize(
                uiPreset = UiPreset.IOS,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3
            )
        )
        assertEquals(
            20.dp,
            resolveHomeTopSettingsIconSize(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3
            )
        )
        assertEquals(
            22.dp,
            resolveHomeTopSettingsIconSize(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
    }

    @Test
    fun `miuix category action trailing padding aligns with unified top settings center`() {
        val miuix = resolveHomeTopPresetStyle(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX,
            labelMode = 2
        )

        assertEquals(4.dp, resolveMiuixTopTabRowHorizontalPadding())
        assertEquals(
            5.dp,
            resolveMiuixTopTabActionTrailingPadding(miuix.unifiedPanelInnerPadding)
        )
    }

    @Test
    fun `clicking selected top tab scrolls to top while other tabs select`() {
        assertEquals(
            TopTabClickAction.SCROLL_TO_TOP,
            resolveTopTabClickAction(index = 2, selectedIndex = 2)
        )
        assertEquals(
            TopTabClickAction.SELECT_TAB,
            resolveTopTabClickAction(index = 3, selectedIndex = 2)
        )
    }

    @Test
    fun `ios top tab tuning uses compact indicator footprint`() {
        val tuning = resolveTopTabVisualTuning(UiPreset.IOS)

        assertEquals(40f, tuning.nonFloatingIndicatorHeightDp, 0.001f)
        assertEquals(20f, tuning.nonFloatingIndicatorCornerDp, 0.001f)
        assertEquals(1.18f, tuning.nonFloatingIndicatorWidthRatio, 0.001f)
        assertEquals(78f, tuning.nonFloatingIndicatorMinWidthDp, 0.001f)
        assertEquals(0f, tuning.nonFloatingIndicatorHorizontalInsetDp, 0.001f)
        assertEquals(40f, tuning.floatingIndicatorHeightDp, 0.001f)
        assertEquals(13f, tuning.tabTextSizeSp, 0.001f)
        assertEquals(17f, tuning.tabTextLineHeightSp, 0.001f)
        assertEquals(36f, tuning.tabContentMinHeightDp, 0.001f)
    }

    @Test
    fun `md3 capsule top tab tuning also uses compact shape`() {
        val tuning = resolveTopTabVisualTuning(UiPreset.MD3)

        assertEquals(40f, tuning.nonFloatingIndicatorHeightDp, 0.001f)
        assertEquals(20f, tuning.nonFloatingIndicatorCornerDp, 0.001f)
        assertEquals(40f, tuning.floatingIndicatorHeightDp, 0.001f)
    }

    @Test
    fun `ios top tab keeps icon plus text scale stable inside large capsule`() {
        assertEquals(
            1f,
            resolveTopTabContentScale(
                selectionFraction = 1f,
                showIcon = true,
                showText = true,
                uiPreset = UiPreset.IOS
            ),
            0.001f
        )
        assertEquals(
            1.03f,
            resolveTopTabContentScale(
                selectionFraction = 1f,
                showIcon = true,
                showText = false,
                uiPreset = UiPreset.IOS
            ),
            0.001f
        )
    }

    @Test
    fun `ios lightweight top tab uses bottom bar capsule indicator shape`() {
        val source = sourceText("app/src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt")
        val itemBlock = source
            .substringAfter("private fun LightweightTopTabItem(")
            .substringBefore("Box(")

        assertTrue(itemBlock.contains("HomeTopTabRenderer.IOS -> resolveSharedBottomBarCapsuleShape()"))
        assertFalse(itemBlock.contains("HomeTopTabRenderer.IOS -> AppShapes.container(ContainerLevel.Pill)"))
    }

    @Test
    fun `ios lightweight top tab capsule uses gray white while content keeps theme primary`() {
        val colorScheme = lightColorScheme(primary = Color(0xFF2D6A4F))
        val capsuleColor = resolveIosTopTabCapsuleContainerColor(
            isDarkTheme = false,
            selectionFraction = 1f
        )
        val bottomIndicatorColor = resolveBottomBarMovingIndicatorSurfaceColor(isDarkTheme = false)

        assertEquals(bottomIndicatorColor.red, capsuleColor.red, 0.001f)
        assertEquals(bottomIndicatorColor.green, capsuleColor.green, 0.001f)
        assertEquals(bottomIndicatorColor.blue, capsuleColor.blue, 0.001f)
        assertEquals(0.28f, capsuleColor.alpha, 0.002f)
        assertEquals(
            colorScheme.primary,
            resolveIosTopTabSelectedContentColor(colorScheme)
        )
        assertFalse(capsuleColor == colorScheme.primary.copy(alpha = 0.10f))
    }

    @Test
    fun `ios top tab icon modes use readable glyph sizes`() {
        assertEquals(18f, resolveTopTabIconSizeDp(labelMode = 0), 0.001f)
        assertEquals(22f, resolveTopTabIconSizeDp(labelMode = 1), 0.001f)
        assertEquals(2f, resolveTopTabIconTextSpacingDp(labelMode = 0), 0.001f)
        assertEquals(52.dp, resolveIosTopTabRowHeight(isFloatingStyle = false))
        assertEquals(52.dp, resolveIosTopTabRowHeight(isFloatingStyle = true))
        assertEquals(44.dp, resolveIosTopTabActionButtonSize(isFloatingStyle = false))
        assertEquals(22.dp, resolveIosTopTabActionIconSize(isFloatingStyle = false))
    }

    @Test
    fun `md3 top tabs keep material typography spacing`() {
        val textSize = resolveTopTabLabelTextSizeSp(labelMode = 0)
        val lineHeight = resolveTopTabLabelLineHeightSp(labelMode = 0)

        assertEquals(14f, textSize, 0.001f)
        assertEquals(18f, lineHeight, 0.001f)
        assertTrue(lineHeight >= textSize)
    }

    @Test
    fun `md3 top tabs should use compact text first underline sizing`() {
        val spec = resolveMd3TopTabVisualSpec(isFloatingStyle = false)

        assertEquals(48.dp, spec.rowHeight)
        assertEquals(2.dp, spec.selectedCapsuleHeight)
        assertEquals(1.dp, spec.selectedCapsuleCornerRadius)
        assertEquals(20.dp, spec.iconSize)
        assertEquals(15.sp, spec.labelTextSize)
        assertEquals(20.sp, spec.labelLineHeight)
        assertEquals(0.dp, spec.iconLabelSpacing)
        assertEquals(12.dp, spec.itemHorizontalPadding)
        assertEquals(0.dp, spec.selectedCapsuleShadowElevation)
        assertEquals(0.dp, spec.selectedCapsuleTonalElevation)
    }

    @Test
    fun `md3 icon plus text top tabs reserve enough height`() {
        val spec = resolveMd3TopTabVisualSpec(
            isFloatingStyle = false,
            labelMode = 0
        )

        assertEquals(60.dp, spec.rowHeight)
        assertEquals(8.dp, spec.itemHorizontalPadding)
        assertEquals(3.dp, spec.iconLabelSpacing)
        assertEquals(20.dp, spec.iconSize)
        assertEquals(14.sp, spec.labelTextSize)
        assertTrue(spec.labelLineHeight >= spec.labelTextSize)
    }

    @Test
    fun `android native miuix top tabs should promote capsule selection styling`() {
        val spec = resolveMd3TopTabVisualSpec(
            isFloatingStyle = false,
            androidNativeVariant = AndroidNativeVariant.MIUIX
        )

        assertEquals(48.dp, spec.rowHeight)
        assertEquals(30.dp, spec.selectedCapsuleHeight)
        assertEquals(15.dp, spec.selectedCapsuleCornerRadius)
        assertEquals(12.dp, spec.itemHorizontalPadding)
        assertEquals(2.dp, spec.iconLabelSpacing)
        assertEquals(15.sp, spec.labelTextSize)
    }

    @Test
    fun `android native miuix top tab content is inset from row bottom`() {
        val rowHeight = 48.dp

        assertEquals(2.dp, resolveMiuixTopTabRowVerticalInset())
        assertEquals(44.dp, resolveMiuixTopTabContentHeight(rowHeight))
    }

    @Test
    fun `android native miuix text tabs should use native miuix row while icon modes stay shared`() {
        assertTrue(
            shouldUseNativeMiuixTopTabRow(
                androidNativeVariant = AndroidNativeVariant.MIUIX,
                labelMode = 2
            )
        )
        assertEquals(
            false,
            shouldUseNativeMiuixTopTabRow(
                androidNativeVariant = AndroidNativeVariant.MIUIX,
                labelMode = 0
            )
        )
        assertFalse(
            shouldUseNativeMiuixTopTabRow(
                androidNativeVariant = AndroidNativeVariant.MIUIX,
                labelMode = 1
            )
        )
    }

    @Test
    fun `android native miuix top tab chrome should avoid large primary color fills`() {
        val source = sourceText("src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt")
        val miuixTabRowSource = source.substringAfter("private fun MiuixCategoryTabRow(")

        assertTrue(
            "MiuiX 分类条必须通过颜色策略收敛普通态背景",
            miuixTabRowSource.contains("resolveMiuixTopTabRowColors(")
        )
        assertTrue(
            "MiuiX 分区按钮必须通过颜色策略收敛普通态背景",
            miuixTabRowSource.contains("resolveMiuixTopTabActionColors(")
        )
        assertFalse(
            "MiuiX 分类条普通容器不应使用 primary 大面积铺色",
            miuixTabRowSource.contains("backgroundColor = MiuixTheme.colorScheme.primary.copy")
        )
        assertFalse(
            "MiuiX 分区按钮普通态不应使用 primary 大面积铺色",
            miuixTabRowSource.contains("color = MiuixTheme.colorScheme.primary.copy")
        )
    }

    @Test
    fun `android native miuix top tabs keep native contour indicator driver`() {
        val source = sourceText("src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt")
        val miuixSelectionSource = source
            .substringAfter("private fun MiuixCategoryTabRow(")
            .substringBefore("val topTabSpec =")

        assertTrue(
            "MiuiX 分类条必须继续使用原生轮廓 TabRow",
            source.substringAfter("private fun MiuixCategoryTabRow(")
                .contains("MiuixTabRowWithContour(")
        )
        assertFalse(
            "MiuiX 原生轮廓指示器不应复用 MD3 的 pager 指示器位置驱动",
            miuixSelectionSource.contains("resolveTopTabIndicatorRenderPosition(")
        )
        assertTrue(
            "MiuiX 可见槽位应由已选中分类驱动，避免滑动中退化成 MD3 指示器语义",
            miuixSelectionSource.contains("selectedIndex = selectedIndex")
        )
    }

    @Test
    fun `android native miuix top tabs skip outer chrome surface`() {
        assertFalse(
            shouldDrawHomeTopTabOuterChromeSurface(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX,
                materialMode = TopTabMaterialMode.LIQUID_GLASS
            )
        )
        assertFalse(
            shouldDrawHomeTopTabOuterChromeSurface(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX,
                materialMode = TopTabMaterialMode.BLUR
            )
        )
        assertFalse(
            shouldDrawHomeTopTabOuterChromeSurface(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX,
                materialMode = TopTabMaterialMode.PLAIN
            )
        )
        assertTrue(
            shouldDrawHomeTopTabOuterChromeSurface(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3,
                materialMode = TopTabMaterialMode.LIQUID_GLASS
            )
        )
        assertTrue(
            shouldDrawHomeTopTabOuterChromeSurface(
                uiPreset = UiPreset.IOS,
                androidNativeVariant = AndroidNativeVariant.MIUIX,
                materialMode = TopTabMaterialMode.LIQUID_GLASS
            )
        )
    }

    @Test
    fun `md3 selected top tab should reuse material primary emphasis`() {
        val colorScheme = lightColorScheme(
            surface = Color.White,
            primary = Color(0xFF2D6A4F),
            secondaryContainer = Color(0xFFDCEFD8),
            onSecondaryContainer = Color(0xFF1A1C18),
            onSurface = Color(0xFF1B1C1F),
            onSurfaceVariant = Color(0xFF6A5E61)
        )

        assertEquals(colorScheme.primary, resolveMd3TopTabSelectedContainerColor(colorScheme))
        assertEquals(colorScheme.primary, resolveMd3TopTabSelectedIconColor(colorScheme))
        assertEquals(colorScheme.primary, resolveMd3TopTabSelectedLabelColor(colorScheme))
        assertEquals(colorScheme.onSurfaceVariant, resolveMd3TopTabUnselectedIconColor(colorScheme))
        assertEquals(colorScheme.onSurfaceVariant, resolveMd3TopTabUnselectedLabelColor(colorScheme))
    }

    @Test
    fun `android native miuix top tabs should use miuix secondary container emphasis`() {
        val colorScheme = lightColorScheme(
            primary = Color(0xFF2D6A4F),
            surfaceContainerHigh = Color(0xFFF4ECE1),
            secondaryContainer = Color(0xFFDCEFD8),
            onSecondaryContainer = Color(0xFF1A1C18),
            onSurface = Color(0xFF1E1B16),
            onSurfaceVariant = Color(0xFF6A5E61)
        )

        assertEquals(
            colorScheme.secondaryContainer,
            resolveMd3TopTabSelectedContainerColor(
                colorScheme = colorScheme,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
        assertEquals(
            colorScheme.onSecondaryContainer,
            resolveMd3TopTabSelectedIconColor(
                colorScheme = colorScheme,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
        assertEquals(
            colorScheme.onSecondaryContainer,
            resolveMd3TopTabSelectedLabelColor(
                colorScheme = colorScheme,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
    }

    @Test
    fun `android native miuix top tab row colors should stay neutral outside selection`() {
        val colorScheme = lightColorScheme(
            primary = Color(0xFF1E88E5),
            surfaceContainer = Color(0xFFF4F5F8),
            secondaryContainer = Color(0xFFE3EAF8),
            onSecondaryContainer = Color(0xFF1B2230),
            onSurfaceVariant = Color(0xFF5F6368)
        )

        val colors = resolveMiuixTopTabRowColors(
            surfaceContainer = colorScheme.surfaceContainer,
            onSurfaceVariant = colorScheme.onSurfaceVariant,
            secondaryContainer = colorScheme.secondaryContainer,
            onSecondaryContainer = colorScheme.onSecondaryContainer
        )

        assertEquals(colorScheme.surfaceContainer.copy(alpha = 0.72f), colors.backgroundColor)
        assertEquals(colorScheme.secondaryContainer.copy(alpha = 0.58f), colors.selectedBackgroundColor)
        assertEquals(colorScheme.onSurfaceVariant, colors.contentColor)
        assertEquals(colorScheme.onSecondaryContainer, colors.selectedContentColor)
        assertFalse(colors.backgroundColor == colorScheme.primary.copy(alpha = 0.10f))
    }

    @Test
    fun `android native miuix top tab action colors should stay neutral`() {
        val colorScheme = lightColorScheme(
            primary = Color(0xFF1E88E5),
            surfaceContainer = Color(0xFFF4F5F8),
            outlineVariant = Color(0xFFC9CDD6),
            onSurfaceVariant = Color(0xFF5F6368)
        )

        val colors = resolveMiuixTopTabActionColors(
            surfaceContainer = colorScheme.surfaceContainer,
            outlineVariant = colorScheme.outlineVariant,
            contentColor = colorScheme.onSurfaceVariant
        )

        assertEquals(colorScheme.surfaceContainer.copy(alpha = 0.74f), colors.containerColor)
        assertEquals(colorScheme.outlineVariant.copy(alpha = 0.42f), colors.borderColor)
        assertEquals(colorScheme.onSurfaceVariant, colors.contentColor)
        assertFalse(colors.containerColor == colorScheme.primary.copy(alpha = 0.10f))
    }

    @Test
    fun `md3 preset uses material tab indicator style`() {
        assertEquals(
            TopTabIndicatorStyle.MATERIAL,
            resolveTopTabIndicatorStyle(UiPreset.MD3)
        )
        assertEquals(
            TopTabIndicatorStyle.CAPSULE,
            resolveTopTabIndicatorStyle(UiPreset.IOS)
        )
    }

    @Test
    fun `md3 top tabs always use material indicator after removing top liquid glass`() {
        assertTrue(
            shouldUseMd3TopTabMaterialIndicator(
                uiPreset = UiPreset.MD3,
                liquidGlassEnabled = true
            )
        )
        assertTrue(
            shouldUseMd3TopTabMaterialIndicator(
                uiPreset = UiPreset.MD3,
                liquidGlassEnabled = false
            )
        )
    }

    @Test
    fun `md3 and miuix use screenshot underline when liquid glass is off`() {
        assertTrue(
            shouldUsePlainMd3TopTabUnderline(
                uiPreset = UiPreset.MD3,
                liquidGlassEnabled = false
            )
        )
        assertFalse(
            shouldUsePlainMd3TopTabUnderline(
                uiPreset = UiPreset.MD3,
                liquidGlassEnabled = true
            )
        )
        assertFalse(
            shouldUsePlainMd3TopTabUnderline(
                uiPreset = UiPreset.IOS,
                liquidGlassEnabled = false
            )
        )
    }

    @Test
    fun `md3 top tabs remove outer dock when liquid glass is off`() {
        assertFalse(
            shouldDrawHomeTopTabOuterChromeSurface(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3,
                materialMode = TopTabMaterialMode.BLUR
            )
        )
        assertFalse(
            shouldDrawHomeTopTabOuterChromeSurface(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3,
                materialMode = TopTabMaterialMode.PLAIN
            )
        )
        assertTrue(
            shouldDrawHomeTopTabOuterChromeSurface(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3,
                materialMode = TopTabMaterialMode.LIQUID_GLASS
            )
        )
    }

    @Test
    fun `md3 top tabs use underline row semantics and tighter action shape`() {
        assertEquals(
            "UNDERLINE_FIXED",
            resolveMd3TopTabRowVariant().name
        )
        assertEquals(16.dp, resolveMd3TopTabActionButtonCorner(isFloatingStyle = true))
        assertEquals(12.dp, resolveMd3TopTabActionButtonCorner(isFloatingStyle = false))
        assertEquals(48.dp, resolveMd3TopTabActionButtonSize(isFloatingStyle = true))
        assertEquals(42.dp, resolveMd3TopTabActionButtonSize(isFloatingStyle = false))
        assertEquals(24.dp, resolveMd3TopTabActionIconSize(isFloatingStyle = true))
        assertEquals(22.dp, resolveMd3TopTabActionIconSize(isFloatingStyle = false))
        assertEquals(4.dp, resolveMd3TopTabActionContentBottomPadding())
        assertEquals(4f, resolveMd3TopTabVerticalLiftDp(), 0.001f)
        assertEquals(8.dp, resolveMd3TopTabIndicatorBottomPadding())
    }

    @Test
    fun `skin top tabs use compact readable underline layout`() {
        assertEquals(46.dp, resolveHomeSkinTopTabRowHeight())
        assertTrue(resolveHomeSkinTopTabRowHeight() >= 44.dp)
        assertTrue(resolveHomeSkinTopTabRowHeight() <= 48.dp)
        assertEquals(44.dp, resolveHomeSkinTopTabActionButtonSize())
        assertEquals(24.dp, resolveHomeSkinTopTabActionIconSize())
        assertEquals(4.dp, resolveHomeSkinTopTabIndicatorBottomPadding())
        assertEquals(32.dp, resolveTopTabSkinStickerIconSize(showText = true))
        assertEquals(36.dp, resolveTopTabSkinStickerIconSize(showText = false))
        assertEquals(32.dp, resolveTopTabSkinPartitionIconSize())
        assertEquals(28.dp, resolveTopTabSkinStickerIndicatorWidth())
        assertEquals(
            64.dp,
            resolveTopTabSkinStickerRowHeight(
                baseRowHeight = 56.dp,
                hasSkinStickerIcons = true,
                showIcon = true,
                showText = true
            )
        )
        assertEquals(
            52.dp,
            resolveTopTabSkinStickerRowHeight(
                baseRowHeight = 52.dp,
                hasSkinStickerIcons = false,
                showIcon = true,
                showText = true
            )
        )
        assertEquals(2.dp, resolveTopTabSkinStickerItemVerticalPadding(showText = true))
    }

    @Test
    fun `skin top tab colors stay readable on light and dark skin backgrounds`() {
        val darkBackgroundContent = resolveHomeSkinTopTabContentColor(Color(0xFF2E2A1E))
        val midDarkBackgroundContent = resolveHomeSkinTopTabContentColor(Color(0xFF778675))
        val lightBackgroundContent = resolveHomeSkinTopTabContentColor(Color(0xFFE4F6FF))

        assertEquals(Color.White.copy(alpha = 0.98f), darkBackgroundContent)
        assertEquals(Color.White.copy(alpha = 0.98f), midDarkBackgroundContent)
        assertEquals(Color(0xFF111820).copy(alpha = 0.96f), lightBackgroundContent)
        assertEquals(
            Color.White.copy(alpha = 0.84f),
            resolveHomeSkinTopTabUnselectedContentColor(darkBackgroundContent)
        )
        assertEquals(
            Color(0xFF111820).copy(alpha = 0.78f),
            resolveHomeSkinTopTabUnselectedContentColor(lightBackgroundContent)
        )
        assertEquals(darkBackgroundContent, resolveHomeSkinTopTabIndicatorColor(darkBackgroundContent))
        assertEquals(lightBackgroundContent, resolveHomeSkinTopTabIndicatorColor(lightBackgroundContent))
    }

    @Test
    fun `skin top tab image backed dark mode uses light content for all presets`() {
        val lightFallbackTint = Color(0xFFDFF5FF)
        val presets = listOf(
            UiPreset.IOS to AndroidNativeVariant.MATERIAL3,
            UiPreset.MD3 to AndroidNativeVariant.MATERIAL3,
            UiPreset.MD3 to AndroidNativeVariant.MIUIX
        )

        presets.forEach { (uiPreset, androidNativeVariant) ->
            resolveHomeTopPresetStyle(
                uiPreset = uiPreset,
                androidNativeVariant = androidNativeVariant,
                labelMode = 0
            )
            val contentColor = resolveHomeSkinTopTabContentColor(
                topAtmosphereTint = lightFallbackTint,
                hasTopAtmosphereImage = true,
                darkTheme = true
            )

            assertEquals(
                "$uiPreset/$androidNativeVariant should stay readable over dark image-backed skin",
                Color.White.copy(alpha = 0.98f),
                contentColor
            )
        }
    }

    @Test
    fun `skin decoration keeps host top tab readability strategy`() {
        assertFalse(shouldUseHomeSkinPlainTopTabs(null))
        assertFalse(
            shouldUseHomeSkinPlainTopTabs(
                HomeUiSkinDecoration(
                    skinId = "test",
                    topAtmosphereTint = Color(0xFFE4F6FF),
                    searchCapsuleTint = Color.White
                )
            )
        )
    }

    @Test
    fun `skin top tabs render sticker image before host vector icon fallback`() {
        val source = sourceText("src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt")
        val rowCallSource = source
            .substringAfter("LightweightHomeTopTabs(")
            .substringBefore("private fun MiuixCategoryTabRow(")
        val itemSource = source
            .substringAfter("private fun LightweightTopTabItem(")
            .substringBefore("@OptIn(ExperimentalMaterial3Api::class)")

        assertTrue(rowCallSource.contains("topTabSkinIconPaths = topTabSkinIconPaths"))
        assertTrue(rowCallSource.contains("partitionSkinIconPath = partitionSkinIconPath"))
        assertTrue(itemSource.contains("skinIconPath"))
        assertTrue(itemSource.contains("AsyncImage("))
        assertTrue(itemSource.contains("model = File(skinIconPath)"))
        assertTrue(itemSource.contains("resolveTopTabSkinStickerIconSize(showText = showText)"))
        assertTrue(rowCallSource.contains("resolveTopTabSkinPartitionIconSize()"))
        assertTrue(rowCallSource.contains("resolveTopTabSkinStickerRowHeight("))
        assertTrue(rowCallSource.contains("if (effectiveRenderer == HomeTopTabRenderer.MD3 && !hasSkinStickerIcons)"))
        assertTrue(itemSource.contains("resolveTopTabSkinStickerItemVerticalPadding(showText = showText)"))
        assertTrue(itemSource.contains("resolveTopTabSkinStickerIndicatorWidth()"))
        assertTrue(itemSource.contains("alpha(selectionFraction)"))
        assertTrue(itemSource.indexOf("AsyncImage(") < itemSource.indexOf("imageVector = icon"))
        assertTrue(itemSource.contains("else {"))
        assertTrue(itemSource.contains("resolveTopTabCategoryIcon(categoryKey, uiPreset)"))
    }

    @Test
    fun `skin top tab stickers keep ios md3 and miuix on shared item indicator path`() {
        val source = sourceText("src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt")
        val categoryTabRowSource = source
            .substringAfter("fun CategoryTabRow(")
            .substringBefore("@Composable\nprivate fun MiuixCategoryTabRow(")

        assertTrue(categoryTabRowSource.contains("val hasSkinStickerIcons = topTabSkinIconPaths.isNotEmpty() || !partitionSkinIconPath.isNullOrBlank()"))
        assertTrue(categoryTabRowSource.contains("if (showPartitionAction && !hasSkinStickerIcons && !skinPlainStyle && presetStyle.renderer == HomeTopTabRenderer.MIUIX)"))
        assertTrue(categoryTabRowSource.contains("topTabSkinIconPaths = topTabSkinIconPaths"))
        assertTrue(categoryTabRowSource.contains("partitionSkinIconPath = partitionSkinIconPath"))
    }

    @Test
    fun `android native miuix top tabs should slightly enlarge action button chrome`() {
        assertEquals(
            18.dp,
            resolveMd3TopTabActionButtonCorner(
                isFloatingStyle = true,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
        assertEquals(
            14.dp,
            resolveMd3TopTabActionButtonCorner(
                isFloatingStyle = false,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
        assertEquals(
            50.dp,
            resolveMd3TopTabActionButtonSize(
                isFloatingStyle = true,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
        assertEquals(
            44.dp,
            resolveMd3TopTabActionButtonSize(
                isFloatingStyle = false,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
    }

    private fun sourceText(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
