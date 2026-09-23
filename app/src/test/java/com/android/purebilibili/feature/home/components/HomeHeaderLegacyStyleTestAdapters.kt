package com.android.purebilibili.feature.home.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.resolveUiStyle
import com.android.purebilibili.core.ui.AppTopChromePolicy
import com.android.purebilibili.core.ui.resolveAppTopChromePolicy
import com.android.purebilibili.feature.home.HomeGlassResolvedColors

/** Keeps legacy style fixtures readable while production helpers consume neutral capabilities. */
// 兼容桥接：旧 pair 输入经迁移表落到两值风格，批 5 清理桥接后随适配器收敛。
private fun legacyHomeChromePolicy(
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
): AppTopChromePolicy = resolveAppTopChromePolicy(resolveUiStyle(uiPreset, androidNativeVariant))

private fun UiPreset.usesNativeContainerTreatment(): Boolean = this == UiPreset.MD3

private fun usesTonalContainerTreatment(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant,
): Boolean = uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX

internal fun resolveHomeTopPresetStyle(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant,
    labelMode: Int,
): HomeTopPresetStyle = resolveHomeTopPresetStyle(
    chromePolicy = legacyHomeChromePolicy(uiPreset, androidNativeVariant),
    labelMode = labelMode,
)

internal fun resolveHomeTopLinkedBottomBarAppearance(
    homeSettings: HomeSettings?,
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant,
): HomeTopLinkedBottomBarAppearance = resolveHomeTopLinkedBottomBarAppearance(
    homeSettings = homeSettings,
    presentation = legacyHomeChromePolicy(uiPreset, androidNativeVariant).tabPresentation,
)

internal fun resolveHomeTopChromeLiquidGlassEnabled(
    homeSettings: HomeSettings?,
    uiPreset: UiPreset,
): Boolean = resolveHomeTopChromeLiquidGlassEnabled(
    homeSettings = homeSettings,
)

internal fun resolveHomeTopTabIndicatorLiquidGlassEnabled(
    homeSettings: HomeSettings?,
    uiPreset: UiPreset,
): Boolean = resolveHomeTopTabIndicatorLiquidGlassEnabled(
    homeSettings = homeSettings,
)

internal fun resolveHomeTopSearchLiquidGlassEnabled(
    homeSettings: HomeSettings?,
    uiPreset: UiPreset,
): Boolean = resolveHomeTopSearchLiquidGlassEnabled(
    homeSettings = homeSettings,
)

@Suppress("UNUSED_PARAMETER")
internal fun resolveHomeTopChromeMaterialMode(
    isHeaderBlurEnabled: Boolean,
    isBottomBarBlurEnabled: Boolean,
    isLiquidGlassEnabled: Boolean,
    androidNativeVariant: AndroidNativeVariant,
): TopTabMaterialMode = resolveHomeTopChromeMaterialMode(
    isHeaderBlurEnabled = isHeaderBlurEnabled,
    isBottomBarBlurEnabled = isBottomBarBlurEnabled,
    isLiquidGlassEnabled = isLiquidGlassEnabled,
)

internal fun shouldDrawHomeTopSearchLegacyHighlight(
    uiPreset: UiPreset,
    useUnifiedTopPanel: Boolean,
    renderMode: HomeTopChromeRenderMode,
    refractionOverlayAlpha: Float,
): Boolean = shouldDrawHomeTopSearchLegacyHighlight(
    presentation = legacyHomeChromePolicy(uiPreset).tabPresentation,
    useUnifiedTopPanel = useUnifiedTopPanel,
    renderMode = renderMode,
    refractionOverlayAlpha = refractionOverlayAlpha,
)

internal fun resolveHomeTopSearchBarHeight(
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
): Dp = resolveHomeTopSearchBarHeight(legacyHomeChromePolicy(uiPreset, androidNativeVariant))

internal fun resolveHomeTopSearchRevealDeadZone(
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
): Dp = resolveHomeTopSearchRevealDeadZone(legacyHomeChromePolicy(uiPreset, androidNativeVariant))

