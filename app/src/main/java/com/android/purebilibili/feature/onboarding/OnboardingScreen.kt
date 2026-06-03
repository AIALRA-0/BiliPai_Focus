package com.android.purebilibili.feature.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.theme.resolveAdaptivePrimaryAccentColors
import com.android.purebilibili.core.ui.motion.rememberSystemReduceMotion
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onApplySettingsProfile: suspend (OnboardingSettingsProfile) -> Unit = {}
) {
    val reduceMotion = rememberSystemReduceMotion()
    val motionSpec = remember(reduceMotion) { resolveOnboardingMotionSpec(reduceMotion) }
    val pageCount = remember { resolveOnboardingPageCount() }
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    val lastPage = remember(pageCount) { resolveOnboardingLastPageIndex(pageCount) }
    var selectedSettingsProfile by remember { mutableStateOf(OnboardingSettingsProfile.RECOMMENDED) }
    var isApplyingSettings by remember { mutableStateOf(false) }

    val advanceOrFinish: () -> Unit = {
        if (!isApplyingSettings) {
            if (pagerState.currentPage >= lastPage) {
                scope.launch {
                    isApplyingSettings = true
                    try {
                        onApplySettingsProfile(selectedSettingsProfile)
                        onFinish()
                    } finally {
                        isApplyingSettings = false
                    }
                }
            } else {
                scope.launch {
                    pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(lastPage))
                }
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxSize()
            .testTag("onboarding_root")
    ) {
        Scaffold(
            bottomBar = {
                OnboardingBottomControls(
                    pageCount = pageCount,
                    currentPage = pagerState.currentPage,
                    isLastPage = pagerState.currentPage == lastPage,
                    isApplyingSettings = isApplyingSettings,
                    onActionClick = advanceOrFinish
                )
            }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = true
            ) { page ->
                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                OnboardingAnimatedPage(
                    pageOffset = pageOffset,
                    motionSpec = motionSpec,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 28.dp)
                ) {
                    when (page) {
                        0 -> WelcomePage(motionSpec)
                        1 -> DesignPage(motionSpec)
                        2 -> FeaturesPage(motionSpec)
                        3 -> GetStartedPage(motionSpec)
                        4 -> SettingsGuidePage(
                            selectedProfile = selectedSettingsProfile,
                            onProfileSelected = { selectedSettingsProfile = it },
                            motionSpec = motionSpec
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingBottomControls(
    pageCount: Int,
    currentPage: Int,
    isLastPage: Boolean,
    isApplyingSettings: Boolean,
    onActionClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .padding(bottom = 48.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount) { index ->
                val selected = currentPage == index
                val width by animateDpAsState(
                    targetValue = if (selected) 24.dp else 8.dp,
                    label = "onboardingIndicatorWidth"
                )
                val color by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    },
                    label = "onboardingIndicatorColor"
                )
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }

        val actionColors = resolveAdaptivePrimaryAccentColors(MaterialTheme.colorScheme)
        Button(
            onClick = onActionClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("onboarding_action_button"),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = actionColors.backgroundColor,
                contentColor = actionColors.contentColor
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            ),
            enabled = !isApplyingSettings
        ) {
            if (isApplyingSettings) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = actionColors.contentColor
                )
            } else {
                Text(
                    text = if (isLastPage) "应用设置并开始" else "下一步",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun OnboardingAnimatedPage(
    pageOffset: Float,
    motionSpec: OnboardingMotionSpec,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.graphicsLayer {
            val clampedOffset = pageOffset.absoluteValue.coerceIn(0f, 1f)
            val scale = lerp(1f, motionSpec.pager.minScale, clampedOffset)
            scaleX = scale
            scaleY = scale
            alpha = lerp(1f, motionSpec.pager.minAlpha, clampedOffset)
            translationY = 24f * clampedOffset
        },
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
private fun WelcomePage(motionSpec: OnboardingMotionSpec) {
    val context = LocalContext.current
    val appIconKey by SettingsManager.getAppIcon(context).collectAsStateWithLifecycle(initialValue = "icon_3d")
    val heroIconSpec = remember(appIconKey) { resolveOnboardingHeroIconSpec(appIconKey) }

    OnboardingFloatingContent(motionSpec = motionSpec) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OnboardingHeroHalo(size = 164.dp, motionSpec = motionSpec) {
                Box(
                    modifier = Modifier
                        .size(118.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = heroIconSpec.iconRes,
                        contentDescription = "App Icon",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = heroIconSpec.imageScale
                                scaleY = heroIconSpec.imageScale
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(44.dp))

            Text(
                text = "Welcome to\nBiliPai",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 42.sp,
                    lineHeight = 48.sp
                ),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "纯净 · 流畅 · 沉浸",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
private fun DesignPage(motionSpec: OnboardingMotionSpec) {
    OnboardingFloatingContent(motionSpec = motionSpec) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OnboardingLayeredPreview()

            Spacer(modifier = Modifier.height(44.dp))

            Text(
                text = "原生外观",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "默认 MD3，关闭液态玻璃，保留轻盈悬浮底栏",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OnboardingLayeredPreview() {
    Box(modifier = Modifier.size(284.dp)) {
        OnboardingPreviewLayer(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    translationY = -42f
                    scaleX = 0.78f
                    scaleY = 0.78f
                    rotationZ = -7f
                },
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        )
        OnboardingPreviewLayer(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    translationY = -22f
                    scaleX = 0.9f
                    scaleY = 0.9f
                    rotationZ = 4f
                },
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(32.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(116.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OnboardingColorChip(Color(0xFFFFCC80))
                    OnboardingColorChip(Color(0xFF81C784))
                    OnboardingColorChip(Color(0xFF64B5F6))
                }
            }
        }
    }
}

@Composable
private fun OnboardingPreviewLayer(
    modifier: Modifier,
    color: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(color)
    )
}

@Composable
private fun OnboardingColorChip(color: Color) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.82f))
    )
}

@Composable
private fun FeaturesPage(motionSpec: OnboardingMotionSpec) {
    OnboardingFloatingContent(motionSpec = motionSpec) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "先认识几个重点",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 38.dp)
            )

            OnboardingFeatureCard(
                icon = Icons.Outlined.AutoAwesome,
                title = "清爽首页",
                subtitle = "顶部 6 个纯文字标签，入口更直观"
            )
            OnboardingFeatureCard(
                icon = Icons.Outlined.Speed,
                title = "顺滑播放",
                subtitle = "保留关键过渡，减少不必要视觉负担"
            )
            OnboardingFeatureCard(
                icon = Icons.Outlined.BlurOn,
                title = "可控外观",
                subtitle = "液态玻璃、底栏和省流量都能继续细调"
            )
        }
    }
}

