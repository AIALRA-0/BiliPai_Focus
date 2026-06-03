package com.android.purebilibili.feature.home.components

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.ui.resolveCompactCapsuleChromeSpec
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset

enum class TopTabMaterialMode {
    PLAIN,
    BLUR,
    LIQUID_GLASS
}

enum class HomeTopTabRenderer {
    IOS,
    MD3,
    MIUIX
}

enum class HomeTopPreset {
    IOS,
    MATERIAL3,
    MIUIX
}

enum class TopTabClickAction {
    SELECT_TAB,
    SCROLL_TO_TOP
}

data class HomeTopPresetStyle(
    val preset: HomeTopPreset,
    val renderer: HomeTopTabRenderer,
    val indicatorStyle: TopTabIndicatorStyle,
    val search: HomeTopSearchStyle,
    val panel: HomeTopPanelStyle,
    val spacing: HomeTopSpacingStyle,
    val tabs: HomeTopTabsStyle,
    val actions: HomeTopActionStyle
) {
    val useUnifiedPanel: Boolean get() = panel.useUnified
    val showUnifiedPanelDivider: Boolean get() = panel.showDivider
    val searchBarHeight: Dp get() = search.barHeight
    val searchRevealDeadZone: Dp get() = search.revealDeadZone
    val searchRowHorizontalPadding: Dp get() = search.rowHorizontalPadding
    val searchPillHeight: Dp get() = search.pillHeight
    val searchContentHorizontalPadding: Dp get() = search.content.horizontalPadding
    val searchIconTextGap: Dp get() = search.content.iconTextGap
    val edgeControlGap: Dp get() = spacing.edgeControlGap
    val unifiedPanelHorizontalPadding: Dp get() = panel.horizontalPadding
    val unifiedPanelInnerPadding: Dp get() = panel.innerPadding
    val unifiedPanelCornerRadius: Dp get() = panel.cornerRadius
    val reservedContentBottomGap: Dp get() = panel.reservedContentBottomGap
    val embeddedTabHorizontalPadding: Dp get() = spacing.embeddedTabHorizontalPadding
    val tabHorizontalPaddingDocked: Dp get() = tabs.horizontalPadding.docked
    val tabHorizontalPaddingFloating: Dp get() = tabs.horizontalPadding.floating
    val searchToTabsSpacing: Dp get() = spacing.searchToTabs
    val searchCollapseExtraSpacing: Dp get() = spacing.searchCollapseExtra
    val continuousSlabOverlap: Dp get() = spacing.continuousSlabOverlap
    val tabRowHeightDocked: Dp get() = tabs.rowHeight.docked
    val tabRowHeightFloating: Dp get() = tabs.rowHeight.floating
    val md3VisualSpec: Md3TopTabVisualSpec get() = tabs.md3VisualSpec
    val actionButtonSizeDocked: Dp get() = actions.buttonSize.docked
    val actionButtonSizeFloating: Dp get() = actions.buttonSize.floating
    val actionButtonCornerDocked: Dp get() = actions.buttonCorner.docked
    val actionButtonCornerFloating: Dp get() = actions.buttonCorner.floating
    val actionIconSizeDocked: Dp get() = actions.iconSize.docked
    val actionIconSizeFloating: Dp get() = actions.iconSize.floating
}

data class HomeTopSearchStyle(
    val barHeight: Dp,
    val revealDeadZone: Dp,
    val rowHorizontalPadding: Dp,
    val pillHeight: Dp,
    val content: HomeTopSearchContentStyle
)

data class HomeTopSearchContentStyle(
    val horizontalPadding: Dp,
    val iconTextGap: Dp
)

data class HomeTopPanelStyle(
    val useUnified: Boolean,
    val showDivider: Boolean,
    val horizontalPadding: Dp,
    val innerPadding: Dp,
    val cornerRadius: Dp,
    val reservedContentBottomGap: Dp
)

data class HomeTopSpacingStyle(
    val edgeControlGap: Dp,
    val embeddedTabHorizontalPadding: Dp,
    val searchToTabs: Dp,
    val searchCollapseExtra: Dp,
    val continuousSlabOverlap: Dp
)