internal fun resolveHomeTopTabRowHeight(
    isTabFloating: Boolean,
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
    labelMode: Int = com.android.purebilibili.core.store.SettingsManager.TopTabLabelMode.TEXT_ONLY,
): Dp = resolveHomeTopTabRowHeight(
    isTabFloating = isTabFloating,
    chromePolicy = legacyHomeChromePolicy(uiPreset, androidNativeVariant),
    labelMode = labelMode,
)

internal fun resolveHomeTopSearchRowHorizontalPadding(
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
): Dp = resolveHomeTopSearchRowHorizontalPadding(legacyHomeChromePolicy(uiPreset, androidNativeVariant))

internal fun resolveHomeTopSearchPillHeight(
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
): Dp = resolveHomeTopSearchPillHeight(legacyHomeChromePolicy(uiPreset, androidNativeVariant))

internal fun resolveHomeTopSearchContentHorizontalPadding(
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
): Dp = resolveHomeTopSearchContentHorizontalPadding(legacyHomeChromePolicy(uiPreset, androidNativeVariant))

internal fun resolveHomeTopSearchIconTextGap(
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
): Dp = resolveHomeTopSearchIconTextGap(legacyHomeChromePolicy(uiPreset, androidNativeVariant))

internal fun resolveHomeTopSearchContainerShape(
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
): Shape = resolveHomeTopSearchContainerShape(legacyHomeChromePolicy(uiPreset, androidNativeVariant))

internal fun resolveHomeTopEdgeButtonShape(
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
): Shape = resolveHomeTopEdgeButtonShape(legacyHomeChromePolicy(uiPreset, androidNativeVariant))

internal fun resolveHomeTopSettingsButtonSize(
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
): Dp = resolveHomeTopSettingsButtonSize(legacyHomeChromePolicy(uiPreset, androidNativeVariant))

internal fun resolveHomeTopSettingsIconSize(
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
): Dp = resolveHomeTopSettingsIconSize(legacyHomeChromePolicy(uiPreset, androidNativeVariant))

internal fun resolveHomeTopEdgeControlGap(
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
): Dp = resolveHomeTopEdgeControlGap(legacyHomeChromePolicy(uiPreset, androidNativeVariant))

internal fun shouldUseUnifiedHomeTopPanel(uiPreset: UiPreset = UiPreset.IOS): Boolean =
    shouldUseUnifiedHomeTopPanel(legacyHomeChromePolicy(uiPreset))

internal fun shouldUseDetachedHomeTopTabDock(
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
): Boolean = shouldUseDetachedHomeTopTabDock(
    legacyHomeChromePolicy(uiPreset, androidNativeVariant).tabPresentation,
)

internal fun resolveHomeTopUnifiedPanelHorizontalPadding(uiPreset: UiPreset = UiPreset.IOS): Dp =
    resolveHomeTopUnifiedPanelHorizontalPadding(legacyHomeChromePolicy(uiPreset))

internal fun resolveHomeTopUnifiedPanelInnerPadding(
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
    collapsedIntoStatusBar: Boolean = false,
): Dp = resolveHomeTopUnifiedPanelInnerPadding(
    chromePolicy = legacyHomeChromePolicy(uiPreset, androidNativeVariant),
    collapsedIntoStatusBar = collapsedIntoStatusBar,
)

internal fun resolveHomeTopUnifiedPanelCornerRadius(
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
    collapsedIntoStatusBar: Boolean = false,
): Dp = resolveHomeTopUnifiedPanelCornerRadius(
    chromePolicy = legacyHomeChromePolicy(uiPreset, androidNativeVariant),
    collapsedIntoStatusBar = collapsedIntoStatusBar,
)

internal fun resolveHomeTopReservedContentBottomGap(
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
): Dp = resolveHomeTopReservedContentBottomGap(legacyHomeChromePolicy(uiPreset, androidNativeVariant))

internal fun resolveHomeTopEmbeddedTabHorizontalPadding(uiPreset: UiPreset = UiPreset.IOS): Dp =
    resolveHomeTopEmbeddedTabHorizontalPadding(legacyHomeChromePolicy(uiPreset))

