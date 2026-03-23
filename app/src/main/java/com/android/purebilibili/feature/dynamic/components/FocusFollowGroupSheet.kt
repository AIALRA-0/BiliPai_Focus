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
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.android.purebilibili.core.store.DEFAULT_FOCUS_FOLLOW_GROUP_ID
import com.android.purebilibili.core.store.FocusFollowGroup
import com.android.purebilibili.core.store.FocusFollowGroupConfig
import com.android.purebilibili.core.store.canCreateFocusFollowGroup
import com.android.purebilibili.core.store.normalizeFocusFollowGroupName
import com.android.purebilibili.core.store.resolveFocusFollowGroupForUser
import com.android.purebilibili.core.ui.IOSModalBottomSheet
import com.android.purebilibili.data.model.response.FollowingUser
import com.android.purebilibili.feature.dynamic.FocusFollowAssignmentSection
import com.android.purebilibili.feature.dynamic.buildFocusFollowAssignmentSections
import com.android.purebilibili.feature.dynamic.filterFocusFollowAssignmentSections

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
    onAssignUserToGroup: (Long, String) -> Unit
) {
    val inputHeight = 60.dp
    val actionButtonHeight = 52.dp
    val inputShape = RoundedCornerShape(26.dp)
    val actionButtonContentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp)
    var newGroupName by rememberSaveable { mutableStateOf("") }
    var followSearchQuery by rememberSaveable { mutableStateOf("") }
    var renameTargetGroup by remember { mutableStateOf<FocusFollowGroup?>(null) }
    var renameDraft by rememberSaveable { mutableStateOf("") }
    var deleteTargetGroupId by remember { mutableStateOf<String?>(null) }
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

    IOSModalBottomSheet(onDismissRequest = onDismissRequest) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "关注分组",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "这里的可见性会影响首页关注分组和动态页",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "关注对象同步",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isLoading) {
                                        "正在刷新完整关注列表..."
                                    } else {
                                        "当前已载入 ${followings.size} 位关注对象"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = onRefreshFollowings,
                                modifier = Modifier
                                    .align(Alignment.Bottom)
                                    .height(actionButtonHeight),
                                shape = inputShape,
                                contentPadding = actionButtonContentPadding
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("刷新")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                            FilledTonalButton(
                                modifier = Modifier
                                    .align(Alignment.Bottom)
                                    .height(actionButtonHeight),
                                enabled = canCreateFocusFollowGroup(newGroupName, config.groups),
                                shape = inputShape,
                                contentPadding = actionButtonContentPadding,
                                onClick = {
                                    onCreateGroup(newGroupName)
                                    newGroupName = ""
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
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
                                    IconButton(onClick = { followSearchQuery = "" }) {
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
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 1.dp,
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Text(
                            text = "没有找到匹配的关注对象",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                Button(
                    enabled = renameDraft.trim().isNotBlank(),
                    onClick = {
                        onRenameGroup(group.id, normalizeFocusFollowGroupName(renameDraft))
                        renameTargetGroup = null
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { renameTargetGroup = null }) {
                    Text("取消")
                }
            },
            title = { Text("重命名分组") },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    singleLine = true,
                    shape = RoundedCornerShape(26.dp),
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
                    Button(
                        onClick = {
                            onDeleteGroup(group.id)
                            deleteTargetGroupId = null
                        }
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { deleteTargetGroupId = null }) {
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
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = section.group.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (section.group.visible) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            }
                        ) {
                            Text(
                                text = if (section.group.visible) "可见" else "隐藏",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
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
                        lineHeight = 18.sp
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
                    modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "显示",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = section.group.visible,
                            onCheckedChange = onToggleVisible
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                onRename?.let {
                    IconButton(onClick = it) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "重命名分组"
                        )
                    }
                }
                onDelete?.let {
                    IconButton(onClick = it) {
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
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                )
                if (section.members.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Text(
                            text = "这个分组里还没有关注对象",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = user.face,
                contentDescription = user.uname,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
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
                FilledTonalButton(onClick = { expanded = true }) {
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