data class HomeTopTabsStyle(
    val horizontalPadding: HomeTopDpPair,
    val rowHeight: HomeTopDpPair,
    val md3VisualSpec: Md3TopTabVisualSpec
)

data class HomeTopActionStyle(
    val buttonSize: HomeTopDpPair,
    val buttonCorner: HomeTopDpPair,
    val iconSize: HomeTopDpPair
)

data class HomeTopDpPair(
    val docked: Dp,
    val floating: Dp
)

internal fun resolveHomeTopPresetStyle(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant,
    labelMode: Int
): HomeTopPresetStyle {
    val normalizedLabelMode = normalizeTopTabLabelMode(labelMode)
    val isIconAndText = normalizedLabelMode == 0
    val compactChrome = resolveCompactCapsuleChromeSpec(uiPreset, androidNativeVariant)
    return when {
        uiPreset == UiPreset.IOS -> {
            HomeTopPresetStyle(
                preset = HomeTopPreset.IOS,
                renderer = HomeTopTabRenderer.IOS,
                indicatorStyle = TopTabIndicatorStyle.CAPSULE,
                search = HomeTopSearchStyle(
                    barHeight = 48.dp,
                    revealDeadZone = 8.dp,
                    rowHorizontalPadding = 14.dp,
                    pillHeight = compactChrome.primaryHeightDp.dp,
                    content = HomeTopSearchContentStyle(
                        horizontalPadding = compactChrome.inputHorizontalPaddingDp.dp,
                        iconTextGap = compactChrome.standardGapDp.dp
                    )
                ),
                panel = HomeTopPanelStyle(
                    useUnified = true,
                    showDivider = false,
                    horizontalPadding = 0.dp,
                    innerPadding = 6.dp,
                    cornerRadius = 32.dp,
                    reservedContentBottomGap = 5.dp
                ),
                spacing = HomeTopSpacingStyle(
                    edgeControlGap = 6.dp,
                    embeddedTabHorizontalPadding = 0.dp,
                    searchToTabs = 4.dp,
                    searchCollapseExtra = 0.dp,
                    continuousSlabOverlap = 0.dp
                ),
                tabs = HomeTopTabsStyle(
                    horizontalPadding = HomeTopDpPair(docked = 0.dp, floating = 14.dp),
                    rowHeight = HomeTopDpPair(
                        docked = if (isIconAndText) 58.dp else 56.dp,
                        floating = 62.dp
                    ),
                    md3VisualSpec = resolveMd3TopTabVisualSpec(
                        false,
                        AndroidNativeVariant.MATERIAL3,
                        normalizedLabelMode
                    )
                ),
                actions = HomeTopActionStyle(
                    buttonSize = HomeTopDpPair(
                        docked = resolveIosTopTabActionButtonSize(false),
                        floating = resolveIosTopTabActionButtonSize(true)
                    ),
                    buttonCorner = HomeTopDpPair(
                        docked = resolveIosTopTabActionButtonCorner(false),
                        floating = resolveIosTopTabActionButtonCorner(true)
                    ),
                    iconSize = HomeTopDpPair(
                        docked = resolveIosTopTabActionIconSize(false),
                        floating = resolveIosTopTabActionIconSize(true)
                    )
                )
            )
        }
        androidNativeVariant == AndroidNativeVariant.MIUIX -> {
            HomeTopPresetStyle(
                preset = HomeTopPreset.MIUIX,
                renderer = if (shouldUseNativeMiuixTopTabRow(androidNativeVariant, normalizedLabelMode)) {
                    HomeTopTabRenderer.MIUIX
                } else {
                    HomeTopTabRenderer.MD3
                },
                indicatorStyle = if (shouldUseNativeMiuixTopTabRow(androidNativeVariant, normalizedLabelMode)) {
                    TopTabIndicatorStyle.CAPSULE
                } else {
                    TopTabIndicatorStyle.MATERIAL
                },
                search = HomeTopSearchStyle(
                    barHeight = 50.dp,
                    revealDeadZone = 0.dp,
                    rowHorizontalPadding = 14.dp,
                    pillHeight = compactChrome.primaryHeightDp.dp,
                    content = HomeTopSearchContentStyle(
                        horizontalPadding = compactChrome.inputHorizontalPaddingDp.dp,
                        iconTextGap = compactChrome.standardGapDp.dp
                    )
                ),
                panel = HomeTopPanelStyle(
                    useUnified = true,
                    showDivider = false,
                    horizontalPadding = 0.dp,
                    innerPadding = 9.dp,
                    cornerRadius = 18.dp,
                    reservedContentBottomGap = 12.dp
                ),
                spacing = HomeTopSpacingStyle(
                    edgeControlGap = 7.dp,
                    embeddedTabHorizontalPadding = 0.dp,
                    searchToTabs = 4.dp,
                    searchCollapseExtra = 5.dp,
                    continuousSlabOverlap = 20.dp
                ),
                tabs = HomeTopTabsStyle(
                    horizontalPadding = HomeTopDpPair(docked = 2.dp, floating = 8.dp),
                    rowHeight = HomeTopDpPair(
                        docked = if (isIconAndText) 56.dp else 48.dp,
                        floating = if (isIconAndText) 60.dp else 54.dp
                    ),
                    md3VisualSpec = resolveMd3TopTabVisualSpec(
                        false,
                        AndroidNativeVariant.MIUIX,
                        normalizedLabelMode
                    )
                ),
                actions = HomeTopActionStyle(
                    buttonSize = HomeTopDpPair(
                        docked = resolveMd3TopTabActionButtonSize(false, AndroidNativeVariant.MIUIX),
                        floating = resolveMd3TopTabActionButtonSize(true, AndroidNativeVariant.MIUIX)
                    ),
                    buttonCorner = HomeTopDpPair(
                        docked = resolveMd3TopTabActionButtonCorner(false, AndroidNativeVariant.MIUIX),
                        floating = resolveMd3TopTabActionButtonCorner(true, AndroidNativeVariant.MIUIX)
                    ),
                    iconSize = HomeTopDpPair(
                        docked = resolveMd3TopTabActionIconSize(false, AndroidNativeVariant.MIUIX),
                        floating = resolveMd3TopTabActionIconSize(true, AndroidNativeVariant.MIUIX)
                    )
                )
            )
        }
        else -> {
            HomeTopPresetStyle(
                preset = HomeTopPreset.MATERIAL3,
                renderer = HomeTopTabRenderer.MD3,
                indicatorStyle = TopTabIndicatorStyle.MATERIAL,
                search = HomeTopSearchStyle(
                    barHeight = 52.dp,
                    revealDeadZone = 0.dp,
                    rowHorizontalPadding = 16.dp,
                    pillHeight = compactChrome.primaryHeightDp.dp,
                    content = HomeTopSearchContentStyle(
                        horizontalPadding = compactChrome.inputHorizontalPaddingDp.dp,
                        iconTextGap = compactChrome.standardGapDp.dp
                    )
                ),
                panel = HomeTopPanelStyle(
                    useUnified = true,
                    showDivider = true,
                    horizontalPadding = 0.dp,
                    innerPadding = 10.dp,
                    cornerRadius = 16.dp,
                    reservedContentBottomGap = 5.dp
                ),
                spacing = HomeTopSpacingStyle(
                    edgeControlGap = 8.dp,
                    embeddedTabHorizontalPadding = 0.dp,
                    searchToTabs = 6.dp,
                    searchCollapseExtra = 5.dp,
                    continuousSlabOverlap = 24.dp
                ),
                tabs = HomeTopTabsStyle(
                    horizontalPadding = HomeTopDpPair(docked = 4.dp, floating = 10.dp),
                    rowHeight = HomeTopDpPair(
                        docked = if (isIconAndText) 60.dp else 48.dp,
                        floating = if (isIconAndText) 62.dp else 52.dp
                    ),
                    md3VisualSpec = resolveMd3TopTabVisualSpec(
                        false,
                        AndroidNativeVariant.MATERIAL3,
                        normalizedLabelMode
                    )
                ),
                actions = HomeTopActionStyle(
                    buttonSize = HomeTopDpPair(
                        docked = resolveMd3TopTabActionButtonSize(false, AndroidNativeVariant.MATERIAL3),
                        floating = resolveMd3TopTabActionButtonSize(true, AndroidNativeVariant.MATERIAL3)
                    ),
                    buttonCorner = HomeTopDpPair(
                        docked = resolveMd3TopTabActionButtonCorner(false, AndroidNativeVariant.MATERIAL3),
                        floating = resolveMd3TopTabActionButtonCorner(true, AndroidNativeVariant.MATERIAL3)
                    ),
                    iconSize = HomeTopDpPair(
                        docked = resolveMd3TopTabActionIconSize(false, AndroidNativeVariant.MATERIAL3),
                        floating = resolveMd3TopTabActionIconSize(true, AndroidNativeVariant.MATERIAL3)
                    )
                )
            )
        }
    }
}

