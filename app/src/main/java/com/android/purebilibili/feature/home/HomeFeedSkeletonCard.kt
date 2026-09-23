package com.android.purebilibili.feature.home

import com.android.purebilibili.core.ui.AppSpacingTokens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.store.HomeWallpaperEffectMode
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.transition.VIDEO_SHARED_COVER_ASPECT_RATIO

@Composable
internal fun rememberHomeFeedSkeletonPulse(): Float {
    if (com.android.purebilibili.core.ui.skeleton.rememberSkeletonBreathingEnabled()) {
        return com.android.purebilibili.core.ui.skeleton.rememberGentleSkeletonPulse().value
    }
    val transition = rememberInfiniteTransition(label = "homeFeedSkeletonPulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = HOME_FEED_SKELETON_PULSE_DURATION_MILLIS,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "homeFeedSkeletonPulseAlpha"
    )
    return pulse
}

@Composable
internal fun HomeFeedSkeletonCard(
    pulse: Float,
    wallpaperTintEnabled: Boolean,
    wallpaperEffectMode: HomeWallpaperEffectMode,
    isDataSaverActive: Boolean,
    coverAspectRatio: Float = VIDEO_SHARED_COVER_ASPECT_RATIO,
    modifier: Modifier = Modifier
) {
    val cardCornerRadius = AppShapes.containerCornerDp(ContainerLevel.Card)
    val cardShape = AppShapes.container(ContainerLevel.Card)
    val isDarkCardTheme = AppSurfaceTokens.chromeBackground().luminance() < 0.5f
    val infoSurfaceAppearance = remember(
        wallpaperTintEnabled,
        wallpaperEffectMode,
        isDarkCardTheme,
        isDataSaverActive
    ) {
        resolveHomeCardInfoSurfaceAppearance(
            wallpaperTintEnabled = wallpaperTintEnabled,
            wallpaperEffectMode = wallpaperEffectMode,
            isDarkTheme = isDarkCardTheme,
            isDataSaverActive = isDataSaverActive
        )
    }
    val blockColor = rememberHomeFeedSkeletonBlockColor(
        pulse = pulse,
        isDarkTheme = isDarkCardTheme
    )
    val coverShape = remember(cardCornerRadius, infoSurfaceAppearance.useTintedSurface) {
        if (infoSurfaceAppearance.useTintedSurface) {
            resolveHomeSkeletonCoverShape(cardCornerRadius)
        } else {
            cardShape
        }
    }
    val infoSurfaceShape = remember(cardCornerRadius) {
        resolveHomeSkeletonInfoShape(cardCornerRadius)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(coverAspectRatio)
                .clip(coverShape)
                .background(blockColor)
        )

        val infoModifier = if (infoSurfaceAppearance.useTintedSurface) {
            Modifier
                .fillMaxWidth()
                .background(
                    color = AppSurfaceTokens.cardContainer()
                        .copy(alpha = infoSurfaceAppearance.containerAlpha),
                    shape = infoSurfaceShape
                )
                .border(
                    border = BorderStroke(
                        width = AppSpacingTokens.Micro * 0.4f,
                        color = MaterialTheme.colorScheme.onSurface
                            .copy(alpha = infoSurfaceAppearance.borderAlpha)
                    ),
                    shape = infoSurfaceShape
                )
                .padding(horizontal = AppSpacingTokens.Small + AppSpacingTokens.Micro, vertical = AppSpacingTokens.Small)
        } else {
            Modifier.fillMaxWidth()
        }

        Column(modifier = infoModifier) {
            if (!infoSurfaceAppearance.useTintedSurface) {
                Spacer(modifier = Modifier.height(AppSpacingTokens.Small))
            }
            HomeFeedSkeletonTitleRow(blockColor = blockColor)
            Spacer(modifier = Modifier.height(AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro))
            HomeFeedSkeletonMetaRow(blockColor = blockColor)
        }
    }
}

