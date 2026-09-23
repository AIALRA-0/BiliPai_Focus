// 私信收件箱页面
package com.android.purebilibili.feature.message
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.ImmersiveAppScaffold as AppScaffold
import com.android.purebilibili.core.ui.AppTopBar
import com.android.purebilibili.core.ui.rememberAppSemanticVisualPolicy
import coil3.compose.AsyncImage
import com.android.purebilibili.core.ui.AdaptivePullToRefreshBox
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppDropdownMenu
import com.android.purebilibili.core.ui.components.AppDropdownMenuItem
import com.android.purebilibili.core.ui.components.AppSegmentOption
import com.android.purebilibili.core.ui.components.AppThemeAdaptiveTabRow
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppOutlinedButton
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppSnackbar
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.data.model.response.SessionItem
import com.android.purebilibili.core.ui.AppSurfaceTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.AppSpacingTokens

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

    AppScaffold(
        blurContentReady = !uiState.isLoading,
        topBar = {
            AppTopBar(
                title = "消息",
                subtitle = uiState.unreadData?.let { unread ->
                    totalPrivateUnreadCount(unread).takeIf { it > 0 }?.let { "私信 $it 条未读" }
                },
                navigationIcon = {
                    AppIconButton(onClick = onBack) {
                        AppIcon(rememberAppBackIcon(), contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    com.android.purebilibili.core.ui.skeleton.ContentMediaListSkeleton(
                        modifier = Modifier.fillMaxSize(),
                        useUserRow = true,
                        itemCount = 8,
                    )
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AppText(uiState.error ?: "加载失败")
                        Spacer(modifier = Modifier.height(8.dp))
                        AppButton(onClick = { viewModel.loadSessions() }) {
                            AppText("重试")
                        }
                    }
                }
                uiState.sessions.isEmpty() -> {
                    AppText(
                        text = "暂无私信",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    // Scaffold body already below topBar.
                    AdaptivePullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        indicatorTopInset = paddingValues.calculateTopPadding()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = paddingValues.calculateTopPadding() + 8.dp, bottom = paddingValues.calculateBottomPadding() + 8.dp)
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
                                                size = 24.dp
                                            )
                                        } else {
                                            AppTextButton(onClick = { viewModel.loadMoreSessions() }) {
                                                AppText("加载更多")
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
                AppSnackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        AppTextButton(onClick = { viewModel.clearOperationError() }) {
                            AppText("知道了")
                        }
                    }
                ) {
                    AppText(error)
                }
            }
        }
    }

    pendingRemoveSession?.let { session ->
        AppAlertDialog(
            onDismissRequest = { pendingRemoveSession = null },
            title = { AppText("删除会话") },
            text = { AppText("会话会从列表中移除，但不会删除聊天记录。") },
            confirmButton = {
                AppTextButton(
                    onClick = {
                        viewModel.removeSession(session)
                        pendingRemoveSession = null
                    }
                ) {
                    AppText("删除")
                }
            },
            dismissButton = {
                AppTextButton(onClick = { pendingRemoveSession = null }) {
                    AppText("取消")
                }
            }
        )
    }

    pendingInterceptSession?.let { session ->
        AppAlertDialog(
            onDismissRequest = { pendingInterceptSession = null },
            title = { AppText("移入拦截") },
            text = { AppText("后续这类会话会进入拦截分类，仍可在拦截列表中查看和恢复。") },
            confirmButton = {
                AppTextButton(
                    onClick = {
                        viewModel.toggleIntercept(session)
                        pendingInterceptSession = null
                    }
                ) {
                    AppText("移入")
                }
            },
            dismissButton = {
                AppTextButton(onClick = { pendingInterceptSession = null }) {
                    AppText("取消")
                }
            }
        )
    }

    if (showClearDustbinConfirm) {
        AppAlertDialog(
            onDismissRequest = { showClearDustbinConfirm = false },
            title = { AppText("清空拦截会话") },
            text = { AppText("所有拦截会话会从列表中移除，聊天记录仍由服务端保留。") },
            confirmButton = {
                AppTextButton(
                    onClick = {
                        viewModel.clearDustbinSessions()
                        showClearDustbinConfirm = false
                    }
                ) {
                    AppText("清空")
                }
            },
            dismissButton = {
                AppTextButton(onClick = { showClearDustbinConfirm = false }) {
                    AppText("取消")
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
            AppText(
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
    val useGroupedListCards = rememberAppSemanticVisualPolicy().prefersGroupedListCards
    AppSurface(
        modifier = modifier
            .height(if (useGroupedListCards) 88.dp else 96.dp)
            .clickable(onClick = onClick),
        shape = AppShapes.container(ContainerLevel.Card),
        color = if (useGroupedListCards) AppSurfaceTokens.surfaceContainer() else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = if (useGroupedListCards) {
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
                AppText(
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
                                shape = AppShapes.container(ContainerLevel.Pill)
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        AppText(
                            text = if (item.unreadCount > 99) "99+" else item.unreadCount.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            AppText(
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
    val uiStyle = LocalAppUiStyle.current
    val rowHorizontalPadding = if (uiStyle == AppUiStyle.MATERIAL3) 0.dp else 16.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        AppText(
            text = "私信会话",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )

        val options = remember(items) {
            items.map { item ->
                AppSegmentOption(
                    value = item.category,
                    label = if (item.unreadCount > 0) {
                        "${item.category.title} ${if (item.unreadCount > 99) "99+" else item.unreadCount}"
                    } else {
                        item.category.title
                    },
                )
            }
        }
        AppThemeAdaptiveTabRow(
            options = options,
            selectedValue = selectedCategory,
            onSelectionChange = onCategoryClick,
            scrollable = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = rowHorizontalPadding),
        )
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
        AppOutlinedButton(
            onClick = onMarkRead,
            enabled = !isOperating,
            modifier = Modifier.weight(1f)
        ) {
            AppText("全部已读")
        }

        AppOutlinedButton(
            onClick = onClear,
            enabled = !isOperating,
            modifier = Modifier.weight(1f)
        ) {
            AppText("清空拦截")
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
                shape = AppShapes.container(ContainerLevel.Chip)
            )
            .padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        AppText(
            text = text,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
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
    val useGroupedListCards = rememberAppSemanticVisualPolicy().prefersGroupedListCards

    val displayName = InboxUserInfoResolver.resolveDisplayName(
        cached = userInfo,
        session = session
    )
    val displayAvatar = InboxUserInfoResolver.resolveDisplayAvatar(
        cached = userInfo,
        session = session
    )

    AppSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = if (useGroupedListCards) 12.dp else 0.dp, vertical = if (useGroupedListCards) 3.dp else 0.dp),
        shape = if (useGroupedListCards) {
            AppShapes.container(ContainerLevel.Card)
        } else {
            RectangleShape
        },
        color = when {
            useGroupedListCards && session.top_ts > 0 -> AppSurfaceTokens.secondaryContainer().copy(alpha = 0.55f)
            useGroupedListCards -> AppSurfaceTokens.surfaceContainer()
            session.top_ts > 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else -> Color.Transparent
        }
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = if (useGroupedListCards) 10.dp else 12.dp),
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
                AppText(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (session.top_ts > 0) {
                    Spacer(modifier = Modifier.width(AppSpacingTokens.ExtraSmall))
                    AppText(
                        text = "置顶",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                AppShapes.container(ContainerLevel.Tag)
                            )
                            .padding(horizontal = AppSpacingTokens.ExtraSmall, vertical = 1.dp)
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

            AppText(
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
            AppText(
                text = formatTime(session.last_msg?.timestamp ?: 0),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box {
                AppIconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    AppIcon(
                        Icons.Default.MoreVert,
                        contentDescription = "更多",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AppDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    AppDropdownMenuItem(
                        text = { AppText(if (session.top_ts > 0) "取消置顶" else "置顶") },
                        onClick = {
                            showMenu = false
                            onToggleTop()
                        }
                    )
                    AppDropdownMenuItem(
                        text = { AppText(if (session.is_dnd == 1) "关闭免打扰" else "开启免打扰") },
                        onClick = {
                            showMenu = false
                            onToggleDnd()
                        }
                    )
                    if (session.session_type == 1) {
                        AppDropdownMenuItem(
                            text = { AppText(if (session.is_intercept == 1) "移出拦截" else "移入拦截") },
                            onClick = {
                                showMenu = false
                                onToggleIntercept()
                            }
                        )
                    }
                    AppDropdownMenuItem(
                        text = { AppText("删除会话") },
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
    AppText(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                AppShapes.container(ContainerLevel.Tag)
            )
            .padding(horizontal = AppSpacingTokens.ExtraSmall, vertical = 1.dp)
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
