package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.rounded.ThumbUp
import com.android.purebilibili.core.ui.components.AppIcon
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.ui.AppIcons
import com.android.purebilibili.core.plugin.skin.LocalUiSkinState
import com.android.purebilibili.core.plugin.skin.UiSkinAnimatedAsset
import com.android.purebilibili.core.plugin.skin.UiSkinSurface
import com.android.purebilibili.core.plugin.skin.assetPath
import com.android.purebilibili.feature.video.ui.feedback.resolveTripleActionMotionSpec
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun LikeBurstAnimation(
    visible: Boolean,
    reducedMotion: Boolean = false,
    onAnimationEnd: () -> Unit = {}
) {
    if (!visible) return

    val uiSkinState = LocalUiSkinState.current
    val skinLikeEffectPath = uiSkinState.assetPath(UiSkinSurface.LIKE_EFFECT) {
        it.likeEffectAnimation ?: it.likeEffectPreview
    }
    if (skinLikeEffectPath != null) {
        LaunchedEffect(skinLikeEffectPath, visible) {
            delay(if (reducedMotion) 300 else 800)
            onAnimationEnd()
        }
        UiSkinAnimatedAsset(
            path = skinLikeEffectPath,
            size = if (reducedMotion) 72.dp else 88.dp,
            iterations = 1,
            contentDescription = null,
        )
        return
    }

    val progress = remember { Animatable(0f) }
    val durationMillis = if (reducedMotion) 260 else 420

    LaunchedEffect(visible, reducedMotion) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
        )
        delay(80)
        onAnimationEnd()
    }

    val primary = MaterialTheme.colorScheme.primary
    val currentProgress = progress.value
    val iconScale by animateFloatAsState(
        targetValue = if (currentProgress < 0.45f) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "likeBurstScale"
    )

    Box(
        modifier = Modifier.size(if (reducedMotion) 72.dp else 88.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val pulseRadius = size.minDimension * (0.18f + currentProgress * 0.24f)
            val ringRadius = size.minDimension * (0.2f + currentProgress * 0.28f)
            val glowAlpha = (1f - currentProgress).coerceIn(0f, 1f) * if (reducedMotion) 0.28f else 0.5f
            val ringAlpha = (1f - currentProgress).coerceIn(0f, 1f) * 0.9f

            drawCircle(
                color = primary.copy(alpha = glowAlpha),
                radius = pulseRadius,
                center = center
            )
            drawCircle(
                color = primary.copy(alpha = ringAlpha),
                radius = ringRadius,
                center = center,
                style = Stroke(width = size.minDimension * 0.045f)
            )
        }

        AppIcon(
            imageVector = Icons.Rounded.ThumbUp,
            contentDescription = null,
            tint = primary.copy(alpha = 0.98f),
            modifier = Modifier
                .size(if (reducedMotion) 26.dp else 30.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                    alpha = 1f - (currentProgress * 0.14f)
                }
        )
    }
}

