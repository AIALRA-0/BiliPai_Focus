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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.ui.AppIcons
import com.android.purebilibili.feature.video.danmaku.CommandDanmakuItem
import com.android.purebilibili.feature.video.danmaku.CommandDanmakuType
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
internal fun CommandDanmakuOverlay(
    items: List<CommandDanmakuItem>,
    player: Player,
    onFollowClick: () -> Unit,
    onTripleClick: () -> Unit,
    isFollowing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val currentPosition by produceState(initialValue = player.currentPosition, key1 = player) {
        while (true) {
            if (player.isPlaying) value = player.currentPosition
            kotlinx.coroutines.delay(80)
        }
    }
    val itemIdentity = remember(items) { items.joinToString(separator = "|") { it.id } }
    var dismissedIds by remember(itemIdentity) { mutableStateOf(emptySet<String>()) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val active = items.filter {
            it.id !in dismissedIds &&
                currentPosition in it.startTimeMs..(it.startTimeMs + it.durationMs)
        }
        active.forEach { item ->
            key(item.id) {
                CommandDanmakuCard(
                    item = item,
                    containerWidth = constraints.maxWidth,
                    containerHeight = constraints.maxHeight,
                    onFollowClick = onFollowClick,
                    onTripleClick = onTripleClick,
                    isFollowing = isFollowing,
                    onDismiss = {
                        dismissedIds = dismissedIds + item.id
                    }
                )
            }
        }
    }
}

@Composable
private fun CommandDanmakuCard(
    item: CommandDanmakuItem,
    containerWidth: Int,
    containerHeight: Int,
    onFollowClick: () -> Unit,
    onTripleClick: () -> Unit,
    isFollowing: Boolean,
    onDismiss: () -> Unit
) {
    val (xRatio, yRatio) = when (item.type) {
        CommandDanmakuType.ATTENTION -> mapAttentionPosition(item.posX, item.posY)
        CommandDanmakuType.UP -> 0.08f to 0.10f
        CommandDanmakuType.LINK -> 0.08f to 0.18f
        CommandDanmakuType.TEXT -> 0.08f to 0.10f
    }
    val cardWidthDp = when (item.type) {
        CommandDanmakuType.ATTENTION -> resolveAttentionCommandCardWidthDp(item.attentionType)
        else -> 220
    }
    val density = LocalDensity.current
    val cardWidthPx = with(density) { cardWidthDp.dp.roundToPx() }
    val x = resolveCommandDanmakuHorizontalOffsetPx(containerWidth, cardWidthPx, xRatio)
    val y = (containerHeight * yRatio).roundToInt()

    Surface(
        modifier = Modifier
            .offset { IntOffset(x, y) }
            .width(cardWidthDp.dp),
        color = resolveCommandDanmakuContainerColor(item.type),
        contentColor = Color.White,
        shape = RoundedCornerShape(8.dp)
    ) {
        Box {
            when (item.type) {
                CommandDanmakuType.ATTENTION -> AttentionCommandCard(
                    item = item,
                    isFollowing = isFollowing,
                    onFollowClick = onFollowClick,
                    onTripleClick = onTripleClick
                )
                else -> InfoCommandCard(item)
            }
            CommandDanmakuCloseButton(
                onDismiss = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
            )
        }
    }
}

@Composable
private fun InfoCommandCard(item: CommandDanmakuItem) {
    Row(
        modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 34.dp, bottom = 8.dp),
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
            Text(
                text = if (item.type == CommandDanmakuType.LINK) "关联视频" else "UP 主提示",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.74f)
            )
            Text(
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
        modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 34.dp, bottom = 8.dp),
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
        Button(
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
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BiliPink,
                contentColor = Color.White
            ),
            elevation = null,
            contentPadding = PaddingValues(horizontal = 14.dp),
            modifier = Modifier
                .height(36.dp)
                .fillMaxWidth()
        ) {
            Text(
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
    IconButton(
        onClick = onDismiss,
        modifier = modifier.size(30.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Black.copy(alpha = 0.34f),
            contentColor = Color.White
        )
    ) {
        Icon(
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
                .clip(RoundedCornerShape(999.dp))
                .background(BiliPink.copy(alpha = 0.16f))
                .widthIn(min = 118.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            CommandTripleActionIcon(
                icon = Icons.Rounded.ThumbUp,
                progress = progress,
                color = BiliPink
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
    Surface(
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
            Icon(
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
