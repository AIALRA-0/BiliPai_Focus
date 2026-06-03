// 私信收件箱页面
package com.android.purebilibili.feature.message

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.LocalAndroidNativeVariant
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.ui.AdaptiveScaffold
import com.android.purebilibili.core.ui.AdaptiveTopAppBar
import coil.compose.AsyncImage
import com.android.purebilibili.core.ui.ComfortablePullToRefreshBox
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.data.model.response.SessionItem
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onBack: () -> Unit,
    onTopItemClick: (MessageCenterDestination) -> Unit,
    onSessionClick: (talkerId: Long, sessionType: Int, userName: String) -> Unit,
    viewModel: InboxViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var lastAutoLoadEndTs by remember { mutableLongStateOf(0L) }
    var pendingRemoveSession by remember { mutableStateOf<SessionItem?>(null) }
    var pendingInterceptSession by remember { mutableStateOf<SessionItem?>(null) }
    var showClearDustbinConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.sessions.firstOrNull()?.talker_id, uiState.isRefreshing, uiState.isLoading) {
        if (uiState.isRefreshing || uiState.isLoading) {
            lastAutoLoadEndTs = 0L
        }
    }

    AdaptiveScaffold(
        topBar = {
            AdaptiveTopAppBar(
                title = "消息",
                subtitle = uiState.unreadData?.let { unread ->
                    totalPrivateUnreadCount(unread).takeIf { it > 0 }?.let { "私信 $it 条未读" }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(rememberAppBackIcon(), contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    com.android.purebilibili.core.ui.CutePersonLoadingIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(uiState.error ?: "加载失败")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadSessions() }) {
                            Text("重试")
                        }
                    }
                }
                uiState.sessions.isEmpty() -> {
                    Text(
                        text = "暂无私信",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    ComfortablePullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refresh() }
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            item {
                                MessageCenterTopShortcutRow(
                                    items = buildMessageCenterTopItems(uiState.feedUnreadData),
                                    onItemClick = onTopItemClick
                                )
                            }

                            item {
                                MessageSessionCategoryRow(
                                    items = buildMessageSessionCategoryItems(uiState.unreadData),
                                    selectedCategory = uiState.selectedCategory,
                                    onCategoryClick = { viewModel.selectCategory(it) }
                                )
                            }

                            if (uiState.selectedCategory == MessageSessionCategory.Dustbin) {
                                item {
                                    DustbinActionRow(
                                        isOperating = uiState.isBatchOperating,
                                        onMarkRead = { viewModel.markDustbinRead() },
                                        onClear = { showClearDustbinConfirm = true }
                                    )
                                }
                            }

                            items(
                                items = uiState.sessions,
                                key = InboxSessionPaginationPolicy::resolveSessionKey
                            ) { session ->
                                if (
                                    uiState.hasMore &&
                                    uiState.endTs > 0L &&
                                    uiState.endTs != lastAutoLoadEndTs &&
                                    session == uiState.sessions.lastOrNull() &&
                                    !uiState.isLoadingMore
                                ) {
                                    LaunchedEffect(session.talker_id, session.session_type, uiState.endTs) {
                                        lastAutoLoadEndTs = uiState.endTs
                                        viewModel.loadMoreSessions()
                                    }
                                }
                                val userInfo = uiState.userInfoMap[session.talker_id]
                                SessionListItem(
                                    session = session,
                                    userInfo = userInfo,
                                    onClick = {
                                        val userName = InboxUserInfoResolver.resolveDisplayName(
                                            cached = userInfo,
                                            session = session
                                        )
                                        onSessionClick(session.talker_id, session.session_type, userName)
                                    },
                                    onRemove = { pendingRemoveSession = session },
                                    onToggleTop = { viewModel.toggleTop(session) },
                                    onToggleDnd = { viewModel.toggleDnd(session) },
                                    onToggleIntercept = {
                                        if (session.is_intercept == 1) {
                                            viewModel.toggleIntercept(session)
                                        } else {
                                            pendingInterceptSession = session
                                        }
                                    }
                                )
                            }

                            if (uiState.hasMore) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (uiState.isLoadingMore) {
                                            com.android.purebilibili.core.ui.CutePersonLoadingIndicator(
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            TextButton(onClick = { viewModel.loadMoreSessions() }) {
                                                Text("加载更多")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            uiState.operationError?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearOperationError() }) {
                            Text("知道了")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }

    pendingRemoveSession?.let { session ->
        AlertDialog(
            onDismissRequest = { pendingRemoveSession = null },
            title = { Text("删除会话") },
            text = { Text("会话会从列表中移除，但不会删除聊天记录。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeSession(session)
                        pendingRemoveSession = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveSession = null }) {
                    Text("取消")
                }
            }
        )
    }

    pendingInterceptSession?.let { session ->
        AlertDialog(
            onDismissRequest = { pendingInterceptSession = null },
            title = { Text("移入拦截") },
            text = { Text("后续这类会话会进入拦截分类，仍可在拦截列表中查看和恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.toggleIntercept(session)
                        pendingInterceptSession = null
                    }
                ) {
                    Text("移入")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingInterceptSession = null }) {
                    Text("取消")
                }
            }
        )
    }

    if (showClearDustbinConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDustbinConfirm = false },
            title = { Text("清空拦截会话") },
            text = { Text("所有拦截会话会从列表中移除，聊天记录仍由服务端保留。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearDustbinSessions()
                        showClearDustbinConfirm = false
                    }
                ) {
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDustbinConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun MessageCenterTopShortcutRow(
    items: List<MessageCenterTopItem>,
    onItemClick: (MessageCenterDestination) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val columns = if (maxWidth >= 820.dp) 4 else 2

        Column {
            Text(
                text = "消息分类",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            items.chunked(columns).forEachIndexed { rowIndex, rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { item ->
                        MessageCenterShortcutCard(
                            item = item,
                            modifier = Modifier.weight(1f),
                            onClick = { onItemClick(item.destination) }
                        )
                    }

                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                if (rowIndex != items.chunked(columns).lastIndex) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun MessageCenterShortcutCard(
    item: MessageCenterTopItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val uiPreset = LocalUiPreset.current
    val androidNativeVariant = LocalAndroidNativeVariant.current
    val isMiuix = uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX
    Surface(
        modifier = modifier
            .height(if (isMiuix) 88.dp else 96.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(if (isMiuix) 18.dp else 20.dp),
        color = if (isMiuix) MiuixTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = if (isMiuix) {
            androidx.compose.foundation.BorderStroke(
                0.8.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
            )
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )

                if (item.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (item.unreadCount > 99) "99+" else item.unreadCount.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = if (item.unreadCount > 0) "${item.unreadCount} 条" else "查看",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun MessageSessionCategoryRow(
    items: List<MessageSessionCategoryItem>,
    selectedCategory: MessageSessionCategory,
    onCategoryClick: (MessageSessionCategory) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "私信会话",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(items, key = { it.category.name }) { item ->
                FilterChip(
                    selected = item.category == selectedCategory,
                    onClick = { onCategoryClick(item.category) },
                    label = {
                        Text(
                            text = if (item.unreadCount > 0) {
                                "${item.category.title} ${if (item.unreadCount > 99) "99+" else item.unreadCount}"
                            } else {
                                item.category.title
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun DustbinActionRow(
    isOperating: Boolean,
    onMarkRead: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onMarkRead,
            enabled = !isOperating,
            modifier = Modifier.weight(1f)
        ) {
            Text("全部已读")
        }

        OutlinedButton(
            onClick = onClear,
            enabled = !isOperating,
            modifier = Modifier.weight(1f)
        ) {
            Text("清空拦截")
        }
    }
}

@Composable
private fun MessageUnreadBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 12.sp
        )
    }
}

@Composable
fun SessionListItem(
    session: SessionItem,
    userInfo: UserBasicInfo? = null,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onToggleTop: () -> Unit,
    onToggleDnd: () -> Unit,
    onToggleIntercept: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val uiPreset = LocalUiPreset.current
    val androidNativeVariant = LocalAndroidNativeVariant.current
    val isMiuix = uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX

    val displayName = InboxUserInfoResolver.resolveDisplayName(
        cached = userInfo,
        session = session
    )
    val displayAvatar = InboxUserInfoResolver.resolveDisplayAvatar(
        cached = userInfo,
        session = session
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = if (isMiuix) 12.dp else 0.dp, vertical = if (isMiuix) 3.dp else 0.dp),
        shape = RoundedCornerShape(if (isMiuix) 16.dp else 0.dp),
        color = when {
            isMiuix && session.top_ts > 0 -> MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
            isMiuix -> MiuixTheme.colorScheme.surfaceContainer
            session.top_ts > 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else -> Color.Transparent
        }
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = if (isMiuix) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = displayAvatar,
                contentDescription = "头像",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            if (session.unread_count > 0) {
                val badgeText = when {
                    session.unread_count > 99 -> "99+"
                    else -> session.unread_count.toString()
                }
                MessageUnreadBadge(
                    text = badgeText,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (session.top_ts > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "置顶",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(2.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                if (session.is_dnd == 1) {
                    Spacer(modifier = Modifier.width(4.dp))
                    MessageSmallFlag(text = "免打扰")
                }

                if (session.is_intercept == 1) {
                    Spacer(modifier = Modifier.width(4.dp))
                    MessageSmallFlag(text = "已拦截")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = MessagePreviewParser.parseSessionPreview(
                    content = session.last_msg?.content,
                    msgType = session.last_msg?.msg_type ?: 1
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatTime(session.last_msg?.timestamp ?: 0),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "更多",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (session.top_ts > 0) "取消置顶" else "置顶") },
                        onClick = {
                            showMenu = false
                            onToggleTop()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (session.is_dnd == 1) "关闭免打扰" else "开启免打扰") },
                        onClick = {
                            showMenu = false
                            onToggleDnd()
                        }
                    )
                    if (session.session_type == 1) {
                        DropdownMenuItem(
                            text = { Text(if (session.is_intercept == 1) "移出拦截" else "移入拦截") },
                            onClick = {
                                showMenu = false
                                onToggleIntercept()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("删除会话") },
                        onClick = {
                            showMenu = false
                            onRemove()
                        }
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun MessageSmallFlag(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                RoundedCornerShape(2.dp)
            )
            .padding(horizontal = 4.dp, vertical = 1.dp)
    )
}

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return ""

    val now = System.currentTimeMillis()
    val msgTime = timestamp * 1000
    val diff = now - msgTime

    return when {
        diff < 60_000 -> "刚刚"
        diff < 3600_000 -> "${diff / 60_000}分钟前"
        diff < 86400_000 -> "${diff / 3600_000}小时前"
        diff < 172800_000 -> "昨天"
        else -> {
            val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
            sdf.format(Date(msgTime))
        }
    }
}
