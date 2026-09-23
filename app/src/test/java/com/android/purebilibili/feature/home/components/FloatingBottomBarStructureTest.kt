package com.android.purebilibili.feature.home.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Structure assertions for [FloatingBottomBar].
 * Interactions must use [com.android.purebilibili.feature.home.components.miuix.DampedDragAnimation],
 * not design-system DampedDragAnimationState / horizontalDragGesture.
 */
class FloatingBottomBarStructureTest {

    @Test
    fun `segmented lens records a full rounded blurred surface`() {
        val source = loadFloatingBottomBarSource()
        assertTrue(source.contains("maxOf(shellHeight, fittedIndicatorHeight)"))
        assertFalse(source.contains("(shellHeight - verticalPadding * 2).coerceAtLeast(0.dp)"))
        assertTrue(source.contains("shape = { pillShape }"))
        assertFalse(source.contains("if (segmentedGeometry) androidx.compose.ui.graphics.RectangleShape else pillShape"))
        assertTrue(source.contains("indicatorIdleSurfaceColorOverride != null || isLiquidGlassMode"))
    }

    @Test
    fun `public API exposes BiliPai layout knobs`() {
        val source = loadFloatingBottomBarSource()

        assertTrue(source.contains("val LocalFloatingBottomBarContentColor"))
        assertTrue(source.contains("val LocalFloatingBottomBarTabScale"))
        assertTrue(source.contains("class FloatingBottomBarColors("))
        assertTrue(source.contains("object FloatingBottomBarDefaults"))
        assertTrue(source.contains("enum class FloatingBottomBarMode"))
        assertTrue(source.contains("LiquidGlass,"))
        assertTrue(source.contains("Blur,"))
        assertTrue(source.contains("None"))
        assertTrue(source.contains("fun RowScope.FloatingBottomBarItem("))
        assertTrue(source.contains("fun FloatingBottomBar("))
        assertTrue(source.contains("shellHeight: Dp = FloatingBottomBarDefaultShellHeight"))
        assertTrue(source.contains("indicatorHeight: Dp = FloatingBottomBarIndicatorHeight"))
        assertTrue(source.contains("dragTrackingMode: DampedDragTrackingMode = DampedDragTrackingMode.SPRING"))
        assertTrue(source.contains("FloatingBottomBarDefaultShellHeight: Dp = 56.dp"))
        assertTrue(source.contains("FloatingBottomBarIndicatorHeight: Dp = 56.dp"))
        assertTrue(source.contains("BottomBarReferencePressedScale"))
    }

    @Test
    fun `compact segmented controls share the home spring drag tracking`() {
        val source = loadFloatingBottomBarSource()
        val segmentedSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBarFloatingSegmentedControl.kt"
        )

