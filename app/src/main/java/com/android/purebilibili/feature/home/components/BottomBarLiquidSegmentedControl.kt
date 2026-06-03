package com.android.purebilibili.feature.home.components

import androidx.compose.animation.core.EaseOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.store.resolveEffectiveLiquidGlassEnabled
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.animation.DampedDragAnimationState
import com.android.purebilibili.core.ui.animation.rememberDampedDragAnimationState
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.blur.currentUnifiedBlurIntensity
import com.android.purebilibili.core.ui.motion.BottomBarMotionProfile
import com.android.purebilibili.core.ui.motion.BottomBarMotionSpec
import com.android.purebilibili.core.ui.motion.MotionSpringConfig
import com.android.purebilibili.core.ui.motion.resolveBottomBarMotionSpec
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

internal fun resolveSegmentedControlLiquidGlassEnabled(
    storedLiquidGlassEnabled: Boolean,
    liquidGlassEffectsEnabled: Boolean,
    uiPreset: UiPreset,
    androidNativeLiquidGlassEnabled: Boolean
): Boolean {
    return liquidGlassEffectsEnabled && resolveEffectiveLiquidGlassEnabled(
        requestedEnabled = storedLiquidGlassEnabled,
        uiPreset = uiPreset,
        androidNativeLiquidGlassEnabled = androidNativeLiquidGlassEnabled
    )
}

internal enum class SegmentedControlChromeStyle {
    LIQUID_PILL,
    ANDROID_NATIVE_UNDERLINE
}

internal const val BOTTOM_BAR_LIQUID_SEGMENTED_CONTROL_HEIGHT_DP = 58
internal const val BOTTOM_BAR_LIQUID_SEGMENTED_CONTROL_INDICATOR_HEIGHT_DP = 56
private const val SEGMENTED_CONTROL_MIN_INDICATOR_ASPECT_RATIO = 1.6f

internal fun resolveSegmentedControlChromeStyle(
    uiPreset: UiPreset,
    androidNativeLiquidGlassEnabled: Boolean,
    preferInlineContentStyle: Boolean = false
): SegmentedControlChromeStyle {
    return if (uiPreset == UiPreset.MD3 && (preferInlineContentStyle || !androidNativeLiquidGlassEnabled)) {
        SegmentedControlChromeStyle.ANDROID_NATIVE_UNDERLINE
    } else {
        SegmentedControlChromeStyle.LIQUID_PILL
    }
}

internal fun resolveSegmentedControlIndicatorWidthDp(
    slotWidthDp: Float,
    indicatorHeightDp: Float,
    itemCount: Int
): Float {
    if (slotWidthDp <= 0f || indicatorHeightDp <= 0f || itemCount <= 0) return 0f
    return slotWidthDp
}

internal fun resolveSegmentedControlIndicatorHeightDp(
    slotWidthDp: Float,
    indicatorHeightDp: Float
): Float {
    if (slotWidthDp <= 0f || indicatorHeightDp <= 0f) return 0f
    return min(
        indicatorHeightDp,
        slotWidthDp / SEGMENTED_CONTROL_MIN_INDICATOR_ASPECT_RATIO
    )
}

internal fun resolveSegmentedControlIndicatorOffsetDp(
    position: Float,
    slotWidthDp: Float,
    contentPaddingDp: Float
): Float {
    return contentPaddingDp + (slotWidthDp * position)
}

internal fun shouldFollowSegmentedControlIndicatorDrag(
    pointerX: Float,
    indicatorPosition: Float,
    itemWidthPx: Float
): Boolean {
    if (itemWidthPx <= 0f) return false
    val startX = indicatorPosition * itemWidthPx
    val endX = startX + itemWidthPx
    return pointerX in startX..endX
}

internal fun resolveSegmentedControlSweepSelectionIndex(
    pointerX: Float,
    itemWidthPx: Float,
    itemCount: Int
): Int {
    if (itemWidthPx <= 0f || itemCount <= 0) return 0
    return (pointerX.coerceAtLeast(0f) / itemWidthPx)
        .toInt()
        .coerceIn(0, itemCount - 1)
}

