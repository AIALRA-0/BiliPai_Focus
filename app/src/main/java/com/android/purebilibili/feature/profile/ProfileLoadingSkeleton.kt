package com.android.purebilibili.feature.profile

import com.android.purebilibili.core.ui.AppSurfaceTokens
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.globalWallpaperAwareBackground
import com.android.purebilibili.core.ui.motion.rememberSystemReduceMotion
import com.android.purebilibili.core.util.LocalWindowSizeClass

private const val PROFILE_SKELETON_PULSE_DURATION_MILLIS = 950

@Composable
internal fun ProfileLoadingSkeleton(
    modifier: Modifier = Modifier,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val blockColor = rememberProfileSkeletonBlockColor()
    Box(
        modifier = modifier
            .fillMaxSize()
            .globalWallpaperAwareBackground()
            .clearAndSetSemantics {
                contentDescription = "个人页正在加载"
            },
    ) {
        if (windowSizeClass.shouldUseSplitLayout) {
            ProfileTabletLoadingSkeleton(blockColor = blockColor)
        } else {
            ProfilePhoneLoadingSkeleton(blockColor = blockColor)
        }
    }
}

@Composable
private fun ProfilePhoneLoadingSkeleton(blockColor: Color) {
    val configuration = LocalConfiguration.current
    val windowSizeClass = LocalWindowSizeClass.current
    val layoutTokens = remember { resolveProfileLayoutTokens() }
    val heroHeight = resolveProfileHeroHeightDp(
        screenHeightDp = configuration.screenHeightDp,
        widthSizeClass = windowSizeClass.widthSizeClass,
    ).dp
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 96.dp,
        ),
        userScrollEnabled = false,
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = layoutTokens.heroBottomInsetDp.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfileSkeletonBlock(80.dp, 80.dp, blockColor, CircleShape)
                        Spacer(modifier = Modifier.weight(1f))
                        repeat(3) { index ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                ProfileSkeletonBlock(28.dp, 14.dp, blockColor)
                                ProfileSkeletonBlock(40.dp, 10.dp, blockColor)
                            }
                            if (index != 2) Spacer(modifier = Modifier.width(18.dp))
                        }
                    }
                    ProfileSkeletonBlock(156.dp, 24.dp, blockColor)
                    ProfileSkeletonBlock(232.dp, 14.dp, blockColor)
                }
            }
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppSurfaceTokens.surface())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    repeat(3) { ProfileSkeletonBlock(64.dp, 32.dp, blockColor) }
                }
                ProfileSkeletonServiceGroup(blockColor = blockColor)
                ProfileSkeletonFeedGroup(blockColor = blockColor)
            }
        }
    }
}

@Composable
private fun ProfileTabletLoadingSkeleton(blockColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 72.dp,
                start = 24.dp,
                end = 24.dp,
                bottom = 24.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(
            modifier = Modifier.widthIn(min = 300.dp, max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileSkeletonBlock(72.dp, 72.dp, blockColor, CircleShape)
                Spacer(modifier = Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfileSkeletonBlock(148.dp, 22.dp, blockColor)
                    ProfileSkeletonBlock(208.dp, 13.dp, blockColor)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                repeat(3) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ProfileSkeletonBlock(32.dp, 14.dp, blockColor)
                        ProfileSkeletonBlock(48.dp, 10.dp, blockColor)
                    }
                }
            }
            ProfileSkeletonServiceGroup(blockColor = blockColor)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) { ProfileSkeletonBlock(78.dp, 36.dp, blockColor) }
            }
            ProfileSkeletonFeedGroup(blockColor = blockColor)
            ProfileSkeletonFeedGroup(blockColor = blockColor)
        }
    }
}

@Composable
private fun ProfileSkeletonServiceGroup(blockColor: Color) {
    val shape = AppShapes.container(ContainerLevel.Card)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        repeat(4) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileSkeletonBlock(36.dp, 36.dp, blockColor, CircleShape)
                Spacer(modifier = Modifier.width(14.dp))
                ProfileSkeletonBlock(112.dp, 15.dp, blockColor)
                Spacer(modifier = Modifier.weight(1f))
                ProfileSkeletonBlock(18.dp, 18.dp, blockColor, CircleShape)
            }
        }
    }
}

@Composable
private fun ProfileSkeletonFeedGroup(blockColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProfileSkeletonBlock(104.dp, 20.dp, blockColor)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(2) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProfileSkeletonBlock(
                        width = Dp.Unspecified,
                        height = 112.dp,
                        color = blockColor,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ProfileSkeletonBlock(96.dp, 14.dp, blockColor)
                    ProfileSkeletonBlock(72.dp, 11.dp, blockColor)
                }
            }
        }
    }
}

@Composable
private fun ProfileSkeletonBlock(
    width: Dp,
    height: Dp,
    color: Color,
    shape: Shape = AppShapes.container(ContainerLevel.Chip),
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .then(if (width != Dp.Unspecified) Modifier.width(width) else Modifier)
            .height(height)
            .clip(shape)
            .background(color),
    )
}

@Composable
private fun rememberProfileSkeletonBlockColor(): Color {
    val reduceMotion = rememberSystemReduceMotion()
    val pulse = if (reduceMotion) {
        0.45f
    } else if (com.android.purebilibili.core.ui.skeleton.rememberSkeletonBreathingEnabled()) {
        com.android.purebilibili.core.ui.skeleton.rememberGentleSkeletonPulse().value
    } else {
        val transition = rememberInfiniteTransition(label = "profileSkeletonPulse")
        val animatedPulse by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = PROFILE_SKELETON_PULSE_DURATION_MILLIS,
                    easing = FastOutSlowInEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "profileSkeletonPulseAlpha",
        )
        animatedPulse
    }
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    return remember(pulse, surfaceVariant, onSurfaceVariant) {
        lerp(surfaceVariant, onSurfaceVariant.copy(alpha = 0.20f), pulse)
    }
}
