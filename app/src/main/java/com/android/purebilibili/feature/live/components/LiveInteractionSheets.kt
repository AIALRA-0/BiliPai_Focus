package com.android.purebilibili.feature.live.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.purebilibili.data.repository.DefaultLiveReportReasons
import com.android.purebilibili.data.repository.LiveEmoticonItem
import com.android.purebilibili.data.repository.LiveEmoticonPackage
import com.android.purebilibili.data.repository.LiveReportReason
import com.android.purebilibili.data.repository.LiveShieldInfo
import com.android.purebilibili.data.repository.LiveShieldUser
import com.android.purebilibili.feature.live.LiveDanmakuItem

@Composable
fun LiveReportDialog(
    target: LiveDanmakuItem,
    onDismiss: () -> Unit,
    onReport: (LiveReportReason) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Report, contentDescription = null) },
        title = { Text("举报弹幕") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "@${target.uname.ifBlank { target.uid.toString() }}：${target.text}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DefaultLiveReportReasons.forEach { reason ->
                        AssistChip(
                            onClick = { onReport(reason) },
                            label = { Text(reason.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
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
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "直播表情",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (packages.isEmpty()) {
                Text(
                    text = "当前直播间暂无可用表情",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    packages.forEach { pkg ->
                        item(key = "title-${pkg.id}") {
                            Text(
                                text = pkg.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        items(pkg.items, key = { "${pkg.id}-${it.emoji}" }) { item ->
                            LiveEmoticonRow(item = item, onClick = { onSelected(item) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveEmoticonRow(
    item: LiveEmoticonItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = item.url,
                contentDescription = item.description.ifBlank { item.emoji },
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(34.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(item.emoji, style = MaterialTheme.typography.bodyLarge)
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "弹幕屏蔽",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (!isLoggedIn) {
                Text(
                    text = "登录后可同步直播间屏蔽词、屏蔽用户和规则。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it.take(20) },
                    enabled = isLoggedIn,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("新增屏蔽词") }
                )
                Button(
                    enabled = isLoggedIn && keyword.trim().isNotBlank(),
                    onClick = {
                        onAddKeyword(keyword.trim())
                        keyword = ""
                    }
                ) {
                    Text("添加")
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
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun LiveRuleSection(
    shieldInfo: LiveShieldInfo?,
    enabled: Boolean,
    onSetRule: (String, Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("规则", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = (shieldInfo?.level ?: 0) > 0,
                enabled = enabled,
                onClick = { onSetRule("level", if ((shieldInfo?.level ?: 0) > 0) 0 else 5) },
                label = { Text("等级") }
            )
            FilterChip(
                selected = (shieldInfo?.medal ?: 0) > 0,
                enabled = enabled,
                onClick = { onSetRule("medal", if ((shieldInfo?.medal ?: 0) > 0) 0 else 1) },
                label = { Text("勋章") }
            )
            FilterChip(
                selected = (shieldInfo?.verify ?: 0) > 0,
                enabled = enabled,
                onClick = { onSetRule("verify", if ((shieldInfo?.verify ?: 0) > 0) 0 else 1) },
                label = { Text("认证") }
            )
        }
    }
}

@Composable
private fun LiveKeywordSection(
    shieldInfo: LiveShieldInfo?,
    enabled: Boolean,
    onDeleteKeyword: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("关键词", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        val keywords = shieldInfo?.keywords.orEmpty()
        if (keywords.isEmpty()) {
            Text("暂无屏蔽词", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            keywords.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.keyword, modifier = Modifier.weight(1f))
                    IconButton(enabled = enabled, onClick = { onDeleteKeyword(item.keyword) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "删除")
                    }
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("用户", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        val users = shieldInfo?.users.orEmpty()
        if (users.isEmpty()) {
            Text("暂无屏蔽用户", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            users.forEach { user ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Block, contentDescription = null)
                    Text(
                        text = user.uname.ifBlank { user.uid.toString() },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp)
                    )
                    TextButton(enabled = enabled, onClick = { onUnblockUser(user) }) {
                        Text("解除")
                    }
                }
            }
        }
    }
}