internal fun shouldDrawSegmentedControlIndicatorBackdrop(
    liquidGlassEnabled: Boolean,
    motionProgress: Float,
    hasExternalBackdrop: Boolean
): Boolean {
    if (!liquidGlassEnabled) return false
    return hasExternalBackdrop || motionProgress > 0.001f
}

@Composable
internal fun BottomBarLiquidIndicatorSurface(
    modifier: Modifier = Modifier,
    shape: Shape = resolveSharedBottomBarCapsuleShape(),
    liquidGlassEnabled: Boolean,
    backdrop: Backdrop? = null,
    hasExternalBackdrop: Boolean = backdrop != null,
    indicatorLensSpec: BottomBarBackdropPresetLensSpec = resolveBottomBarBackdropPresetIndicatorLens(
        progress = if (liquidGlassEnabled) 1f else 0f
    ),
    indicatorHighlightAlpha: Float = resolveBottomBarLiquidGlassHighlightAlpha(
        motionProgress = if (liquidGlassEnabled) 1f else 0f
    ),
    indicatorGlowAlpha: Float = resolveBottomBarIndicatorGlowAlpha(
        glassEnabled = liquidGlassEnabled,
        pressProgress = 0f
    ),
    motionProgress: Float = 0f,
    idleSurfaceColor: Color = Color.Unspecified,
    layerBlock: GraphicsLayerScope.() -> Unit = {}
) {
    val resolvedIdleSurfaceColor = if (idleSurfaceColor == Color.Unspecified) {
        resolveAndroidNativeIdleIndicatorSurfaceColor(darkTheme = isSystemInDarkTheme())
    } else {
        idleSurfaceColor
    }
    Box(
        modifier = modifier.run {
            if (backdrop != null && shouldDrawSegmentedControlIndicatorBackdrop(
                    liquidGlassEnabled = liquidGlassEnabled,
                    motionProgress = motionProgress,
                    hasExternalBackdrop = hasExternalBackdrop
                )
            ) {
                drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        lens(
                            refractionHeight = indicatorLensSpec.refractionHeightDp.dp.toPx(),
                            refractionAmount = indicatorLensSpec.refractionAmountDp.dp.toPx(),
                            depthEffect = true,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        Highlight.Default.copy(alpha = maxOf(indicatorHighlightAlpha, indicatorGlowAlpha))
                    },
                    shadow = {
                        Shadow(alpha = indicatorGlowAlpha)
                    },
                    innerShadow = {
                        InnerShadow(
                            radius = 8.dp * indicatorGlowAlpha,
                            alpha = indicatorGlowAlpha
                        )
                    },
                    layerBlock = layerBlock,
                    onDrawSurface = {
                        drawRect(
                            color = resolvedIdleSurfaceColor,
                            alpha = 1f - motionProgress
                        )
                        drawRect(Color.Black.copy(alpha = 0.03f * motionProgress))
                    }
                )
            } else {
                background(resolvedIdleSurfaceColor, shape)
            }
        }
    )
}

internal fun resolveSegmentedControlMotionProgress(
    pressProgress: Float,
    refractionProgress: Float,
    tapPressRefractionEnabled: Boolean
): Float {
    val resolvedPressProgress = if (tapPressRefractionEnabled) pressProgress else 0f
    return maxOf(resolvedPressProgress, refractionProgress)
}

internal fun resolveSegmentedControlMotionSpec(): BottomBarMotionSpec {
    val base = resolveBottomBarMotionSpec(profile = BottomBarMotionProfile.ANDROID_NATIVE_FLOATING)
    return base.copy(
        drag = base.drag.copy(
            selectionSpring = MotionSpringConfig(
                dampingRatio = 0.88f,
                stiffness = 320f
            ),
            offsetSnapSpring = MotionSpringConfig(
                dampingRatio = 0.84f,
                stiffness = 300f
            )
        ),
        refraction = base.refraction.copy(
            speedProgressDivisorPxPerSecond = 1700f,
            dragProgressFloor = 0.14f,
            panelOffsetMaxDp = 3f
        ),
        indicator = base.indicator.copy(
            scaleSpring = MotionSpringConfig(
                dampingRatio = 0.58f,
                stiffness = 420f
            ),
            dragScaleSpring = MotionSpringConfig(
                dampingRatio = 0.64f,
                stiffness = 320f
            ),
            lensVelocityRangePxPerSecond = 3200f,
            capsuleVelocityNormalizationDivisor = 14f,
            capsuleVelocityScaleXMultiplier = 0.52f,
            capsuleVelocityScaleYMultiplier = 0.18f,
            capsuleVelocityClamp = 0.14f
        )
    )
}

