package com.android.purebilibili.feature.live.components
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppHorizontalDivider

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.ThumbUpOffAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import com.android.purebilibili.feature.live.LiveDanmakuItem
import com.android.purebilibili.feature.live.rememberLiveChromePalette
import com.android.purebilibili.feature.live.resolveLiveBiliPaiChatBubbleTokens
import com.android.purebilibili.feature.live.resolveLiveBiliPaiRoomColorTokens
import com.android.purebilibili.feature.live.resolveLiveChatInputVisualSpec
import com.android.purebilibili.feature.live.shouldRenderLiveDanmakuImageEmoticon
import com.android.purebilibili.feature.live.LiveStatusPalette
import com.android.purebilibili.feature.live.resolveLiveLevelColor
import com.android.purebilibili.feature.live.resolveLiveMedalColor
import com.android.purebilibili.feature.live.resolveLiveMedalBadgeVisualSpec
import com.android.purebilibili.feature.live.resolveLiveSuperChatColor
import com.android.purebilibili.feature.live.shouldRenderLiveDanmaku
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Send
import kotlinx.coroutines.flow.SharedFlow
import coil3.compose.AsyncImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppDropdownMenu
import com.android.purebilibili.core.ui.components.AppDropdownMenuItem
import com.android.purebilibili.core.ui.components.AppFilledIconButton
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppIconButtonDefaults
import com.android.purebilibili.core.ui.components.AppSurface

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
    val chatVisualSpec = remember { resolveLiveChatInputVisualSpec() }
    val darkOverlay = isOverlay && palette.isDark
    val messages = remember { mutableStateListOf<LiveDanmakuItem>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var isAnyMenuOpen by remember { mutableStateOf(false) }
    var pendingScroll by remember { mutableStateOf(false) }
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
                    val shouldAutoScroll = !listState.isScrollInProgress && !isAwayFromBottom && !isAnyMenuOpen
                    messages.add(item)
                    if (messages.size > 200) messages.removeAt(0)
                    // 节流平滑滚动（通知 LaunchedEffect 批处理）
                    if (shouldAutoScroll) {
                        pendingScroll = true
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LiveChatSection", "❌ Message add error: ${e.message}")
                }
            }
        }
    }

    // 节流平滑滚动（300ms 批处理，对齐 PiliPlus 节流设计，防止瞬时密集弹幕引起的持续动画打断与掉帧）
    LaunchedEffect(pendingScroll) {
        if (pendingScroll) {
            delay(300L)
            if (!listState.isScrollInProgress && !isAwayFromBottom && !isAnyMenuOpen && messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
            pendingScroll = false
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
                    .padding(
                        horizontal = AppSpacingTokens.Large,
                        vertical = AppSpacingTokens.Medium
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    AppText(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.primaryText
                    )
                    Spacer(Modifier.height(AppSpacingTokens.Micro))
                    AppText(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryText
                    )
                }
                AppSurface(
                    shape = AppShapes.container(ContainerLevel.Pill),
                    color = palette.accentSoft
                ) {
                    AppText(
                        text = "弹幕流",
                        color = palette.accentStrong,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(
                            horizontal = AppSpacingTokens.Medium,
                            vertical = AppSpacingTokens.ExtraSmall
                        )
                    )
                }
            }

            AppHorizontalDivider(color = palette.border)
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
                    horizontal = if (isOverlay) AppSpacingTokens.Medium else AppSpacingTokens.Large,
                    vertical = if (isOverlay) AppSpacingTokens.Small else AppSpacingTokens.Medium
                ),
                verticalArrangement = Arrangement.spacedBy(
                    space = if (isOverlay) {
                        chatVisualSpec.overlayMessageSpaceDp.dp
                    } else {
                        AppSpacingTokens.Small
                    },
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
                        onReportDanmaku = onReportDanmaku,
                        onMenuVisibilityChange = { isAnyMenuOpen = it }
                    )
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = isAwayFromBottom,
                enter = fadeIn() + scaleIn(initialScale = 0.85f),
                exit = fadeOut() + scaleOut(targetScale = 0.85f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = AppSpacingTokens.Medium,
                        bottom = AppSpacingTokens.Small
                    )
            ) {
                AppSurface(
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem(messages.lastIndex.coerceAtLeast(0))
                        }
                    },
                    shape = AppShapes.container(ContainerLevel.Pill),
                    color = if (darkOverlay) palette.bubbleStrong else palette.surfaceMuted,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(
                            horizontal = AppSpacingTokens.Medium,
                            vertical = AppSpacingTokens.Small
                        )
                    ) {
                        AppIcon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            tint = if (darkOverlay) LiveStatusPalette.MediaContent else palette.primaryText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(AppSpacingTokens.ExtraSmall))
                        AppText(
                            text = "回到底部",
                            color = if (darkOverlay) LiveStatusPalette.MediaContent else palette.primaryText,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
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
    onReportDanmaku: (LiveDanmakuItem) -> Unit,
    onMenuVisibilityChange: (Boolean) -> Unit = {}
) {
    if (item.isSuperChat) {
        SuperChatMessageItem(item = item, isOverlay = isOverlay)
        return
    }
    if (!shouldRenderLiveDanmaku(item.text, item.emoticonUrl)) {
        return
    }
    val context = LocalContext.current
    val palette = rememberLiveChromePalette()
    var showMenu by remember { mutableStateOf(false) }
    LaunchedEffect(showMenu) {
        onMenuVisibilityChange(showMenu)
    }
    val tokens = resolveLiveBiliPaiChatBubbleTokens(isOverlay = isOverlay, isDark = palette.isDark)
    val bubbleShape = RoundedCornerShape(tokens.cornerRadiusDp.dp)
    val colorScheme = MaterialTheme.colorScheme
    val bubbleBackground = when {
        isOverlay -> LiveStatusPalette.MediaScrim.copy(alpha = tokens.backgroundAlpha)
        else -> colorScheme.surfaceContainerHigh
    }

    // Match video/dynamic comments: semantic theme colors keep contrast consistent in both
    // light and dark themes instead of relying on a separate hardcoded live palette.
    val readableText = colorScheme.onSurface
    val readableSecondaryText = colorScheme.onSurfaceVariant
    val usernameColor = if (item.isAdmin) {
        LiveStatusPalette.AdminName
    } else if (item.isSelf) {
        palette.accentStrong
    } else if (isOverlay) {
        LiveStatusPalette.MediaContent.copy(alpha = tokens.nameAlpha)
    } else {
        readableText.copy(alpha = tokens.nameAlpha.coerceAtLeast(0.9f))
    }
    val bodyColor = if (isOverlay) LiveStatusPalette.MediaContent else readableText
    val emoticonMap by DanmakuEmoticonMapper.emoticonMap.collectAsStateWithLifecycle()
    val replyColor = if (isOverlay) LiveStatusPalette.Reply else readableSecondaryText

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val bubbleWidthFraction = if (isOverlay) 0.90f else 0.86f
        val bubbleModifier = Modifier
            .widthIn(max = maxWidth * bubbleWidthFraction)
            .clip(bubbleShape)
            .background(bubbleBackground)
            .clickable(enabled = item.uid > 0L || item.uname.isNotBlank()) { showMenu = true }
            .padding(horizontal = tokens.horizontalPaddingDp.dp, vertical = tokens.verticalPaddingDp.dp)

        Box(modifier = bubbleModifier) {
            if (shouldRenderLiveDanmakuImageEmoticon(item.emoticonUrl)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppText(
                        text = "${item.uname.ifBlank { "直播观众" }}: ",
                        color = usernameColor,
                        fontSize = tokens.fontSizeSp.sp,
                        fontWeight = FontWeight.Medium
                    )
                    AsyncImage(
                        model = item.emoticonUrl,
                        contentDescription = item.text,
                        modifier = Modifier.size(AppSpacingTokens.DoubleExtraLarge)
                    )
                }
            } else {
                val annotatedText = remember(item.text, item.uname, item.replyToName, emoticonMap, replyColor, usernameColor) {
                    val builder = androidx.compose.ui.text.AnnotatedString.Builder()
                    builder.pushStyle(
                        androidx.compose.ui.text.SpanStyle(
                            color = usernameColor,
                            fontSize = tokens.fontSizeSp.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    builder.append("${item.uname.ifBlank { "直播观众" }}: ")
                    builder.pop()
                    if (item.replyToName.isNotBlank()) {
                        builder.pushStyle(
                            androidx.compose.ui.text.SpanStyle(
                                color = replyColor,
                                fontWeight = FontWeight.Medium,
                                fontSize = tokens.fontSizeSp.sp
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
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = tokens.fontSizeSp.sp,
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
    AppDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        AppDropdownMenuItem(
            text = {
                AppText(
                    text = item.uname.ifBlank { "弹幕" },
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            onClick = {}
        )
        AppHorizontalDivider()
        AppDropdownMenuItem(
            text = { AppText("复制弹幕信息") },
            onClick = {
                onDismiss()
                onCopyInfo()
            }
        )
        AppDropdownMenuItem(
            text = { AppText("去TA的个人空间") },
            enabled = item.uid > 0L,
            onClick = {
                onDismiss()
                onUserClick(item.uid)
            }
        )
        AppDropdownMenuItem(
            text = { AppText("@TA") },
            enabled = item.uname.isNotBlank(),
            onClick = {
                onDismiss()
                onAtUser(item)
            }
        )
        AppDropdownMenuItem(
            text = { AppText("屏蔽发送者") },
            enabled = item.uname.isNotBlank() || item.uid > 0L,
            onClick = {
                onDismiss()
                onBlockUser(item)
            }
        )
        AppDropdownMenuItem(
            text = { AppText("举报选中弹幕") },
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
    val bg = resolveLiveSuperChatColor(item.superChatBackgroundColor)
        .copy(alpha = if (isOverlay) 0.82f else 1f)
    AppSurface(
        color = bg,
        shape = AppShapes.container(ContainerLevel.Card),
        modifier = Modifier.fillMaxWidth(if (isOverlay) 0.72f else 0.82f)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = AppSpacingTokens.Medium,
                vertical = AppSpacingTokens.Small
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppText(
                    text = item.uname.ifBlank { "醒目留言" },
                    color = LiveStatusPalette.MediaContent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (item.superChatPrice.isNotBlank()) {
                    AppSurface(
                        shape = AppShapes.container(ContainerLevel.Pill),
                        color = LiveStatusPalette.MediaScrim.copy(alpha = 0.18f)
                    ) {
                        AppText(
                            text = item.superChatPrice,
                            color = LiveStatusPalette.MediaContent,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                horizontal = AppSpacingTokens.Small,
                                vertical = AppSpacingTokens.Micro
                            )
                        )
                    }
                }
            }
            if (item.text.isNotBlank()) {
                Spacer(Modifier.height(AppSpacingTokens.ExtraSmall))
                AppText(
                    text = item.text,
                    color = LiveStatusPalette.MediaContent,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (isOverlay) 2 else 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// [新增] 粉丝牌组件
@Composable
private fun MedalBadge(name: String, level: Int, colorInt: Int) {
    val color = resolveLiveMedalColor(colorInt)
    val visualSpec = remember { resolveLiveMedalBadgeVisualSpec() }
    
    AppSurface(
        color = color,
        shape = AppShapes.container(ContainerLevel.Tag),
        modifier = Modifier.padding(top = AppSpacingTokens.Micro)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = AppSpacingTokens.ExtraSmall,
                vertical = visualSpec.verticalPaddingDp.dp,
            )
        ) {
            AppText(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = LiveStatusPalette.MediaContent,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.width(AppSpacingTokens.Micro))
            // 简单的竖线分隔
            Box(
                Modifier
                    .width(visualSpec.dividerWidthDp.dp)
                    .height(AppSpacingTokens.Small)
                    .background(LiveStatusPalette.MediaContent.copy(0.7f))
            )
            Spacer(Modifier.width(AppSpacingTokens.Micro))
            AppText(
                text = "$level",
                style = MaterialTheme.typography.labelSmall,
                color = LiveStatusPalette.MediaContent,
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
    val color = resolveLiveLevelColor(level)
    
    AppSurface(
                border = androidx.compose.foundation.BorderStroke(AppSpacingTokens.Micro / 2, color),
        shape = AppShapes.borderedContainer(ContainerLevel.Tag),
        color = Color.Transparent, // 空心
        modifier = Modifier.padding(top = AppSpacingTokens.Micro)
    ) {
         AppText(
            text = "UL$level",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = AppSpacingTokens.Micro)
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
    val inputVisualSpec = remember { resolveLiveChatInputVisualSpec() }
    val roomTokens = resolveLiveBiliPaiRoomColorTokens(
        inputOverlayColor = LiveStatusPalette.MediaContent,
        inputContentColor = LiveStatusPalette.MediaContent
    )
    val textColor = if (isOverlay) roomTokens.inputContentColor else palette.primaryText
    val placeholderColor = if (isOverlay) roomTokens.inputContentColor else palette.secondaryText
    val fieldColor = if (isOverlay) Color.Transparent else palette.searchField
    val iconTint = if (isOverlay) roomTokens.inputContentColor else palette.secondaryText
    
    AppSurface(
        color = if (isOverlay) roomTokens.inputOverlayColor.copy(alpha = roomTokens.inputContainerAlpha) else palette.surfaceElevated,
        shape = if (isOverlay) AppShapes.container(ContainerLevel.Sheet) else RectangleShape,
                tonalElevation = if (isOverlay) AppSpacingTokens.None else AppSpacingTokens.Micro,
                shadowElevation = AppSpacingTokens.None,
        border = androidx.compose.foundation.BorderStroke(
                    AppSpacingTokens.Micro / 2,
            if (isOverlay) roomTokens.inputOverlayColor.copy(alpha = 0.10f) else palette.border
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    horizontal = AppSpacingTokens.Medium,
                    vertical = AppSpacingTokens.Small
                )
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconButton(
                onClick = onToggleDanmaku,
            ) {
                AppIcon(
                    imageVector = Icons.Filled.ChatBubble,
                    contentDescription = if (isDanmakuEnabled) "关闭弹幕" else "开启弹幕",
                    tint = if (isDanmakuEnabled) iconTint else iconTint.copy(alpha = 0.42f),
                    modifier = Modifier.size(inputVisualSpec.iconSizeDp.dp)
                )
            }
            Spacer(modifier = Modifier.width(AppSpacingTokens.ExtraSmall))

            // 输入框
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = textColor,
                ),
                cursorBrush = SolidColor(if (isOverlay) roomTokens.inputContentColor else palette.accent),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .height(inputVisualSpec.inputFieldHeightDp.dp)
                            .clip(AppShapes.container(ContainerLevel.Pill))
                            .background(fieldColor)
                            .padding(horizontal = AppSpacingTokens.Large),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (text.isEmpty()) {
                            AppText(
                                text = if (isOverlay) "发送弹幕" else "发个弹幕和主播互动吧~",
                                color = placeholderColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(AppSpacingTokens.Small))

            LiveLikeButton(
                tint = iconTint,
                onLike = onLike
            )

            AppIconButton(
                onClick = onOpenEmote,
            ) {
                AppIcon(
                    imageVector = Icons.Outlined.EmojiEmotions,
                    contentDescription = "表情",
                    tint = iconTint,
                    modifier = Modifier.size(inputVisualSpec.iconSizeDp.dp)
                )
            }

            val isEnabled = text.isNotBlank()
            AppFilledIconButton(
                onClick = {
                    if (isEnabled) {
                        onSend(text)
                        text = ""
                        focusManager.clearFocus()
                    }
                },
                enabled = isEnabled,
                colors = AppIconButtonDefaults.colors(
                    containerColor = if (isOverlay) {
                        roomTokens.inputContentColor.copy(alpha = 0.16f)
                    } else {
                        palette.accent
                    },
                    contentColor = roomTokens.inputContentColor,
                    disabledContainerColor = if (isOverlay) {
                        roomTokens.inputContentColor.copy(alpha = 0.10f)
                    } else {
                        palette.surfaceMuted
                    },
                    disabledContentColor = iconTint.copy(alpha = 0.48f),
                ),
            ) {
                AppIcon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "发送",
                    modifier = Modifier.size(inputVisualSpec.iconSizeDp.dp)
                )
            }
        }
    }
}

@Composable
internal fun LiveLikeButton(
    tint: Color,
    onLike: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var likeCount by remember { mutableIntStateOf(0) }
    var flushJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val palette = rememberLiveChromePalette()
    val visualSpec = remember { resolveLiveChatInputVisualSpec() }

    DisposableEffect(Unit) {
        onDispose { flushJob?.cancel() }
    }

    Box {
        AppIconButton(
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
        ) {
            AppIcon(
                imageVector = Icons.Outlined.ThumbUpOffAlt,
                contentDescription = "点赞",
                tint = tint,
                modifier = Modifier.size(visualSpec.iconSizeDp.dp)
            )
        }
        AnimatedContent(
            targetState = likeCount,
            transitionSpec = {
                (scaleIn(initialScale = 0.72f) + fadeIn()) togetherWith
                    (scaleOut(targetScale = 1.18f) + fadeOut())
            },
            label = "live-like-burst",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(
                    x = visualSpec.likeBadgeOffsetXDp.dp,
                    y = visualSpec.likeBadgeOffsetYDp.dp,
                ),
        ) { count ->
            if (count <= 0) {
                Spacer(Modifier)
            } else {
                AppSurface(
                    shape = AppShapes.container(ContainerLevel.Tag),
                    color = palette.accent.copy(alpha = 0.96f),
                ) {
                    AppText(
                        text = "x$count",
                        color = palette.onAccent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            horizontal = AppSpacingTokens.ExtraSmall,
                            vertical = AppSpacingTokens.Micro
                        )
                    )
                }
            }
        }
    }
}
