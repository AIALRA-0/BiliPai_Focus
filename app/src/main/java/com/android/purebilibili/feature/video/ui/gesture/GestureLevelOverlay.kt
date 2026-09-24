package com.android.purebilibili.feature.video.ui.gesture

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import com.android.purebilibili.core.ui.components.AppIcon
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.android.purebilibili.core.ui.components.AppSlider
import com.android.purebilibili.core.ui.components.AppSliderDefaults
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.motion.AppMotionTokens
import com.android.purebilibili.core.ui.rememberAppPlayerChromeProfile
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.feature.video.ui.components.AnimatedGesturePercentText
import com.android.purebilibili.feature.video.ui.components.CircularGesturePercentText
import com.android.purebilibili.feature.video.ui.components.shouldTriggerGesturePercentHaptic
import com.android.purebilibili.feature.video.ui.section.VideoGestureMode
import com.android.purebilibili.feature.video.ui.section.resolveVideoGestureMotionSpec
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Theme-native volume / brightness feedback:
 * - MD3: centered, theme-colored circular indicator
 * - iOS: centered frosted capsule
 * - MIUIX: native animated horizontal slider at the top of the player
 */
@Composable
fun BoxScope.GestureLevelOverlayHost(
    visible: Boolean,
    mode: VideoGestureMode,
    percent: Float,
    modifier: Modifier = Modifier
) {
    val kind = resolveGestureLevelKind(mode) ?: return
    val playerChromeProfile = rememberAppPlayerChromeProfile()
    val style = rememberGestureLevelOverlayStyle(playerChromeProfile.tabPresentation)
    val motionSpec = remember { resolveVideoGestureMotionSpec() }
    val colorScheme = MaterialTheme.colorScheme
    val miuixColorScheme = MiuixTheme.colorScheme
    val spec = remember(style, kind, percent, colorScheme, miuixColorScheme) {
        resolveGestureLevelOverlaySpec(
            style = style,
            kind = kind,
            percent = percent,
            colorScheme = colorScheme,
            miuixContainerColor = miuixColorScheme.surfaceContainerHigh,
            miuixContentColor = miuixColorScheme.onSurface
        )
    }
    val progress by animateFloatAsState(
        targetValue = percent.coerceIn(0f, 1f),
        animationSpec = tween(motionSpec.levelProgressDurationMillis),
        label = "gesture-level-progress"
    )
    val icon = resolveGestureLevelIcon(style = style, kind = kind, percent = percent)
    val percentInt = (percent.coerceIn(0f, 1f) * 100f).roundToInt().coerceIn(0, 100)
    GestureLevelStepHaptics(
        style = style,
        kind = kind,
        percent = percentInt,
        active = visible
    )

    AnimatedVisibility(
        visible = visible,
        modifier = modifier
            .align(spec.alignment)
            .then(
                when (style) {
                    GestureLevelOverlayStyle.Md3 -> Modifier
                    GestureLevelOverlayStyle.Miuix -> Modifier
                    GestureLevelOverlayStyle.Ios -> Modifier.padding(horizontal = 22.dp)
                }
            )
            .zIndex(40f),
        enter = fadeIn(animationSpec = tween(motionSpec.levelOverlayEnterFadeDurationMillis)) +
            scaleIn(
                initialScale = if (style == GestureLevelOverlayStyle.Miuix) 0.92f else 0.84f,
                animationSpec = tween(motionSpec.levelOverlayEnterTransformDurationMillis)
            ) +
            slideInVertically(
                initialOffsetY = { if (style == GestureLevelOverlayStyle.Ios) it / 8 else 0 },
                animationSpec = tween(motionSpec.levelOverlayEnterTransformDurationMillis)
            ),
        exit = fadeOut(animationSpec = tween(motionSpec.levelOverlayExitDurationMillis)) +
            scaleOut(
                targetScale = 0.92f,
                animationSpec = tween(motionSpec.levelOverlayExitDurationMillis)
            ) +
            slideOutVertically(
                targetOffsetY = { if (style == GestureLevelOverlayStyle.Ios) -it / 10 else 0 },
                animationSpec = tween(motionSpec.levelOverlayExitDurationMillis)
            )
    ) {
        when (style) {
            GestureLevelOverlayStyle.Md3 -> Md3GestureLevelIndicator(
                spec = spec,
                icon = icon,
                progress = { progress },
                percent = percentInt
            )
            GestureLevelOverlayStyle.Ios -> IosGestureLevelCapsule(
                spec = spec,
                icon = icon,
                progress = progress,
                percent = percentInt
            )
            GestureLevelOverlayStyle.Miuix -> MiuixGestureLevelSlider(
                spec = spec,
                icon = icon,
                progress = progress
            )
        }
    }
}