private fun Modifier.segmentedControlSelectionGesture(
    dragState: DampedDragAnimationState,
    itemWidthPx: Float,
    itemCount: Int,
    currentSelectedIndex: Int,
    onSweepSelected: (Int) -> Unit,
    shouldFollowIndicatorFrom: (Float) -> Boolean
): Modifier = this.pointerInput(
    dragState,
    itemWidthPx,
    itemCount,
    currentSelectedIndex,
    shouldFollowIndicatorFrom
) {
    val velocityTracker = VelocityTracker()

    awaitPointerEventScope {
        while (true) {
            val down = awaitFirstDown(requireUnconsumed = false)
            val shouldFollowIndicator = shouldFollowIndicatorFrom(down.position.x)
            var latestPositionX = down.position.x
            velocityTracker.resetTracking()
            velocityTracker.addPosition(down.uptimeMillis, down.position)

            val dragStart = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                latestPositionX = change.position.x
                change.consume()
                if (shouldFollowIndicator) {
                    dragState.onDrag(over, itemWidthPx)
                }
            }

            if (dragStart != null) {
                velocityTracker.addPosition(dragStart.uptimeMillis, dragStart.position)

                var isCancelled = false
                try {
                    horizontalDrag(dragStart.id) { change ->
                        change.consume()
                        latestPositionX = change.position.x
                        velocityTracker.addPosition(change.uptimeMillis, change.position)

                        if (shouldFollowIndicator) {
                            val dragAmount = change.position.x - change.previousPosition.x
                            dragState.onDrag(dragAmount, itemWidthPx)
                        }
                    }
                } catch (e: Exception) {
                    isCancelled = true
                }

                if (shouldFollowIndicator) {
                    val velocityX = if (isCancelled) {
                        0f
                    } else {
                        velocityTracker.calculateVelocity().x
                    }
                    dragState.onDragEnd(
                        velocityX = velocityX,
                        itemWidthPx = itemWidthPx,
                        settleIndex = null,
                        notifyIndexChanged = true
                    )
                } else if (!isCancelled) {
                    val releaseIndex = resolveSegmentedControlSweepSelectionIndex(
                        pointerX = latestPositionX,
                        itemWidthPx = itemWidthPx,
                        itemCount = itemCount
                    )
                    if (releaseIndex != currentSelectedIndex) {
                        onSweepSelected(releaseIndex)
                    }
                }
            }
        }
    }
}

