package com.android.purebilibili.feature.dynamic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.purebilibili.core.store.DEFAULT_FOCUS_FOLLOW_GROUP_ID
import com.android.purebilibili.core.store.FocusFollowGroup
import com.android.purebilibili.core.store.FocusFollowGroupConfig
import com.android.purebilibili.core.store.FocusFollowHomeFeedSortMode
import com.android.purebilibili.core.store.canCreateFocusFollowGroup
import com.android.purebilibili.core.store.normalizeFocusFollowGroupName
import com.android.purebilibili.core.store.resolveFocusFollowGroupForUser
import com.android.purebilibili.core.ui.AppChromeSizeTokens
import com.android.purebilibili.core.ui.AppModalBottomSheet
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppOutlinedButton
import com.android.purebilibili.data.model.response.FollowingUser
import com.android.purebilibili.feature.dynamic.FocusFollowAssignmentSection
import com.android.purebilibili.feature.dynamic.DynamicTypographyPolicy
import com.android.purebilibili.feature.dynamic.buildFocusFollowAssignmentSections
import com.android.purebilibili.feature.dynamic.filterFocusFollowAssignmentSections

// Component-specific radii preserve the existing Focus group sheet across theme migrations.
private object FocusFollowGroupShapeSpec {
    const val SmallRadiusDp = 14
    const val MediumRadiusDp = 16
    const val SectionRadiusDp = 18
    const val CardRadiusDp = 20
    const val InputRadiusDp = 26
    const val PillRadiusDp = 999
}

