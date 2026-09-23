package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.ThumbUp
import com.android.purebilibili.core.ui.components.AppButton
import androidx.compose.material3.ButtonDefaults
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppIconButtonDefaults
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil3.compose.AsyncImage

import com.android.purebilibili.core.ui.AppIcons
import com.android.purebilibili.feature.video.danmaku.CommandDanmakuItem
import com.android.purebilibili.feature.video.danmaku.CommandDanmakuType
import com.android.purebilibili.feature.video.danmaku.VoteDanmakuKind
import com.android.purebilibili.feature.video.danmaku.VoteOption
import com.android.purebilibili.feature.video.danmaku.resolveGradeStarOptions
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Density
import com.android.purebilibili.feature.video.danmaku.DanmakuViewport

@Composable
internal fun CommandDanmakuOverlay(
    items: List<CommandDanmakuItem>,
    player: Player,
    viewport: DanmakuViewport,
    state: CommandDanmakuOverlayState,
    fontScale: Float,
    onFollowClick: () -> Unit,
    onTripleClick: () -> Unit,
    onVoteSubmit: (CommandDanmakuItem, VoteOption) -> Unit = { _, _ -> },
    isFollowing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val currentPosition by produceState(initialValue = player.currentPosition, key1 = player) {
        while (true) {
            value = player.currentPosition
            kotlinx.coroutines.delay(80)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        val active = items.filter {
            !state.isDismissed(it.id) &&
                currentPosition in it.startTimeMs..(it.startTimeMs + it.durationMs)
        }
        active.forEach { item ->
            key(item.id) {
                CommandDanmakuCard(
                    item = item,
                    viewport = viewport,
                    state = state,
                    fontScale = fontScale,
                    onFollowClick = onFollowClick,
                    onTripleClick = onTripleClick,
                    onVoteSubmit = onVoteSubmit,
                    isFollowing = isFollowing,
                    onDismiss = { state.dismiss(item.id) }
                )
            }
        }
    }
}

@Composable
private fun CommandDanmakuCard(
    item: CommandDanmakuItem,
    viewport: DanmakuViewport,
    state: CommandDanmakuOverlayState,
    fontScale: Float,
    onFollowClick: () -> Unit,
    onTripleClick: () -> Unit,
    onVoteSubmit: (CommandDanmakuItem, VoteOption) -> Unit,
    isFollowing: Boolean,
    onDismiss: () -> Unit
) {
    val containerWidth = viewport.widthPx
    val containerHeight = viewport.heightPx
    val (xRatio, yRatio) = when (item.type) {
        CommandDanmakuType.ATTENTION -> mapAttentionPosition(item.posX, item.posY)
        CommandDanmakuType.VOTE -> 0.08f to 0.10f
        CommandDanmakuType.UP -> 0.08f to 0.10f
        CommandDanmakuType.LINK -> 0.08f to 0.18f
        CommandDanmakuType.TEXT -> 0.08f to 0.10f
    }
    val requestedCardWidthDp = when (item.type) {
        CommandDanmakuType.ATTENTION -> resolveAttentionCommandCardWidthDp(item.attentionType)
        // Five 48dp star targets fit without scrolling while leaving the close control above.
        CommandDanmakuType.VOTE -> if (item.voteKind == VoteDanmakuKind.GRADE) 260 else 224
        else -> 220
    }
    val density = LocalDensity.current
    val visualDensity = remember(density.density, density.fontScale, viewport.scale, fontScale) {
        Density(density.density * viewport.scale, density.fontScale * fontScale.coerceIn(0.3f, 2f))
    }
    val requestedCardWidthPx = with(visualDensity) { requestedCardWidthDp.dp.roundToPx() }
    val cardWidthPx = resolveCommandDanmakuCardWidthPx(
        containerWidthPx = containerWidth,
        requestedCardWidthPx = requestedCardWidthPx
    )
    val cardWidthDp = with(density) { cardWidthPx.toDp() }
    val maxCardHeightDp = with(visualDensity) { containerHeight.toDp() }
    // Start conservatively at the full viewport height so the first frame cannot extend below it;
    // onSizeChanged then tightens the position to the measured card height.
    var measuredCardHeightPx by remember(item.id, viewport, density.fontScale) {
        mutableIntStateOf(containerHeight)
    }
    val x = resolveCommandDanmakuHorizontalOffsetPx(containerWidth, cardWidthPx, xRatio)
    val y = resolveCommandDanmakuVerticalOffsetPx(
        containerHeightPx = containerHeight,
        cardHeightPx = measuredCardHeightPx,
        yRatio = yRatio
    )

    Box(
        modifier = Modifier
            .offset { IntOffset(x, y) }
            .width(cardWidthDp)
            .heightIn(max = with(density) { containerHeight.toDp() })
            .onSizeChanged { measuredCardHeightPx = it.height }
    ) {
        CompositionLocalProvider(LocalDensity provides visualDensity) {
            AppSurface(
                modifier = Modifier.fillMaxWidth(),
                color = resolveCommandDanmakuContainerColor(item.type),
                contentColor = Color.White,
                shape = AppShapes.container(ContainerLevel.Chip)
            ) {
        Box {
            when (item.type) {
                CommandDanmakuType.ATTENTION -> AttentionCommandCard(
                    item = item,
                    isFollowing = isFollowing,
                    onFollowClick = onFollowClick,
                    onTripleClick = onTripleClick
                )
                CommandDanmakuType.VOTE -> VoteCommandCard(
                    item = item,
                    state = state,
                    maxHeightDp = maxCardHeightDp,
                    onVoteSubmit = onVoteSubmit
                )
                else -> InfoCommandCard(item)
            }
            CommandDanmakuCloseButton(
                onDismiss = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(1.dp)
            )
        }
            }
        }
    }
}

@Composable
private fun InfoCommandCard(item: CommandDanmakuItem) {
    Row(
        modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 52.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.iconUrl.isNotBlank()) {
            AsyncImage(
                model = item.iconUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
            )
            Spacer(Modifier.width(8.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AppText(
                text = if (item.type == CommandDanmakuType.LINK) "关联视频" else "UP 主提示",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.74f)
            )
            AppText(
                text = item.linkTitle.ifBlank { item.content },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AttentionCommandCard(
    item: CommandDanmakuItem,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onTripleClick: () -> Unit
) {
    var tripleBurstKey by remember(item.id) { mutableIntStateOf(0) }

    fun playTripleAction() {
        tripleBurstKey += 1
        onTripleClick()
    }

    Column(
        modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 52.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (item.iconUrl.isNotBlank()) {
            AsyncImage(
                model = item.iconUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
            )
                Spacer(Modifier.height(6.dp))
        }
        val label = resolveAttentionCommandLabel(item.attentionType)
        AppButton(
            onClick = {
                val action = resolveAttentionCommandClickAction(
                    attentionType = item.attentionType,
                    isFollowing = isFollowing
                )
                if (action.shouldFollow) {
                    onFollowClick()
                }
                if (action.shouldTriple) {
                    playTripleAction()
                }
            },
            shape = AppShapes.container(ContainerLevel.Pill),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = null,
            contentPadding = PaddingValues(horizontal = 14.dp),
            modifier = Modifier
                .heightIn(min = 48.dp)
                .fillMaxWidth()
        ) {
            AppText(
                text = label,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
        CommandTripleActionBurst(
            triggerKey = tripleBurstKey,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun CommandDanmakuCloseButton(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppIconButton(
        onClick = onDismiss,
        modifier = modifier.size(48.dp),
        colors = AppIconButtonDefaults.colors(
            containerColor = Color.Black.copy(alpha = 0.34f),
            contentColor = Color.White
        )
    ) {
        AppIcon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "关闭提示",
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun CommandTripleActionBurst(
    triggerKey: Int,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (visible) {
            tween(durationMillis = 520, easing = LinearEasing)
        } else {
            tween(durationMillis = 160, easing = FastOutSlowInEasing)
        },
        label = "commandTripleBurstProgress"
    )

    LaunchedEffect(triggerKey) {
        if (triggerKey > 0) {
            visible = true
            delay(820)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(90)) + scaleIn(
            initialScale = 0.86f,
            animationSpec = tween(180, easing = FastOutSlowInEasing)
        ),
        exit = fadeOut(tween(120)) + scaleOut(
            targetScale = 0.92f,
            animationSpec = tween(120, easing = FastOutSlowInEasing)
        ),
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(AppShapes.container(ContainerLevel.Pill))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                .widthIn(min = 118.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            CommandTripleActionIcon(
                icon = Icons.Rounded.ThumbUp,
                progress = progress,
                color = MaterialTheme.colorScheme.primary
            )
            CommandTripleActionIcon(
                icon = AppIcons.BiliCoin,
                progress = progress,
                color = Color(0xFFFFB300)
            )
            CommandTripleActionIcon(
                icon = Icons.Rounded.Star,
                progress = progress,
                color = Color(0xFFFFC107)
            )
        }
    }
}

@Composable
private fun CommandTripleActionIcon(
    icon: ImageVector,
    progress: Float,
    color: Color
) {
    AppSurface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.92f),
        contentColor = color,
        border = BorderStroke(1.dp, color.copy(alpha = 0.42f)),
        modifier = Modifier
            .size(28.dp)
            .graphicsLayer {
                scaleX = 0.88f + 0.12f * progress
                scaleY = 0.88f + 0.12f * progress
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Canvas(modifier = Modifier.size(28.dp)) {
                val stroke = 2.dp.toPx()
                val diameter = size.minDimension - stroke
                val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            AppIcon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

internal data class AttentionCommandClickAction(
    val shouldFollow: Boolean,
    val shouldTriple: Boolean
)

internal fun resolveAttentionCommandClickAction(
    attentionType: Int,
    isFollowing: Boolean
): AttentionCommandClickAction {
    return when (attentionType) {
        1 -> AttentionCommandClickAction(shouldFollow = false, shouldTriple = true)
        2 -> AttentionCommandClickAction(shouldFollow = !isFollowing, shouldTriple = true)
        else -> AttentionCommandClickAction(shouldFollow = !isFollowing, shouldTriple = false)
    }
}

internal fun resolveAttentionCommandLabel(attentionType: Int): String {
    return when (attentionType) {
        1 -> "一键三连"
        2 -> "关注并三连"
        else -> "关注 UP"
    }
}

internal fun resolveAttentionCommandCardWidthDp(attentionType: Int): Int {
    return when (attentionType) {
        1 -> 172
        2 -> 196
        else -> 154
    }
}

internal fun resolveCommandDanmakuContainerColor(type: CommandDanmakuType): Color {
    return when (type) {
        CommandDanmakuType.ATTENTION -> Color.Transparent
        else -> Color.Black.copy(alpha = 0.54f)
    }
}

/**
 * 投票/打分弹幕卡片：普通投票保留选项按钮，打分使用固定五颗星。
 * 星位始终按 API 的合法分数 2/4/6/8/10 对齐，不按服务端列表顺序猜测分数。
 */
@Composable
private fun VoteCommandCard(
    item: CommandDanmakuItem,
    maxHeightDp: Dp,
    state: CommandDanmakuOverlayState,
    onVoteSubmit: (CommandDanmakuItem, VoteOption) -> Unit
) {
    val selectedOptionId = state.selection(item.id)?.id
    val selectedGradeScore = state.selection(item.id)?.score
    val isGrade = item.voteKind == VoteDanmakuKind.GRADE
    val title = item.voteTitle.ifBlank { item.content }
    val gradeStarOptions = remember(item.id, item.voteOptions) {
        resolveGradeStarOptions(item.voteOptions)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeightDp)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(start = 10.dp, end = 52.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AppText(
                text = if (isGrade) "打分弹幕" else "互动投票",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.74f)
            )
            AppText(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        when {
            isGrade -> GradeStarRating(
                modifier = Modifier.padding(start = 10.dp),
                starOptions = gradeStarOptions,
                selectedScore = selectedGradeScore,
                onSelect = { option ->
                    if (state.select(item.id, option)) {
                        onVoteSubmit(item, option)
                    }
                }
            )
            item.voteOptions.isEmpty() -> AppText(
                text = "点击屏幕参与投票",
                modifier = Modifier.padding(start = 10.dp, end = 52.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            else -> item.voteOptions.forEach { option ->
                val isSelected = selectedOptionId == option.id
                val isSubmitted = selectedOptionId != null
                AppButton(
                    onClick = {
                        if (state.select(item.id, option)) {
                            onVoteSubmit(item, option)
                        }
                    },
                    enabled = !isSubmitted,
                    shape = AppShapes.container(ContainerLevel.Chip),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.14f),
                        contentColor = Color.White,
                        disabledContainerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.10f),
                        disabledContentColor = Color.White.copy(alpha = 0.85f)
                    ),
                    elevation = null,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .padding(start = 10.dp, end = 52.dp)
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    AppText(
                        text = option.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (selectedOptionId != null) {
            AppText(
                text = if (isGrade) "✓ 已打分" else "✓ 已投票",
                modifier = Modifier.padding(start = 10.dp, end = 52.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun GradeStarRating(
    modifier: Modifier = Modifier,
    starOptions: List<VoteOption?>,
    selectedScore: Int?,
    onSelect: (VoteOption) -> Unit
) {
    val hasAvailableScore = starOptions.any { it != null }
    val selectedIndex = selectedScore?.let { score ->
        starOptions.indexOfFirst { it?.score == score }
    } ?: -1
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        starOptions.forEachIndexed { index, option ->
            val isFilled = selectedIndex >= 0 && index <= selectedIndex
            val canSelect = option != null && selectedScore == null
            AppIconButton(
                onClick = { option?.let(onSelect) },
                enabled = canSelect,
                modifier = Modifier.size(48.dp),
                colors = AppIconButtonDefaults.colors(
                    containerColor = if (isFilled) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    } else {
                        Color.White.copy(alpha = 0.08f)
                    },
                    contentColor = if (option != null) Color(0xFFFFC107) else Color.White.copy(alpha = 0.26f),
                    disabledContainerColor = if (isFilled && option != null) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
                    } else {
                        Color.White.copy(alpha = 0.04f)
                    },
                    disabledContentColor = if (isFilled && option != null) {
                        Color(0xFFFFC107)
                    } else {
                        Color.White.copy(alpha = 0.26f)
                    }
                )
            ) {
                AppIcon(
                    imageVector = if (isFilled) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = if (option == null) {
                        "${index + 1}星不可用"
                    } else {
                        "${index + 1}星"
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
    if (!hasAvailableScore) {
        AppText(
            text = "当前评分档位不可提交",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

internal fun resolveCommandDanmakuCardWidthPx(
    containerWidthPx: Int,
    requestedCardWidthPx: Int
): Int {
    return requestedCardWidthPx.coerceIn(0, containerWidthPx.coerceAtLeast(0))
}

internal fun resolveCommandDanmakuVerticalOffsetPx(
    containerHeightPx: Int,
    cardHeightPx: Int,
    yRatio: Float
): Int {
    val safeHeight = containerHeightPx.coerceAtLeast(0)
    val maxOffset = (safeHeight - cardHeightPx.coerceAtLeast(0)).coerceAtLeast(0)
    return (safeHeight * yRatio).roundToInt().coerceIn(0, maxOffset)
}

internal fun resolveCommandDanmakuHorizontalOffsetPx(
    containerWidthPx: Int,
    cardWidthPx: Int,
    xRatio: Float
): Int {
    val maxOffset = (containerWidthPx - cardWidthPx).coerceAtLeast(0)
    return (containerWidthPx * xRatio).roundToInt().coerceIn(0, maxOffset)
}

internal fun mapAttentionPosition(posX: Float, posY: Float): Pair<Float, Float> {
    val x = ((posX - 118f) / (549f - 118f)).coerceIn(0f, 0.82f)
    val y = ((posY - 82f) / (293f - 82f)).coerceIn(0f, 0.78f)
    return x to y
}