@Composable
fun BottomBarLiquidSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    itemWidth: Dp? = null,
    height: Dp = BOTTOM_BAR_LIQUID_SEGMENTED_CONTROL_HEIGHT_DP.dp,
    indicatorHeight: Dp = BOTTOM_BAR_LIQUID_SEGMENTED_CONTROL_INDICATOR_HEIGHT_DP.dp,
    labelFontSize: TextUnit = 14.sp,
    containerHorizontalPadding: Dp = 3.dp,
    containerVerticalPadding: Dp = 3.dp,
    liquidGlassEffectsEnabled: Boolean = true,
    dragSelectionEnabled: Boolean = true,
    preferInlineContentStyle: Boolean = false,
    forceLiquidChrome: Boolean = false,
    backdrop: Backdrop? = null,
    tapPressRefractionEnabled: Boolean = true,
    onIndicatorPositionChanged: ((Float) -> Unit)? = null
) {
    if (items.isEmpty()) return

    val context = LocalContext.current
    val uiPreset = LocalUiPreset.current
    val homeSettings by SettingsManager
        .getHomeSettings(context)
        .collectAsStateWithLifecycle(initialValue = HomeSettings(),
            context = kotlin.coroutines.EmptyCoroutineContext
        )
    val effectiveAndroidNativeLiquidGlassEnabled =
        forceLiquidChrome || homeSettings.androidNativeLiquidGlassEnabled
    val chromeStyle = resolveSegmentedControlChromeStyle(
        uiPreset = uiPreset,
        androidNativeLiquidGlassEnabled = effectiveAndroidNativeLiquidGlassEnabled,
        preferInlineContentStyle = preferInlineContentStyle
    )
    if (chromeStyle == SegmentedControlChromeStyle.ANDROID_NATIVE_UNDERLINE) {
        AndroidNativeUnderlinedSegmentedControl(
            items = items,
            selectedIndex = selectedIndex,
            onSelected = onSelected,
            modifier = modifier,
            enabled = enabled,
            itemWidth = itemWidth,
            height = height,
            labelFontSize = labelFontSize,
            onIndicatorPositionChanged = onIndicatorPositionChanged
        )
        return
    }

    val liquidGlassEnabled = resolveSegmentedControlLiquidGlassEnabled(
        storedLiquidGlassEnabled = homeSettings.isBottomBarLiquidGlassEnabled,
        liquidGlassEffectsEnabled = liquidGlassEffectsEnabled,
        uiPreset = uiPreset,
        androidNativeLiquidGlassEnabled = effectiveAndroidNativeLiquidGlassEnabled
    )
    val blurIntensity = currentUnifiedBlurIntensity()
    val density = LocalDensity.current
    val itemCount = items.size
    val safeSelectedIndex = selectedIndex.coerceIn(0, itemCount - 1)
    val motionSpec = remember { resolveSegmentedControlMotionSpec() }
    val clickPulseKey = remember { mutableIntStateOf(0) }
    val clickPulseTransform = rememberBottomBarClickPulseTransform(clickPulseKey.intValue)
    val dragState = rememberDampedDragAnimationState(
        initialIndex = safeSelectedIndex,
        itemCount = itemCount,
        motionSpec = motionSpec,
        onIndexChanged = { index ->
            if (enabled && index in items.indices) {
                onSelected(index)
            }
        }
    )
    val indicatorShape = resolveSharedBottomBarCapsuleShape()
    val containerShape = AppShapes.container(ContainerLevel.Pill)
    val indicatorCorner = indicatorHeight / 2
    val isDarkTheme = isSystemInDarkTheme()
    val surfaceColor = AppSurfaceTokens.cardContainer()
    val androidNativeTuning = resolveAndroidNativeBottomBarTuning(
        blurEnabled = liquidGlassEnabled,
        darkTheme = isDarkTheme
    )
    val containerColor = resolveAndroidNativeFloatingBottomBarContainerColor(
        surfaceColor = surfaceColor,
        tuning = androidNativeTuning,
        glassEnabled = liquidGlassEnabled,
        blurEnabled = liquidGlassEnabled,
        blurIntensity = blurIntensity,
        liquidGlassPreset = homeSettings.bottomBarLiquidGlassPreset
    )
    val selectedTextColor = MaterialTheme.colorScheme.primary
    val unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.78f else 0.42f)
    val exportTintColor = resolveAndroidNativeExportTintColor(
        themeColor = selectedTextColor,
        darkTheme = isDarkTheme
    )
    fun selectFromTap(index: Int) {
        if (!enabled || index !in items.indices) return
        clickPulseKey.intValue += 1
        onSelected(index)
    }
    LaunchedEffect(safeSelectedIndex) {
        dragState.updateIndex(safeSelectedIndex)
    }

    BoxWithConstraints(
        modifier = modifier
            .then(
                if (itemWidth != null) {
                    Modifier.width((itemWidth.value * itemCount).dp + containerHorizontalPadding * 2)
                } else {
                    Modifier.fillMaxWidth()
                }
            )
            .height(height)
    ) {
        val contentPadding = containerHorizontalPadding
        val contentVerticalInset = containerVerticalPadding
        val slotWidth = (maxWidth - (contentPadding * 2)) / itemCount
        val indicatorWidth = resolveSegmentedControlIndicatorWidthDp(
            slotWidthDp = slotWidth.value,
            indicatorHeightDp = indicatorHeight.value,
            itemCount = itemCount
        ).dp
        val resolvedIndicatorHeight = resolveSegmentedControlIndicatorHeightDp(
            slotWidthDp = slotWidth.value,
            indicatorHeightDp = indicatorHeight.value
        ).dp
        val indicatorOffset = resolveSegmentedControlIndicatorOffsetDp(
            position = dragState.value,
            slotWidthDp = slotWidth.value,
            contentPaddingDp = contentPadding.value
        ).dp
        val itemWidthPx = with(density) { slotWidth.toPx() }.coerceAtLeast(1f)
        val dragModifier = if (enabled && itemCount > 1 && dragSelectionEnabled) {
            Modifier.segmentedControlSelectionGesture(
                dragState = dragState,
                itemWidthPx = itemWidthPx,
                itemCount = itemCount,
                currentSelectedIndex = safeSelectedIndex,
                onSweepSelected = { index ->
                    if (index != safeSelectedIndex) {
                        onSelected(index)
                    }
                },
                shouldFollowIndicatorFrom = { downX ->
                    shouldFollowSegmentedControlIndicatorDrag(
                        pointerX = downX,
                        indicatorPosition = dragState.value,
                        itemWidthPx = itemWidthPx
                    )
                }
            )
        } else {
            Modifier
        }
        val indicatorPosition = dragState.value
        SideEffect {
            onIndicatorPositionChanged?.invoke(indicatorPosition)
        }
        val pressMotionProgress by remember {
            derivedStateOf { dragState.pressProgress }
        }
        val refractionMotionProfile = resolveBottomBarRefractionMotionProfile(
            position = indicatorPosition,
            velocity = dragState.velocityPxPerSecond,
            isDragging = dragState.isDragging,
            motionSpec = motionSpec
        )
        val motionProgress = resolveSegmentedControlMotionProgress(
            pressProgress = pressMotionProgress,
            refractionProgress = refractionMotionProfile.progress,
            tapPressRefractionEnabled = tapPressRefractionEnabled
        )
        val tapPressProgress = if (tapPressRefractionEnabled) pressMotionProgress else 0f
        val indicatorDragScaleProgress = rememberBottomBarIndicatorDragScaleProgress(
            isDragging = dragState.isDragging
        )
        val indicatorLayerScaleProgress = maxOf(indicatorDragScaleProgress, tapPressProgress)
        val indicatorLayerScaleTransform = BottomBarIndicatorLayerTransform(
            scaleX = dragState.scaleX,
            scaleY = dragState.scaleY
        )
        val panelOffsetPx by remember(density, itemWidthPx) {
            derivedStateOf {
                val fraction = (dragState.dragOffset / itemWidthPx).coerceIn(-1f, 1f)
                with(density) {
                    motionSpec.refraction.panelOffsetMaxDp.dp.toPx() *
                        fraction.sign *
                        EaseOut.transform(abs(fraction))
                }
            }
        }
        val tabsBackdrop = rememberLayerBackdrop()
        val containerBackdrop = backdrop ?: tabsBackdrop
        val contentBackdrop = tabsBackdrop
        val backdropPresetProgress = resolveBottomBarBackdropPresetProgress(
            motionProgress = motionProgress,
            verticalProgress = 0f,
            pressProgress = tapPressProgress
        )
        val captureLensSpec = resolveBottomBarBackdropPresetCaptureLens(
            progress = backdropPresetProgress.captureProgress
        )
        val indicatorLensSpec = resolveBottomBarBackdropPresetIndicatorLens(
            progress = backdropPresetProgress.indicatorProgress
        )
        val captureHighlightAlpha = resolveBottomBarLiquidGlassHighlightAlpha(
            backdropPresetProgress.captureProgress
        )
        val indicatorHighlightAlpha = resolveBottomBarLiquidGlassHighlightAlpha(
            backdropPresetProgress.indicatorProgress
        )
        val indicatorGlowAlpha = resolveBottomBarIndicatorGlowAlpha(
            glassEnabled = liquidGlassEnabled,
            pressProgress = tapPressProgress,
            motionProgress = motionProgress
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .kernelSuFloatingDockSurface(
                    shape = containerShape,
                    backdrop = backdrop,
                    containerColor = containerColor,
                    blurEnabled = liquidGlassEnabled,
                    glassEnabled = liquidGlassEnabled,
                    blurRadius = androidNativeTuning.shellBlurRadiusDp.dp,
                    hazeState = null,
                    motionTier = MotionTier.Normal,
                    isTransitionRunning = false,
                    forceLowBlurBudget = false,
                    liquidGlassPreset = homeSettings.bottomBarLiquidGlassPreset
                )
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clearAndSetSemantics {}
                .alpha(0f)
                .layerBackdrop(tabsBackdrop)
                .graphicsLayer { translationX = panelOffsetPx }
                .run {
                    if (backdrop != null && liquidGlassEnabled) {
                        drawBackdrop(
                            backdrop = containerBackdrop,
                            shape = { containerShape },
                            effects = {
                                vibrancy()
                                blur(androidNativeTuning.shellBlurRadiusDp.dp.toPx())
                                lens(
                                    refractionHeight = captureLensSpec.refractionHeightDp.dp.toPx(),
                                    refractionAmount = captureLensSpec.refractionAmountDp.dp.toPx(),
                                    depthEffect = true,
                                    chromaticAberration = true
                                )
                            },
                            highlight = {
                                Highlight.Default.copy(alpha = captureHighlightAlpha)
                            },
                            onDrawSurface = {
                                drawRect(containerColor)
                            }
                        )
                    } else {
                        this
                    }
                }
                .graphicsLayer(colorFilter = ColorFilter.tint(exportTintColor))
        ) {
            BottomBarLiquidSegmentedLabels(
                items = items,
                selectedIndex = safeSelectedIndex,
                indicatorPosition = indicatorPosition,
                motionProgress = motionProgress,
                selectionEmphasis = refractionMotionProfile.exportSelectionEmphasis,
                selectedTextColor = selectedTextColor,
                unselectedTextColor = selectedTextColor,
                enabled = enabled,
                labelFontSize = labelFontSize,
                indicatorCorner = indicatorCorner,
                onSelected = onSelected,
                interactive = false,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = contentPadding, vertical = contentVerticalInset)
            )
        }

        KernelSuBottomBarIndicatorLayer(
            visible = true,
            dockContentAlpha = 1f,
            indicatorTranslationXPx = with(density) { indicatorOffset.toPx() },
            indicatorPanelOffsetPx = panelOffsetPx,
            indicatorSettleReboundTransform = clickPulseTransform,
            indicatorWidth = indicatorWidth,
            indicatorHeight = resolvedIndicatorHeight,
            shellShape = indicatorShape,
            liquidGlassPreset = homeSettings.bottomBarLiquidGlassPreset,
            contentBackdrop = contentBackdrop,
            backdrop = backdrop,
            indicatorLensSpec = indicatorLensSpec,
            effectivePressProgress = tapPressProgress,
            indicatorIdleSurfaceColor = if (isDarkTheme) Color.White.copy(0.1f) else Color.Black.copy(0.1f),
            glassEnabled = liquidGlassEnabled,
            motionProgress = motionProgress,
            velocityItemsPerSecond = dragState.deformationVelocityItemsPerSecond,
            isDragging = dragState.isDragging,
            indicatorLayerScaleProgress = indicatorLayerScaleProgress,
            indicatorLayerScaleTransform = indicatorLayerScaleTransform,
            bottomBarMotionSpec = motionSpec,
            isDarkTheme = isDarkTheme
        )

        BottomBarLiquidSegmentedLabels(
            items = items,
            selectedIndex = safeSelectedIndex,
            indicatorPosition = indicatorPosition,
            motionProgress = motionProgress,
            selectionEmphasis = refractionMotionProfile.visibleSelectionEmphasis,
            selectedTextColor = selectedTextColor,
            unselectedTextColor = unselectedTextColor,
            enabled = enabled,
            labelFontSize = labelFontSize,
            indicatorCorner = indicatorCorner,
            onSelected = onSelected,
            interactive = false,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = contentPadding, vertical = contentVerticalInset)
                .graphicsLayer { translationX = panelOffsetPx }
        )

        BottomBarLiquidSegmentedLabels(
            items = items,
            selectedIndex = safeSelectedIndex,
            indicatorPosition = indicatorPosition,
            motionProgress = motionProgress,
            selectionEmphasis = refractionMotionProfile.visibleSelectionEmphasis,
            selectedTextColor = selectedTextColor,
            unselectedTextColor = unselectedTextColor,
            enabled = enabled,
            labelFontSize = labelFontSize,
            indicatorCorner = indicatorCorner,
            onSelected = ::selectFromTap,
            interactive = true,
            onPressChanged = dragState::setPressed,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = contentPadding, vertical = contentVerticalInset)
                .alpha(0f)
                .graphicsLayer { translationX = panelOffsetPx }
                .then(dragModifier)
        )
    }
}