internal fun resolveHomeTopTabRenderer(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant,
    labelMode: Int
): HomeTopTabRenderer {
    return resolveHomeTopPresetStyle(uiPreset, androidNativeVariant, labelMode).renderer
}

internal fun resolveHomeTopTabMaterialMode(headerBlurEnabled: Boolean): TopTabMaterialMode {
    return if (headerBlurEnabled) TopTabMaterialMode.BLUR else TopTabMaterialMode.PLAIN
}

internal fun resolveTopTabClickAction(
    index: Int,
    selectedIndex: Int
): TopTabClickAction {
    return if (index == selectedIndex) {
        TopTabClickAction.SCROLL_TO_TOP
    } else {
        TopTabClickAction.SELECT_TAB
    }
}

internal fun resolveTopTabRenderMaterialMode(
    liquidGlassEnabled: Boolean,
    hasHazeState: Boolean
): TopTabMaterialMode {
    return when {
        liquidGlassEnabled -> TopTabMaterialMode.LIQUID_GLASS
        hasHazeState -> TopTabMaterialMode.BLUR
        else -> TopTabMaterialMode.PLAIN
    }
}

enum class TopTabIndicatorStyle {
    CAPSULE,
    MATERIAL
}

internal const val CompactTopTabIndicatorHeightDp = 40f
internal const val CompactTopTabIndicatorCornerDp = CompactTopTabIndicatorHeightDp / 2f

