// 文件路径: feature/video/ui/components/CollectionRow.kt
package com.android.purebilibili.feature.video.ui.components
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.rememberAppShareIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.data.model.response.UgcSeason
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Folder
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.feature.video.ui.VideoDetailShapes

/**
 *  视频合集展示行
 * 显示合集名称、当前集数/总集数
 */
@Composable
fun CollectionRow(
    ugcSeason: UgcSeason,
    currentBvid: String,
    currentCid: Long = 0L,
    isPlaying: Boolean = false,
    immersive: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shareIcon = rememberAppShareIcon()
    val collectionSubscriptionId = remember(ugcSeason) { resolveCollectionSubscriptionId(ugcSeason) }
    val allEpisodes = remember(ugcSeason.sections) { ugcSeason.sections.flatMap { it.episodes } }
    val currentAid = remember(allEpisodes, currentBvid, currentCid) {
        resolveCurrentUgcEpisodeAid(
            episodes = allEpisodes,
            currentBvid = currentBvid,
            currentCid = currentCid
        )
    }
    val sortMode by SettingsManager
        .getCollectionSortMode(context, collectionSubscriptionId)
        .collectAsStateWithLifecycle(initialValue = CollectionSortMode.ASCENDING
        )

    // 计算当前视频在合集中的位置
    val currentIndex = resolveCurrentUgcEpisodeIndex(
        episodes = allEpisodes,
        currentBvid = currentBvid,
        currentCid = currentCid
    )
    val currentPosition = if (currentIndex >= 0) currentIndex + 1 else 0
    val totalCount = allEpisodes.size.takeIf { it > 0 } ?: ugcSeason.ep_count
    val accentColor = if (immersive) Color.White else MaterialTheme.colorScheme.primary
    val titleColor = if (immersive) Color.White.copy(alpha = 0.94f) else MaterialTheme.colorScheme.onSurface
    val secondaryColor = if (immersive) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
    
    AppSurface(
        modifier = modifier
            .fillMaxWidth(),
        shape = androidx.compose.ui.graphics.RectangleShape,
        color = Color.Transparent  // 透明背景，与周围统一
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            //  合集图标
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(VideoDetailShapes.compactIcon())
                    .background(accentColor.copy(alpha = if (immersive) 0.18f else 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(
                    Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            //  合集信息
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppText(
                        text = "合集",
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    AppText(
                        text = ugcSeason.title,
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.bodyMedium,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CollectionPlaybackIndicator(
                        isPlaying = isPlaying,
                        color = accentColor,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentPosition > 0 && totalCount > 0) {
                        AppText(
                            text = "$currentPosition/$totalCount",
                            style = MaterialTheme.typography.labelMedium,
                            color = secondaryColor,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    AppText(
                        text = resolveCollectionSortLabel(sortMode),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (immersive) secondaryColor else accentColor.copy(alpha = 0.88f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            CollectionSubscriptionButton(
                collectionId = collectionSubscriptionId,
                currentBvid = currentBvid,
                currentAid = currentAid,
                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                immersive = immersive,
            )

            //  分享按钮
            AppIconButton(
                onClick = {
                    val shareUrl = "https://space.bilibili.com/${ugcSeason.mid}/lists/${ugcSeason.id}?type=season"
                    val shareText = "${ugcSeason.title}\n$shareUrl"
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "分享合集"))
                },
                modifier = Modifier.size(28.dp)
            ) {
                AppIcon(
                    shareIcon,
                    contentDescription = "分享合集",
                    modifier = Modifier.size(16.dp),
                    tint = accentColor.copy(alpha = if (immersive) 0.9f else 1f)
                )
            }
            
            //  右侧箭头
            AppIcon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "查看合集",
                tint = secondaryColor.copy(alpha = if (immersive) 0.8f else 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** Three native-drawn equalizer bars: animated only while playback is active. */
@Composable
private fun CollectionPlaybackIndicator(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val progress = if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "collectionPlayback")
        val animatedProgress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "collectionPlaybackProgress",
        )
        animatedProgress
    } else {
        0f
    }

    Canvas(
        modifier = modifier
            .size(width = 14.dp, height = 16.dp)
            .semantics {
                contentDescription = if (isPlaying) "合集视频正在播放" else "合集视频已暂停"
            },
    ) {
        drawCollectionPlaybackBars(
            progress = progress,
            isPlaying = isPlaying,
            color = color,
        )
    }
}

private fun DrawScope.drawCollectionPlaybackBars(
    progress: Float,
    isPlaying: Boolean,
    color: Color,
) {
    val barWidth = size.width * 0.18f
    val gap = size.width * 0.14f
    val minHeight = size.height * 0.28f
    val availableHeight = size.height - minHeight
    val pausedFractions = floatArrayOf(0.42f, 0.72f, 0.52f)

    repeat(3) { index ->
        val heightFraction = if (isPlaying) {
            val phase = progress * 2f * PI.toFloat() + index * 2.1f
            0.5f + 0.5f * sin(phase)
        } else {
            pausedFractions[index]
        }
        val barHeight = minHeight + availableHeight * heightFraction
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(
                x = size.width * 0.09f + index * (barWidth + gap),
                y = size.height - barHeight,
            ),
            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f),
        )
    }
}
