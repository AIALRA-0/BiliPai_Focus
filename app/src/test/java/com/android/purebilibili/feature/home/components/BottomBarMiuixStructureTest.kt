package com.android.purebilibili.feature.home.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomBarMiuixStructureTest {

    @Test
    fun `android native floating branch renders through kernelsu aligned renderer`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val kernelSuRendererSource = source
            .substringAfter("private fun KernelSuAlignedBottomBar(")
            .substringBefore("@Composable\nprivate fun AndroidNativeBottomBarItem(")
        val kernelSuAlignedBodySource = source
            .substringAfter("private fun KernelSuAlignedBottomBar(")
            .substringBefore("@Composable\nprivate fun KernelSuBottomBarShell(")

        assertTrue(source.contains("KernelSuAlignedBottomBar("))
        assertTrue(source.contains("private data class KernelSuBottomBarSearchLayoutState("))
        assertTrue(source.contains("private fun rememberKernelSuBottomBarSearchLayoutState("))
        assertTrue(source.contains("private fun KernelSuBottomBarShell("))
        assertTrue(source.contains("KernelSuMiuixBottomBarIndicatorLayer("))
        assertTrue(source.contains("internal fun BoxScope.KernelSuBottomBarIndicatorLayer("))
        assertTrue(source.contains("private fun KernelSuBottomBarSearchSlot("))
        assertTrue(source.contains("KernelSuBottomBarInputLayer("))
        assertTrue(source.contains("uiSkinDecoration: BottomBarUiSkinDecoration? = null"))
        assertTrue(source.contains("decoration = uiSkinDecoration"))
        assertTrue(source.contains("BottomBarSkinDecorativeTrim("))
        assertTrue(kernelSuRendererSource.contains("AndroidNativeBottomBarTuning"))
        assertTrue(source.contains("resolveKernelSuFloatingBottomBarWidth("))
        assertTrue(source.contains("resolveKernelSuBottomBarSearchLayout("))
        assertTrue(kernelSuRendererSource.contains("val dockContentPadding = if (uiSkinDecoration != null)"))
        assertTrue(kernelSuRendererSource.contains("resolveBottomBarSkinDockContentPadding()"))
        assertTrue(kernelSuRendererSource.contains("hasUiSkinDecoration = uiSkinDecoration != null"))
        assertTrue(source.contains("val shellHeight = if (dockHeight > searchHeight) dockHeight else searchHeight"))
        assertTrue(kernelSuRendererSource.contains("KernelSuBottomBarSearchSlot("))
        assertTrue(source.contains("KernelSuBottomBarSearchCapsule("))
        assertTrue(source.contains("val collapsedSearchWidth = searchCircleSize"))
        assertTrue(source.contains("label = \"bottomBarDockWidth\""))
        assertTrue(kernelSuRendererSource.contains("label = \"bottomBarDockContentAlpha\""))
        assertTrue(kernelSuRendererSource.contains("easing = AppMotionEasing.Continuity"))
        assertTrue(source.contains("launchAdjustedSearchGap = searchGap"))
        assertFalse(kernelSuRendererSource.contains("val searchLaunchProgressState = remember { Animatable(0f) }"))
        assertFalse(kernelSuRendererSource.contains("scaleX = lerp(1f, searchLaunchSpec.targetScaleX, searchLaunchProgress)"))
        assertFalse(kernelSuRendererSource.contains("scaleY = lerp(1f, searchLaunchSpec.targetScaleY, searchLaunchProgress)"))
        assertFalse(kernelSuRendererSource.contains("alpha = lerp(1f, searchLaunchSpec.targetAlpha, searchLaunchProgress)"))
        assertTrue(kernelSuRendererSource.contains("val searchLaunchMorphSpec = remember { resolveBottomBarSearchLaunchMorphSpec() }"))
        assertTrue(kernelSuRendererSource.contains("var searchLaunchInProgress by remember"))
        assertTrue(kernelSuRendererSource.contains("searchExpansionOverride = BottomBarSearchExpansionOverride.EXPANDED"))
        assertTrue(kernelSuRendererSource.contains("onSearchLaunchTransitionFinished(searchLaunchKey)"))
        assertTrue(kernelSuRendererSource.contains(".width(dockWidth)"))
        assertTrue(kernelSuRendererSource.contains("resolveSharedBottomBarCapsuleShape("))
        assertTrue(kernelSuRendererSource.contains(".kernelSuMiuixFloatingDockSurface("))
        assertTrue(kernelSuRendererSource.contains("blurRadius = tuning.shellBlurRadiusDp.dp"))
        assertTrue(source.contains("BottomBarGlassMaterialSpec"))
        assertTrue(source.contains("resolveBottomBarGlassMaterialSpec("))
        assertTrue(source.contains("resolveBottomBarGlassMaterialContainerColor("))
        assertTrue(source.contains("val innerRimGlowProvider: () -> MiuixInnerShadow? = remember("))
        assertTrue(source.contains("materialSpec.innerRimGlow"))
        assertTrue(source.contains(".miuixInnerShadow(shape = shape, shadow = innerRimGlowProvider)"))
        assertFalse(source.contains("BottomBarShellEffectSpec"))
        assertFalse(source.contains("resolveBottomBarIOS26SurfaceTint("))
        assertFalse(kernelSuRendererSource.contains("bottomBarIOS26ScrollGlassProgress"))
        assertFalse(kernelSuRendererSource.contains("scrollGlassProgress = scrollGlassProgress"))
        assertTrue(source.contains("liquidGlassPreset = homeSettings.bottomBarLiquidGlassPreset"))
        assertTrue(kernelSuRendererSource.contains("miuixDrawBackdrop("))
        assertTrue(kernelSuRendererSource.contains("miuixVibrancy()"))
        assertTrue(kernelSuRendererSource.contains("miuixLens("))
        assertTrue(kernelSuRendererSource.contains("rememberMiuixCombinedBackdrop(miuixBackdrop, tabsBackdrop)"))
        assertTrue(kernelSuRendererSource.contains("val tabsBackdrop = rememberMiuixLayerBackdrop()"))
        assertTrue(
            kernelSuRendererSource.contains("shellProgress = backdropPresetProgress.shellProgress")
        )
        assertTrue(kernelSuRendererSource.contains("notifyIndexChangedOnReleaseStart = false"))
        assertTrue(kernelSuRendererSource.contains("holdPressUntilReleaseTargetSettles = true"))
        assertTrue(kernelSuRendererSource.contains("dampedDragState.updateIndex(index)"))
        assertTrue(source.contains("private const val BOTTOM_BAR_INDICATOR_DRAG_SCALE_TARGET = 88f / 56f"))
        assertFalse(kernelSuAlignedBodySource.contains("var bottomBarTapSwitchPulseKey by remember"))
        assertFalse(kernelSuAlignedBodySource.contains("val tapSwitchPressProgress = rememberBottomBarTapSwitchPressProgress("))
        assertTrue(kernelSuRendererSource.contains("val effectivePressProgress = dampedDragState.pressProgress"))
        assertTrue(kernelSuRendererSource.contains("val isBottomBarPressActive ="))
        assertTrue(kernelSuRendererSource.contains("val effectiveIndicatorEffectProgress = maxOf("))
        assertTrue(kernelSuRendererSource.contains("effectivePressProgress"))
        assertTrue(kernelSuRendererSource.contains("allowTransitionIndicatorPulse = isBottomBarPressActive"))
        assertTrue(kernelSuRendererSource.contains("val shouldRenderIndicatorContentCapture ="))
        assertTrue(kernelSuRendererSource.contains("shouldRenderRefractionCapture || isBottomBarPressActive"))
        assertTrue(kernelSuRendererSource.contains("if (shouldRenderIndicatorContentCapture && miuixBackdrop != null)"))
        assertTrue(kernelSuRendererSource.contains("if (isBottomBarPressActive && item != null)"))
        assertTrue(kernelSuRendererSource.contains("return selectedContentColor(item)"))
        assertTrue(kernelSuRendererSource.contains(".zIndex(if (foregroundAboveIndicator) 1f else 0f)"))
        assertTrue(kernelSuRendererSource.contains("indicatorBackdropEnabled = shouldRenderIndicatorBackdrop"))
        assertTrue(kernelSuRendererSource.contains("indicatorProgress = effectiveIndicatorEffectProgress"))
        assertFalse(kernelSuRendererSource.contains("keepNeutralDuringClickPulse"))
        assertFalse(kernelSuRendererSource.contains("selectedIndicatorClickPulse"))
        assertFalse(kernelSuRendererSource.contains("pendingSelectedIndicatorPulse"))
        assertFalse(kernelSuRendererSource.contains("foregroundAboveIndicator || isBottomBarPressActive"))
        assertFalse(kernelSuRendererSource.contains("resolveBottomBarIndicatorClickSettlePulseTransform("))
        assertFalse(kernelSuRendererSource.contains("clickPulseKey = if (item == BottomNavItem.HOME)"))
        assertFalse(kernelSuRendererSource.contains("selectedSettlePulseKey"))
        assertFalse(kernelSuRendererSource.contains("settlePulseKey = if (index == selectedIndex)"))
        assertTrue(kernelSuRendererSource.contains("if (effectiveSearchExpanded) {\n                                    Modifier.clickable("))
        assertFalse(kernelSuRendererSource.contains("ColorFilter.tint(exportTintColor)"))
        assertFalse(kernelSuRendererSource.contains("ColorFilter.tint(uiSkinDecoration"))
        assertFalse(kernelSuRendererSource.contains("val contentColor = Color.White"))
        assertTrue(kernelSuRendererSource.contains("val contentColor = exportItemContentColor(item, coverage)"))
        assertFalse(kernelSuRendererSource.contains("BottomBarStyleIndicatorSurface("))
        assertFalse(source.contains("internal fun BottomBarStyleIndicatorSurface("))
        assertTrue(kernelSuRendererSource.contains("velocityItemsPerSecond = dampedDragState.deformationVelocityItemsPerSecond"))
        assertTrue(kernelSuRendererSource.contains("val rawIndicatorLayerTransform = if (glassEnabled)"))
        assertTrue(kernelSuRendererSource.contains("val indicatorLayerTransform = if (swapMotionAxes)"))
        assertTrue(kernelSuRendererSource.contains("resolveBottomBarIndicatorLayerTransform("))
        assertFalse(kernelSuRendererSource.contains("val indicatorLayerWidth = indicatorWidth * indicatorLayerTransform.scaleX"))
        assertFalse(kernelSuRendererSource.contains("val indicatorLayerHeight = indicatorHeight * indicatorLayerTransform.scaleY"))
        assertFalse(kernelSuRendererSource.contains("val indicatorLayerVerticalCenterOffset = if (centerLayerOnIndicatorY)"))
        assertTrue(kernelSuRendererSource.contains("translationX = indicatorTranslationXPx + indicatorPanelOffsetPx"))
        assertTrue(kernelSuRendererSource.contains("translationY = indicatorTranslationYPx + indicatorPanelOffsetYPx"))
        assertFalse(kernelSuRendererSource.contains("((indicatorLayerWidth - indicatorWidth) / 2f).toPx()"))
        assertFalse(kernelSuRendererSource.contains("((indicatorLayerHeight - indicatorHeight) / 2f).toPx()"))
        assertTrue(kernelSuRendererSource.contains(".width(indicatorWidth)"))
        assertTrue(kernelSuRendererSource.contains(".height(indicatorHeight)"))
        assertTrue(kernelSuRendererSource.contains(".zIndex(2f)"))
        assertTrue(kernelSuRendererSource.contains(".horizontalDragGesture("))
        assertTrue(kernelSuRendererSource.contains("dampedDragState = dampedDragState"))
        assertTrue(kernelSuRendererSource.contains("itemWidthPx = itemWidthPx"))
        assertTrue(kernelSuRendererSource.contains("interactive = false"))
        assertTrue(kernelSuRendererSource.contains("onPressChanged = dampedDragState::setPressed"))
        assertFalse(kernelSuAlignedBodySource.contains("scaleX = indicatorSettleReboundTransform.scaleX"))
        assertFalse(kernelSuAlignedBodySource.contains("scaleY = indicatorSettleReboundTransform.scaleY"))
        assertTrue(kernelSuRendererSource.contains("dragScaleProgress = indicatorLayerScaleProgress"))
        val indicatorLayerSource = source
            .substringAfter("@Composable\nprivate fun BoxScope.KernelSuMiuixBottomBarIndicatorLayer(")
            .substringBefore("@Composable\nprivate fun KernelSuBottomBarSearchSlot(")
        val backdropLayerBlockSource = indicatorLayerSource
            .substringAfter("layerBlock = {")
            .substringBefore("}")
        assertTrue(backdropLayerBlockSource.contains("scaleX = indicatorLayerTransform.scaleX"))
        assertTrue(backdropLayerBlockSource.contains("scaleY = indicatorLayerTransform.scaleY"))
        assertTrue(kernelSuRendererSource.contains("indicatorLayerScaleTransform = null"))
        assertFalse(kernelSuRendererSource.contains("scaleX = dampedDragState.scaleX"))
        assertFalse(indicatorLayerSource.contains("visualIndicatorWidth"))
        assertFalse(kernelSuAlignedBodySource.contains("rememberBottomBarSettleReboundTransform("))
        assertFalse(kernelSuAlignedBodySource.contains("indicatorSettleReboundTransform.scaleX"))
        assertFalse(kernelSuAlignedBodySource.contains("indicatorSettleReboundTransform.scaleY"))
        assertFalse(kernelSuRendererSource.contains(".offset(x = dockHorizontalPadding + indicatorWidth * dampedDragState.value)"))
        assertTrue(kernelSuRendererSource.contains("val visualIndicatorPosition by remember"))
        assertTrue(kernelSuRendererSource.contains("resolveBottomBarVisualIndicatorPosition("))
        assertTrue(kernelSuRendererSource.contains("resolveBottomBarEdgeStrain("))
        assertTrue(kernelSuRendererSource.contains("dockHorizontalPadding"))
        assertTrue(
            kernelSuRendererSource.contains("indicatorPanelOffsetPx = presetPanelOffsets.indicatorPanelOffsetPx") &&
                kernelSuRendererSource.contains("translationX = indicatorTranslationXPx + indicatorPanelOffsetPx")
        )
        assertTrue(kernelSuRendererSource.contains("val interactiveHighlightCenterXPx by remember("))
        assertTrue(kernelSuRendererSource.contains("presetPanelOffsets.indicatorPanelOffsetPx"))
        assertTrue(kernelSuRendererSource.contains("resolveBottomBarInteractiveHighlightCenterX("))
        assertTrue(kernelSuRendererSource.contains("val shellHighlightAlpha = resolveBottomBarShellHighlightAlpha("))
        assertTrue(kernelSuRendererSource.contains(".bottomBarInteractiveHighlight("))
        assertTrue(kernelSuRendererSource.contains("enabled = glassEnabled && interactiveHighlightEnabled"))
        assertTrue(kernelSuRendererSource.contains("alpha = shellHighlightAlpha"))
        assertTrue(kernelSuRendererSource.contains("centerXPx = interactiveHighlightCenterXPx"))
        assertFalse(
            kernelSuRendererSource.contains(
                ".width(dockWidth)\n                        .height(dockHeight)\n                        .graphicsLayer { scaleX = edgeCompressionScaleX }"
            )
        )
        assertTrue(kernelSuRendererSource.contains("scaleX = edgeCompressionScaleX"))
        assertTrue(kernelSuRendererSource.contains("chromaticAberration = 0.5f"))
        assertTrue(
            kernelSuRendererSource.contains(
                "val backdropPresetProgress = resolveBottomBarEffectiveBackdropPresetProgress("
            )
        )
        assertTrue(kernelSuRendererSource.contains("preset = liquidGlassPreset"))
        assertTrue(kernelSuRendererSource.contains("motionProgress = motionProgress"))
        assertTrue(kernelSuRendererSource.contains("pressProgress = effectivePressProgress"))
        assertTrue(kernelSuRendererSource.contains("val indicatorLayerScaleProgress = maxOf(indicatorDragScaleProgress, effectivePressProgress)"))
        assertTrue(kernelSuRendererSource.contains("resolveBottomBarBackdropPresetIndicatorLens("))
        assertTrue(kernelSuRendererSource.contains("effectiveIndicatorEffectProgress"))
        assertTrue(kernelSuRendererSource.contains("pressProgress = effectivePressProgress"))
        assertTrue(kernelSuRendererSource.contains("shouldRenderBottomBarRefractionCapture("))
        assertTrue(kernelSuRendererSource.contains("if (shouldRenderIndicatorContentCapture && miuixBackdrop != null)"))
        assertTrue(kernelSuRendererSource.contains(".miuixLayerBackdrop(tabsBackdrop)"))
        assertTrue(kernelSuRendererSource.contains("val shouldRenderIndicatorBackdropRaw = shouldRenderBottomBarIndicatorBackdrop("))
        assertTrue(kernelSuRendererSource.contains("val glassLayersAlwaysOn = glassEnabled && miuixBackdrop != null"))
        assertTrue(kernelSuRendererSource.contains("glassLayersAlwaysOn || shouldRenderRefractionCaptureRaw"))
        assertTrue(kernelSuRendererSource.contains("glassLayersAlwaysOn || shouldRenderIndicatorBackdropRaw"))
        assertTrue(kernelSuRendererSource.contains("isBottomBarInteractionActive = isBottomBarInteractionActive"))
        assertTrue(kernelSuRendererSource.contains("shouldRenderIndicatorBackdrop && captureWarm && miuixBackdrop != null"))
        assertTrue(kernelSuRendererSource.contains("captureWarm = true"))
        assertTrue(kernelSuRendererSource.contains("alpha = effectivePressProgress"))
        assertTrue(kernelSuRendererSource.contains("radius = 8.dp * effectivePressProgress"))
        assertTrue(kernelSuRendererSource.contains("color = Color.Black.copy(alpha = 0.15f)"))
        assertTrue(kernelSuRendererSource.contains("translationX = presetPanelOffsets.exportPanelOffsetPx"))
        assertTrue(kernelSuRendererSource.contains("resolveBottomBarGlassVisibleContentColor("))
        assertTrue(kernelSuRendererSource.contains("resolveBottomBarGlassExportContentColor("))
        assertTrue(kernelSuRendererSource.contains("indicatorProgress = effectiveIndicatorEffectProgress"))
        assertTrue(kernelSuRendererSource.contains("resolveBottomBarIdleIndicatorSurfaceColor("))
        assertTrue(kernelSuRendererSource.contains("preset = liquidGlassPreset"))
        assertFalse(kernelSuRendererSource.contains("resolveAndroidNativeIndicatorColor("))
        assertTrue(kernelSuRendererSource.contains("selected = coverage >= 0.5f,"))
        assertTrue(kernelSuRendererSource.contains("contentColorOverride = contentColor,"))
        assertFalse(kernelSuRendererSource.contains("selectionEmphasis = refractionMotionProfile.exportSelectionEmphasis"))
        assertTrue(kernelSuRendererSource.contains("fun itemCoverage(index: Int): Float = resolveBottomBarItemCoverage("))
        val coverageResolverSource = source
            .substringAfter("internal fun resolveBottomBarItemCoverage(")
            .substringBefore("internal fun resolveBottomBarItemMotionScale(")
        assertFalse(coverageResolverSource.contains("itemIndex == currentSelectedIndex"))
        assertTrue(coverageResolverSource.contains("indicatorPosition"))
        val visibleDockContentSource = kernelSuRendererSource
            .substringAfter("if (shouldComposeDockContent) {")
            .substringBefore("if (shouldRenderIndicatorContentCapture && miuixBackdrop != null)")
        assertTrue(visibleDockContentSource.contains("scale = 1f"))
        assertFalse(visibleDockContentSource.contains("scale = sampledItemScale(coverage)"))
        val indicatorCaptureContentSource = kernelSuRendererSource
            .substringAfter("if (shouldRenderIndicatorContentCapture && miuixBackdrop != null) {")
            .substringBefore("if (searchEnabled) {")
        assertTrue(indicatorCaptureContentSource.contains("scale = sampledItemScale(coverage)"))
        assertTrue(kernelSuRendererSource.contains("resolveBottomBarIdleIndicatorSurfaceColor("))
        assertFalse(kernelSuRendererSource.contains("val indicatorSurfaceOverlayAlpha"))
        assertFalse(kernelSuRendererSource.contains("Color.Black.copy(indicatorSurfaceOverlayAlpha)"))
        assertFalse(kernelSuRendererSource.contains("0.03f * backdropPresetProgress.indicatorProgress"))
        assertFalse(kernelSuRendererSource.contains("item = currentItem,"))
        assertFalse(kernelSuRendererSource.contains("val tintedContentBackdrop = rememberLayerBackdrop()"))
        assertFalse(kernelSuRendererSource.contains("val refractionMotionProfile by remember"))
        assertFalse(kernelSuRendererSource.contains("blur(8.dp.toPx())"))
        assertFalse(source.contains("private fun MiuixFloatingCapsuleBottomBar("))
        assertFalse(source.contains("private fun MiuixFloatingBottomBarItem("))
        assertFalse(source.contains("resolveBottomBarChromeMaterialMode("))
        assertFalse(source.contains("resolveBottomBarContainerColor("))
        assertFalse(source.contains("LocalSoftwareKeyboardController"))
        assertFalse(source.contains("focusRequester.requestFocus()"))
        assertFalse(kernelSuRendererSource.contains("enabled = searchExpanded"))
    }

    @Test
    fun `disabled sukisu search path skips search layout animations`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val layoutStateSource = source
            .substringAfter("private fun rememberKernelSuBottomBarSearchLayoutState(")
            .substringBefore("@Composable\nprivate fun KernelSuBottomBarShell(")

        assertTrue(layoutStateSource.contains("if (!searchEnabled) {"))
        assertTrue(layoutStateSource.contains("searchWidth = 0.dp"))
        assertTrue(layoutStateSource.contains("searchGap = 0.dp"))
        assertTrue(layoutStateSource.contains("searchHeight = 0.dp"))
        assertTrue(layoutStateSource.contains("return KernelSuBottomBarSearchLayoutState("))

        val disabledBranch = layoutStateSource
            .substringAfter("if (!searchEnabled) {")
            .substringBefore("val searchWidth by animateDpAsState(")
        assertFalse(disabledBranch.contains("label = \"bottomBarSearchWidth\""))
        assertFalse(disabledBranch.contains("label = \"bottomBarSearchGap\""))
        assertFalse(disabledBranch.contains("label = \"bottomBarSearchHeight\""))
    }

    @Test
    fun `skin decoration participates in refraction capture without replacing indicator`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val skinDecorationSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBarUiSkin.kt")
        val kernelSuRendererSource = source
            .substringAfter("private fun KernelSuAlignedBottomBar(")
            .substringBefore("@Composable\nprivate fun AndroidNativeBottomBarItem(")
        val refractionCaptureSource = source
            .substringAfter("if (shouldRenderIndicatorContentCapture && miuixBackdrop != null) {")
            .substringBefore("KernelSuMiuixBottomBarIndicatorLayer(")

        val shellSource = source
            .substringAfter("private fun KernelSuBottomBarShell(")
            .substringBefore("@Composable\nprivate fun BoxScope.KernelSuMiuixBottomBarIndicatorLayer(")
        val shellIndex = shellSource.indexOf(".kernelSuMiuixFloatingDockSurface(")
        val skinIndex = shellSource.indexOf("BottomBarSkinDecorativeTrim(")
        val visibleContentIndex = kernelSuRendererSource.indexOf(
            "val coverage = itemCoverage(index)"
        )
        val captureIndex = kernelSuRendererSource.indexOf(".miuixLayerBackdrop(tabsBackdrop)")
        val indicatorIndex = kernelSuRendererSource.indexOf("backdrop = indicatorBackdrop")

        assertTrue(shellIndex >= 0)
        assertTrue(skinIndex > shellIndex)
        assertTrue(visibleContentIndex > skinIndex)
        assertTrue(captureIndex > visibleContentIndex)
        assertTrue(indicatorIndex > captureIndex)
        assertTrue(kernelSuRendererSource.contains("KernelSuBottomBarInputLayer("))
        val capturedSkinIndex = refractionCaptureSource.indexOf("BottomBarSkinDecorativeTrim(")
        val capturedContentIndex = refractionCaptureSource.indexOf("val coverage = itemCoverage(index)")
        assertTrue(capturedSkinIndex >= 0)
        assertTrue(capturedContentIndex > capturedSkinIndex)
        val visibleSkinCall = kernelSuRendererSource
            .substringAfter("BottomBarSkinDecorativeTrim(")
            .substringBefore("if (shouldComposeDockContent)")
        val captureSkinCall = refractionCaptureSource
            .substringAfter("BottomBarSkinDecorativeTrim(")
            .substringBefore("Row(")
        assertTrue(visibleSkinCall.contains("clipShape = shellShape"))
        assertTrue(captureSkinCall.contains("clipShape = shellShape"))
        assertTrue(skinDecorationSource.contains("AsyncImage("))
        assertTrue(skinDecorationSource.contains("model = File(imagePath)"))
        assertTrue(skinDecorationSource.contains("model = File(iconPath)"))
        assertFalse(skinDecorationSource.contains("ColorFilter.tint"))
    }

    @Test
    fun `skin icons replace only visual icon layer and keep clickable hit layer unchanged`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val skinDecorationSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBarUiSkin.kt")
        val kernelSuRendererSource = source
            .substringAfter("private fun KernelSuAlignedBottomBar(")
            .substringBefore("@Composable\nprivate fun AndroidNativeBottomBarItem(")
        val itemRendererSource = source
            .substringAfter("private fun RowScope.AndroidNativeBottomBarItem(")
            .substringBefore("@Composable\nprivate fun KernelSuBottomBarSearchCapsule(")

        assertTrue(skinDecorationSource.contains("fun iconPathFor(item: BottomNavItem, selected: Boolean = false): String?"))
        assertTrue(kernelSuRendererSource.contains("skinIconPath = uiSkinDecoration?.iconPathFor(item, selected = coverage >= 0.5f)"))
        assertTrue(source.contains("skinIconPath = uiSkinDecoration?.iconPathFor(item, selected = currentItem == item)"))
        assertTrue(itemRendererSource.contains("skinIconPath: String? = null"))
        assertTrue(itemRendererSource.contains("BottomBarSkinIcon("))
        assertTrue(itemRendererSource.contains("skinIconPath != null ->"))
        assertTrue(itemRendererSource.contains("val shouldUseSkinItemLayout = skinIconPath != null && showIcon && showText"))
        assertTrue(itemRendererSource.contains("resolveBottomBarSkinIconLabelGap()"))
        assertTrue(itemRendererSource.contains("val skinIconPathForLayout = if (shouldUseSkinItemLayout) skinIconPath else null"))
        assertTrue(itemRendererSource.contains("if (skinIconPathForLayout != null)"))
        assertTrue(itemRendererSource.contains("resolveBottomBarSkinDockIconTopPadding()"))
        assertTrue(itemRendererSource.contains("resolveBottomBarSkinDockLabelBottomPadding()"))
        assertTrue(itemRendererSource.contains(".align(Alignment.BottomCenter)"))
        assertTrue(itemRendererSource.contains("verticalArrangement = Arrangement.Center"))
        assertFalse(itemRendererSource.contains("readabilityBackdropColor"))
        assertFalse(itemRendererSource.contains("translationY = -3.dp"))
        assertTrue(source.contains("BottomBarInputTarget("))
        assertTrue(source.contains("KernelSuBottomBarInputLayer("))
    }

    @Test
    fun `sukisu bottom bar item content shares indicator slot width`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val kernelSuRendererSource = source
            .substringAfter("private fun KernelSuAlignedBottomBar(")
            .substringBefore("@Composable\nprivate fun KernelSuBottomBarShell(")
        val itemRendererSource = source
            .substringAfter("private fun RowScope.AndroidNativeBottomBarItem(")
            .substringBefore("private fun resolveMaterialBottomBarIcon(")

        assertTrue(kernelSuRendererSource.contains("resolveKernelSuBottomBarItemSlotWidth("))
        assertTrue(kernelSuRendererSource.contains("itemWidth = indicatorWidth"))
        assertTrue(itemRendererSource.contains("itemWidth: Dp"))
        assertTrue(itemRendererSource.contains(".width(itemWidth)"))
        assertFalse(itemRendererSource.contains(".defaultMinSize(minWidth = 76.dp)"))
        assertTrue(source.contains("BottomBarInputTarget("))
    }

    @Test
    fun `home top skin does not render broad atmosphere block`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/iOSHomeHeader.kt")
        val skinDecorationSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBarUiSkin.kt")
        val headerSource = source
            .substringAfter("fun iOSHomeHeader(")
            .substringBefore("@Composable\nprivate fun")

        assertFalse(skinDecorationSource.contains("HomeSkinAtmosphere("))
        assertFalse(skinDecorationSource.contains("statusBarHeight: Dp"))
        assertFalse(headerSource.contains("HomeSkinAtmosphere("))
    }

    @Test
    fun `home and sidebar consume imported skin assets without changing host-only items`() {
        val navigationSource = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")
        val sidebarSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/SideBar.kt")
        val headerSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/iOSHomeHeader.kt")

        val sideBarCallSource = navigationSource
            .substringAfter("FrostedSideBar(")
            .substringBefore(")\n                    }")
        val sideBarBodySource = sidebarSource
            .substringAfter("fun FrostedSideBar(")

        assertTrue(sideBarCallSource.contains("uiSkinDecoration = bottomBarUiSkinDecoration"))
        assertTrue(sideBarBodySource.contains("uiSkinDecoration: BottomBarUiSkinDecoration? = null"))
        assertTrue(sideBarBodySource.contains("val skinIconPath = uiSkinDecoration?.iconPathFor(item, selected = isSelected)"))
        assertTrue(sideBarBodySource.contains("BottomBarSkinIcon("))
        assertTrue(headerSource.contains("val topAtmosphereImagePath = uiSkinDecoration?.topAtmosphereImagePath"))
        assertTrue(headerSource.contains("model = File(topAtmosphereImagePath)"))
        assertFalse(headerSource.contains("val topTabBackgroundImagePath = uiSkinDecoration?.topTabBackgroundImagePath"))
        assertFalse(headerSource.contains("model = File(topTabBackgroundImagePath)"))
        assertTrue(headerSource.contains("ContentScale.Crop"))
    }

    @Test
    fun `bottom bar search click keeps capsule scale stable`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val refractionProfileSource = source
            .substringAfter("internal fun resolveBottomBarRefractionMotionProfile(")
            .substringBefore("@Composable\nfun FrostedBottomBar(")
        val searchCapsuleSource = source
            .substringAfter("private fun KernelSuBottomBarSearchCapsule(")
            .substringBefore("@Composable\nprivate fun AndroidNativeBottomBarItem(")

        assertTrue(searchCapsuleSource.contains("label = \"bottomBarSearchFieldAlpha\""))
        assertTrue(searchCapsuleSource.contains("label = \"bottomBarSearchIconScale\""))
        assertTrue(searchCapsuleSource.contains("label = \"bottomBarSearchLongPressHorizontalScale\""))
        assertFalse(searchCapsuleSource.contains("rememberBottomBarClickPulseTransform(searchClickPulseKey)"))
        assertFalse(searchCapsuleSource.contains("searchClickPulseKey += 1"))
        assertTrue(searchCapsuleSource.contains("detectTapGestures("))
        assertTrue(searchCapsuleSource.contains("onLongPress = {"))
        assertTrue(searchCapsuleSource.contains("currentHaptic(HapticType.SELECTION)"))
        assertTrue(searchCapsuleSource.contains("val currentOnSubmit by rememberUpdatedState(onSubmit)"))
        assertTrue(searchCapsuleSource.contains("val currentHaptic by rememberUpdatedState(haptic)"))
        assertTrue(searchCapsuleSource.contains("Modifier.pointerInput(Unit)"))
        assertFalse(searchCapsuleSource.contains("Modifier.pointerInput(onExpandChange)"))
        val collapsedTapSource = searchCapsuleSource
            .substringAfter("onTap = {")
            .substringBefore("},\n                            onLongPress")
        assertTrue(collapsedTapSource.contains("currentOnSubmit()"))
        assertFalse(collapsedTapSource.contains("currentOnExpandChange(true)"))
        assertTrue(searchCapsuleSource.contains("BasicTextField("))
        assertTrue(searchCapsuleSource.contains("onClick = onSubmit"))
        assertTrue(searchCapsuleSource.contains("keyboardActions = KeyboardActions(onSearch = { onSubmit() })"))
        assertTrue(searchCapsuleSource.contains("val launchSearchFromExpandedBlankQuery = expanded && query.isBlank()"))
        assertTrue(searchCapsuleSource.contains("if (launchSearchFromExpandedBlankQuery) {"))
        assertTrue(searchCapsuleSource.contains(".matchParentSize()"))
        assertTrue(searchCapsuleSource.contains(".clip(shape)"))
        val expandedBlankTapSource = searchCapsuleSource
            .substringAfter("if (launchSearchFromExpandedBlankQuery) {")
            .substringBefore("\n        }\n    }\n}")
        assertTrue(expandedBlankTapSource.contains("currentHaptic(HapticType.LIGHT)"))
        assertTrue(expandedBlankTapSource.contains("currentOnSubmit()"))
        assertTrue(searchCapsuleSource.contains("easing = AppMotionEasing.Continuity"))
        assertFalse(source.contains("private fun rememberBottomBarSettlePulseTransform("))
        assertFalse(source.contains("settlePulseKey = if (index == selectedIndex)"))
        assertTrue(refractionProfileSource.contains("rawProgress * rawProgress * (3f - 2f * rawProgress)"))
        assertFalse(refractionProfileSource.contains("resolveBottomBarIOSMotionProgress"))
    }

    @Test
    fun `search launch expands search box before top bar handoff without compressing dock`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")

        val spec = resolveBottomBarSearchLaunchMorphSpec()
        assertEquals(190, spec.expandDurationMillis)
        assertEquals(40L, spec.postHandoffResetDelayMillis)

        assertTrue(source.contains("searchLaunchKey: Int = 0"))
        assertTrue(source.contains("onSearchLaunchTransitionFinished: (Int) -> Unit = {}"))
        assertTrue(source.contains("searchLaunchInProgress = true"))
        assertTrue(source.contains("delay(searchLaunchMorphSpec.expandDurationMillis.toLong())"))
        assertTrue(source.contains("onSearchLaunchTransitionFinished(searchLaunchKey)"))
        assertFalse(source.contains("searchLaunchProgressState.animateTo("))
        assertFalse(source.contains("scaleX = lerp(1f, searchLaunchSpec.targetScaleX, searchLaunchProgress)"))
        assertFalse(source.contains("scaleY = lerp(1f, searchLaunchSpec.targetScaleY, searchLaunchProgress)"))
        assertFalse(source.contains("alpha = lerp(1f, searchLaunchSpec.targetAlpha, searchLaunchProgress)"))
        assertTrue(source.contains("launchAdjustedSearchGap = searchGap"))
        assertFalse(source.contains("Spacer(modifier = Modifier.width(searchGap))"))
        assertTrue(source.contains("Spacer(modifier = Modifier.width(launchAdjustedSearchGap))"))
    }

    @Test
    fun `sukisu renderer uses clickable content items and indicator-owned drag`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val kernelSuRendererSource = source
            .substringAfter("private fun KernelSuAlignedBottomBar(")
            .substringBefore("@Composable\nprivate fun AndroidNativeBottomBarItem(")

        val visibleContentIndex = kernelSuRendererSource.indexOf(
            "val coverage = itemCoverage(index)"
        )
        val tintCaptureIndex = kernelSuRendererSource.indexOf(".miuixLayerBackdrop(tabsBackdrop)")
        val indicatorIndex = kernelSuRendererSource.indexOf("backdrop = indicatorBackdrop")

        assertTrue(visibleContentIndex >= 0)
        assertTrue(tintCaptureIndex > visibleContentIndex)
        assertTrue(indicatorIndex > tintCaptureIndex)
        assertTrue(kernelSuRendererSource.contains("KernelSuBottomBarInputLayer("))
        assertTrue(kernelSuRendererSource.contains("interactive = false"))
        assertTrue(kernelSuRendererSource.contains(".horizontalDragGesture("))
    }

    @Test
    fun `sukisu renderer skips dock and export content when search is stably expanded`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val kernelSuRendererSource = source
            .substringAfter("private fun KernelSuAlignedBottomBar(")
            .substringBefore("@Composable\nprivate fun AndroidNativeBottomBarItem(")

        assertTrue(kernelSuRendererSource.contains("val shouldComposeDockContent = shouldComposeBottomBarDockContent("))
        assertTrue(kernelSuRendererSource.contains("if (shouldComposeDockContent) {"))
        assertTrue(kernelSuRendererSource.contains("shouldRenderRefractionCapture || isBottomBarPressActive"))
        assertTrue(kernelSuRendererSource.contains("if (shouldRenderIndicatorContentCapture && miuixBackdrop != null) {"))
        assertTrue(kernelSuRendererSource.contains("val rawCaptureWidth = resolveBottomBarRefractionCaptureWidth("))
        assertTrue(kernelSuRendererSource.contains("dockWidth = dockWidth"))
        assertTrue(kernelSuRendererSource.contains("launchAdjustedSearchGap = launchAdjustedSearchGap"))
        assertTrue(kernelSuRendererSource.contains("searchWidth = searchWidth"))
        assertTrue(kernelSuRendererSource.contains("searchEnabled = searchEnabled"))
        assertTrue(kernelSuRendererSource.contains("val captureWidth = rawCaptureWidth"))
        assertTrue(kernelSuRendererSource.contains("shape = { shellShape }"))
        assertTrue(kernelSuRendererSource.contains(".width(captureWidth)"))
    }

    @Test
    fun `sukisu search content shares ksu full width refraction capture`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val kernelSuRendererSource = source
            .substringAfter("private fun KernelSuAlignedBottomBar(")
            .substringBefore("@Composable\nprivate fun AndroidNativeBottomBarItem(")
        val refractionCaptureSource = source
            .substringAfter("if (shouldRenderIndicatorContentCapture && miuixBackdrop != null) {")
            .substringBefore("KernelSuMiuixBottomBarIndicatorLayer(")

        assertFalse(source.contains("private fun KernelSuBottomBarSearchRefractionCapture("))
        assertFalse(kernelSuRendererSource.contains("KernelSuBottomBarSearchRefractionCapture("))
        assertTrue(refractionCaptureSource.contains("val rawCaptureWidth = resolveBottomBarRefractionCaptureWidth("))
        assertTrue(refractionCaptureSource.contains("launchAdjustedSearchGap = launchAdjustedSearchGap"))
        assertTrue(refractionCaptureSource.contains("searchWidth = searchWidth"))
        assertTrue(refractionCaptureSource.contains("searchEnabled = searchEnabled"))
        assertTrue(refractionCaptureSource.contains("val captureWidth = rawCaptureWidth"))
        assertTrue(refractionCaptureSource.contains("shape = { shellShape }"))
        assertTrue(refractionCaptureSource.contains(".width(captureWidth)"))
        assertTrue(refractionCaptureSource.contains(".miuixLayerBackdrop(tabsBackdrop)"))
        assertTrue(refractionCaptureSource.contains(".offset(x = dockWidth + launchAdjustedSearchGap)"))
        assertTrue(refractionCaptureSource.contains("KernelSuBottomBarSearchVisualContent("))
        assertFalse(refractionCaptureSource.contains("kernelSuMiuixFloatingDockSurface("))
    }

    @Test
    fun `android native clickable items forward press state to indicator animation`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val kernelSuRendererSource = source
            .substringAfter("private fun KernelSuAlignedBottomBar(")
            .substringBefore("@Composable\nprivate fun AndroidNativeBottomBarItem(")
        val itemSource = source.substringAfter("@Composable\nprivate fun RowScope.AndroidNativeBottomBarItem(")

        assertTrue(kernelSuRendererSource.contains("onPressChanged = dampedDragState::setPressed"))
        assertTrue(source.contains("BottomBarInputTarget("))
        assertTrue(source.contains("KernelSuBottomBarInputLayer("))
        assertTrue(itemSource.contains("collectIsPressedAsState()"))
        assertTrue(itemSource.contains("LaunchedEffect(isPressed, interactive)"))
        assertTrue(itemSource.contains("DisposableEffect(interactive)"))
        assertTrue(itemSource.contains("currentOnPressChanged(false)"))
    }

    @Test
    fun `ios floating bottom bar also routes to sukisu renderer`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val iosRendererSource = source
            .substringAfter("fun FrostedBottomBar(")
            .substringBefore("@Composable\nprivate fun MaterialBottomBar(")

        assertTrue(iosRendererSource.contains("KernelSuAlignedBottomBar("))
        assertTrue(iosRendererSource.contains("iconStyle = SharedFloatingBottomBarIconStyle.CUPERTINO"))
        assertTrue(iosRendererSource.contains("if (isFloating) {"))
        assertFalse(iosRendererSource.contains("if (isFloating && homeSettings.isBottomBarLiquidGlassEnabled)"))
    }

    @Test
    fun `android native miuix variant routes to dedicated miuix bottom bar renderer`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")

        assertTrue(source.contains("val androidNativeVariant = LocalAndroidNativeVariant.current"))
        assertTrue(source.contains("androidNativeVariant == AndroidNativeVariant.MIUIX"))
        assertTrue(source.contains("MiuixBottomBar("))
        assertTrue(source.contains("if (isFloating) {"))
        assertTrue(source.contains("KernelSuAlignedBottomBar("))
        assertTrue(source.contains("iconStyle = SharedFloatingBottomBarIconStyle.CUPERTINO"))
        assertTrue(source.contains("private enum class SharedFloatingBottomBarIconStyle"))
        assertTrue(source.contains("MiuixNavigationBar("))
        assertTrue(source.contains("MiuixDockedBottomBarItem("))
        assertTrue(source.contains("NavigationBarDisplayMode as MiuixNavigationBarDisplayMode"))
    }

    @Test
    fun `docked miuix bottom bar avoids floating navigation insets`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val miuixRendererSource = source
            .substringAfter("private fun MiuixBottomBar(")
            .substringBefore("@Composable\nprivate fun RowScope.MiuixDockedBottomBarItem(")

        assertTrue(miuixRendererSource.contains("MiuixNavigationBar("))
        assertTrue(miuixRendererSource.contains("MiuixDockedBottomBarItem("))
        assertFalse(miuixRendererSource.contains("MiuixNavigationBarItem("))
        assertFalse(miuixRendererSource.contains("MiuixFloatingNavigationBar("))
        assertFalse(miuixRendererSource.contains("MiuixFloatingNavigationBarItem("))
    }

    @Test
    fun `docked bottom bars render skin trim behind navigation items`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val materialRendererSource = source
            .substringAfter("private fun MaterialBottomBar(")
            .substringBefore("@Composable\nprivate fun MiuixBottomBar(")
        val miuixRendererSource = source
            .substringAfter("private fun MiuixBottomBar(")
            .substringBefore("@Composable\nprivate fun RowScope.MiuixDockedBottomBarItem(")
        val miuixDockedItemSource = source
            .substringAfter("private fun RowScope.MiuixDockedBottomBarItem(")
            .substringBefore("@Composable\nprivate fun KernelSuAlignedBottomBar(")

        assertTrue(materialRendererSource.contains("DockedBottomBarSkinContainer("))
        assertTrue(materialRendererSource.contains("decoration = uiSkinDecoration"))
        assertTrue(materialRendererSource.indexOf("DockedBottomBarSkinContainer(") < materialRendererSource.indexOf("NavigationBar("))
        assertTrue(materialRendererSource.contains("val skinIconPath = uiSkinDecoration?.iconPathFor(item, selected = currentItem == item)"))
        assertTrue(materialRendererSource.contains("if (skinIconPath != null)"))
        assertTrue(materialRendererSource.contains("BottomBarSkinIcon("))
        assertTrue(miuixRendererSource.contains("DockedBottomBarSkinContainer("))
        assertTrue(miuixRendererSource.contains("decoration = uiSkinDecoration"))
        assertTrue(miuixRendererSource.indexOf("DockedBottomBarSkinContainer(") < miuixRendererSource.indexOf("MiuixNavigationBar("))
        assertTrue(miuixRendererSource.contains("Modifier.height(resolveBottomBarSkinDockHeight())"))
        assertTrue(miuixDockedItemSource.contains("height(resolveMiuixDockedBottomBarItemHeight(skinIconPath != null))"))
        assertFalse(miuixDockedItemSource.contains("height(64.dp)"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