data class TopTabVisualTuning(
    val nonFloatingIndicatorHeightDp: Float = CompactTopTabIndicatorHeightDp,
    val nonFloatingIndicatorCornerDp: Float = CompactTopTabIndicatorCornerDp,
    val nonFloatingIndicatorWidthRatio: Float = 0.72f,
    val nonFloatingIndicatorMinWidthDp: Float = 44f,
    val nonFloatingIndicatorHorizontalInsetDp: Float = 18f,
    val floatingIndicatorWidthMultiplier: Float = 1.18f,
    val floatingIndicatorMinWidthDp: Float = 82f,
    val floatingIndicatorMaxWidthDp: Float = 112f,
    val floatingIndicatorMaxWidthToItemRatio: Float = 1.18f,
    val floatingIndicatorHeightDp: Float = CompactTopTabIndicatorHeightDp,
    val tabTextSizeSp: Float = 13f,
    val tabTextLineHeightSp: Float = 17f,
    val tabContentMinHeightDp: Float = 36f,
    val tabIconWithTextSizeDp: Float = 18f,
    val tabIconOnlySizeDp: Float = 22f,
    val tabIconTextSpacingDp: Float = 2f
)

data class TopTabVisualState(
    val floating: Boolean,
    val materialMode: TopTabMaterialMode
)

data class MiuixTopTabRowColors(
    val backgroundColor: Color,
    val contentColor: Color,
    val selectedBackgroundColor: Color,
    val selectedContentColor: Color
)

data class MiuixTopTabActionColors(
    val containerColor: Color,
    val borderColor: Color,
    val contentColor: Color
)

data class Md3TopTabVisualSpec(
    val rowHeight: Dp,
    val selectedCapsuleHeight: Dp,
    val selectedCapsuleCornerRadius: Dp,
    val selectedCapsuleTonalElevation: Dp,
    val selectedCapsuleShadowElevation: Dp,
    val itemHorizontalPadding: Dp,
    val iconSize: Dp,
    val labelTextSize: TextUnit,
    val labelLineHeight: TextUnit,
    val iconLabelSpacing: Dp
)

fun resolveTopTabVisualTuning(): TopTabVisualTuning = TopTabVisualTuning()

