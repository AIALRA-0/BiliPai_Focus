package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.android.purebilibili.core.ui.motion.AppMotionEasing
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.core.util.rememberHapticFeedback
import kotlinx.coroutines.launch

enum class GesturePercentTransitionDirection {
    None,
    Increase,
    Decrease
}

fun resolveGesturePercentTransitionDirection(
    previousPercent: Int,
    currentPercent: Int
): GesturePercentTransitionDirection {
    val previous = previousPercent.coerceIn(0, 100)
    val current = currentPercent.coerceIn(0, 100)
    return when {
        current > previous -> GesturePercentTransitionDirection.Increase
        current < previous -> GesturePercentTransitionDirection.Decrease
        else -> GesturePercentTransitionDirection.None
    }
}

fun shouldTriggerGesturePercentHaptic(
    previousPercent: Int,
    currentPercent: Int,
    stepPercent: Int = 5
): Boolean {
    if (stepPercent <= 0) return false
    val previous = previousPercent.coerceIn(0, 100)
    val current = currentPercent.coerceIn(0, 100)
    if (previous == current) return false
    if (current == 0 || current == 100) return true
    return if (current > previous) {
        previous / stepPercent != current / stepPercent
    } else {
        (previous - 1).coerceAtLeast(0) / stepPercent !=
            (current - 1).coerceAtLeast(0) / stepPercent
    }
}

internal object GesturePercentMotionDefaults {
    const val InitialBlurRadiusDp = 10f
    const val InitialAlpha = 0.42f
    const val BlurHoldDurationMillis = 40
    const val BlurResetDurationMillis = 260
    const val AlphaResetDurationMillis = 220
    const val EnterFadeDurationMillis = 220
    const val ExitFadeDurationMillis = 140
    const val SlideSpringDampingRatio = 0.82f
    const val SlideSpringStiffness = 520f

    fun <T> slideSpring(): SpringSpec<T> = spring(
        dampingRatio = SlideSpringDampingRatio,
        stiffness = SlideSpringStiffness
    )
}

@Composable
fun AnimatedGesturePercentText(
    percent: Int,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
    label: String = "gesture-percent-blur-fade"
) {
    val normalizedPercent = percent.coerceIn(0, 100)
    val blurAnim = remember { Animatable(0f) }
    val alphaAnim = remember { Animatable(1f) }
    var initialized by remember { mutableStateOf(false) }
    var previousPercent by remember { mutableIntStateOf(normalizedPercent) }
    val haptic = rememberHapticFeedback()

    LaunchedEffect(normalizedPercent) {
        if (!initialized) {
            initialized = true
            previousPercent = normalizedPercent
            return@LaunchedEffect
        }
        if (shouldTriggerGesturePercentHaptic(previousPercent, normalizedPercent)) {
            haptic(HapticType.SELECTION)
        }
        previousPercent = normalizedPercent
        blurAnim.snapTo(GesturePercentMotionDefaults.InitialBlurRadiusDp)
        alphaAnim.snapTo(GesturePercentMotionDefaults.InitialAlpha)
        launch {
            blurAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = GesturePercentMotionDefaults.BlurResetDurationMillis,
                    delayMillis = GesturePercentMotionDefaults.BlurHoldDurationMillis,
                    easing = AppMotionEasing.EmphasizedEnter
                )
            )
        }
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = GesturePercentMotionDefaults.AlphaResetDurationMillis,
                easing = AppMotionEasing.EmphasizedEnter
            )
        )
    }

    AnimatedContent(
        targetState = normalizedPercent,
        transitionSpec = {
            val direction = resolveGesturePercentTransitionDirection(initialState, targetState)
            val enterOffset: (Int) -> Int = { height ->
                when (direction) {
                    GesturePercentTransitionDirection.Increase -> height / 2
                    GesturePercentTransitionDirection.Decrease -> -height / 2
                    GesturePercentTransitionDirection.None -> 0
                }
            }
            val exitOffset: (Int) -> Int = { height ->
                when (direction) {
                    GesturePercentTransitionDirection.Increase -> -height / 2
                    GesturePercentTransitionDirection.Decrease -> height / 2
                    GesturePercentTransitionDirection.None -> 0
                }
            }
            (slideInVertically(
                animationSpec = GesturePercentMotionDefaults.slideSpring(),
                initialOffsetY = enterOffset
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = GesturePercentMotionDefaults.EnterFadeDurationMillis,
                    easing = AppMotionEasing.EmphasizedEnter
                )
            ) togetherWith
                slideOutVertically(
                    animationSpec = GesturePercentMotionDefaults.slideSpring(),
                    targetOffsetY = exitOffset
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = GesturePercentMotionDefaults.ExitFadeDurationMillis,
                        easing = AppMotionEasing.EmphasizedExit
                    )
                )) using
                SizeTransform(clip = false)
        },
        label = label
    ) { targetPercent ->
        Text(
            text = "$targetPercent%",
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            modifier = modifier
                .alpha(alphaAnim.value)
                .blur(
                    radius = blurAnim.value.dp,
                    edgeTreatment = BlurredEdgeTreatment.Unbounded
                )
        )
    }
}
