// 文件路径: feature/video/SponsorSkipUI.kt
package com.android.purebilibili.feature.video.ui.components
import com.android.purebilibili.core.ui.components.AppFilterChip
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppTextButton

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.rememberAppClearIcon
import com.android.purebilibili.data.model.response.SponsorCategory
import com.android.purebilibili.feature.plugin.sponsorBlockAllowedActionTypes
import com.android.purebilibili.data.model.response.SponsorSegment
import com.android.purebilibili.feature.video.viewmodel.SponsorContributionPhase
import com.android.purebilibili.feature.video.viewmodel.SponsorContributionUiState
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel

/**
 * 空降助手跳过按钮 UI
 * 显示在视频右下角，允许用户跳过广告片段
 */
@Composable
fun SponsorSkipButton(
    segment: SponsorSegment?,
    visible: Boolean,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
    onVote: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val clearIcon = rememberAppClearIcon()

    AnimatedVisibility(
        visible = visible && segment != null,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(200)),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(200)),
        modifier = modifier
    ) {
        segment?.let { seg ->
            AppSurface(
                modifier = Modifier
                    .padding(16.dp)
                    .clip(AppShapes.container(ContainerLevel.Card)),
                color = Color.Black.copy(alpha = 0.8f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 类别标签
                    AppText(
                        text = seg.categoryName,
                        color = Color(0xFFFFA500),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // 跳过按钮
                    AppSurface(
                        modifier = Modifier
                            .clip(AppShapes.container(ContainerLevel.Chip))
                            .clickable { onSkip() },
                        color = Color(0xFF00C853)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AppIcon(
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = "跳过",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            AppText(
                                text = "跳过",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    AppTextButton(onClick = { onVote(1) }) { AppText("有用") }
                    AppTextButton(onClick = { onVote(-1) }) { AppText("不准确") }
                    
                    // 关闭按钮
                    AppIconButton(onClick = onDismiss) {
                        AppIcon(
                            imageVector = clearIcon,
                            contentDescription = "忽略",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Explicit community-contribution control. The ViewModel owns the submission state; this
 * composable only renders the current phase and forwards user intent.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SponsorContributionOverlay(
    state: SponsorContributionUiState,
    onMarkBoundary: () -> Unit,
    onCategoryChange: (String) -> Unit,
    onActionTypeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.showsMarkAction) {
        val marking = state.phase == SponsorContributionPhase.MARKING
        AppSurface(
            modifier = modifier
                .clip(AppShapes.container(ContainerLevel.Card))
                .clickable { onMarkBoundary() },
            color = Color.Black.copy(alpha = 0.8f),
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppIcon(
                    imageVector = Icons.Outlined.Send,
                    contentDescription = null,
                    tint = Color(0xFF7C9EFF),
                    modifier = Modifier.size(18.dp),
                )
                Column {
                    AppText(
                        text = if (marking) "结束标记" else "标记片段",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    AppText(
                        text = if (marking) {
                            "起点 ${formatSponsorContributionTime(state.startMs ?: 0L)}"
                        } else {
                            "投稿前会再次确认"
                        },
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }

    if (state.showsReview) {
        AppAlertDialog(
            onDismissRequest = {
                if (state.phase != SponsorContributionPhase.SUBMITTING) onCancel()
            },
            title = {
                AppText(
                    when (state.phase) {
                        SponsorContributionPhase.SUCCESS -> "社区片段已提交"
                        SponsorContributionPhase.SUBMITTING -> "正在提交社区片段"
                        else -> "确认提交社区片段"
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppText(
                        text = "${formatSponsorContributionTime(state.startMs ?: 0L)} – ${formatSponsorContributionTime(state.endMs ?: 0L)}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (state.phase != SponsorContributionPhase.SUCCESS) {
                        AppText(
                            text = "选择片段类别",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SponsorCategory.ALL_CATEGORIES.forEach { category ->
                                AppFilterChip(
                                    selected = state.category == category,
                                    onClick = { onCategoryChange(category) },
                                    enabled = state.phase == SponsorContributionPhase.REVIEW,
                                    label = { AppText(SponsorCategory.getCategoryName(category)) },
                                )
                            }
                        }
                        AppText(
                            text = "动作类型",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            sponsorBlockAllowedActionTypes(state.category).forEach { actionType ->
                                AppFilterChip(
                                    selected = state.actionType == actionType,
                                    onClick = { onActionTypeChange(actionType) },
                                    enabled = state.phase == SponsorContributionPhase.REVIEW,
                                    label = { AppText(sponsorActionTypeLabel(actionType)) },
                                )
                            }
                        }
                        AppText(
                            text = "提交将发送类别、时间段、视频标识和社区用户 ID 至 ${state.serverBaseUrl}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    state.message?.let { message ->
                        AppText(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.phase == SponsorContributionPhase.SUCCESS) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    }
                }
            },
            confirmButton = {
                when (state.phase) {
                    SponsorContributionPhase.REVIEW -> {
                        AppTextButton(onClick = onSubmit) { AppText("确认提交") }
                    }
                    SponsorContributionPhase.SUCCESS -> {
                        AppTextButton(onClick = onCancel) { AppText("完成") }
                    }
                    SponsorContributionPhase.SUBMITTING -> AppText("提交中…")
                    else -> Unit
                }
            },
            dismissButton = {
                if (state.phase == SponsorContributionPhase.REVIEW) {
                    AppTextButton(onClick = onCancel) { AppText("取消") }
                }
            },
        )
    }
}

private fun sponsorActionTypeLabel(actionType: String): String = when (actionType) {
    "skip" -> "跳过"
    "mute" -> "静音"
    "full" -> "整段标记"
    "poi" -> "精彩时刻"
    else -> actionType
}

private fun formatSponsorContributionTime(positionMs: Long): String {
    val totalSeconds = (positionMs.coerceAtLeast(0L) / 1_000L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

/**
 * 自动跳过提示 Toast
 * 屏幕顶部短暂显示
 */
@Composable
fun SponsorSkipToast(
    message: String?,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && message != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        message?.let {
            AppSurface(
                modifier = Modifier
                    .padding(top = 60.dp, start = 16.dp, end = 16.dp)
                    .clip(AppShapes.container(ContainerLevel.Floating)),
                color = Color(0xFF00C853).copy(alpha = 0.9f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppIcon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    AppText(
                        text = it,
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