fun resolveTopTabVisualTuning(uiPreset: UiPreset): TopTabVisualTuning {
    return when (uiPreset) {
        UiPreset.IOS -> TopTabVisualTuning(
            nonFloatingIndicatorHeightDp = CompactTopTabIndicatorHeightDp,
            nonFloatingIndicatorCornerDp = CompactTopTabIndicatorCornerDp,
            nonFloatingIndicatorWidthRatio = 1.18f,
            nonFloatingIndicatorMinWidthDp = 78f,
            nonFloatingIndicatorHorizontalInsetDp = 0f,
            floatingIndicatorWidthMultiplier = 1.18f,
            floatingIndicatorMinWidthDp = 82f,
            floatingIndicatorMaxWidthDp = 112f,
            floatingIndicatorMaxWidthToItemRatio = 1.18f,
            floatingIndicatorHeightDp = CompactTopTabIndicatorHeightDp,
            tabTextSizeSp = 13f,
            tabTextLineHeightSp = 17f,
            tabContentMinHeightDp = 36f,
            tabIconWithTextSizeDp = 18f,
            tabIconOnlySizeDp = 22f,
            tabIconTextSpacingDp = 2f
        )
        UiPreset.MD3 -> resolveTopTabVisualTuning()
    }
}

internal fun resolveTopTabContentScale(
    selectionFraction: Float,
    showIcon: Boolean,
    showText: Boolean,
    uiPreset: UiPreset
): Float {
    if (showIcon && showText) return 1f

    val clampedFraction = selectionFraction.coerceIn(0f, 1f)
    val maxScale = when (uiPreset) {
        UiPreset.IOS -> 1.03f
        UiPreset.MD3 -> 1.04f
    }
    return 1f + ((maxScale - 1f) * clampedFraction)
}

internal fun resolveMiuixTopTabRowColors(
    surfaceContainer: Color,
    onSurfaceVariant: Color,
    secondaryContainer: Color,
    onSecondaryContainer: Color
): MiuixTopTabRowColors {
    return MiuixTopTabRowColors(
        backgroundColor = surfaceContainer.copy(alpha = 0.72f),
        contentColor = onSurfaceVariant,
        selectedBackgroundColor = secondaryContainer.copy(alpha = 0.58f),
        selectedContentColor = onSecondaryContainer
    )
}

internal fun resolveMiuixTopTabActionColors(
    surfaceContainer: Color,
    outlineVariant: Color,
    contentColor: Color
): MiuixTopTabActionColors {
    return MiuixTopTabActionColors(
        containerColor = surfaceContainer.copy(alpha = 0.74f),
        borderColor = outlineVariant.copy(alpha = 0.42f),
        contentColor = contentColor
    )
}

internal fun resolveMiuixTopTabRowVerticalInset(): Dp = 2.dp

internal fun resolveMiuixTopTabRowHorizontalPadding(): Dp = 4.dp

internal fun resolveMiuixTopTabActionTrailingPadding(panelInnerPadding: Dp): Dp {
    return (panelInnerPadding - resolveMiuixTopTabRowHorizontalPadding()).coerceAtLeast(0.dp)
}

internal fun resolveMiuixTopTabContentHeight(rowHeight: Dp): Dp {
    val contentHeight = rowHeight - (resolveMiuixTopTabRowVerticalInset() * 2)
    return contentHeight.coerceAtLeast(40.dp)
}

