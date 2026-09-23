package com.android.purebilibili.feature.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.purebilibili.core.ui.AdaptiveLoadingIndicator
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppPopupSurface
import com.android.purebilibili.core.ui.AppPopupSurfaceType
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.motion.AppMotionTokens
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppCheckbox
import com.android.purebilibili.core.ui.components.AppCheckboxDefaults
import com.android.purebilibili.core.ui.components.AppCircularProgressIndicator
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.core.util.CacheClearTarget
import com.android.purebilibili.core.util.CacheUtils
import kotlin.math.min

data class CacheClearProgress(
    val current: Long,
    val total: Long,
    val isComplete: Boolean = false,
    val clearedSize: String = ""
)

@Composable
internal fun CacheClearConfirmDialog(
    breakdown: CacheUtils.CacheBreakdown?,
    selectedCacheSizeSummary: String,
    options: List<CacheClearOptionUiModel>,
    selectedTargets: Set<CacheClearTarget>,
    onTargetToggle: (CacheClearTarget, Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val segments = remember(breakdown, selectedTargets, options) {
        resolveCacheClearDonutSegments(
            breakdown = breakdown,
            selectedTargets = selectedTargets,
            options = options
        )
    }
    val colorScheme = MaterialTheme.colorScheme
    val segmentColors = remember(colorScheme) {
        listOf(
            colorScheme.primary,
            colorScheme.tertiary,
            colorScheme.secondary,
            colorScheme.error,
            colorScheme.primaryContainer,
            colorScheme.tertiaryContainer
        )
    }
    val centerSize = resolveCacheClearDonutCenterSize(breakdown, selectedTargets)
    val buttonLabel = resolveCacheClearButtonLabel(breakdown, selectedTargets)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppPopupSurface(
            type = AppPopupSurfaceType.DIALOG,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .widthIn(max = 480.dp)
                .fillMaxWidth(),
            shape = AppShapes.container(ContainerLevel.Dialog),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CacheClearUsageDonut(
                    segments = segments,
                    colors = segmentColors,
                    centerSize = centerSize,
                    onSegmentClick = { target ->
                        onTargetToggle(target, target !in selectedTargets)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                AppText(
                    text = "存储使用情况",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                AppText(
                    text = selectedCacheSizeSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppText(
                    text = resolveCacheClearConfirmationMessage(selectedTargets),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                segments.forEach { segment ->
                    val color = segmentColors[segment.colorIndex % segmentColors.size]
                    CacheClearCategoryRow(
                        segment = segment,
                        color = color,
                        onToggle = { checked -> onTargetToggle(segment.target, checked) }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                AppButton(
                    onClick = onConfirm,
                    enabled = selectedTargets.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    AppText(buttonLabel)
                }
                AppTextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppText("取消")
                }
            }
        }
    }
}

@Composable
private fun CacheClearUsageDonut(
    segments: List<CacheClearDonutSegment>,
    colors: List<Color>,
    centerSize: String,
    onSegmentClick: (CacheClearTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(220.dp)
            .semantics {
                contentDescription = "缓存占用圆环，点击扇区可选择要清理的类型"
            }
            .pointerInput(segments) {
                detectTapGestures { offset ->
                    val radius = min(size.width, size.height) / 2f
                    val dx = offset.x - size.width / 2f
                    val dy = offset.y - size.height / 2f
                    val target = resolveCacheClearDonutHitTarget(
                        segments = segments,
                        dx = dx,
                        dy = dy,
                        innerRadius = radius * 0.58f,
                        outerRadius = radius
                    )
                    if (target != null) onSegmentClick(target)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val donutSpec = AppMotionTokens.spatialSpec<Float>()
        val sweepStates = segments.map { segment ->
            animateFloatAsState(
                targetValue = segment.sweepAngle,
                animationSpec = donutSpec,
                label = "cacheDonutSweep-${segment.target}"
            )
        }
        val startStates = segments.map { segment ->
            animateFloatAsState(
                targetValue = segment.startAngle,
                animationSpec = donutSpec,
                label = "cacheDonutStart-${segment.target}"
            )
        }
        val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.18f
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            segments.forEachIndexed { index, segment ->
                val sweep = sweepStates[index].value
                if (sweep <= 0.5f) return@forEachIndexed
                val start = startStates[index].value
                val color = colors[segment.colorIndex % colors.size]
                val gap = if (sweep > 8f) 3f else 0f
                drawArc(
                    color = color,
                    startAngle = start + gap / 2f,
                    sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppText(
                text = "BiliPai",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AppText(
                text = centerSize,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CacheClearCategoryRow(
    segment: CacheClearDonutSegment,
    color: Color,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(AppShapes.container(ContainerLevel.Card))
            .clickable { onToggle(!segment.selected) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppCheckbox(
            checked = segment.selected,
            onCheckedChange = onToggle,
            colors = AppCheckboxDefaults.colors(
                checkedColor = color,
                uncheckedColor = color.copy(alpha = 0.6f)
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            AppText(
                text = segment.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            AppText(
                text = segment.percentLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        AppText(
            text = formatCacheClearBytes(segment.bytes),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CacheClearAnimationDialog(
    progress: CacheClearProgress,
    onDismiss: () -> Unit
) {
    val progressValue = if (progress.total > 0) {
        (progress.current.toFloat() / progress.total.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = if (progress.isComplete) 1f else progressValue,
        label = "cacheClearProgress"
    )

    LaunchedEffect(progress.isComplete) {
        if (progress.isComplete) {
            kotlinx.coroutines.delay(2000L)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = progress.isComplete,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AppPopupSurface(
                type = AppPopupSurfaceType.DIALOG,
                modifier = Modifier
                    .padding(48.dp)
                    .widthIn(max = 360.dp)
                    .fillMaxWidth(),
                shape = AppShapes.container(ContainerLevel.Dialog),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier.size(96.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            progress.isComplete -> {
                                CacheClearSuccessAnimation()
                            }

                            progressValue <= 0f -> {
                                AdaptiveLoadingIndicator(size = 64.dp)
                            }

                            else -> {
                                AppCircularProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.size(72.dp)
                                )
                            }
                        }
                    }
                    AppText(
                        text = if (progress.isComplete) "清理完成" else "正在清理",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AppText(
                        text = if (progress.clearedSize.isNotEmpty()) {
                            if (progress.isComplete) "共释放 ${progress.clearedSize}"
                            else "已清理 ${progress.clearedSize}"
                        } else {
                            "准备中..."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (progress.isComplete) {
                        AppText(
                            text = "即将自动关闭...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CacheClearSuccessAnimation() {
    val transitionState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }
    val transition = rememberTransition(transitionState = transitionState, label = "cacheClearSuccess")
    val scale by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 420) },
        label = "cacheClearSuccessScale"
    ) { complete -> if (complete) 1f else 0.65f }
    val sparkleAlpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 650, delayMillis = 180) },
        label = "cacheClearSparkleAlpha"
    ) { complete -> if (complete) 1f else 0f }
    val successColor = Color(0xFF34C759)

    Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = sparkleAlpha }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            repeat(10) { index ->
                val angle = Math.toRadians(index * 36.0)
                val radius = size.minDimension * (0.39f + (index % 3) * 0.045f)
                val point = Offset(
                    x = center.x + kotlin.math.cos(angle).toFloat() * radius,
                    y = center.y + kotlin.math.sin(angle).toFloat() * radius
                )
                drawCircle(
                    color = successColor.copy(alpha = 0.35f + (index % 2) * 0.25f),
                    radius = if (index % 2 == 0) 4.dp.toPx() else 2.5.dp.toPx(),
                    center = point
                )
            }
        }
        Surface(
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            shape = CircleShape,
            color = successColor.copy(alpha = 0.14f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                AppIcon(
                    imageVector = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_check_fill_24),
                    contentDescription = "存储已清除",
                    modifier = Modifier.size(40.dp),
                    tint = successColor
                )
            }
        }
    }
}