        assertTrue(source.contains("dragTrackingMode: DampedDragTrackingMode = DampedDragTrackingMode.SPRING"))
        assertTrue(segmentedSource.contains("dragTrackingMode = DampedDragTrackingMode.SPRING"))
    }

    @Test
    fun `three layer liquid glass structure is present`() {
        val source = loadFloatingBottomBarSource()
        val body = source.substringAfter("fun FloatingBottomBar(")

        assertTrue(body.contains(".dropShadow("))
        assertTrue(body.contains("vibrancy(liquidGlassTuning.saturation)"))
        assertTrue(body.contains("liquidGlassTuning.backdropBlurRadius.dp.toPx()"))
        assertTrue(body.contains("padding = maxOf("))
        assertTrue(body.contains("refractionHeight = shellRefractionHeightPx"))
        assertTrue(body.contains("refractionAmount = shellRefractionAmountPx"))
        assertTrue(body.contains("baseHighlight.copy(alpha = 0.75f)"))

        assertTrue(body.contains(".alpha(0f)"))
        assertTrue(body.contains(".then(tabsBackdropSource.modifier)"))
        assertTrue(body.contains("rememberChromeBackdropSource()"))

        assertTrue(body.contains("rememberCombinedBackdrop(backdrop, tabsBackdrop)"))
        assertTrue(body.contains("depthEffect = true"))
        assertTrue(body.contains("resolveLiquidGlassIndicatorChromaticAberration("))
        assertTrue(body.contains("refractionHeight = indicatorLensHeightPx * progress"))
        assertTrue(body.contains("refractionAmount = indicatorLensAmountPx * progress"))
        assertTrue(body.contains("LiquidGlassReadabilityMode.ADAPTIVE"))
        assertTrue(body.contains("rememberLiquidGlassAdaptiveReadabilityState("))
        assertTrue(body.contains("trackLiquidGlassAdaptiveReadability("))
        assertTrue(body.contains("LocalFloatingBottomBarContentColor provides resolvedContentColor"))
        assertTrue(body.contains("LocalFloatingBottomBarContentColor provides colors.activeContentColor"))
        assertTrue(body.contains("LocalFloatingBottomBarItemAlignmentOffset provides itemAlignmentOffsetProvider"))
        assertTrue(body.contains("translationX = itemIndex?.let(alignmentOffset) ?: 0f"))
        assertFalse(body.contains("translationX = panelOffset + if (isLtr) alignmentPx"))
        assertFalse(body.contains("resolvedActiveContentColor"))
        assertTrue(body.contains(".innerShadow(shape = pillShape)"))
        assertTrue(body.contains("InnerShadow("))
        assertTrue(body.contains("radius = innerShadowRadius * dampedDragAnimation.pressProgress"))
    }

    @Test
    fun `drag uses local DampedDragAnimation not design-system horizontalDragGesture`() {
        val source = loadFloatingBottomBarSource()
        val dragPort = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/miuix/DampedDragAnimation.kt"
        )
        val body = source.substringAfter("fun FloatingBottomBar(")
        val baseRow = body
            .substringAfter("LocalFloatingBottomBarContentColor provides resolvedContentColor")
            .substringBefore("if (isLiquidGlassMode && backdrop != null)")
        val movingIndicator = body.substringAfter("if (tabWidthPx > 0f)")

        assertTrue(source.contains("import com.android.purebilibili.feature.home.components.miuix.DampedDragAnimation"))
        assertTrue(body.contains("DampedDragAnimation("))
        assertTrue(body.contains("pressedScale = pressedScale"))
        assertTrue(body.contains("val pressedScale = matchedGeometry.pressedScale"))
        assertTrue(body.contains("floatingDockScaleOverflow("))
        assertTrue(body.contains("overflow = scaleOverflowDp"))
        assertTrue(body.contains("shellHeight = shellHeight"))
        assertTrue(body.contains("dampedDragAnimation.press()"))
        assertTrue(body.contains("dampedDragAnimation.release()"))
        assertTrue(body.contains("canDrag = { offset ->"))
        assertTrue(body.contains("shouldAcceptFloatingDockDragAtWindowX("))
        assertTrue(body.contains("fittedIndicatorHeight"))
        assertTrue(body.contains("onDragStarted = {"))
        assertTrue(body.contains("pagerFollowGate.ownedTargetIndex = null"))
        assertTrue(body.contains("onDragStopped = {"))
        assertTrue(body.contains("updateValue("))
        assertTrue(body.contains("selectedIndexLatest.value().coerceIn(0, maxTabIndex),"))
        assertTrue(body.contains("dampedDragAnimation.isDragging"))
        assertTrue(body.contains("shouldAnimateIndicatorToSelectedIndex("))
        assertTrue(body.contains("shouldSuppressExternalPagerIndicatorFollow("))
        assertTrue(body.contains("resolveIndicatorOwnedTargetOnDragStop("))
        assertTrue(body.contains("isPagerScrolling = scrolling"))
        assertFalse(body.contains("snapshotFlow { currentIndex }"))
        assertFalse(body.contains(".drop(1)"))
        val dragStopBody = body.substringAfter("onDragStopped = {").substringBefore("onDrag = {")
        assertEquals(1, Regex("animateToValue\\(").findAll(dragStopBody).count())
        assertTrue(dragStopBody.contains("animatePress = false"))
        assertTrue(dragStopBody.contains("onSelectedLatest.value(targetIndex)"))
        assertTrue(dragStopBody.contains("pagerFollowGate.ownedTargetIndex = resolveIndicatorOwnedTargetOnDragStop("))
        assertFalse(baseRow.contains("interactiveHighlight.gestureModifier"))
        assertFalse(baseRow.contains(".then(dampedDragAnimation.modifier)"))
        assertTrue(movingIndicator.contains("interactiveHighlight?.gestureModifier"))
        assertTrue(movingIndicator.contains(".then(dampedDragAnimation.modifier)"))
        assertTrue(body.contains("offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))"))
        val dragRememberKeys = body
            .substringAfter("val dampedDragAnimation = remember(")
            .substringBefore(") {")
        assertFalse(dragRememberKeys.contains("matchedGeometry.pressedScale"))
        assertTrue(body.contains("dampedDragAnimation.pressedScale = pressedScale"))
        assertTrue(body.contains("remember(animationScope) {"))
        assertFalse(body.contains("remember(animationScope, tabWidthPx)"))
        assertTrue(dragPort.contains("releaseJob?.cancel()"))
        // 照搬 HyperIsland：release 直接接管放大动画，不等待 pressJob 跑完，
        // 否则缩放会先到峰值再停住、之后才缩回（放大 → 停顿 → 缩小）。
        assertFalse(dragPort.contains("pressJob?.join()"))
        assertFalse(dragPort.contains("isInside && wasInside"))
        assertTrue(body.contains("resolveFloatingDockIndicatorLayerScaleX("))
        assertTrue(body.contains("LocalFloatingBottomBarIndicatorStretchX provides indicatorStretchXProvider"))
        assertTrue(body.contains("resolveFloatingDockCapturedContentHorizontalScale("))
        // Must not fall back to BiliPai self-developed drag stack.
        assertFalse(source.contains("rememberDampedDragAnimationState"))
        assertFalse(source.contains("horizontalDragGesture"))
        assertTrue(source.contains("DampedDragTrackingMode"))

        assertTrue(dragPort.contains("spring(1f, 1000f, visibilityThreshold)"))
        assertTrue(dragPort.contains("spring(0.5f, 300f, visibilityThreshold * 10f)"))
        assertTrue(dragPort.contains("spring(1f, 1000f, 0.001f)"))
        assertTrue(dragPort.contains("spring(0.6f, 250f, 0.001f)"))
        assertTrue(dragPort.contains("spring(0.7f, 250f, 0.001f)"))
        assertTrue(dragPort.contains("inspectDragGestures("))
        assertTrue(dragPort.contains("val modifier: Modifier = Modifier.pointerInput(Unit)"))
        assertTrue(dragPort.contains("var gestureAccepted = false"))
        assertTrue(dragPort.contains("gestureAccepted = canDrag(down.position)"))
        assertTrue(dragPort.contains("if (!gestureAccepted) return@inspectDragGestures"))
        assertTrue(dragPort.contains("private var requestedValue = initialValue.coerceIn(valueRange)"))
        assertTrue(dragPort.contains("private var valueTrackingJob: Job? = null"))
        assertTrue(dragPort.contains("valueTrackingJob?.cancel()"))
        assertTrue(dragPort.contains("private fun launchValueTracking("))
        assertTrue(dragPort.contains("start = CoroutineStart.UNDISPATCHED"))
        assertTrue(dragPort.contains("launchValueTracking { valueAnimation.snapTo(next) }"))
        assertFalse(
            dragPort.contains(
                "launch { valueAnimation.animateTo(targetValue, valueAnimationSpec) { updateVelocity() } }"
            )
        )
        assertTrue(dragPort.contains("val targetValue: Float get() = requestedValue"))
        assertTrue(dragPort.contains("requestedValue = targetValue"))
        assertTrue(dragPort.contains("trackingMode == DampedDragTrackingMode.DIRECT"))
        assertTrue(dragPort.contains("valueAnimation.snapTo(targetValue)"))
        assertTrue(dragPort.contains("animatePress: Boolean = true"))
        assertTrue(dragPort.contains("if (animatePress) press()"))
        assertTrue(dragPort.contains("if (animatePress) release()"))
        assertTrue(dragPort.contains("onDragStopped()"))
        val dragEndBody = dragPort.substringAfter("onDragEnd = {").substringBefore("onDragCancel = {")
        assertTrue(dragEndBody.indexOf("onDragStopped()") < dragEndBody.indexOf("isDragging = false"))
        assertFalse(dragPort.contains("onDragCancelled"))
    }

    @Test
    fun `gravity highlight rubber band and InteractiveHighlight are wired together`() {
        val source = loadFloatingBottomBarSource()

        assertTrue(source.contains("fun rememberGravityRotatedHighlight("))
        assertTrue(source.contains("rememberDeviceTilt()"))
        assertTrue(source.contains("LIGHT_REF_X = 0.5f"))
        assertTrue(source.contains("LIGHT_REF_Y = 0.7f"))
        assertTrue(source.contains("GRAVITY_DIR_THRESHOLD_SQ = 0.01f"))
        assertTrue(source.contains("extraDegrees = -45f"))
        assertTrue(source.contains("extraDegrees = 90f"))
        assertTrue(source.contains("val offsetAnimation = remember { Animatable(0f) }"))
        assertTrue(source.contains("rubberBandPx"))
        assertTrue(source.contains("EaseOut.transform(abs(fraction))"))
        assertTrue(source.contains("4.dp.toPx()"))
        assertTrue(source.contains("InteractiveHighlight("))
        assertTrue(source.contains("resolveDockInteractiveHighlightRadiusPx("))
        assertTrue(source.contains("resolveDockPillHighlightWidthDp("))
        assertTrue(source.contains("Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU"))
        assertTrue(source.contains("selectedIndexLatest.value().coerceIn(0, maxTabIndex) to"))
        assertTrue(source.contains("onSelectedLatest.value(targetIndex)"))
        assertFalse(source.contains("pendingUserSelectedIndex"))
    }

    @Test
    fun `item provides dual LocalContentColor and tab scale`() {
        val source = loadFloatingBottomBarSource()
        val item = source
            .substringAfter("fun RowScope.FloatingBottomBarItem(")
            .substringBefore("fun FloatingBottomBar(")

        assertTrue(item.contains("LocalFloatingBottomBarTabScale.current"))
        assertTrue(item.contains("LocalFloatingBottomBarContentColor.current"))
        assertTrue(item.contains("MiuixLocalContentColor provides contentColor"))
        assertTrue(item.contains("M3LocalContentColor provides contentColor"))
        assertTrue(item.contains("role = Role.Tab"))
        assertTrue(item.contains(".semantics {"))
        assertTrue(item.contains(".clickable("))
        assertTrue(item.contains("onClick = onClick"))
        assertFalse(item.contains(".onKeyEvent { event ->"))
        assertTrue(item.contains(".weight(1f)"))
    }

    @Test
    fun `selected indicator forwards taps without replacing drag handling`() {
        val source = loadFloatingBottomBarSource()
        val indicatorSource = source.substringAfter("if (tabWidthPx > 0f)")

        assertTrue(source.contains("onReselected: () -> Unit = {}"))
        assertTrue(indicatorSource.contains(".then(dampedDragAnimation.modifier)"))
        assertTrue(indicatorSource.contains("onClick = onReselected"))
        assertTrue(indicatorSource.contains(".clearAndSetSemantics {}"))
    }

    @Test
    fun `selected indicator hit target stays inside logical tab slot`() {
        val source = loadFloatingBottomBarSource()
        val indicatorSource = source.substringAfter("if (tabWidthPx > 0f)")
        val hitTarget = indicatorSource.substringAfter(
            "Keep pointer input in the logical tab slot"
        )

        assertTrue(hitTarget.contains("val slotOffsetPx = dampedDragAnimation.value * tabWidthPx"))
        assertTrue(hitTarget.contains(".width(tabWidthDp)"))
        assertTrue(hitTarget.contains("onClick = onReselected"))
        assertFalse(
            indicatorSource
                .substringBefore("Keep pointer input in the logical tab slot")
                .contains("dampedDragAnimation.modifier")
        )
    }

    @Test
    fun `widened indicator centers item content in every visual mode`() {
        val source = loadFloatingBottomBarSource()
        val alignmentProvider = source
            .substringAfter("val itemAlignmentOffsetProvider")
            .substringBefore("LaunchedEffect(dampedDragAnimation, maxTabIndex, isLiquidGlassMode)")
        val nonLiquidIndicator = source
            .substringAfter("if (isLiquidGlassMode && combinedBackdrop != null)")
            .substringBefore("// The selected capsule can be wider than its tab")
            .substringAfterLast("} else {")

        assertTrue(alignmentProvider.contains("resolveFloatingDockIndicatorContentAlignmentPx("))
        assertFalse(alignmentProvider.contains("!isLiquidGlassMode"))
        assertTrue(
            nonLiquidIndicator.contains(
                "LocalFloatingBottomBarItemAlignmentOffset provides itemAlignmentOffsetProvider"
            )
        )
    }

    @Test
    fun `non liquid capsule does not reuse liquid indicator movement deformation`() {
        val source = loadFloatingBottomBarSource()
        val nonLiquidIndicator = source
            .substringAfter("if (isLiquidGlassMode && combinedBackdrop != null)")
            .substringBefore("// The selected capsule can be wider than its tab")
            .substringAfterLast("} else {")

        assertFalse(nonLiquidIndicator.contains("scaleX = dampedDragAnimation.scaleX"))
        assertFalse(nonLiquidIndicator.contains("scaleY = dampedDragAnimation.scaleY"))
        assertFalse(nonLiquidIndicator.contains("val velocity = dampedDragAnimation.velocity / 10f"))
    }

    @Test
    fun `all indicator materials keep animated settling`() {
        val source = loadFloatingBottomBarSource()
        val selectionSync = source
            .substringAfter("shouldAnimateIndicatorToSelectedIndex(")
            .substringBefore(
                "LaunchedEffect(dampedDragAnimation, maxTabIndex)",
                missingDelimiterValue = ""
            )

        assertTrue(selectionSync.contains("dampedDragAnimation.animateToValue(index.toFloat())"))
        assertFalse(selectionSync.contains("dampedDragAnimation.snapTo(index.toFloat())"))
        assertFalse(source.contains("dampedDragAnimation.longPressModifier"))
        assertTrue(source.contains("(dragSelectionEnabled || longPressDragSelectionEnabled) && safeTabsCount > 1"))
        assertFalse(source.contains("if (isLiquidGlassMode && dragSelectionEnabled"))
    }

    @Test
    fun `selected dock label fades under the indicator in every visual mode`() {
        val source = loadFloatingBottomBarSource()
        val alphaProvider = source
            .substringAfter("val baseContentAlphaProvider")
            .substringBefore("LaunchedEffect(dampedDragAnimation, maxTabIndex)")

        assertTrue(source.contains("LocalFloatingBottomBarBaseContentAlpha"))
        assertTrue(source.contains("LocalFloatingBottomBarActiveContent.current"))
        assertTrue(source.contains("baseContentAlpha(itemIndex)"))
        assertTrue(source.contains("1f - coverage"))
        assertTrue(!alphaProvider.contains("if (isLiquidGlassMode)"))
        assertTrue(
            source.contains(
                "LocalFloatingBottomBarBaseContentAlpha provides baseContentAlphaProvider"
            )
        )
    }

    @Test
    fun `home icons scale continuously with indicator coverage without settle pulse`() {
        val source = loadFloatingBottomBarSource()
        val body = source.substringAfter("fun FloatingBottomBar(")

        assertTrue(source.contains("LocalFloatingBottomBarIndicatorPosition"))
        assertTrue(source.contains("LocalFloatingBottomBarItemSelectionScale"))
        assertTrue(source.contains("itemIndex: Int? = null"))
        assertTrue(source.contains("resolveNavigationIconCrossScale("))
        assertFalse(body.contains("indicatorSettlePulseKey"))
        assertFalse(body.contains("rememberNavigationIndicatorSettleTransform("))

        val bottomBar = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt"
        )
        assertTrue(bottomBar.contains("itemIndex = index"))
        assertTrue(bottomBar.contains("LocalFloatingBottomBarItemSelectionScale.current"))
    }

    @Test
    fun `caller width constrains dock before intrinsic measurement`() {
        val source = loadFloatingBottomBarSource()
        val body = source.substringAfter("fun FloatingBottomBar(")

        assertTrue(body.contains("modifier = modifier,"))
        assertFalse(body.contains("modifier = modifier.width(IntrinsicSize.Min)"))
    }

    @Test
    fun `pager-backed liquid docks share FloatingBottomBar follow gating`() {
        val video = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/screen/VideoContentSection.kt"
        )
        val search = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/search/SearchScreen.kt"
        )
        val listen = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/audio/screen/ListenVideoScreen.kt"
        )
        val music = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/audio/screen/MusicPlayerContent.kt"
        )
        val dynamic = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicTopBar.kt"
        )
        val floating = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBarFloatingSegmentedControl.kt"
        )

        assertTrue(video.contains("BottomBarLiquidSegmentedControl("))
        assertTrue(video.contains("pagerState.currentPage + pagerState.currentPageOffsetFraction"))
        assertTrue(search.contains("BottomBarLiquidSegmentedControl("))
        assertTrue(search.contains("pagerState.currentPage + pagerState.currentPageOffsetFraction"))
        assertTrue(listen.contains("BottomBarLiquidSegmentedControl("))
        assertTrue(listen.contains("pagerState.currentPage + pagerState.currentPageOffsetFraction"))
        assertTrue(music.contains("indicatorPositionProvider = {"))
        assertTrue(dynamic.contains("indicatorPositionProvider = indicatorPositionProvider"))
        assertTrue(floating.contains("FloatingBottomBar("))
        assertTrue(floating.contains("indicatorPositionProvider = indicatorPositionProvider"))
    }

    @Test
    fun `no dual-render bridge stub remains`() {
        val source = loadFloatingBottomBarSource()

        assertFalse(source.contains("FloatingBottomBarHost("))
        assertFalse(source.contains("FloatingBottomBarBridge("))
        assertFalse(source.contains("结构占位"))
        assertFalse(source.contains("委托给 BiliPai 现有的 BiliPaiFloatingBottomBar"))
    }

    private fun loadFloatingBottomBarSource(): String =
        loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/FloatingBottomBar.kt")

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(File(path), File(normalizedPath)).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText().replace("\r\n", "\n")
    }
}