@Composable
private fun Md3GestureLevelIndicator(
    spec: GestureLevelOverlaySpec,
    icon: ImageVector,
    progress: () -> Float,
    percent: Int,
    modifier: Modifier = Modifier
) {
    // Use player bounds, not device orientation: embedded and split-screen players
    // can have much less height than the window in either orientation.
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val diameter = resolveMd3GestureLevelDiameterDp(maxWidth.value, maxHeight.value)
        val compact = diameter < 112f
        Box(
            modifier = Modifier.size(diameter.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularWavyProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = resolveGestureLevelLabel(spec.kind) },
                color = spec.fillColor,
                trackColor = spec.trackColor
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                GestureLevelIconSlot(
                    icon = icon,
                    tint = spec.iconTint,
                    sizeDp = if (compact) 18 else spec.iconSizeDp,
                    glowColor = spec.accentColor.copy(alpha = 0.12f)
                )
                key(spec.kind) {
                    CircularGesturePercentText(
                        percent = percent,
                        color = spec.textColor,
                        textStyle = if (compact) {
                            MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        } else {
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun IosGestureLevelCapsule(
    spec: GestureLevelOverlaySpec,
    icon: ImageVector,
    progress: Float,
    percent: Int
) {
    val shape = AppShapes.container(ContainerLevel.Pill)
    AppSurface(
        shape = shape,
        color = spec.containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, spec.borderColor),
        shadowElevation = 10.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = spec.capsuleMinWidthDp.dp, max = 188.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GestureLevelIconSlot(
                icon = icon,
                tint = spec.accentColor,
                sizeDp = spec.iconSizeDp,
                glowColor = spec.accentColor.copy(alpha = 0.34f)
            )
            if (spec.showLabel) {
                AppText(
                    text = resolveGestureLevelLabel(spec.kind),
                    color = Color.White.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
            AnimatedGesturePercentText(
                percent = percent,
                color = spec.textColor,
                fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                fontWeight = FontWeight.Bold,
                label = "ios-gesture-level-percent",
                // Host-level GestureLevelStepHaptics already ticks for all themes.
                enableHaptic = false
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(spec.trackColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    spec.fillColor.copy(alpha = 0.7f),
                                    spec.fillColor
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun MiuixGestureLevelSlider(
    spec: GestureLevelOverlaySpec,
    icon: ImageVector,
    progress: Float
) {
    val standardMotion = AppMotionTokens.standardSpec<Float>()
    val emphasizedMotion = AppMotionTokens.emphasizedSpec<Float>()
    val expressiveMotion = AppMotionTokens.expressiveSpec<Float>()
    val sliderColors = AppSliderDefaults.colors(
        thumbColor = spec.iconTint,
        activeTrackColor = spec.fillColor,
        inactiveTrackColor = spec.containerColor,
    )
    Row(
        modifier = Modifier.padding(top = spec.topInsetDp.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size((spec.iconSizeDp + 14).dp)
                .background(spec.containerColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = icon,
                transitionSpec = {
                    (fadeIn(standardMotion) +
                        scaleIn(initialScale = 0.82f, animationSpec = emphasizedMotion))
                        .togetherWith(
                            fadeOut(expressiveMotion) +
                                scaleOut(
                                    targetScale = 1.12f,
                                    animationSpec = standardMotion
                                )
                        )
                },
                label = "miuix-gesture-icon"
            ) { target ->
                AppIcon(
                    imageVector = target,
                    contentDescription = null,
                    tint = spec.iconTint,
                    modifier = Modifier.size(spec.iconSizeDp.dp)
                )
            }
        }
        AppSlider(
            value = progress,
            onValueChange = {},
            modifier = Modifier
                .width(spec.railWidthDp.dp)
                .height(spec.railHeightDp.dp)
                .semantics { contentDescription = resolveGestureLevelLabel(spec.kind) },
            enabled = false,
            colors = sliderColors
        )
    }
}

@Composable
private fun GestureLevelIconSlot(
    icon: ImageVector,
    tint: Color,
    sizeDp: Int,
    glowColor: Color
) {
    val standardMotion = AppMotionTokens.standardSpec<Float>()
    val emphasizedMotion = AppMotionTokens.emphasizedSpec<Float>()
    val expressiveMotion = AppMotionTokens.expressiveSpec<Float>()
    Box(
        modifier = Modifier.size((sizeDp + 16).dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(glowColor, CircleShape)
                .graphicsLayer { alpha = 0.9f }
        )
        AnimatedContent(
            targetState = icon,
            transitionSpec = {
                (fadeIn(standardMotion) +
                    scaleIn(initialScale = 0.8f, animationSpec = emphasizedMotion))
                    .togetherWith(
                        fadeOut(expressiveMotion) +
                            scaleOut(
                                targetScale = 1.15f,
                                animationSpec = standardMotion
                            )
                    )
            },
            label = "gesture-level-icon"
        ) { target ->
            AppIcon(
                imageVector = target,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(sizeDp.dp)
            )
        }
    }
}

/**
 * Stepped haptics for all three skins while dragging volume / brightness.
 * MD3: throttled 5% ticks with boundary confirmation. iOS: every 5%.
 * MIUIX: every ~7% (closer to system stream steps).
 */
@Composable
private fun GestureLevelStepHaptics(
    style: GestureLevelOverlayStyle,
    kind: GestureLevelKind,
    percent: Int,
    active: Boolean
) {
    val haptic = rememberHapticFeedback()
    if (style == GestureLevelOverlayStyle.Md3) {
        val policy = remember(kind) { Md3GestureLevelHapticPolicy() }
        LaunchedEffect(active, percent, policy) {
            when (policy.update(percent, active, SystemClock.uptimeMillis())) {
                GestureLevelHapticFeedback.Tick -> haptic(HapticType.SELECTION)
                GestureLevelHapticFeedback.Boundary -> haptic(HapticType.LIGHT)
                null -> Unit
            }
        }
        return
    }
    var previousPercent by remember { mutableIntStateOf(percent) }
    val stepPercent = when (style) {
        GestureLevelOverlayStyle.Miuix -> 7
        GestureLevelOverlayStyle.Md3 -> 5
        GestureLevelOverlayStyle.Ios -> 5
    }
    LaunchedEffect(active, percent, style) {
        if (!active) {
            previousPercent = percent
            return@LaunchedEffect
        }
        if (
            shouldTriggerGesturePercentHaptic(
                previousPercent = previousPercent,
                currentPercent = percent,
                stepPercent = stepPercent
            )
        ) {
            haptic(
                when (style) {
                    GestureLevelOverlayStyle.Miuix -> HapticType.LIGHT
                    GestureLevelOverlayStyle.Md3 -> HapticType.SELECTION
                    GestureLevelOverlayStyle.Ios -> HapticType.SELECTION
                }
            )
        }
        previousPercent = percent
    }
}

/** Convenience for non-BoxScope hosts (fullscreen / bangumi / offline). */
@Composable
fun GestureLevelOverlayContent(
    mode: VideoGestureMode,
    percent: Float,
    style: GestureLevelOverlayStyle,
    modifier: Modifier = Modifier
) {
    val kind = resolveGestureLevelKind(mode) ?: return
    val motionSpec = remember { resolveVideoGestureMotionSpec() }
    val colorScheme = MaterialTheme.colorScheme
    val miuixColorScheme = MiuixTheme.colorScheme
    val spec = remember(style, kind, percent, colorScheme, miuixColorScheme) {
        resolveGestureLevelOverlaySpec(
            style = style,
            kind = kind,
            percent = percent,
            colorScheme = colorScheme,
            miuixContainerColor = miuixColorScheme.surfaceContainerHigh,
            miuixContentColor = miuixColorScheme.onSurface
        )
    }
    val progress by animateFloatAsState(
        targetValue = percent.coerceIn(0f, 1f),
        animationSpec = tween(motionSpec.levelProgressDurationMillis),
        label = "gesture-level-progress-content"
    )
    val icon = resolveGestureLevelIcon(style = style, kind = kind, percent = percent)
    val percentInt = (percent.coerceIn(0f, 1f) * 100f).roundToInt().coerceIn(0, 100)
    GestureLevelStepHaptics(
        style = style,
        kind = kind,
        percent = percentInt,
        active = true
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (style) {
            GestureLevelOverlayStyle.Md3 -> Md3GestureLevelIndicator(
                spec = spec,
                icon = icon,
                progress = { progress },
                percent = percentInt
            )
            GestureLevelOverlayStyle.Ios -> IosGestureLevelCapsule(
                spec = spec,
                icon = icon,
                progress = progress,
                percent = percentInt
            )
            GestureLevelOverlayStyle.Miuix -> MiuixGestureLevelSlider(
                spec = spec,
                icon = icon,
                progress = progress
            )
        }
    }
}