@Composable
private fun OnboardingFeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(18.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GetStartedPage(motionSpec: OnboardingMotionSpec) {
    OnboardingFloatingContent(motionSpec = motionSpec) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OnboardingHeroHalo(size = 156.dp, motionSpec = motionSpec) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(44.dp))

            Text(
                text = "最后一步",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "下一页会带你快速选一套设置预案",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SettingsGuidePage(
    selectedProfile: OnboardingSettingsProfile,
    onProfileSelected: (OnboardingSettingsProfile) -> Unit,
    motionSpec: OnboardingMotionSpec
) {
    val selectedPreset = remember(selectedProfile) {
        resolveOnboardingSettingsGuidePreset(selectedProfile)
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "带你过一版设置",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "选一套预设，之后随时能在设置里改",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(26.dp))

        OnboardingSettingsProfile.entries.forEach { profile ->
            SettingsProfileCard(
                profile = profile,
                selected = profile == selectedProfile,
                motionSpec = motionSpec,
                onClick = { onProfileSelected(profile) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectedPreset.summaryLines.forEach { line ->
                Text(
                    text = "• $line",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsProfileCard(
    profile: OnboardingSettingsProfile,
    selected: Boolean,
    motionSpec: OnboardingMotionSpec,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
        },
        label = "settingsProfileBorder"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        },
        label = "settingsProfileBackground"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) motionSpec.card.selectedScale else motionSpec.card.unselectedScale,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 420f),
        label = "settingsProfileScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (profile) {
                OnboardingSettingsProfile.RECOMMENDED -> Icons.Filled.Star
                OnboardingSettingsProfile.PERFORMANCE -> Icons.Filled.Refresh
                OnboardingSettingsProfile.DATA_SAVER -> Icons.Filled.Lock
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = profile.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun OnboardingFloatingContent(
    motionSpec: OnboardingMotionSpec,
    content: @Composable () -> Unit
) {
    if (motionSpec.floating.durationMillis <= 0 || motionSpec.floating.translationYPx == 0f) {
        Box(contentAlignment = Alignment.Center) { content() }
        return
    }
    val transition = rememberInfiniteTransition(label = "onboardingFloating")
    val offsetY by transition.animateFloat(
        initialValue = -motionSpec.floating.translationYPx,
        targetValue = motionSpec.floating.translationYPx,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = motionSpec.floating.durationMillis,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "onboardingFloatY"
    )
    Box(
        modifier = Modifier.graphicsLayer { translationY = offsetY },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun OnboardingHeroHalo(
    size: Dp,
    motionSpec: OnboardingMotionSpec,
    content: @Composable BoxScope.() -> Unit
) {
    val pulse = if (motionSpec.halo.durationMillis <= 0) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "onboardingHalo")
        val animatedPulse by transition.animateFloat(
            initialValue = motionSpec.halo.minScale,
            targetValue = motionSpec.halo.maxScale,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = motionSpec.halo.durationMillis,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "onboardingHaloPulse"
        )
        animatedPulse
    }

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                    alpha = (1.12f - pulse).coerceIn(0f, 0.28f)
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
        )
        Box(
            modifier = Modifier
                .size(size * 0.82f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}
