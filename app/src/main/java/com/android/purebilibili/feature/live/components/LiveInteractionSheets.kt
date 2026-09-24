package com.android.purebilibili.feature.live.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Report
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.components.AppAssistChip
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppInputChip
import com.android.purebilibili.core.ui.rememberAppClearIcon
import androidx.compose.material3.ExperimentalMaterial3Api
import com.android.purebilibili.core.ui.components.AppFilterChip
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.AppModalBottomSheet
import com.android.purebilibili.core.ui.components.AppOutlinedTextField
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.ui.components.AppHorizontalDivider
import com.android.purebilibili.core.ui.components.AppSegmentOption
import com.android.purebilibili.core.ui.components.AppThemeAdaptiveTabRow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.purebilibili.data.repository.DefaultLiveReportReasons
import com.android.purebilibili.data.repository.LiveEmoticonItem
import com.android.purebilibili.data.repository.LiveEmoticonPackage
import com.android.purebilibili.data.repository.LiveReportReason
import com.android.purebilibili.data.repository.LiveShieldInfo
import com.android.purebilibili.data.repository.LiveShieldUser
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.feature.live.LiveDanmakuItem
import com.android.purebilibili.feature.live.resolveLiveSheetVisualSpec

private object LiveInteractionSheetLayoutSpec {
    const val EmoticonEmptyStateHeightDp = 200f
    const val EmoticonTabDividerThicknessDp = 0.5f
    const val DeleteKeywordActionIconSizeDp = 14f
    const val ShieldedUserIconSizeDp = 18f
}

