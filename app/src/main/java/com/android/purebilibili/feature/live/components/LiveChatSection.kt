package com.android.purebilibili.feature.live.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.ThumbUpOffAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import com.android.purebilibili.feature.live.LiveDanmakuItem
import com.android.purebilibili.feature.live.rememberLiveChromePalette
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Paperplane
import io.github.alexzhirkevich.cupertino.icons.filled.TextBubble
import kotlinx.coroutines.flow.SharedFlow
import coil.compose.AsyncImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 直播聊天区域组件
 * 包含：
 * 1. 聊天列表 (LazyColumn)
 * 2. 底部输入栏
 */
@Composable
fun LiveChatSection(
    danmakuFlow: SharedFlow<LiveDanmakuItem>,
    onSendDanmaku: (String) -> Unit,
    headerTitle: String = "实时互动",
    supportingText: String = "发送弹幕和主播互动",
    isOverlay: Boolean = false,
    showHeader: Boolean = true,
    isDanmakuEnabled: Boolean = true,
    onToggleDanmaku: () -> Unit = {},
    onLike: (Int) -> Unit = {},
    onOpenEmote: () -> Unit = {},
    onUserClick: (Long) -> Unit = {},
    onAtUser: (LiveDanmakuItem) -> Unit = {},
    onBlockUser: (LiveDanmakuItem) -> Unit = {},
    onReportDanmaku: (LiveDanmakuItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = rememberLiveChromePalette()
    val darkOverlay = isOverlay && palette.isDark
    val messages = remember { mutableStateListOf<LiveDanmakuItem>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val isAwayFromBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            messages.isNotEmpty() && lastVisible < messages.lastIndex - 1
        }
    }
    
    LaunchedEffect(danmakuFlow) {
        danmakuFlow.collect { item ->
            // 确保列表操作在主线程执行 (Compose 状态修改必须在主线程)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
                try {
                    val shouldAutoScroll = !listState.isScrollInProgress && !isAwayFromBottom
                    messages.add(item)
                    if (messages.size > 200) messages.removeFirst()
                    // 只有当用户没有滚动时才自动滚动
                    if (shouldAutoScroll && messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LiveChatSection", "❌ Message add error: ${e.message}")
                }
            }
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        if (showHeader) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.primaryText
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryText
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = palette.accentSoft
                ) {
                    Text(
                        text = "弹幕流",
                        color = palette.accentStrong,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            HorizontalDivider(color = palette.border)
        }

        // 1. 聊天列表
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = if (isOverlay) 12.dp else 16.dp,
                    vertical = if (isOverlay) 8.dp else 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(
                    space = if (isOverlay) 10.dp else 8.dp,
                    alignment = Alignment.Bottom
                )
            ) {
                items(messages) { item ->
                    ChatMessageItem(
                        item = item,
                        isOverlay = isOverlay,
                        onUserClick = onUserClick,
                        onAtUser = onAtUser,
                        onBlockUser = onBlockUser,
                        onReportDanmaku = onReportDanmaku
                    )
                }
            }
            if (isAwayFromBottom) {
                Surface(
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem(messages.lastIndex.coerceAtLeast(0))
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    color = if (darkOverlay) palette.bubbleStrong else palette.surfaceMuted,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = "回到底部",
                        color = if (darkOverlay) Color.White else palette.primaryText,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }
        
        // 2. 底部输入栏
        ChatInputBar(
            isOverlay = isOverlay,
            isDanmakuEnabled = isDanmakuEnabled,
            onToggleDanmaku = onToggleDanmaku,
            onLike = onLike,
            onOpenEmote = onOpenEmote,
            onSend = onSendDanmaku
        )
    }
}



@Composable
private fun ChatMessageItem(
    item: LiveDanmakuItem,
    isOverlay: Boolean,
    onUserClick: (Long) -> Unit,
    onAtUser: (LiveDanmakuItem) -> Unit,
    onBlockUser: (LiveDanmakuItem) -> Unit,
    onReportDanmaku: (LiveDanmakuItem) -> Unit
) {
    if (item.isSuperChat) {
        SuperChatMessageItem(item = item, isOverlay = isOverlay)
        return
    }
    val context = LocalContext.current
    val palette = rememberLiveChromePalette()
    val darkOverlay = isOverlay && palette.isDark
    var showMenu by remember { mutableStateOf(false) }
    val bubbleShape = RoundedCornerShape(if (isOverlay) 16.dp else 18.dp)
    val bubbleBackground = when {
        darkOverlay -> palette.bubble
        isOverlay -> palette.surface.copy(alpha = 0.96f)
        else -> palette.surfaceMuted
    }

    val usernameColor = if (item.isAdmin) {
        Color(0xFFFF7B92)
    } else if (item.isSelf) {
        palette.accentStrong
    } else if (darkOverlay) {
        Color.White.copy(alpha = 0.80f)
    } else {
        palette.secondaryText
    }
    val bodyColor = if (darkOverlay) Color.White else palette.primaryText
    val metaColor = if (darkOverlay) Color.White.copy(alpha = 0.62f) else palette.tertiaryText
    val emoticonMap by DanmakuEmoticonMapper.emoticonMap.collectAsState()
    val replyColor = if (darkOverlay) Color(0xFF8FD5FF) else palette.accent

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val bubbleModifier = Modifier
            .widthIn(max = maxWidth * 0.92f)
            .clip(bubbleShape)
            .background(bubbleBackground)
            .border(
                width = 1.dp,
                color = if (isOverlay) palette.border else Color.Transparent,
                shape = bubbleShape
            )
            .clickable(enabled = item.uid > 0L || item.uname.isNotBlank()) { showMenu = true }
            .padding(horizontal = 14.dp, vertical = if (isOverlay) 9.dp else 10.dp)

        Column(modifier = bubbleModifier) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (item.medalLevel > 0) {
                    MedalBadge(
                        name = item.medalName,
                        level = item.medalLevel,
                        colorInt = item.medalColor
                    )
                }
                if (item.userLevel > 0) {
                    UserLevelBadge(level = item.userLevel)
                }
                Text(
                    text = item.uname.ifBlank { "直播观众" },
                    color = usernameColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.dmType > 0) {
                    Text(
                        text = "· ${item.dmType}",
                        color = metaColor,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            if (item.emoticonUrl != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (item.replyToName.isNotBlank()) {
                        Text(
                            text = "@${item.replyToName}",
                            color = replyColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    AsyncImage(
                        model = item.emoticonUrl,
                        contentDescription = item.text,
                        modifier = Modifier.size(46.dp)
                    )
                }
            } else {
                val annotatedText = remember(item.text, item.replyToName, emoticonMap, replyColor) {
                    val builder = androidx.compose.ui.text.AnnotatedString.Builder()
                    if (item.replyToName.isNotBlank()) {
                        builder.pushStyle(
                            androidx.compose.ui.text.SpanStyle(
                                color = replyColor,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        )
                        builder.append("@${item.replyToName} ")
                        builder.pop()
                    }
                    builder.append(DanmakuEmoticonMapper.parse(item.text, emoticonMap))
                    builder.toAnnotatedString()
                }

                val inlineContentMap = remember(item.text, emoticonMap) {
                    val usedKeys = Regex("\\[(.*?)\\]").findAll(item.text).map { it.value }.toSet()
                    emoticonMap.filterKeys { it in usedKeys }.mapValues { (_, url) ->
                        androidx.compose.foundation.text.InlineTextContent(
                            androidx.compose.ui.text.Placeholder(
                                width = 1.35.em,
                                height = 1.35.em,
                                placeholderVerticalAlign = androidx.compose.ui.text.PlaceholderVerticalAlign.Center
                            )
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                androidx.compose.foundation.text.BasicText(
                    text = annotatedText,
                    style = TextStyle(
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = bodyColor,
                        fontWeight = FontWeight.Medium
                    ),
                    inlineContent = inlineContentMap
                )
            }
        }

        LiveDanmakuUserMenu(
            expanded = showMenu,
            item = item,
            onDismiss = { showMenu = false },
            onCopyInfo = {
                copyLiveDanmakuInfo(context, item)
            },
            onUserClick = onUserClick,
            onAtUser = onAtUser,
            onBlockUser = onBlockUser,
            onReportDanmaku = onReportDanmaku
        )
    }
}

@Composable
private fun LiveDanmakuUserMenu(
    expanded: Boolean,
    item: LiveDanmakuItem,
    onDismiss: () -> Unit,
    onCopyInfo: () -> Unit,
    onUserClick: (Long) -> Unit,
    onAtUser: (LiveDanmakuItem) -> Unit,
    onBlockUser: (LiveDanmakuItem) -> Unit,
    onReportDanmaku: (LiveDanmakuItem) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    text = item.uname.ifBlank { "弹幕" },
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            onClick = {}
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("复制弹幕信息") },
            onClick = {
                onDismiss()
                onCopyInfo()
            }
        )
        DropdownMenuItem(
            text = { Text("去TA的个人空间") },
            enabled = item.uid > 0L,
            onClick = {
                onDismiss()
                onUserClick(item.uid)
            }
        )
        DropdownMenuItem(
            text = { Text("@TA") },
            enabled = item.uname.isNotBlank(),
            onClick = {
                onDismiss()
                onAtUser(item)
            }
        )
        DropdownMenuItem(
            text = { Text("屏蔽发送者") },
            enabled = item.uname.isNotBlank() || item.uid > 0L,
            onClick = {
                onDismiss()
                onBlockUser(item)
            }
        )
        DropdownMenuItem(
            text = { Text("举报选中弹幕") },
            enabled = item.text.isNotBlank(),
            onClick = {
                onDismiss()
                onReportDanmaku(item)
            }
        )
    }
}

private fun copyLiveDanmakuInfo(
    context: Context,
    item: LiveDanmakuItem
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(
        ClipData.newPlainText(
            "直播弹幕信息",
            "uid=${item.uid}, uname=${item.uname}, text=${item.text}"
        )
    )
    Toast.makeText(context, "已复制弹幕信息", Toast.LENGTH_SHORT).show()
}

@Composable
private fun SuperChatMessageItem(
    item: LiveDanmakuItem,
    isOverlay: Boolean
) {
    val bg = if (item.superChatBackgroundColor != 0) {
        Color(item.superChatBackgroundColor).copy(alpha = if (isOverlay) 0.82f else 1f)
    } else {
        Color(0xFFE6A23C).copy(alpha = if (isOverlay) 0.82f else 1f)
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(if (isOverlay) 0.88f else 1f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.uname.ifBlank { "醒目留言" },
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (item.superChatPrice.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = item.superChatPrice,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            if (item.text.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    text = item.text,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    maxLines = if (isOverlay) 3 else 6,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// [新增] 粉丝牌组件
@Composable
private fun MedalBadge(name: String, level: Int, colorInt: Int) {
    val color = if (colorInt != 0) Color(colorInt) else Color(0xFFFF6699)
    
    Surface(
        color = color,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(top = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp)
        ) {
            Text(
                text = name,
                fontSize = 10.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.width(2.dp))
            // 简单的竖线分隔
            Box(Modifier.width(0.5.dp).height(8.dp).background(Color.White.copy(0.7f)))
            Spacer(Modifier.width(2.dp))
            Text(
                text = "$level",
                fontSize = 10.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// [新增] 用户等级组件
@Composable
private fun UserLevelBadge(level: Int) {
    // 简单的胶囊样式
    // 颜色根据等级变化 (简化处理：低等级灰/蓝，高等级橙/红)
    val color = when {
        level >= 40 -> Color(0xFFFF3333) // 红
        level >= 20 -> Color(0xFFFFAA33) // 橙
        else -> Color(0xFF66CCFF)        // 蓝
    }
    
    Surface(
        border = androidx.compose.foundation.BorderStroke(1.dp, color),
        shape = RoundedCornerShape(3.dp),
        color = Color.Transparent, // 空心
        modifier = Modifier.padding(top = 2.dp)
    ) {
         Text(
            text = "UL$level",
            fontSize = 9.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 0.dp)
        )
    }
}

@Composable
private fun ChatInputBar(
    isOverlay: Boolean,
    isDanmakuEnabled: Boolean,
    onToggleDanmaku: () -> Unit,
    onLike: (Int) -> Unit,
    onOpenEmote: () -> Unit,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val palette = rememberLiveChromePalette()
    val darkOverlay = isOverlay && palette.isDark
    val textColor = if (darkOverlay) Color.White else palette.primaryText
    val placeholderColor = if (darkOverlay) Color.White.copy(alpha = 0.68f) else palette.secondaryText
    val fieldColor = if (darkOverlay) palette.bubble else palette.searchField
    val iconTint = if (darkOverlay) Color.White else palette.secondaryText
    
    Surface(
        color = if (darkOverlay) palette.surface.copy(alpha = 0.92f) else palette.surfaceElevated,
        shape = if (isOverlay) RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp) else RoundedCornerShape(0.dp),
        tonalElevation = if (isOverlay) 0.dp else 2.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            palette.border
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleDanmaku,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = CupertinoIcons.Filled.TextBubble,
                    contentDescription = if (isDanmakuEnabled) "关闭弹幕" else "开启弹幕",
                    tint = if (isDanmakuEnabled) iconTint else iconTint.copy(alpha = 0.42f),
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))

            // 输入框
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = TextStyle(
                    color = textColor,
                    fontSize = 15.sp
                ),
                cursorBrush = SolidColor(if (darkOverlay) Color.White else palette.accent),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(fieldColor)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (text.isEmpty()) {
                            Text(
                                text = if (isOverlay) "发送弹幕" else "发个弹幕和主播互动吧~",
                                color = placeholderColor,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(8.dp))

            LiveLikeButton(
                tint = iconTint,
                onLike = onLike
            )

            IconButton(
                onClick = onOpenEmote,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.EmojiEmotions,
                    contentDescription = "表情",
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            // 发送按钮
            val isEnabled = text.isNotBlank()
            Surface(
                onClick = {
                    if (isEnabled) {
                        onSend(text)
                        text = ""
                        focusManager.clearFocus()
                    }
                },
                enabled = isEnabled,
                shape = CircleShape,
                color = if (isEnabled) {
                    if (darkOverlay) palette.accent.copy(alpha = 0.82f) else palette.accent
                } else {
                    if (darkOverlay) Color.White.copy(alpha = 0.10f) else palette.surfaceMuted
                },
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = CupertinoIcons.Filled.Paperplane,
                        contentDescription = "发送",
                        tint = if (isEnabled) palette.onAccent else iconTint.copy(alpha = 0.48f),
                        modifier = Modifier.size(20.dp).offset(x = (-2).dp, y = 2.dp) // 视觉居中微调
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveLikeButton(
    tint: Color,
    onLike: (Int) -> Unit
) {
    var likeCount by remember { mutableStateOf(0) }
    var flushJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val palette = rememberLiveChromePalette()

    DisposableEffect(Unit) {
        onDispose { flushJob?.cancel() }
    }

    Box {
        IconButton(
            onClick = {
                likeCount += 1
                flushJob?.cancel()
                flushJob = scope.launch {
                    delay(800)
                    val count = likeCount
                    likeCount = 0
                    if (count > 0) onLike(count)
                }
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ThumbUpOffAlt,
                contentDescription = "点赞",
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        if (likeCount > 0) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = palette.accent.copy(alpha = 0.96f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-6).dp)
            ) {
                Text(
                    text = "x$likeCount",
                    color = palette.onAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
    }
}