@Composable
private fun AndroidNativeUnderlinedSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    itemWidth: Dp? = null,
    height: Dp,
    labelFontSize: TextUnit,
    onIndicatorPositionChanged: ((Float) -> Unit)? = null
) {
    val itemCount = items.size
    val safeSelectedIndex = selectedIndex.coerceIn(0, itemCount - 1)
    val selectedTextColor = MaterialTheme.colorScheme.primary
    val unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.78f else 0.42f)
    val underlineShape = CircleShape

    SideEffect {
        onIndicatorPositionChanged?.invoke(safeSelectedIndex.toFloat())
    }

    BoxWithConstraints(
        modifier = modifier
            .then(
                if (itemWidth != null) {
                    Modifier.width(itemWidth * itemCount)
                } else {
                    Modifier.fillMaxWidth()
                }
            )
            .height(height)
    ) {
        val segmentWidth = maxWidth / itemCount
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, label ->
                val selected = index == safeSelectedIndex
                Box(
                    modifier = Modifier
                        .width(segmentWidth)
                        .fillMaxHeight()
                        .clickable(enabled = enabled) { onSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) selectedTextColor else unselectedTextColor,
                        fontSize = labelFontSize,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (selected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .width(segmentWidth * 0.42f)
                                .widthIn(min = 28.dp, max = 56.dp)
                                .height(3.dp)
                                .clip(underlineShape)
                                .background(selectedTextColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBarLiquidSegmentedLabels(
    items: List<String>,
    selectedIndex: Int,
    indicatorPosition: Float,
    motionProgress: Float,
    selectionEmphasis: Float,
    selectedTextColor: Color,
    unselectedTextColor: Color,
    enabled: Boolean,
    labelFontSize: TextUnit,
    indicatorCorner: Dp,
    onSelected: (Int) -> Unit,
    interactive: Boolean,
    onPressChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, label ->
            val interactionSource = remember { MutableInteractionSource() }
            if (interactive && onPressChanged != null) {
                val pressed by interactionSource.collectIsPressedAsState()
                LaunchedEffect(pressed) {
                    onPressChanged(pressed)
                }
            }
            val visual = resolveBottomBarItemMotionVisual(
                itemIndex = index,
                indicatorPosition = indicatorPosition,
                currentSelectedIndex = selectedIndex,
                motionProgress = motionProgress,
                selectionEmphasis = selectionEmphasis
            )
            val textColor = if (enabled) {
                androidx.compose.ui.graphics.lerp(
                    start = unselectedTextColor,
                    stop = selectedTextColor,
                    fraction = visual.themeWeight
                )
            } else {
                unselectedTextColor.copy(alpha = 0.44f)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(indicatorCorner))
                    .then(
                        if (interactive) {
                            Modifier.clickable(
                                enabled = enabled,
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                onSelected(index)
                            }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = textColor,
                    fontSize = labelFontSize,
                    fontWeight = if (visual.themeWeight > 0.5f) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Medium
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.graphicsLayer {
                        scaleX = 1f
                        scaleY = 1f
                    }
                )
            }
        }
    }
}
