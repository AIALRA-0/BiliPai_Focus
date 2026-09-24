package com.android.purebilibili.feature.home.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Structure tests for BiliPai bottom-bar orchestration after BiliPai FloatingBottomBar migration.
 *
 * Liquid-glass material chain lives in FloatingBottomBar.kt (see FloatingBottomBarStructureTest).
 * This file asserts the BiliPai host path: search / skin / badge / tablet / routing.
 */
class BottomBarMiuixStructureTest {

    @Test
    fun `runtime low blur budget gates liquid glass via FloatingBottomBar mode`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val renderer = biliPaiFloatingBody(source)

        assertTrue(renderer.contains("val effectiveGlassEnabled = shouldRenderBottomBarLiquidGlassEffects("))
        assertTrue(renderer.contains("FloatingBottomBarMode.LiquidGlass"))
        assertTrue(renderer.contains("FloatingBottomBarMode.Blur"))
        assertTrue(renderer.contains("FloatingBottomBarMode.None"))
        assertTrue(renderer.contains("containerColor = floatingContainerColor"))
        assertTrue(
            renderer.contains("effectiveGlassEnabled && miuixBackdrop != null -> FloatingBottomBarMode.LiquidGlass")
        )
    }

    @Test
    fun `android native floating branch renders through FloatingBottomBar`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val floatingSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/FloatingBottomBar.kt"
        )
        val renderer = biliPaiFloatingBody(source)

        assertTrue(source.contains("BiliPaiFloatingBottomBar("))
        assertTrue(source.contains("extraDegrees = if (swapMotionAxes) 0f else 90f"))
        assertTrue(renderer.contains("FloatingBottomBar("))
        assertTrue(renderer.contains("FloatingBottomBarItem("))
        assertTrue(renderer.contains("FloatingBottomBarTabVisual("))
        assertTrue(renderer.contains("FloatingBottomBarColors("))
        assertTrue(renderer.contains("shellHeight = dockHeight"))
        assertTrue(renderer.contains("indicatorHeight = resolveBiliPaiBottomBarIndicatorHeight(dockHeight)"))
        assertTrue(renderer.contains("BiliPaiBottomBarSearchSlot("))
        assertTrue(renderer.contains("BottomBarSkinDecorativeTrim("))
        assertTrue(renderer.contains("uiSkinDecoration: BottomBarUiSkinDecoration? = null"))
        assertTrue(source.contains("private data class BiliPaiBottomBarSearchLayoutState("))
        assertTrue(source.contains("private fun rememberBiliPaiBottomBarSearchLayoutState("))
        assertTrue(source.contains("resolveBiliPaiFloatingBottomBarWidth("))
        assertTrue(source.contains("resolveBiliPaiBottomBarSearchLayout("))
        assertTrue(source.contains("val shellHeight = if (dockHeight > searchHeight) dockHeight else searchHeight"))
        assertTrue(source.contains("BOTTOM_BAR_INDICATOR_DRAG_SCALE_TARGET =") ||
            floatingSource.contains("BottomBarReferencePressedScale"))

        // Old multi-layer path removed (no dual render).
        assertFalse(source.contains("private fun BiliPaiBottomBarShell("))
        assertFalse(source.contains("private fun BoxScope.BiliPaiBottomBarInputLayer("))
        assertFalse(renderer.contains("BiliPaiBottomBarShell("))
        assertFalse(renderer.contains("BiliPaiBottomBarInputLayer("))
        assertFalse(renderer.contains("glassLayersAlwaysOn"))
        assertFalse(renderer.contains("shouldRenderIndicatorContentCapture"))
        assertFalse(renderer.contains("rememberMiuixCombinedBackdrop(miuixBackdrop, tabsBackdrop)"))
    }

    @Test
    fun `floating bar keeps selection providers stable across page changes`() {
        val renderer = biliPaiFloatingBody(
            loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        )

        assertTrue(renderer.contains("rememberUpdatedState(selectedIndexForBar)"))
        assertTrue(renderer.contains("val floatingSelectedIndex = remember(selectedIndexForBarState)"))
        assertTrue(renderer.contains("selectedIndex = floatingSelectedIndex"))
        assertTrue(renderer.contains("onSelected = floatingOnSelected"))
        assertTrue(renderer.contains("val floatingOnReselected = remember(selectedIndexForBarState, handleSelectedState)"))
        assertTrue(renderer.contains("onReselected = floatingOnReselected"))
        assertTrue(renderer.contains("if (index != selectedIndexForBarState.value)"))
        assertFalse(renderer.contains("selectedIndex = { selectedIndexForBar }"))
    }

    @Test
    fun `BiliPai material chain lives in FloatingBottomBar not host orchestrator`() {
        val floating = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/FloatingBottomBar.kt"
        )
        val host = biliPaiFloatingBody(
            loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        )
        val floatingBody = floating.substringAfter("fun FloatingBottomBar(")
        val baseRow = floatingBody
            .substringAfter("CompositionLocalProvider(LocalFloatingBottomBarContentColor provides colors.contentColor)")
            .substringBefore("if (isLiquidGlassMode && backdrop != null)")
        val movingIndicator = floatingBody.substringAfter("if (tabWidthPx > 0f)")

        assertTrue(floating.contains("vibrancy(liquidGlassTuning.saturation)"))
        assertTrue(floating.contains("liquidGlassTuning.backdropBlurRadius.dp.toPx()"))
        assertTrue(floating.contains("rememberCombinedBackdrop(backdrop, tabsBackdrop)"))
        assertTrue(floating.contains("rememberChromeBackdropSource()"))
        assertTrue(floating.contains(".then(tabsBackdropSource?.modifier ?: Modifier)"))
        // Floating-bar-local interaction stack, not the design-system drag stack.
        assertTrue(floating.contains("DampedDragAnimation("))
        assertFalse(baseRow.contains(".then(dampedDragAnimation.modifier)"))
        assertFalse(baseRow.contains("interactiveHighlight.gestureModifier"))
        assertTrue(movingIndicator.contains("dampedDragAnimation.modifier"))
        assertTrue(movingIndicator.contains("interactiveHighlight?.gestureModifier"))
        assertTrue(floating.contains("canDrag = { offset ->"))
        assertTrue(floating.contains("snapshotFlow { dampedDragAnimation.value }"))
        assertTrue(floating.contains("selectedIndexLatest.value().coerceIn(0, maxTabIndex)"))
        assertFalse(floating.contains(".drop(1)"))
        assertTrue(floating.contains("onSelectedLatest.value(targetIndex)"))
        assertFalse(floating.contains("horizontalDragGesture"))
        assertFalse(floating.contains("rememberDampedDragAnimationState"))
        assertTrue(floating.contains("resolveLiquidGlassIndicatorChromaticAberration("))
        assertTrue(floating.contains("rememberBiliPaiGravityHighlight(extraDegrees = -45f)"))
        assertTrue(floating.contains("val pillHighlight = if (isLiquidGlassMode) rememberBiliPaiGravityHighlight("))

        // Host must not re-implement the three-layer drawBackdrop path.
        assertFalse(host.contains("miuixDrawBackdrop("))
        assertFalse(host.contains("miuixVibrancy()"))
        assertFalse(host.contains("miuixLens("))
        assertFalse(host.contains(".biliPaiMiuixFloatingDockSurface("))
    }

    @Test
    fun `disabled sukisu search path skips search layout animations`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val layoutStateSource = source
            .substringAfter("private fun rememberBiliPaiBottomBarSearchLayoutState(")
            .substringBefore("private const val BottomBarSearchTopThresholdPx")

        assertTrue(layoutStateSource.contains("if (!searchEnabled) {"))
        assertTrue(layoutStateSource.contains("searchWidth = AppSpacingTokens.None"))
        assertTrue(layoutStateSource.contains("searchGap = AppSpacingTokens.None"))
        assertTrue(layoutStateSource.contains("searchHeight = AppSpacingTokens.None"))
        assertTrue(layoutStateSource.contains("return BiliPaiBottomBarSearchLayoutState("))

        val disabledBranch = layoutStateSource
            .substringAfter("if (!searchEnabled) {")
            .substringBefore("val searchWidth by animateDpAsState(")
        assertFalse(disabledBranch.contains("label = \"bottomBarSearchWidth\""))
        assertFalse(disabledBranch.contains("label = \"bottomBarSearchGap\""))
        assertFalse(disabledBranch.contains("label = \"bottomBarSearchHeight\""))
    }

    @Test
    fun `skin decoration sits outside FloatingBottomBar without dual glass path`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val skinDecorationSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBarUiSkin.kt"
        )
        val renderer = biliPaiFloatingBody(source)

        assertTrue(renderer.contains("BottomBarSkinDecorativeTrim("))
        assertTrue(renderer.contains("clipShape = resolveSharedBottomBarCapsuleShape()"))
        assertTrue(renderer.contains("FloatingBottomBar("))
        // Skin is composed first (behind), FloatingBottomBar second (interactive on top).
        assertTrue(
            renderer.indexOf("BottomBarSkinDecorativeTrim(") < renderer.indexOf("FloatingBottomBar(")
        )
        assertTrue(skinDecorationSource.contains("AsyncImage("))
        assertTrue(skinDecorationSource.contains("model = File(iconPath)"))
        assertFalse(skinDecorationSource.contains("ColorFilter.tint"))
        assertFalse(renderer.contains("BiliPaiBottomBarShell("))
    }

    @Test
    fun `skin icons replace visual icon layer via FloatingBottomBarTabVisual`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val skinDecorationSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBarUiSkin.kt"
        )
        val tabVisual = source
            .substringAfter("private fun ColumnScope.FloatingBottomBarTabVisual(")
            .substringBefore("@Composable\ninternal fun BoxScope.BiliPaiMiuixBottomBarIndicatorLayer(")
            .ifEmpty {
                source.substringAfter("private fun ColumnScope.FloatingBottomBarTabVisual(")
                    .substringBefore("internal fun BoxScope.BiliPaiMiuixBottomBarIndicatorLayer(")
            }
        val renderer = biliPaiFloatingBody(source)

        assertTrue(skinDecorationSource.contains("fun iconPathFor(item: BottomNavItem, selected: Boolean = false): String?"))
        assertTrue(skinDecorationSource.contains("targetState = iconPath"))
        assertTrue(skinDecorationSource.contains("contentKey = { stableIconPath -> stableIconPath }"))
        assertTrue(renderer.contains("uiSkinDecoration?.iconPathFor("))
        assertTrue(tabVisual.contains("BottomBarSkinIcon(") || renderer.contains("BottomBarSkinIcon("))
        assertTrue(source.contains("FloatingBottomBarItem("))
        assertFalse(source.contains("BiliPaiBottomBarInputLayer("))
    }

    @Test
    fun `home top skin does not render broad atmosphere block`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/HomeHeader.kt")
        val skinDecorationSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBarUiSkin.kt"
        )
        val headerSource = source
            .substringAfter("fun HomeHeader(")
            .substringBefore("@Composable\nprivate fun")

        assertFalse(skinDecorationSource.contains("HomeSkinAtmosphere("))
        assertFalse(skinDecorationSource.contains("statusBarHeight: Dp"))
        assertFalse(headerSource.contains("HomeSkinAtmosphere("))
    }

    @Test
    fun `home and sidebar consume imported skin assets without changing host-only items`() {
        val navigationSource = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")
        val sidebarSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/SideBar.kt")
        val headerSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/HomeHeader.kt")
        val topTabChromeSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/HomeTopTabChrome.kt")

        val sideBarCallSource = navigationSource
            .substringAfter("FrostedSideBar(")
            .substringBefore(")\n                    }")
        val sideBarBodySource = sidebarSource
            .substringAfter("fun FrostedSideBar(")

        assertTrue(sideBarCallSource.contains("uiSkinDecoration = bottomBarUiSkinDecoration"))
        assertTrue(sideBarBodySource.contains("uiSkinDecoration: BottomBarUiSkinDecoration? = null"))
        assertTrue(sideBarBodySource.contains("val skinIconPath = uiSkinDecoration?.iconPathFor(item, selected = isSelected)"))
        assertTrue(sideBarBodySource.contains("BottomBarSkinIcon("))
        assertTrue(headerSource.contains("val topTrimImagePath = uiSkinDecoration?.topAtmosphereImagePath"))
        assertTrue(headerSource.contains("model = File(topTrimImagePath)"))
        assertFalse(headerSource.contains("skinBackgroundImagePath = uiSkinDecoration?.topTabBackgroundImagePath"))
        assertTrue(topTabChromeSource.contains("model = File(skinBackgroundImagePath)"))
        assertTrue(headerSource.contains("ContentScale.Crop"))
    }

    @Test
    fun `fold posture keeps large screen bottom dock geometry`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val bottomBarSource = source.substringAfter("fun FrostedBottomBar(")

        assertTrue(
            bottomBarSource.contains(
                "val isTablet = com.android.purebilibili.core.util.LocalWindowSizeClass.current.isTablet"
            )
        )
        assertFalse(bottomBarSource.contains("isTablet &&\n        !forceBottomNavigation"))
        assertTrue(bottomBarSource.contains("onToggleSidebar.takeUnless { forceBottomNavigation }"))
    }

    @Test
    fun `search layout reservation does not follow the current home item`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val renderer = source.substringAfter("fun BiliPaiFloatingBottomBar(")

        assertTrue(renderer.contains("val searchLayoutReserved = shouldReserveBottomBarSearchLayout("))
        assertTrue(renderer.contains("searchEnabled = searchLayoutReserved"))
        assertTrue(renderer.contains("visible = searchLayoutReserved"))
    }

    @Test
    fun `floating bottom bar forwards pager position to draggable indicator`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val floatingBarSource = source.substringAfter("fun BiliPaiFloatingBottomBar(")

        assertTrue(floatingBarSource.contains("indicatorPositionProvider: (() -> Float)? = null"))
        assertTrue(floatingBarSource.contains("indicatorPositionProvider = indicatorPositionProvider"))
        assertTrue(
            floatingBarSource.contains(
                "isScrollInProgressProvider = isPagerScrollInProgressProvider"
            )
        )
    }

    @Test
    fun `bottom bar search click keeps capsule scale stable`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val refractionProfileSource = source
            .substringAfter("internal fun resolveBottomBarRefractionMotionProfile(")
            .substringBefore("@Composable\nfun FrostedBottomBar(")
        val searchCapsuleSource = source
            .substringAfter("private fun BiliPaiBottomBarSearchCapsule(")
            .substringBefore("@Composable\nprivate fun RowScope.BottomBarInputTarget(")
            .ifEmpty {
                source.substringAfter("private fun BiliPaiBottomBarSearchCapsule(")
                    .substringBefore("@Composable\nprivate fun RowScope.AndroidNativeBottomBarItem(")
            }

        assertTrue(searchCapsuleSource.contains("label = \"bottomBarSearchFieldAlpha\""))
        assertTrue(searchCapsuleSource.contains("label = \"bottomBarSearchIconScale\""))
        assertFalse(searchCapsuleSource.contains("bottomBarSearchLongPressHorizontalScale"))
        assertFalse(searchCapsuleSource.contains("rememberBottomBarClickPulseTransform(searchClickPulseKey)"))
        assertFalse(searchCapsuleSource.contains("searchClickPulseKey += 1"))
        assertFalse(searchCapsuleSource.contains("detectTapGestures("))
        assertFalse(searchCapsuleSource.contains("onLongPress = {"))
        assertFalse(searchCapsuleSource.contains("currentHaptic(HapticType.SELECTION)"))
        assertTrue(searchCapsuleSource.contains("val currentOnSubmit by rememberUpdatedState(onSubmit)"))
        assertTrue(searchCapsuleSource.contains("val currentHaptic by rememberUpdatedState(haptic)"))
        assertTrue(searchCapsuleSource.contains("role = Role.Button"))
        assertFalse(searchCapsuleSource.contains("modifier.pointerInput(onExpandChange)"))
        val collapsedTapSource = searchCapsuleSource
            .substringAfter("role = Role.Button")
            .substringBefore("} else {")
        assertTrue(collapsedTapSource.contains("currentOnCompactClick()"))
        assertFalse(collapsedTapSource.contains("currentOnExpandChange(true)"))
        assertTrue(searchCapsuleSource.contains("BasicTextField("))
        assertTrue(searchCapsuleSource.contains("onClick = onSubmit"))
        assertTrue(searchCapsuleSource.contains("keyboardActions = KeyboardActions(onSearch = { onSubmit() })"))
        assertFalse(searchCapsuleSource.contains("launchSearchFromExpandedBlankQuery"))
        assertTrue(searchCapsuleSource.contains("contentDescription = \"搜索输入框\""))
        assertTrue(searchCapsuleSource.contains("animationSpec = bottomBarContentVisibilityMotionSpec()"))
        assertFalse(source.contains("private fun rememberBottomBarSettlePulseTransform("))
        assertFalse(source.contains("settlePulseKey = if (index == selectedIndex)"))
        assertTrue(refractionProfileSource.contains("rawProgress * rawProgress * (3f - 2f * rawProgress)"))
        assertFalse(refractionProfileSource.contains("resolveBottomBarIOSMotionProgress"))
    }

    @Test
    fun `search launch completes handoff without forcing compact home dock`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")

        val spec = resolveBottomBarSearchLaunchMorphSpec()
        assertEquals(190, spec.expandDurationMillis)
        assertEquals(40L, spec.postHandoffResetDelayMillis)

        assertTrue(source.contains("searchLaunchKey: Int = 0"))
        assertTrue(source.contains("onSearchLaunchTransitionFinished: (Int) -> Unit = {}"))
        assertFalse(source.contains("searchLaunchInProgress = true"))
        assertFalse(source.contains("searchExpansionOverride = BottomBarSearchExpansionOverride.EXPANDED"))
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
    fun `sukisu search stays outside FloatingBottomBar dock`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val renderer = biliPaiFloatingBody(source)

        assertFalse(source.contains("private fun BiliPaiBottomBarSearchRefractionCapture("))
        assertFalse(renderer.contains("BiliPaiBottomBarSearchRefractionCapture("))
        assertTrue(renderer.contains("BiliPaiBottomBarSearchSlot("))
        assertTrue(renderer.contains("FloatingBottomBar("))
        // Search is a sibling after the dock Box, not inside FloatingBottomBar content.
        val floatingCall = renderer.indexOf("FloatingBottomBar(")
        val searchCall = renderer.indexOf("BiliPaiBottomBarSearchSlot(")
        assertTrue(floatingCall >= 0)
        assertTrue(searchCall > floatingCall)
    }

    @Test
    fun `miuix floating bottom bar also routes to sukisu renderer`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val miuixRendererSource = source
            .substringAfter("fun MiuixBottomBar(")
            .substringBefore("@Composable\nprivate fun BiliPaiFloatingBottomBar(")

        assertTrue(miuixRendererSource.contains("BiliPaiFloatingBottomBar("))
        assertTrue(miuixRendererSource.contains("iconStyle = sharedBarIconStyle"))
        assertTrue(miuixRendererSource.contains("if (isFloating) {"))
        assertFalse(miuixRendererSource.contains("if (isFloating && homeSettings.isBottomBarLiquidGlassEnabled)"))
    }

    @Test
    fun `neutral bottom bar host routes platform content to dedicated implementation`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")

        assertTrue(source.contains("AppBottomNavigationHost("))
        assertTrue(source.contains("platformContent = { policy ->"))
        assertFalse(source.contains("LocalUiPreset"))
        assertFalse(source.contains("LocalAndroidNativeVariant"))
        assertFalse(source.contains("AndroidNativeVariant"))
        assertTrue(source.contains("MiuixBottomBar("))
        assertTrue(source.contains("if (isFloating) {"))
        assertTrue(source.contains("BiliPaiFloatingBottomBar("))
        assertTrue(source.contains("iconStyle = sharedBarIconStyle"))
        assertTrue(source.contains("enum class SharedFloatingBottomBarIconStyle"))
        assertTrue(source.contains("AppNavigationBar("))
        assertTrue(source.contains("AppPlatformNavigationBar("))
        assertTrue(source.contains("MiuixDockedBottomBarItem("))
        assertTrue(source.contains("fun Md3BottomBarDisplayMode.toAppPlatformNavigationDisplayMode()"))
        assertFalse(source.contains("import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar"))
        assertFalse(source.contains("import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem"))
    }

    @Test
    fun `docked miuix bottom bar avoids floating navigation insets`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val miuixRendererSource = source
            .substringAfter("private fun MiuixBottomBar(")
            .substringBefore("@Composable\nprivate fun RowScope.MiuixDockedBottomBarItem(")

        assertTrue(miuixRendererSource.contains("showDivider = !sharedLiquidGlassEnabled"))
        assertTrue(miuixRendererSource.contains("AppPlatformNavigationBar("))
        assertTrue(miuixRendererSource.contains("AppPlatformNavigationBarItem("))
        assertTrue(miuixRendererSource.contains("colors = MiuixNavigationBarDefaults.navigationBarItemColors("))
        assertTrue(miuixRendererSource.contains("selectedContentColor = skinItemColors.selectedColor"))
        assertTrue(miuixRendererSource.contains("unselectedContentColor = skinItemColors.unselectedColor"))
        assertTrue(miuixRendererSource.contains("AppPlatformNavigationBadge {"))
        assertTrue(miuixRendererSource.contains("shouldUseMiuixOfficialNavigationBarItem("))
        assertTrue(miuixRendererSource.contains("MiuixDockedBottomBarItem("))
        assertTrue(miuixRendererSource.contains("resolveSharedBottomBarIcon("))
        assertTrue(miuixRendererSource.contains("resolveSharedBottomBarSidebarIcon("))
        assertFalse(miuixRendererSource.contains("icon = resolveMaterialBottomBarIcon("))
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
            .substringBefore("@Composable\nprivate fun BiliPaiFloatingBottomBar(")

        assertTrue(materialRendererSource.contains("DockedBottomBarSkinContainer("))
        assertTrue(materialRendererSource.contains("decoration = uiSkinDecoration"))
        assertTrue(materialRendererSource.indexOf("DockedBottomBarSkinContainer(") < materialRendererSource.indexOf("AppNavigationBar("))
        assertTrue(materialRendererSource.contains("AppNavigationBarItem("))
        assertTrue(materialRendererSource.contains("BottomBarReminderBadgeAnchor("))
        assertTrue(materialRendererSource.contains("val skinIconPath = uiSkinDecoration?.iconPathFor(item, selected = currentItem == item)"))
        assertTrue(materialRendererSource.contains("if (skinIconPath != null)"))
        assertTrue(materialRendererSource.contains("BottomBarSkinIcon("))
        assertTrue(materialRendererSource.contains("MaterialBottomBarAnimatedIcon("))
        assertTrue(materialRendererSource.contains("indicatorColor = dockedIndicatorColor"))
        assertTrue(materialRendererSource.contains("rememberNavigationSelectionTransform("))
        assertTrue(miuixRendererSource.contains("DockedBottomBarSkinContainer("))
        assertTrue(miuixRendererSource.contains("decoration = uiSkinDecoration"))
        assertTrue(miuixRendererSource.indexOf("DockedBottomBarSkinContainer(") < miuixRendererSource.indexOf("AppPlatformNavigationBar("))
        assertTrue(miuixRendererSource.contains("AppPlatformNavigationBadge {"))
        assertTrue(miuixRendererSource.contains("indicatorColor = dockedIndicatorColor"))
        val platformBarSource = miuixRendererSource
            .substringAfter("AppPlatformNavigationBar(")
            .substringBefore("defaultWindowInsetsPadding = true")
        assertTrue(platformBarSource.contains("uiSkinDecoration != null"))
        assertTrue(platformBarSource.contains("Modifier.height(resolveBottomBarSkinDockHeight())"))
        assertTrue(miuixDockedItemSource.contains("height(resolveMiuixDockedBottomBarItemHeight(skinIconPath != null))"))
        assertFalse(miuixDockedItemSource.contains("height(64.dp)"))
    }

    @Test
    fun `official md3 floating bar handles nowPlayingContent via LinkedBottomDock without falling back to miuix`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val materialBarSource = source
            .substringAfter("private fun MaterialBottomBar(")
            .substringBefore("private fun MiuixBottomBar(")

        // Must not drop to BiliPaiFloatingBottomBar when nowPlayingContent is present
        assertFalse(
            materialBarSource.contains("nowPlayingContent == null"),
            "MaterialBottomBar should not bypass official MD3 toolbar when nowPlayingContent is active"
        )
        // Must use LinkedBottomDock with OfficialMd3FloatingToolbarContent
        assertTrue(
            materialBarSource.contains("OfficialMd3FloatingToolbarContent("),
            "MaterialBottomBar must pass OfficialMd3FloatingToolbarContent to LinkedBottomDock"
        )
        assertTrue(
            materialBarSource.contains("iconStyle = SharedFloatingBottomBarIconStyle.MATERIAL"),
            "MaterialBottomBar must configure LinkedBottomDock with MATERIAL icon style"
        )
        assertTrue(
            materialBarSource.contains("nowPlayingContent = nowPlayingContent"),
            "MaterialBottomBar must pass nowPlayingContent to LinkedBottomDock"
        )

        val officialToolbarSource = source
            .substringAfter("private fun OfficialMd3FloatingToolbarContent(")
            .substringBefore("private fun OfficialMd3FloatingBottomBar(")

        assertTrue(
            officialToolbarSource.contains("HorizontalFloatingToolbar("),
            "OfficialMd3FloatingToolbarContent must render HorizontalFloatingToolbar"
        )
    }

    private fun biliPaiFloatingBody(source: String): String {
        val after = source.substringAfter("private fun BiliPaiFloatingBottomBar(")
        // End at next major private composable after the floating host + tab visual.
        val cutMarkers = listOf(
            "@Composable\ninternal fun BoxScope.BiliPaiMiuixBottomBarIndicatorLayer(",
            "internal fun BoxScope.BiliPaiMiuixBottomBarIndicatorLayer(",
            "@Composable\nprivate fun BiliPaiBottomBarSearchSlot(",
            "private fun BiliPaiBottomBarSearchSlot("
        )
        for (marker in cutMarkers) {
            if (after.contains(marker)) {
                return after.substringBefore(marker)
            }
        }
        return after
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText().replace("\r\n", "\n")
    }
}
