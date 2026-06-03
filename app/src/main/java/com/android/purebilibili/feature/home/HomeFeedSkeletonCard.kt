package com.android.purebilibili.feature.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.android.purebilibili.core.theme.LocalCornerRadiusScale
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.transition.VIDEO_SHARED_COVER_ASPECT_RATIO

@Composable
internal fun rememberHomeFeedSkeletonPulse(): Float {
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
    modifier: Modifier = Modifier
) {
    val cornerRadiusScale = LocalCornerRadiusScale.current
    val cardCornerRadius = 12.dp * cornerRadiusScale
    val cardShape = remember(cardCornerRadius) { RoundedCornerShape(cardCornerRadius) }
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
            RoundedCornerShape(
                topStart = cardCornerRadius,
                topEnd = cardCornerRadius,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            )
        } else {
            cardShape
        }
    }
    val infoSurfaceShape = remember(cardCornerRadius) {
        RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = cardCornerRadius,
            bottomEnd = cardCornerRadius
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(VIDEO_SHARED_COVER_ASPECT_RATIO)
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
                        width = 0.8.dp,
                        color = MaterialTheme.colorScheme.onSurface
                            .copy(alpha = infoSurfaceAppearance.borderAlpha)
                    ),
                    shape = infoSurfaceShape
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        } else {
            Modifier.fillMaxWidth()
        }

        Column(modifier = infoModifier) {
            if (!infoSurfaceAppearance.useTintedSurface) {
                Spacer(modifier = Modifier.height(8.dp))
            }
            HomeFeedSkeletonTitleRow(blockColor = blockColor)
            Spacer(modifier = Modifier.height(6.dp))
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
                    .height(16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            HomeFeedSkeletonBlock(
                color = blockColor,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        HomeFeedSkeletonBlock(
            color = blockColor,
            modifier = Modifier.size(20.dp),
            shape = CircleShape
        )
    }
}

@Composable
private fun HomeFeedSkeletonMetaRow(blockColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        HomeFeedSkeletonBlock(
            color = blockColor,
            modifier = Modifier
                .width(28.dp)
                .height(14.dp)
        )
        HomeFeedSkeletonBlock(
            color = blockColor,
            modifier = Modifier
                .width(96.dp)
                .height(14.dp)
        )
    }
}

@Composable
private fun HomeFeedSkeletonBlock(
    color: Color,
    modifier: Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(color)
    )
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