internal fun resolveMd3TopTabVisualSpec(
    isFloatingStyle: Boolean,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3,
    labelMode: Int = 2
): Md3TopTabVisualSpec {
    val normalizedLabelMode = normalizeTopTabLabelMode(labelMode)
    val showIconAndText = normalizedLabelMode == 0
    if (androidNativeVariant == AndroidNativeVariant.MIUIX) {
        return if (isFloatingStyle) {
            Md3TopTabVisualSpec(
                rowHeight = if (showIconAndText) 60.dp else 54.dp,
                selectedCapsuleHeight = 30.dp,
                selectedCapsuleCornerRadius = 15.dp,
                selectedCapsuleTonalElevation = 0.dp,
                selectedCapsuleShadowElevation = 0.dp,
                itemHorizontalPadding = 12.dp,
                iconSize = 22.dp,
                labelTextSize = 14.sp,
                labelLineHeight = 18.sp,
                iconLabelSpacing = 3.dp
            )
        } else {
            Md3TopTabVisualSpec(
                rowHeight = if (showIconAndText) 56.dp else 48.dp,
                selectedCapsuleHeight = 30.dp,
                selectedCapsuleCornerRadius = 15.dp,
                selectedCapsuleTonalElevation = 0.dp,
                selectedCapsuleShadowElevation = 0.dp,
                itemHorizontalPadding = 12.dp,
                iconSize = 20.dp,
                labelTextSize = 15.sp,
                labelLineHeight = 20.sp,
                iconLabelSpacing = 2.dp
            )
        }
    }

    return if (isFloatingStyle) {
        Md3TopTabVisualSpec(
            rowHeight = if (showIconAndText) 62.dp else 52.dp,
            selectedCapsuleHeight = 2.dp,
            selectedCapsuleCornerRadius = 1.dp,
            selectedCapsuleTonalElevation = 0.dp,
            selectedCapsuleShadowElevation = 0.dp,
            itemHorizontalPadding = if (showIconAndText) 8.dp else 14.dp,
            iconSize = 22.dp,
            labelTextSize = if (showIconAndText) 14.sp else 15.sp,
            labelLineHeight = if (showIconAndText) 18.sp else 20.sp,
            iconLabelSpacing = if (showIconAndText) 3.dp else 0.dp
        )
    } else {
        Md3TopTabVisualSpec(
            rowHeight = if (showIconAndText) 60.dp else 48.dp,
            selectedCapsuleHeight = 2.dp,
            selectedCapsuleCornerRadius = 1.dp,
            selectedCapsuleTonalElevation = 0.dp,
            selectedCapsuleShadowElevation = 0.dp,
            itemHorizontalPadding = if (showIconAndText) 8.dp else 12.dp,
            iconSize = 20.dp,
            labelTextSize = if (showIconAndText) 14.sp else 15.sp,
            labelLineHeight = if (showIconAndText) 18.sp else 20.sp,
            iconLabelSpacing = if (showIconAndText) 3.dp else 0.dp
        )
    }
}

internal fun resolveMd3TopTabSelectedContainerColor(
    colorScheme: ColorScheme,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
): androidx.compose.ui.graphics.Color = when {
    androidNativeVariant == AndroidNativeVariant.MIUIX -> colorScheme.secondaryContainer
    else -> colorScheme.primary
}

internal fun resolveMd3TopTabSelectedIconColor(
    colorScheme: ColorScheme,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
): androidx.compose.ui.graphics.Color = when {
    androidNativeVariant == AndroidNativeVariant.MIUIX -> colorScheme.onSecondaryContainer
    else -> colorScheme.primary
}

internal fun resolveMd3TopTabSelectedLabelColor(
    colorScheme: ColorScheme,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
): androidx.compose.ui.graphics.Color = when {
    androidNativeVariant == AndroidNativeVariant.MIUIX -> colorScheme.onSecondaryContainer
    else -> colorScheme.primary
}

internal fun resolveMd3TopTabUnselectedIconColor(
    colorScheme: ColorScheme
): androidx.compose.ui.graphics.Color = colorScheme.onSurfaceVariant

internal fun resolveMd3TopTabUnselectedLabelColor(
    colorScheme: ColorScheme
): androidx.compose.ui.graphics.Color = colorScheme.onSurfaceVariant

internal fun resolveMd3TopTabIconTint(
    selectionFraction: Float,
    colorScheme: ColorScheme,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
) = androidx.compose.ui.graphics.lerp(
    resolveMd3TopTabUnselectedIconColor(colorScheme),
    resolveMd3TopTabSelectedIconColor(colorScheme, androidNativeVariant),
    selectionFraction.coerceIn(0f, 1f)
)

internal fun resolveMd3TopTabLabelTint(
    selectionFraction: Float,
    colorScheme: ColorScheme,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
) = androidx.compose.ui.graphics.lerp(
    resolveMd3TopTabUnselectedLabelColor(colorScheme),
    resolveMd3TopTabSelectedLabelColor(colorScheme, androidNativeVariant),
    selectionFraction.coerceIn(0f, 1f)
)