internal fun resolveHomeTopTabHorizontalPadding(
    isTabFloating: Boolean,
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
): Dp = resolveHomeTopTabHorizontalPadding(
    isTabFloating = isTabFloating,
    chromePolicy = legacyHomeChromePolicy(uiPreset, androidNativeVariant),
)

internal fun resolveHomeTopSearchToTabsSpacing(
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
): Dp = resolveHomeTopSearchToTabsSpacing(legacyHomeChromePolicy(uiPreset, androidNativeVariant))

internal fun resolveHomeTopTabsToContentSpacing(
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
): Dp = resolveHomeTopTabsToContentSpacing(legacyHomeChromePolicy(uiPreset, androidNativeVariant))

internal fun resolveHomeTopSearchCollapseExtraSpacing(
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
): Dp = resolveHomeTopSearchCollapseExtraSpacing(legacyHomeChromePolicy(uiPreset, androidNativeVariant))

internal fun resolveHomeTopSearchCollapseDistance(
    searchBarHeight: Dp,
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
): Dp = resolveHomeTopSearchCollapseDistance(
    searchBarHeight = searchBarHeight,
    chromePolicy = legacyHomeChromePolicy(uiPreset, androidNativeVariant),
)

internal fun shouldUseIntegratedCollapsedHomeTopBar(
    searchRevealFraction: Float,
    uiPreset: UiPreset = UiPreset.IOS,
): Boolean = shouldUseIntegratedCollapsedHomeTopBar(
    searchRevealFraction = searchRevealFraction,
    presentation = legacyHomeChromePolicy(uiPreset).tabPresentation,
)

internal fun resolveHomeTopContinuousSlabOverlap(
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
): Dp = resolveHomeTopContinuousSlabOverlap(legacyHomeChromePolicy(uiPreset, androidNativeVariant))

@Suppress("UNUSED_PARAMETER")
internal fun resolveHomeTopContinuousSlabShape(uiPreset: UiPreset): Shape =
    resolveHomeTopContinuousSlabShape()

internal fun resolveHomeTopReservedListPadding(
    statusBarHeight: Dp,
    searchBarHeight: Dp,
    tabRowHeight: Dp,
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
    isTabFloating: Boolean = false,
): Dp = resolveHomeTopReservedListPadding(
    statusBarHeight = statusBarHeight,
    searchBarHeight = searchBarHeight,
    tabRowHeight = tabRowHeight,
    chromePolicy = legacyHomeChromePolicy(uiPreset, androidNativeVariant),
    isTabFloating = isTabFloating,
)

@Suppress("UNUSED_PARAMETER")
internal fun resolveHomeTopContinuousSlabRenderMode(
    renderMode: HomeTopChromeRenderMode,
    uiPreset: UiPreset,
): HomeTopChromeRenderMode = resolveHomeTopContinuousSlabRenderMode(renderMode)

@Suppress("UNUSED_PARAMETER")
internal fun resolveHomeTopContinuousSlabHeight(
    statusBarHeight: Dp,
    searchBarHeight: Dp,
    tabRowHeight: Dp,
    renderMode: HomeTopChromeRenderMode,
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
    hasVisibleTopContent: Boolean = true,
): Dp = resolveHomeTopContinuousSlabHeight(
    statusBarHeight = statusBarHeight,
    searchBarHeight = searchBarHeight,
    tabRowHeight = tabRowHeight,
    renderMode = renderMode,
    hasVisibleTopContent = hasVisibleTopContent,
)

internal fun resolveHomeTopContinuousSlabSurfaceColor(
    baseColor: Color,
    blurAlpha: Float,
    uiPreset: UiPreset = UiPreset.IOS,
    renderMode: HomeTopChromeRenderMode,
): Color = resolveHomeTopContinuousSlabSurfaceColor(
    baseColor = baseColor,
    blurAlpha = blurAlpha,
    usesNativeContainerTreatment = uiPreset.usesNativeContainerTreatment(),
    renderMode = renderMode,
)

