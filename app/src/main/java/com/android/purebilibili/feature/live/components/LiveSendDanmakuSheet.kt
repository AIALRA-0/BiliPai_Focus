package com.android.purebilibili.feature.live.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.android.purebilibili.core.ui.components.AppButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.AppModalBottomSheet
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppOutlinedTextField
import com.android.purebilibili.core.ui.components.AppSegmentOption
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppThemeAdaptiveTabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.data.repository.LiveDanmakuPermission
import com.android.purebilibili.feature.live.LiveDanmakuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.EmojiEmotions

/** 弹幕默认颜色（白色） */
private const val LIVE_DANMAKU_DEFAULT_COLOR = 16777215
/** 弹幕默认模式（1 = 滚动） */
private const val LIVE_DANMAKU_DEFAULT_MODE = 1
/** 颜色选择器最多展示的色块数量 */
private const val LIVE_DANMAKU_MAX_COLOR_SWATCHES = 8

/**
 * 发送直播弹幕弹窗
 *
 * 支持选择弹幕颜色与滚动模式（依据服务端返回的权限 [LiveDanmakuPermission]），
 * 发送时携带 color / mode 字段；无权限时回退默认白色滚动。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveSendDanmakuSheet(
    onDismiss: () -> Unit,
    onSend: (String, Int, Int) -> Unit,
    permission: LiveDanmakuPermission = LiveDanmakuPermission(),
    replyTarget: LiveDanmakuItem? = null,
    onOpenEmote: (() -> Unit)? = null,
) {
    var message by remember { mutableStateOf("") }
    val maxLength = permission.maxLength.takeIf { it > 0 } ?: 40
    val defaultColor = permission.availableColors.firstOrNull()?.color ?: LIVE_DANMAKU_DEFAULT_COLOR
    val defaultMode = permission.availableModes.firstOrNull { it.mode == LIVE_DANMAKU_DEFAULT_MODE }?.mode
        ?: permission.availableModes.firstOrNull()?.mode
        ?: LIVE_DANMAKU_DEFAULT_MODE
    var selectedColor by remember { mutableIntStateOf(defaultColor) }
    var selectedMode by remember { mutableIntStateOf(defaultMode) }
    val canChooseStyle = permission.availableColors.isNotEmpty() && permission.availableModes.isNotEmpty()

    AppModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .align(Alignment.CenterHorizontally)
                .padding(
                    horizontal = AppSpacingTokens.ExtraLarge,
                    vertical = AppSpacingTokens.Small,
                ),
            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Large)
        ) {
            AppText(
                text = if (replyTarget == null) "发弹幕" else "回复 @${replyTarget.uname.ifBlank { replyTarget.uid.toString() }}",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            AppSurface(
                color = AppSurfaceTokens.cardContainer(),
                shape = AppShapes.container(ContainerLevel.Card)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacingTokens.Large),
                    verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Medium)
                ) {
                    AppOutlinedTextField(
                        value = message,
                        onValueChange = { message = it.take(maxLength) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 4,
                        placeholder = { AppText(if (replyTarget == null) "输入弹幕内容" else "输入回复内容") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            val content = message.trim()
                            if (content.isNotEmpty() && permission.canSend) {
                                onSend(content, selectedColor, selectedMode)
                            }
                        })
                    )
                    if (canChooseStyle) {
                        DanmakuColorSelector(
                            options = permission.availableColors,
                            selectedColor = selectedColor,
                            onColorSelected = { selectedColor = it }
                        )
                        DanmakuModeSelector(
                            options = permission.availableModes,
                            selectedMode = selectedMode,
                            onModeSelected = { selectedMode = it }
                        )
                    }
                    AppText(
                        text = buildString {
                            append(permission.statusText)
                            append(" · ")
                            append(message.length)
                            append("/")
                            append(maxLength)
                            if (permission.availableColors.isNotEmpty()) {
                                append(" · ")
                                append(permission.availableColors.take(3).joinToString("、") { it.name })
                            }
                            if (permission.availableModes.isNotEmpty()) {
                                append(" · ")
                                append(permission.availableModes.take(2).joinToString("、") { it.name })
                            }
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (onOpenEmote != null) {
                            AppIconButton(onClick = onOpenEmote) {
                                AppIcon(
                                    imageVector = Icons.Outlined.EmojiEmotions,
                                    contentDescription = "表情",
                                )
                            }
                        } else {
                            Box(Modifier)
                        }
                        AppButton(
                            enabled = permission.canSend && message.trim().isNotEmpty(),
                            onClick = { onSend(message.trim(), selectedColor, selectedMode) }
                        ) {
                            AppText("发送")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DanmakuColorSelector(
    options: List<com.android.purebilibili.data.repository.LiveDanmakuColorOption>,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall)) {
        AppText(
            text = "弹幕颜色",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
        // FlowRow：窄屏自动换行，避免 8 个色块横向溢出
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small)
        ) {
            options.take(LIVE_DANMAKU_MAX_COLOR_SWATCHES).forEach { option ->
                val argb = (0xFF000000L or option.color.toLong()).toInt()
                val isSelected = selectedColor == option.color
                val swatchColor = Color(argb)
                // 外层 48dp 触摸目标（满足最小交互尺寸），内层 24dp 视觉色块
                Box(
                    modifier = Modifier
                        .size(AppSpacingTokens.TripleExtraLarge)
                        .clickable(
                            role = Role.RadioButton,
                            onClickLabel = option.name,
                            onClick = { onColorSelected(option.color) }
                        )
                        .semantics(mergeDescendants = true) {
                            contentDescription = option.name
                            this.selected = isSelected
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(AppSpacingTokens.ExtraLarge)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .border(
                                width = if (isSelected) {
                                    AppSpacingTokens.Micro
                                } else {
                                    AppSurfaceTokens.OutlineWidth
                                },
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            AppIcon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                // 浅色色块用黑色勾，深色用白色勾，保证对比度
                                tint = if (swatchColor.luminance() > 0.5f) {
                                    Color.Black
                                } else {
                                    Color.White
                                },
                                modifier = Modifier.size(AppSpacingTokens.Large)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DanmakuModeSelector(
    options: List<com.android.purebilibili.data.repository.LiveDanmakuModeOption>,
    selectedMode: Int,
    onModeSelected: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall)) {
        AppText(
            text = "弹幕模式",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
        AppThemeAdaptiveTabRow(
            options = options.map { option -> AppSegmentOption(option.mode, option.name) },
            selectedValue = selectedMode,
            onSelectionChange = onModeSelected,
            scrollable = true,
            minTabWidth = 72.dp,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally),
        )
    }
}