internal fun resolveTopTabIndicatorStyle(uiPreset: UiPreset): TopTabIndicatorStyle {
    return if (uiPreset == UiPreset.MD3) {
        TopTabIndicatorStyle.MATERIAL
    } else {
        TopTabIndicatorStyle.CAPSULE
    }
}

internal fun shouldUseMd3TopTabMaterialIndicator(
    uiPreset: UiPreset,
    liquidGlassEnabled: Boolean
): Boolean {
    return resolveTopTabIndicatorStyle(uiPreset) == TopTabIndicatorStyle.MATERIAL
}

internal fun shouldUsePlainMd3TopTabUnderline(
    uiPreset: UiPreset,
    liquidGlassEnabled: Boolean
): Boolean {
    return uiPreset == UiPreset.MD3 && !liquidGlassEnabled
}

fun resolveTopTabLabelTextSizeSp(labelMode: Int): Float {
    val tuning = resolveTopTabVisualTuning()
    return when (normalizeTopTabLabelMode(labelMode)) {
        0 -> resolveMd3TopTabVisualSpec(isFloatingStyle = false, labelMode = labelMode).labelTextSize.value
        2 -> tuning.tabTextSizeSp
        else -> tuning.tabTextSizeSp
    }
}

fun resolveTopTabLabelLineHeightSp(labelMode: Int): Float {
    return when (normalizeTopTabLabelMode(labelMode)) {
        0 -> resolveMd3TopTabVisualSpec(isFloatingStyle = false, labelMode = labelMode).labelLineHeight.value
        else -> {
            val tuning = resolveTopTabVisualTuning()
            val textSize = resolveTopTabLabelTextSizeSp(labelMode)
            maxOf(tuning.tabTextLineHeightSp, textSize)
        }
    }
}

fun resolveTopTabContentMinHeightDp(labelMode: Int = 2): Float {
    return when (normalizeTopTabLabelMode(labelMode)) {
        0 -> 42f
        else -> resolveTopTabVisualTuning().tabContentMinHeightDp
    }
}

fun resolveTopTabContentVerticalPaddingDp(labelMode: Int): Float {
    return when (normalizeTopTabLabelMode(labelMode)) {
        0 -> 2f
        else -> 4f
    }
}

fun resolveTopTabIconSizeDp(labelMode: Int): Float {
    val tuning = resolveTopTabVisualTuning()
    return when (normalizeTopTabLabelMode(labelMode)) {
        0 -> tuning.tabIconWithTextSizeDp
        1 -> tuning.tabIconOnlySizeDp
        else -> 0f
    }
}

fun resolveTopTabIconTextSpacingDp(labelMode: Int): Float {
    return if (normalizeTopTabLabelMode(labelMode) == 0) {
        resolveTopTabVisualTuning().tabIconTextSpacingDp
    } else {
        0f
    }
}

fun resolveTopTabStyle(
    isBottomBarFloating: Boolean,
    isBottomBarBlurEnabled: Boolean,
    isLiquidGlassEnabled: Boolean
): TopTabVisualState {
    val materialMode = when {
        isLiquidGlassEnabled -> TopTabMaterialMode.LIQUID_GLASS
        isBottomBarBlurEnabled -> TopTabMaterialMode.BLUR
        else -> TopTabMaterialMode.PLAIN
    }

    return TopTabVisualState(
        floating = isBottomBarFloating,
        materialMode = materialMode
    )
}

internal fun resolveEffectiveHomeHeaderTabMaterialMode(
    materialMode: TopTabMaterialMode,
    interactionBudget: HomeInteractionMotionBudget
): TopTabMaterialMode {
    return materialMode
}

internal fun resolveEffectiveTopTabLiquidGlassEnabled(
    isLiquidGlassEnabled: Boolean,
    interactionBudget: HomeInteractionMotionBudget
): Boolean {
    return isLiquidGlassEnabled
}

internal fun shouldDrawHomeTopTabOuterChromeSurface(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant,
    materialMode: TopTabMaterialMode
): Boolean {
    if (uiPreset == UiPreset.MD3 && materialMode != TopTabMaterialMode.LIQUID_GLASS) {
        return false
    }
    return !(uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX)
}