@Composable
fun TripleSuccessAnimation(
    visible: Boolean,
    isCompact: Boolean = false,
    reducedMotion: Boolean = false,
    onAnimationEnd: () -> Unit = {}
) {
    if (!visible) return

    val motionSpec = resolveTripleActionMotionSpec(reducedMotion = reducedMotion)
    val totalDurationMillis =
        motionSpec.activationDurationMillis +
            motionSpec.convergenceDurationMillis +
            motionSpec.resolutionDurationMillis +
            motionSpec.dwellDurationMillis
    val progress = remember { Animatable(0f) }

    LaunchedEffect(visible, isCompact, reducedMotion) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = totalDurationMillis, easing = FastOutSlowInEasing)
        )
        delay(120)
        onAnimationEnd()
    }

    val totalDurationFloat = totalDurationMillis.toFloat().coerceAtLeast(1f)
    val activationEnd = motionSpec.activationDurationMillis / totalDurationFloat
    val convergenceEnd =
        (motionSpec.activationDurationMillis + motionSpec.convergenceDurationMillis) / totalDurationFloat
    val resolutionEnd =
        (
            motionSpec.activationDurationMillis +
                motionSpec.convergenceDurationMillis +
                motionSpec.resolutionDurationMillis
            ) / totalDurationFloat

    val currentProgress = progress.value
    val activationProgress = phaseProgress(currentProgress, 0f, activationEnd)
    val convergenceProgress = phaseProgress(currentProgress, activationEnd, convergenceEnd)
    val resolutionProgress = phaseProgress(currentProgress, convergenceEnd, resolutionEnd)
    val dissolveProgress = phaseProgress(currentProgress, resolutionEnd, 1f)
    val primary = MaterialTheme.colorScheme.primary
    val accent = Color.White
    val baseDistance = if (isCompact) 84f else 102f
    val baseYOffset = if (isCompact) 58f else 72f
    val iconSize = if (isCompact) 30.dp else 34.dp
    val containerWidth = if (isCompact) 260.dp else 320.dp
    val containerHeight = if (isCompact) 200.dp else 240.dp
    val badgeScale = 0.74f + (resolutionProgress * 0.4f) - (dissolveProgress * 0.08f)
    val badgeAlpha = (resolutionProgress * (1f - dissolveProgress * 0.45f)).coerceIn(0f, 1f)
    val trailAlpha = (convergenceProgress * (1f - dissolveProgress * 0.7f)).coerceIn(0f, 1f) * 0.82f

    Box(
        modifier = Modifier
            .width(containerWidth)
            .height(containerHeight),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val iconStarts = listOf(
                Offset(center.x - baseDistance.dp.toPx(), center.y + baseYOffset.dp.toPx()),
                Offset(center.x, center.y - (baseYOffset + 18f).dp.toPx()),
                Offset(center.x + baseDistance.dp.toPx(), center.y + baseYOffset.dp.toPx())
            )

            if (!reducedMotion) {
                iconStarts.forEach { start ->
                    val end = center.copy(y = center.y - 6.dp.toPx())
                    val currentX = lerp(start.x, end.x, convergenceProgress)
                    val currentY = lerp(start.y, end.y, convergenceProgress)
                    drawCircle(
                        color = primary.copy(alpha = trailAlpha * 0.72f),
                        radius = if (isCompact) 5.dp.toPx() else 6.dp.toPx(),
                        center = Offset(currentX, currentY)
                    )
                }
            }

            if (badgeAlpha > 0f) {
                val ringRadius = if (isCompact) 42.dp.toPx() else 48.dp.toPx()
                val burstAlpha = (resolutionProgress * (1f - dissolveProgress)).coerceIn(0f, 1f)

                for (index in 0 until 18) {
                    val angle = (index * 20f) * (Math.PI / 180f).toFloat()
                    val distance = ringRadius * (1.15f + resolutionProgress * 0.9f)
                    val particleCenter = Offset(
                        x = center.x + kotlin.math.cos(angle) * distance,
                        y = center.y + kotlin.math.sin(angle) * distance
                    )
                    drawCircle(
                        color = accent.copy(alpha = burstAlpha * 0.9f),
                        radius = if (index % 3 == 0) 4.dp.toPx() else 2.8.dp.toPx(),
                        center = particleCenter
                    )
                }

                drawCircle(
                    color = primary.copy(alpha = badgeAlpha * 0.18f),
                    radius = ringRadius * (1.55f - dissolveProgress * 0.12f),
                    center = center
                )
                drawCircle(
                    color = primary.copy(alpha = badgeAlpha * 0.95f),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = if (isCompact) 3.dp.toPx() else 3.5.dp.toPx())
                )
                drawCircle(
                    color = primary.copy(alpha = badgeAlpha * 0.88f),
                    radius = ringRadius * 0.78f,
                    center = center
                )
                drawCircle(
                    color = accent.copy(alpha = badgeAlpha * 0.95f),
                    radius = ringRadius * 0.48f,
                    center = center
                )
            }
        }

        if (!reducedMotion) {
            TripleActionIcon(
                image = Icons.Rounded.ThumbUp,
                tint = primary,
                baseX = -baseDistance,
                baseY = baseYOffset,
                activationProgress = iconActivationProgress(activationProgress, 0),
                convergenceProgress = convergenceProgress,
                dissolveProgress = dissolveProgress,
                iconSize = iconSize
            )
            TripleActionIcon(
                image = AppIcons.BiliCoin,
                tint = primary,
                baseX = 0f,
                baseY = -(baseYOffset + 18f),
                activationProgress = iconActivationProgress(activationProgress, 1),
                convergenceProgress = convergenceProgress,
                dissolveProgress = dissolveProgress,
                iconSize = iconSize
            )
            TripleActionIcon(
                image = Icons.Outlined.StarBorder,
                tint = primary,
                baseX = baseDistance,
                baseY = baseYOffset,
                activationProgress = iconActivationProgress(activationProgress, 2),
                convergenceProgress = convergenceProgress,
                dissolveProgress = dissolveProgress,
                iconSize = iconSize
            )
        }

        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = badgeScale
                scaleY = badgeScale
                alpha = badgeAlpha
            },
            contentAlignment = Alignment.Center
        ) {
            AppIcon(
                imageVector = Icons.Rounded.ThumbUp,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(if (isCompact) 26.dp else 30.dp)
            )
        }

        AppText(
            text = "三连完成!",
            color = accent.copy(alpha = badgeAlpha),
            fontSize = if (isCompact) 22.sp else 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = if (isCompact) (-20).dp else (-28).dp)
                .graphicsLayer {
                    alpha = badgeAlpha
                    translationY = (1f - resolutionProgress).coerceIn(0f, 1f) * 18f
                }
        )
        AppText(
            text = "点赞  投币  收藏",
            color = accent.copy(alpha = badgeAlpha * 0.9f),
            fontSize = if (isCompact) 12.sp else 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = if (isCompact) 2.dp else 8.dp)
                .graphicsLayer {
                    alpha = badgeAlpha
                    translationY = (1f - resolutionProgress).coerceIn(0f, 1f) * 12f
                }
        )
    }
}