// Nonstandard dimensions stay named by their role instead of being folded into
// the shared spacing scale. Common layout gaps continue to use AppSpacingTokens.
private object FocusFollowGroupLayoutSpec {
    const val InputHeightDp = 60
    const val ActionButtonHeightDp = 52
    const val ActionButtonHorizontalPaddingDp = 18
    const val SortControlHorizontalPaddingDp = 18
    const val SheetBottomPaddingDp = 28
    const val SectionSpacingDp = 14
    const val CompactGapDp = 6
    const val BadgeHorizontalPaddingDp = 10
    const val LeadingIconSizeDp = 18
    const val EmptyStateHorizontalPaddingDp = 14
    const val EmptyStateVerticalPaddingDp = 14
    const val MemberRowSpacingDp = 10
    const val SectionDividerThicknessDp = 1
    const val SurfaceTonalElevationDp = 1
    const val AvatarSizeDp = 46
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusFollowGroupSheet(
    config: FocusFollowGroupConfig,
    followings: List<FollowingUser>,
    isLoading: Boolean,
    onDismissRequest: () -> Unit,
    onRefreshFollowings: () -> Unit,
    onCreateGroup: (String) -> Unit,
    onRenameGroup: (String, String) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onSetGroupVisible: (String, Boolean) -> Unit,
    onSetHomeFeedSortMode: (FocusFollowHomeFeedSortMode) -> Unit,
    onAssignUserToGroup: (Long, String) -> Unit
) {
    val inputHeight = FocusFollowGroupLayoutSpec.InputHeightDp.dp
    val actionButtonHeight = FocusFollowGroupLayoutSpec.ActionButtonHeightDp.dp
    val inputShape = RoundedCornerShape(FocusFollowGroupShapeSpec.InputRadiusDp.dp)
    val actionButtonContentPadding = PaddingValues(
        horizontal = FocusFollowGroupLayoutSpec.ActionButtonHorizontalPaddingDp.dp,
        vertical = AppSpacingTokens.None
    )
    var newGroupName by rememberSaveable { mutableStateOf("") }
    var followSearchQuery by rememberSaveable { mutableStateOf("") }
    var renameTargetGroup by remember { mutableStateOf<FocusFollowGroup?>(null) }
    var renameDraft by rememberSaveable { mutableStateOf("") }
    var deleteTargetGroupId by remember { mutableStateOf<String?>(null) }
    var sortModeMenuExpanded by remember { mutableStateOf(false) }
    val groupStateKey = remember(config.groups) {
        config.groups.joinToString(separator = "|") { group -> group.id }
    }
    var expandedGroupId by rememberSaveable(groupStateKey) { mutableStateOf<String?>(null) }
    val assignmentSections = remember(followings, config) {
        buildFocusFollowAssignmentSections(
            followings = followings,
            config = config
        )
    }
    val filteredAssignmentSections = remember(assignmentSections, followSearchQuery) {
        filterFocusFollowAssignmentSections(
            sections = assignmentSections,
            query = followSearchQuery
        )
    }

    AppModalBottomSheet(onDismissRequest = onDismissRequest) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = AppSpacingTokens.Large,
                end = AppSpacingTokens.Large,
                top = AppSpacingTokens.ExtraSmall,
                bottom = FocusFollowGroupLayoutSpec.SheetBottomPaddingDp.dp
            ),
            verticalArrangement = Arrangement.spacedBy(FocusFollowGroupLayoutSpec.SectionSpacingDp.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(FocusFollowGroupLayoutSpec.CompactGapDp.dp)) {
                    Text(
                        text = "关注分组",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "这里的可见性会影响首页关注分组和动态页",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = DynamicTypographyPolicy.followGroupIntroLineHeight
                    )
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(FocusFollowGroupShapeSpec.CardRadiusDp.dp),
                    tonalElevation = FocusFollowGroupLayoutSpec.SurfaceTonalElevationDp.dp,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacingTokens.Large),
                        verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Medium)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(AppSpacingTokens.Small + AppSpacingTokens.Micro))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "关注对象同步",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isLoading) {
                                        "自动同步中，正在补齐完整关注列表..."
                                    } else {
                                        "已自动同步 ${followings.size} 位关注对象，默认会在后台静默更新"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AppButton(
                                onClick = onRefreshFollowings,
                                modifier = Modifier
                                    .align(Alignment.Bottom)
                                    .height(actionButtonHeight),
                                shape = inputShape,
                                colors = ButtonDefaults.filledTonalButtonColors(),
                                elevation = ButtonDefaults.filledTonalButtonElevation(),
                                contentPadding = actionButtonContentPadding
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(FocusFollowGroupLayoutSpec.LeadingIconSizeDp.dp)
                                )
                                Spacer(modifier = Modifier.width(FocusFollowGroupLayoutSpec.CompactGapDp.dp))
                                Text("立即重拉")
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(inputShape)
                                    .clickable { sortModeMenuExpanded = true },
                                shape = inputShape,
                                tonalElevation = AppSpacingTokens.None,
                                color = com.android.purebilibili.core.ui.AppSurfaceTokens.surface()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(inputHeight)
                                        .padding(horizontal = FocusFollowGroupLayoutSpec.SortControlHorizontalPaddingDp.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Sort,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(AppSpacingTokens.Medium))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "首页关注排序",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = resolveFocusFollowHomeFeedSortModeLabel(config.homeFeedSortMode),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Outlined.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = sortModeMenuExpanded,
                                onDismissRequest = { sortModeMenuExpanded = false }
                            ) {
                                FocusFollowHomeFeedSortMode.entries.forEach { sortMode ->
                                    DropdownMenuItem(
                                        text = {
                                            Column(verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Micro)) {
                                                Text(resolveFocusFollowHomeFeedSortModeLabel(sortMode))
                                                Text(
                                                    text = resolveFocusFollowHomeFeedSortModeDescription(sortMode),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            sortModeMenuExpanded = false
                                            onSetHomeFeedSortMode(sortMode)
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Medium),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newGroupName,
                                onValueChange = { newGroupName = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(inputHeight),
                                singleLine = true,
                                shape = inputShape,
                                label = { Text("新分组名称") },
                                placeholder = { Text("例如：高优先、朋友、暂时隐藏") }
                            )
                            AppButton(
                                modifier = Modifier
                                    .align(Alignment.Bottom)
                                    .height(actionButtonHeight),
                                enabled = canCreateFocusFollowGroup(newGroupName, config.groups),
                                shape = inputShape,
                                colors = ButtonDefaults.filledTonalButtonColors(),
                                elevation = ButtonDefaults.filledTonalButtonElevation(),
                                contentPadding = actionButtonContentPadding,
                                onClick = {
                                    onCreateGroup(newGroupName)
                                    newGroupName = ""
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(FocusFollowGroupLayoutSpec.LeadingIconSizeDp.dp)
                                )
                                Spacer(modifier = Modifier.width(FocusFollowGroupLayoutSpec.CompactGapDp.dp))
                                Text("添加")
                            }
                        }

                        OutlinedTextField(
                            value = followSearchQuery,
                            onValueChange = { followSearchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(inputHeight),
                            singleLine = true,
                            shape = inputShape,
                            label = { Text("搜索关注对象") },
                            placeholder = { Text("搜索 UP 名称或 UID") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null
                                )
                            },
                            trailingIcon = if (followSearchQuery.isNotBlank()) {
                                {
                                    AppIconButton(onClick = { followSearchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "清除搜索"
                                        )
                                    }
                                }
                            } else {
                                null
                            }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "分组管理",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (followSearchQuery.isNotBlank() && filteredAssignmentSections.isEmpty()) {
                item("group_search_empty") {
                    Surface(
                        shape = RoundedCornerShape(FocusFollowGroupShapeSpec.SectionRadiusDp.dp),
                        tonalElevation = FocusFollowGroupLayoutSpec.SurfaceTonalElevationDp.dp,
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Text(
                            text = "没有找到匹配的关注对象",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = AppSpacingTokens.Large,
                                    vertical = FocusFollowGroupLayoutSpec.EmptyStateVerticalPaddingDp.dp
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredAssignmentSections, key = { section -> "group_manager_${section.group.id}" }) { section ->
                    FocusFollowGroupManagementCard(
                        section = section,
                        groups = config.groups,
                        expanded = expandedGroupId == section.group.id,
                        onToggleExpanded = {
                            expandedGroupId = if (expandedGroupId == section.group.id) {
                                null
                            } else {
                                section.group.id
                            }
                        },
                        onToggleVisible = { visible ->
                            onSetGroupVisible(section.group.id, visible)
                        },
                        onRename = if (section.group.id == DEFAULT_FOCUS_FOLLOW_GROUP_ID) {
                            null
                        } else {
                            {
                                renameTargetGroup = section.group
                                renameDraft = section.group.name
                            }
                        },
                        onDelete = if (section.group.id == DEFAULT_FOCUS_FOLLOW_GROUP_ID) {
                            null
                        } else {
                            { deleteTargetGroupId = section.group.id }
                        },
                        onAssignUserToGroup = onAssignUserToGroup,
                        resolveCurrentGroup = { mid -> resolveFocusFollowGroupForUser(config, mid) }
                    )
                }
            }
        }
    }

    renameTargetGroup?.let { group ->
        AlertDialog(
            onDismissRequest = { renameTargetGroup = null },
            confirmButton = {
                AppButton(
                    enabled = renameDraft.trim().isNotBlank(),
                    onClick = {
                        onRenameGroup(group.id, normalizeFocusFollowGroupName(renameDraft))
                        renameTargetGroup = null
                    },
                    colors = ButtonDefaults.buttonColors(),
                    elevation = ButtonDefaults.buttonElevation(),
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                AppOutlinedButton(onClick = { renameTargetGroup = null }) {
                    Text("取消")
                }
            },
            title = { Text("重命名分组") },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    singleLine = true,
                    shape = RoundedCornerShape(FocusFollowGroupShapeSpec.InputRadiusDp.dp),
                    label = { Text("分组名称") }
                )
            }
        )
    }

    deleteTargetGroupId?.let { groupId ->
        val group = config.groups.firstOrNull { it.id == groupId }
        if (group != null) {
            AlertDialog(
                onDismissRequest = { deleteTargetGroupId = null },
                confirmButton = {
                    AppButton(
                        onClick = {
                            onDeleteGroup(group.id)
                            deleteTargetGroupId = null
                        },
                        colors = ButtonDefaults.buttonColors(),
                        elevation = ButtonDefaults.buttonElevation(),
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    AppOutlinedButton(onClick = { deleteTargetGroupId = null }) {
                        Text("取消")
                    }
                },
                title = { Text("删除分组") },
                text = {
                    Text("删除后，该分组下的关注对象会自动回到“默认分组”")
                }
            )
        }
    }
}

private fun resolveFocusFollowHomeFeedSortModeLabel(sortMode: FocusFollowHomeFeedSortMode): String {
    return when (sortMode) {
        FocusFollowHomeFeedSortMode.RANDOM -> "随机排序"
        FocusFollowHomeFeedSortMode.CREATOR_CLUSTER_DESC -> "UP聚类倒序"
        FocusFollowHomeFeedSortMode.CREATOR_CLUSTER_ASC -> "UP聚类正序"
        FocusFollowHomeFeedSortMode.PUBLISH_TIME_DESC -> "时间倒序"
        FocusFollowHomeFeedSortMode.PUBLISH_TIME_ASC -> "时间正序"
    }
}

private fun resolveFocusFollowHomeFeedSortModeDescription(sortMode: FocusFollowHomeFeedSortMode): String {
    return when (sortMode) {
        FocusFollowHomeFeedSortMode.RANDOM -> "跨 UP 交错打乱，每次刷新都会换一种顺序"
        FocusFollowHomeFeedSortMode.CREATOR_CLUSTER_DESC -> "按最近活跃 UP 分组展示，组内视频按时间倒序"
        FocusFollowHomeFeedSortMode.CREATOR_CLUSTER_ASC -> "按较早活跃 UP 分组展示，组内视频按时间正序"
        FocusFollowHomeFeedSortMode.PUBLISH_TIME_DESC -> "所有可见视频按发布时间全局倒序"
        FocusFollowHomeFeedSortMode.PUBLISH_TIME_ASC -> "所有可见视频按发布时间全局正序"
    }
}

@Composable
private fun FocusFollowGroupManagementCard(
    section: FocusFollowAssignmentSection,
    groups: List<FocusFollowGroup>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleVisible: (Boolean) -> Unit,
    onRename: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onAssignUserToGroup: (Long, String) -> Unit,
    resolveCurrentGroup: (Long) -> FocusFollowGroup
) {
    Surface(
        shape = RoundedCornerShape(FocusFollowGroupShapeSpec.CardRadiusDp.dp),
        tonalElevation = FocusFollowGroupLayoutSpec.SurfaceTonalElevationDp.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacingTokens.Large),
            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Medium)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AppChromeSizeTokens.MinimumTouchTarget)
                    .clickable(onClick = onToggleExpanded),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(FocusFollowGroupLayoutSpec.CompactGapDp.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small)
                    ) {
                        Text(
                            text = section.group.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(FocusFollowGroupShapeSpec.PillRadiusDp.dp),
                            color = if (section.group.visible) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            }
                        ) {
                            Text(
                                text = if (section.group.visible) "可见" else "隐藏",
                                modifier = Modifier.padding(
                                    horizontal = FocusFollowGroupLayoutSpec.BadgeHorizontalPaddingDp.dp,
                                    vertical = AppSpacingTokens.ExtraSmall
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (section.group.visible) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                    Text(
                        text = "${section.members.size} 位关注对象 · ${if (section.group.visible) "动态与首页可见" else "动态与首页隐藏"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = DynamicTypographyPolicy.followGroupSummaryLineHeight
                    )
                }
                Icon(
                    imageVector = if (expanded) {
                        Icons.Outlined.KeyboardArrowDown
                    } else {
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight
                    },
                    contentDescription = if (expanded) "收起分组成员" else "展开分组成员",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = AppSpacingTokens.Medium, top = AppSpacingTokens.Micro)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(FocusFollowGroupShapeSpec.SmallRadiusDp.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier.padding(
                            start = AppSpacingTokens.Medium,
                            end = AppSpacingTokens.Small,
                            top = FocusFollowGroupLayoutSpec.CompactGapDp.dp,
                            bottom = FocusFollowGroupLayoutSpec.CompactGapDp.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "显示",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(AppSpacingTokens.Small))
                        Switch(
                            checked = section.group.visible,
                            onCheckedChange = onToggleVisible
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                onRename?.let {
                    AppIconButton(onClick = it) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "重命名分组"
                        )
                    }
                }
                onDelete?.let {
                    AppIconButton(onClick = it) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "删除分组"
                        )
                    }
                }
            }

            if (expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FocusFollowGroupLayoutSpec.SectionDividerThicknessDp.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                )
                if (section.members.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(FocusFollowGroupShapeSpec.MediumRadiusDp.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Text(
                            text = "这个分组里还没有关注对象",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = FocusFollowGroupLayoutSpec.EmptyStateHorizontalPaddingDp.dp,
                                    vertical = AppSpacingTokens.Medium
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(FocusFollowGroupLayoutSpec.MemberRowSpacingDp.dp)) {
                        section.members.forEach { user ->
                            FocusFollowUserAssignmentRow(
                                user = user,
                                groups = groups,
                                currentGroup = resolveCurrentGroup(user.mid),
                                onAssignToGroup = { groupId ->
                                    onAssignUserToGroup(user.mid, groupId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusFollowUserAssignmentRow(
    user: FollowingUser,
    groups: List<FocusFollowGroup>,
    currentGroup: FocusFollowGroup,
    onAssignToGroup: (String) -> Unit
) {
    var expanded by remember(user.mid) { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(FocusFollowGroupShapeSpec.CardRadiusDp.dp),
        tonalElevation = FocusFollowGroupLayoutSpec.SurfaceTonalElevationDp.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacingTokens.Large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = user.face,
                contentDescription = user.uname,
                modifier = Modifier
                    .size(FocusFollowGroupLayoutSpec.AvatarSizeDp.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(AppSpacingTokens.Medium))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Micro)
            ) {
                Text(
                    text = user.uname.ifBlank { user.mid.toString() },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val secondaryText = if (currentGroup.visible) {
                    "当前分组：${currentGroup.name}"
                } else {
                    "当前分组：${currentGroup.name} · 已隐藏"
                }
                Text(
                    text = secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (currentGroup.visible) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box {
                AppButton(
                    onClick = { expanded = true },
                    shape = ButtonDefaults.filledTonalShape,
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    elevation = ButtonDefaults.filledTonalButtonElevation(),
                ) {
                    Text(
                        text = currentGroup.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    groups.forEach { group ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (group.visible) group.name else "${group.name}（隐藏）"
                                )
                            },
                            onClick = {
                                expanded = false
                                onAssignToGroup(group.id)
                            }
                        )
                    }
                }
            }
        }
    }
}