@Composable
fun LiveReportDialog(
    target: LiveDanmakuItem,
    onDismiss: () -> Unit,
    onReport: (LiveReportReason) -> Unit
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        icon = { AppIcon(Icons.Outlined.Report, contentDescription = null) },
        title = { AppText("举报弹幕") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Medium)) {
                AppText(
                    text = "@${target.uname.ifBlank { target.uid.toString() }}：${target.text}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small)) {
                    DefaultLiveReportReasons.forEach { reason ->
                        AppAssistChip(
                            onClick = { onReport(reason) },
                            label = { AppText(reason.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            AppTextButton(onClick = onDismiss) {
                AppText("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveEmoticonSheet(
    packages: List<LiveEmoticonPackage>,
    onSelected: (LiveEmoticonItem) -> Unit,
    onDismiss: () -> Unit
) {
    val visualSpec = remember { resolveLiveSheetVisualSpec() }
    var selectedPkgIndex by remember { mutableIntStateOf(0) }

    AppModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppSpacingTokens.Large,
                    vertical = AppSpacingTokens.Small,
                ),
            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Medium)
        ) {
            AppText(
                text = "直播表情",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = AppSpacingTokens.Small)
            )
            if (packages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LiveInteractionSheetLayoutSpec.EmoticonEmptyStateHeightDp.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AppText(
                        text = "当前直播间暂无可用表情",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                // 表情包分类 Tab 栏（对齐 PiliPlus / B站表情选择体验）
                if (packages.size > 1) {
                    val safeIndex = selectedPkgIndex.coerceIn(0, packages.lastIndex)
                    AppThemeAdaptiveTabRow(
                        options = packages.mapIndexed { index, pkg ->
                            AppSegmentOption(index, pkg.name)
                        },
                        selectedValue = safeIndex,
                        onSelectionChange = { selectedPkgIndex = it },
                        modifier = Modifier.fillMaxWidth(),
                        scrollable = true,
                        labelFontSize = visualSpec.emoticonTabLabelFontSizeSp.sp,
                    )
                    AppHorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        thickness = LiveInteractionSheetLayoutSpec.EmoticonTabDividerThicknessDp.dp
                    )
                }

                val currentPackage = packages.getOrNull(selectedPkgIndex.coerceIn(0, packages.lastIndex))
                val currentEmotes = currentPackage?.items.orEmpty()

                if (currentEmotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(LiveInteractionSheetLayoutSpec.EmoticonEmptyStateHeightDp.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            text = "该表情包暂无表情",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(visualSpec.emoticonListMaxHeightDp.dp),
                        contentPadding = PaddingValues(AppSpacingTokens.ExtraSmall),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
                        verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small)
                    ) {
                        items(
                            items = currentEmotes,
                            key = { "${currentPackage?.id ?: 0}-${it.emoji}" }
                        ) { item ->
                            LiveEmoticonGridCell(item = item, onClick = { onSelected(item) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveEmoticonGridCell(
    item: LiveEmoticonItem,
    onClick: () -> Unit
) {
    AppSurface(
        onClick = onClick,
        color = AppSurfaceTokens.cardContainer(),
        shape = AppShapes.container(ContainerLevel.Chip),
        modifier = Modifier
            .aspectRatio(1f)
            .clip(AppShapes.container(ContainerLevel.Chip))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppSpacingTokens.Small),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = item.url,
                contentDescription = item.description.ifBlank { item.emoji },
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveDmBlockSheet(
    shieldInfo: LiveShieldInfo?,
    isLoggedIn: Boolean,
    onAddKeyword: (String) -> Unit,
    onDeleteKeyword: (String) -> Unit,
    onUnblockUser: (LiveShieldUser) -> Unit,
    onSetRule: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var keyword by remember { mutableStateOf("") }
    AppModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppSpacingTokens.ExtraLarge,
                    vertical = AppSpacingTokens.Small,
                )
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Large)
        ) {
            AppText(
                text = "弹幕屏蔽",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (!isLoggedIn) {
                AppText(
                    text = "登录后可同步直播间屏蔽词、屏蔽用户和规则。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppOutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it.take(20) },
                    enabled = isLoggedIn,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { AppText("新增屏蔽词") }
                )
                AppButton(
                    enabled = isLoggedIn && keyword.trim().isNotBlank(),
                    onClick = {
                        onAddKeyword(keyword.trim())
                        keyword = ""
                    }
                ) {
                    AppText("添加")
                }
            }
            LiveRuleSection(
                shieldInfo = shieldInfo,
                enabled = isLoggedIn,
                onSetRule = onSetRule
            )
            LiveKeywordSection(
                shieldInfo = shieldInfo,
                enabled = isLoggedIn,
                onDeleteKeyword = onDeleteKeyword
            )
            LiveShieldUserSection(
                shieldInfo = shieldInfo,
                enabled = isLoggedIn,
                onUnblockUser = onUnblockUser
            )
            Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiveRuleSection(
    shieldInfo: LiveShieldInfo?,
    enabled: Boolean,
    onSetRule: (String, Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small)) {
        AppText("规则", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        // FlowRow：5 个规则 chip 在窄屏自动换行
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small)) {
            AppFilterChip(
                selected = (shieldInfo?.level ?: 0) > 0,
                enabled = enabled,
                onClick = { onSetRule("level", if ((shieldInfo?.level ?: 0) > 0) 0 else 5) },
                label = { AppText("等级") }
            )
            AppFilterChip(
                selected = (shieldInfo?.medal ?: 0) > 0,
                enabled = enabled,
                onClick = { onSetRule("medal", if ((shieldInfo?.medal ?: 0) > 0) 0 else 1) },
                label = { AppText("勋章") }
            )
            AppFilterChip(
                selected = (shieldInfo?.verify ?: 0) > 0,
                enabled = enabled,
                onClick = { onSetRule("verify", if ((shieldInfo?.verify ?: 0) > 0) 0 else 1) },
                label = { AppText("认证") }
            )
            AppFilterChip(
                selected = (shieldInfo?.rank ?: 0) > 0,
                enabled = enabled,
                onClick = { onSetRule("rank", if ((shieldInfo?.rank ?: 0) > 0) 0 else 1) },
                label = { AppText("非正式会员") }
            )
            AppFilterChip(
                selected = (shieldInfo?.phone ?: 0) > 0,
                enabled = enabled,
                onClick = { onSetRule("phone", if ((shieldInfo?.phone ?: 0) > 0) 0 else 1) },
                label = { AppText("未绑定手机") }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiveKeywordSection(
    shieldInfo: LiveShieldInfo?,
    enabled: Boolean,
    onDeleteKeyword: (String) -> Unit
) {
    val clearIcon = rememberAppClearIcon()
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small)) {
        AppText("关键词", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        val keywords = shieldInfo?.keywords.orEmpty()
        if (keywords.isEmpty()) {
            AppText("暂无屏蔽词", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
                verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
                modifier = Modifier.fillMaxWidth()
            ) {
                keywords.forEach { item ->
                    AppInputChip(
                        selected = false,
                        onClick = {},
                        label = {
                            AppText(
                                text = item.keyword,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        trailingIcon = {
                            AppIconButton(
                                onClick = { if (enabled) onDeleteKeyword(item.keyword) },
                                enabled = enabled,
                                modifier = Modifier.size(AppSpacingTokens.ExtraLarge)
                            ) {
                                AppIcon(
                                    clearIcon,
                                    contentDescription = "删除屏蔽词",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(
                                        LiveInteractionSheetLayoutSpec.DeleteKeywordActionIconSizeDp.dp,
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveShieldUserSection(
    shieldInfo: LiveShieldInfo?,
    enabled: Boolean,
    onUnblockUser: (LiveShieldUser) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small)) {
        AppText("用户", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        val users = shieldInfo?.users.orEmpty()
        if (users.isEmpty()) {
            AppText("暂无屏蔽用户", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
                modifier = Modifier.fillMaxWidth()
            ) {
                users.forEach { user ->
                    AppSurface(
                        color = AppSurfaceTokens.cardContainer(),
                        shape = AppShapes.container(ContainerLevel.Chip),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = AppSpacingTokens.Medium,
                                    vertical = AppSpacingTokens.Small
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(
                                Icons.Outlined.Block,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(
                                    LiveInteractionSheetLayoutSpec.ShieldedUserIconSizeDp.dp,
                                )
                            )
                            AppText(
                                text = user.uname.ifBlank { user.uid.toString() },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = AppSpacingTokens.Medium)
                            )
                            AppTextButton(enabled = enabled, onClick = { onUnblockUser(user) }) {
                                AppText("解除")
                            }
                        }
                    }
                }
            }
        }
    }
}