@Composable
private fun HomeFeedSkeletonTitleRow(blockColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            HomeFeedSkeletonBlock(
                color = blockColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSpacingTokens.Large)
            )
            Spacer(modifier = Modifier.height(AppSpacingTokens.Small))
            HomeFeedSkeletonBlock(
                color = blockColor,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(AppSpacingTokens.Large)
            )
        }
        Spacer(modifier = Modifier.width(AppSpacingTokens.Small))
        HomeFeedSkeletonBlock(
            color = blockColor,
            modifier = Modifier.size(AppSpacingTokens.Large + AppSpacingTokens.ExtraSmall),
            shape = CircleShape
        )
    }
}

@Composable
private fun HomeFeedSkeletonMetaRow(blockColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall + AppSpacingTokens.Micro)
    ) {
        HomeFeedSkeletonBlock(
            color = blockColor,
            modifier = Modifier
                .width(AppSpacingTokens.ExtraLarge + AppSpacingTokens.ExtraSmall)
                .height(AppSpacingTokens.Medium + AppSpacingTokens.Micro)
        )
        HomeFeedSkeletonBlock(
            color = blockColor,
            modifier = Modifier
                .width(AppSpacingTokens.TripleExtraLarge * 2)
                .height(AppSpacingTokens.Medium + AppSpacingTokens.Micro)
        )
    }
}

@Composable
private fun HomeFeedSkeletonBlock(
    color: Color,
    modifier: Modifier,
    shape: androidx.compose.ui.graphics.Shape? = null
) {
    val resolvedShape = shape ?: AppShapes.container(ContainerLevel.Tag)
    Box(
        modifier = modifier
            .clip(resolvedShape)
            .background(color)
    )
}

/**
 * 首页横幅（Hero Carousel）骨架占位。
 * 与真实横幅 [HomeHeroCarousel] 对齐：垂直 padding、当前窗口限宽限高、卡片圆角。
 */
@Composable
internal fun HomeFeedHeroCarouselSkeleton(
    pulse: Float,
    modifier: Modifier = Modifier
) {
    val cardShape = AppShapes.container(ContainerLevel.Card)
    val isDarkCardTheme = AppSurfaceTokens.chromeBackground().luminance() < 0.5f
    val blockColor = rememberHomeFeedSkeletonBlockColor(
        pulse = pulse,
        isDarkTheme = isDarkCardTheme
    )
    val windowSizeClass = com.android.purebilibili.core.util.LocalWindowSizeClass.current
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacingTokens.ExtraSmall)
    ) {
        val layout = remember(
            maxWidth,
            windowSizeClass.widthDp,
            windowSizeClass.heightDp,
        ) {
            resolveHomeHeroCarouselLayout(
                containerWidthDp = maxWidth.value,
                windowWidthDp = windowSizeClass.widthDp.value,
                windowHeightDp = windowSizeClass.heightDp.value,
            )
        }
        Box(
            modifier = Modifier
                .width(layout.widthDp.dp)
                .aspectRatio(layout.aspectRatio)
                .clip(cardShape)
                .background(blockColor)
                .align(Alignment.Center)
        )
    }
}

@Composable
private fun rememberHomeFeedSkeletonBlockColor(
    pulse: Float,
    isDarkTheme: Boolean
): Color {
    val alpha = if (isDarkTheme) {
        HOME_FEED_SKELETON_DARK_MIN_ALPHA +
            (HOME_FEED_SKELETON_DARK_MAX_ALPHA - HOME_FEED_SKELETON_DARK_MIN_ALPHA) * pulse
    } else {
        HOME_FEED_SKELETON_LIGHT_MIN_ALPHA +
            (HOME_FEED_SKELETON_LIGHT_MAX_ALPHA - HOME_FEED_SKELETON_LIGHT_MIN_ALPHA) * pulse
    }
    return MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
}

private const val HOME_FEED_SKELETON_PULSE_DURATION_MILLIS = 2_000
private const val HOME_FEED_SKELETON_LIGHT_MIN_ALPHA = 0.06f
private const val HOME_FEED_SKELETON_LIGHT_MAX_ALPHA = 0.11f
private const val HOME_FEED_SKELETON_DARK_MIN_ALPHA = 0.10f
private const val HOME_FEED_SKELETON_DARK_MAX_ALPHA = 0.16f