@Composable
private fun BoxScope.TripleActionIcon(
    image: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    baseX: Float,
    baseY: Float,
    activationProgress: Float,
    convergenceProgress: Float,
    dissolveProgress: Float,
    iconSize: androidx.compose.ui.unit.Dp
) {
    val currentX = lerp(baseX, 0f, convergenceProgress)
    val currentY = lerp(baseY, 0f, convergenceProgress)
    val alpha = (activationProgress * (1f - dissolveProgress * 0.85f)).coerceIn(0f, 1f)
    val scale = 0.78f + (activationProgress * 0.24f) - (convergenceProgress * 0.06f)

    AppIcon(
        imageVector = image,
        contentDescription = null,
        tint = tint.copy(alpha = alpha),
        modifier = Modifier
            .align(Alignment.Center)
            .offset {
                IntOffset(currentX.roundToInt(), currentY.roundToInt())
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .size(iconSize)
    )
}

@Composable
fun CoinSuccessAnimation(
    visible: Boolean,
    coinCount: Int = 1,
    onAnimationEnd: () -> Unit = {}
) {
    if (!visible) return

    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            animatedProgress.snapTo(0f)
            animatedProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
            delay(100)
            onAnimationEnd()
        }
    }

    val progress = animatedProgress.value
    val scale by animateFloatAsState(
        targetValue = if (progress < 0.5f) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "coinScale"
    )

    Box(
        modifier = Modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        AppIcon(
            imageVector = AppIcons.BiliCoin,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 1f - progress * 0.12f),
            modifier = Modifier
                .size(if (coinCount >= 2) 32.dp else 28.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationY = -progress * 12f
                }
        )
    }
}

private fun phaseProgress(
    progress: Float,
    start: Float,
    end: Float
): Float {
    if (end <= start) return 0f
    return ((progress - start) / (end - start)).coerceIn(0f, 1f)
}

private fun iconActivationProgress(
    activationProgress: Float,
    index: Int
): Float {
    val stagger = index * 0.18f
    return ((activationProgress - stagger) / 0.64f).coerceIn(0f, 1f)
}

private fun lerp(
    start: Float,
    end: Float,
    progress: Float
): Float {
    return start + (end - start) * progress.coerceIn(0f, 1f)
}