internal fun resolveHomeTopPanelChromeRenderMode(
    renderMode: HomeTopChromeRenderMode,
    uiPreset: UiPreset = UiPreset.IOS,
    useUnifiedPanel: Boolean = false,
): HomeTopChromeRenderMode = resolveHomeTopPanelChromeRenderMode(
    renderMode = renderMode,
    usesNativeContainerTreatment = uiPreset.usesNativeContainerTreatment(),
    useUnifiedPanel = useUnifiedPanel,
)

@Suppress("UNUSED_PARAMETER")
internal fun resolveHomeTopSearchChromeRenderMode(
    renderMode: HomeTopChromeRenderMode,
    uiPreset: UiPreset = UiPreset.IOS,
    useUnifiedPanel: Boolean = false,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
): HomeTopChromeRenderMode = resolveHomeTopSearchChromeRenderMode(
    renderMode = renderMode,
    useUnifiedPanel = useUnifiedPanel,
    usesNativeContainerTreatment = uiPreset.usesNativeContainerTreatment(),
)

internal fun resolveHomeTopUnifiedTabChromeRenderMode(
    localTabChromeRenderMode: HomeTopChromeRenderMode,
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
    useUnifiedLiquidChrome: Boolean,
): HomeTopChromeRenderMode = resolveHomeTopUnifiedTabChromeRenderMode(
    localTabChromeRenderMode = localTabChromeRenderMode,
    usesTonalContainerTreatment = usesTonalContainerTreatment(uiPreset, androidNativeVariant),
    useUnifiedLiquidChrome = useUnifiedLiquidChrome,
)

internal fun resolveHomeTopUnifiedLocalTabChromeRenderMode(
    renderMode: HomeTopChromeRenderMode,
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
): HomeTopChromeRenderMode = resolveHomeTopUnifiedLocalTabChromeRenderMode(
    renderMode = renderMode,
    usesNativeContainerTreatment = uiPreset.usesNativeContainerTreatment(),
    usesTonalContainerTreatment = usesTonalContainerTreatment(uiPreset, androidNativeVariant),
)

internal fun resolveHomeTopUnifiedTabSurfaceColor(
    tabContainerColor: Color,
    tabOverlayAlpha: Float,
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
    useUnifiedLiquidChrome: Boolean,
    tabChromeRenderMode: HomeTopChromeRenderMode = HomeTopChromeRenderMode.PLAIN,
): Color = resolveHomeTopUnifiedTabSurfaceColor(
    tabContainerColor = tabContainerColor,
    tabOverlayAlpha = tabOverlayAlpha,
    usesTonalContainerTreatment = usesTonalContainerTreatment(uiPreset, androidNativeVariant),
    useUnifiedLiquidChrome = useUnifiedLiquidChrome,
    tabChromeRenderMode = tabChromeRenderMode,
)

internal fun resolveHomeTopLocalChromeRenderMode(
    renderMode: HomeTopChromeRenderMode,
    uiPreset: UiPreset = UiPreset.IOS,
): HomeTopChromeRenderMode = resolveHomeTopLocalChromeRenderMode(
    renderMode = renderMode,
    usesNativeContainerTreatment = uiPreset.usesNativeContainerTreatment(),
)

internal fun resolveHomeTopContainerColors(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
    emphasized: Boolean,
    blurEnabled: Boolean,
    fallbackColors: HomeGlassResolvedColors,
    surfaceContainerColor: Color,
    surfaceContainerHighColor: Color,
    outlineVariantColor: Color,
): HomeGlassResolvedColors = resolveHomeTopContainerColors(
    usesNativeContainerTreatment = uiPreset.usesNativeContainerTreatment(),
    usesTonalContainerTreatment = usesTonalContainerTreatment(uiPreset, androidNativeVariant),
    emphasized = emphasized,
    blurEnabled = blurEnabled,
    fallbackColors = fallbackColors,
    surfaceContainerColor = surfaceContainerColor,
    surfaceContainerHighColor = surfaceContainerHighColor,
    outlineVariantColor = outlineVariantColor,
)

internal fun shouldShowUnifiedHomeTopPanelDivider(
    uiPreset: UiPreset = UiPreset.IOS,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
): Boolean = shouldShowUnifiedHomeTopPanelDivider(legacyHomeChromePolicy(uiPreset, androidNativeVariant))
