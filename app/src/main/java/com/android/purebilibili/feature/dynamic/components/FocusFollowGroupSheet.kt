package com.android.purebilibili.feature.dynamic.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
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
import com.android.purebilibili.core.store.countFocusFollowGroupMembers
import com.android.purebilibili.core.store.normalizeFocusFollowGroupName
import com.android.purebilibili.core.store.resolveFocusFollowGroupForUser
import com.android.purebilibili.core.store.resolveFocusFollowGroupIdForUser
import com.android.purebilibili.core.ui.IOSModalBottomSheet
import com.android.purebilibili.data.model.response.FollowingUser

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
    var newGroupName by rememberSaveable { mutableStateOf("") }
    var renameTargetGroup by remember { mutableStateOf<FocusFollowGroup?>(null) }
    var renameDraft by rememberSaveable { mutableStateOf("") }
    var deleteTargetGroupId by remember { mutableStateOf<String?>(null) }

    val groupCounts = remember(followings, config) {
        countFocusFollowGroupMembers(
            followingMids = followings.map { it.mid },
            config = config
        )
    }
    val orderedFollowings = remember(followings, config) {
        followings.sortedWith(
            compareBy<FollowingUser>(
                {
                    val groupId = resolveFocusFollowGroupIdForUser(config, it.mid)
                    config.groups.indexOfFirst { group -> group.id == groupId }
                        .takeIf { index -> index >= 0 }
                        ?: Int.MAX_VALUE
                },
                { it.uname.lowercase() },
                { it.mid }
            )
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
                        text = "这里的可见性会同时影响\n" +
                            "- 动态页\n" +
                            "- 首页“关注”分组流\n\n" +
                            "默认规则\n" +
                            "- 所有关注对象默认进入“默认分组”\n" +
                            "- 每位对象只能属于一个分组",
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
                            FilledTonalButton(onClick = onRefreshFollowings) {
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newGroupName,
                                onValueChange = { newGroupName = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(26.dp),
                                label = { Text("新分组名称") },
                                placeholder = { Text("例如：高优先、朋友、暂时隐藏") }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                enabled = canCreateFocusFollowGroup(newGroupName, config.groups),
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
                    }
                }
            }

            item {
                Text(
                    text = "分组可见性",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(config.groups, key = { it.id }) { group ->
                FocusFollowGroupRow(
                    group = group,
                    memberCount = groupCounts[group.id] ?: 0,
                    onToggleVisible = { visible ->
                        onSetGroupVisible(group.id, visible)
                    },
                    onRename = if (group.id == DEFAULT_FOCUS_FOLLOW_GROUP_ID) {
                        null
                    } else {
                        {
                            renameTargetGroup = group
                            renameDraft = group.name
                        }
                    },
                    onDelete = if (group.id == DEFAULT_FOCUS_FOLLOW_GROUP_ID) {
                        null
                    } else {
                        { deleteTargetGroupId = group.id }
                    }
                )
            }

            item {
                Text(
                    text = "关注对象归属",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (!isLoading && orderedFollowings.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 1.dp,
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Text(
                            text = "还没有载入完整关注列表；点上面的“刷新”后，就可以把每位关注对象移动到任意分组",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(orderedFollowings, key = { it.mid }) { user ->
                FocusFollowUserAssignmentRow(
                    user = user,
                    groups = config.groups,
                    currentGroup = resolveFocusFollowGroupForUser(config, user.mid),
                    onAssignToGroup = { groupId ->
                        onAssignUserToGroup(user.mid, groupId)
                    }
                )
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
private fun FocusFollowGroupRow(
    group: FocusFollowGroup,
    memberCount: Int,
    onToggleVisible: (Boolean) -> Unit,
    onRename: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${memberCount} 位关注对象 · ${if (group.visible) "当前可见" else "当前隐藏"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = group.visible,
                onCheckedChange = onToggleVisible
            )
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
