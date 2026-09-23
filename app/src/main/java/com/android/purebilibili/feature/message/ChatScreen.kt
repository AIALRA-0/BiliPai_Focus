// 聊天详情页面
package com.android.purebilibili.feature.message
import android.os.Build

import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.store.HomeWallpaperEffectMode
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.LocalGlobalWallpaperBackdropVisible
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.FeedTitleHierarchy
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.core.ui.resolveAppPlayIcon
import com.android.purebilibili.core.ui.ImmersiveAppScaffold as AppScaffold
import com.android.purebilibili.core.ui.AppTopBar
import com.android.purebilibili.core.ui.feedContentTypography
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppWindowAction
import com.android.purebilibili.core.ui.components.AppWindowActionMenu
import com.android.purebilibili.core.ui.components.AppOutlinedTextField
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppSnackbar
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.PickGalleryVisualMedia
import com.android.purebilibili.data.model.response.EmoteInfo
import com.android.purebilibili.data.model.response.PrivateMessageItem
import com.android.purebilibili.data.repository.MessageSessionControlInfo
import com.android.purebilibili.feature.home.HomeWallpaperBackdrop
import com.android.purebilibili.feature.home.resolveHomeWallpaperBackdropAppearance
import com.android.purebilibili.feature.home.resolveHomeWallpaperUri
import com.android.purebilibili.feature.home.components.cards.HorizontalVideoCardFrame
import com.android.purebilibili.feature.home.components.cards.VideoCardCoverDurationText
import com.android.purebilibili.feature.home.components.cards.LocalWallpaperPalette
import com.android.purebilibili.feature.home.components.cards.WallpaperPaletteStore
import com.android.purebilibili.feature.home.components.BottomBarMatchedReusableLiquidDock
import com.android.purebilibili.feature.home.components.liquid.rememberCombinedBackdrop
import com.android.purebilibili.feature.home.components.resolveFloatingDockGeometryScale
import com.android.purebilibili.feature.home.components.resolveSharedBottomBarCapsuleShape
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.MediaContrastPalette
import com.android.purebilibili.core.ui.blur.ChromeBackdropSource
import com.android.purebilibili.core.ui.blur.hazeSourceCompat
import com.android.purebilibili.core.ui.blur.rememberChromeBackdropSource
import com.android.purebilibili.core.ui.blur.rememberRecoverableHazeState
import com.android.purebilibili.core.ui.blur.shouldAllowRenderEffectBackedHazeEffect
import com.android.purebilibili.core.ui.performance.isLowBlurBudgetForced
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import dev.chrisbanes.haze.HazeState

private const val MESSAGE_LARGE_VIDEO_COVER_ASPECT_RATIO = 4f / 3f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    talkerId: Long,
    sessionType: Int,
    userName: String,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onOpenBilibiliLink: (String) -> Unit = {},
    viewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory(talkerId, sessionType))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var pendingWithdrawMessage by remember { mutableStateOf<PrivateMessageItem?>(null) }
    var showInterceptConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = PickGalleryVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.sendImageMessage(context, uri)
        }
    }
    
    // 滚动到底部
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    val chatThemeConfig = LocalAppThemeConfig.current
    val lowBlurBudget = isLowBlurBudgetForced()
    val chatChromeSource = if (
        chatThemeConfig.progressiveTopBlurEnabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !lowBlurBudget
    ) {
        rememberChromeBackdropSource()
    } else {
        null
    }
    val chatHazeState = if (
        chatThemeConfig.headerBlurEnabled &&
            !chatThemeConfig.progressiveTopBlurEnabled &&
            !lowBlurBudget &&
            shouldAllowRenderEffectBackedHazeEffect(Build.VERSION.SDK_INT)
    ) {
        rememberRecoverableHazeState(initialBlurEnabled = true)
    } else {
        null
    }

    val chatWallpaperBackdrop = if (chatThemeConfig.liquidGlassEnabled) {
        rememberLayerBackdrop()
    } else {
        null
    }
    val chatContentBackdrop = if (chatThemeConfig.liquidGlassEnabled) {
        rememberLayerBackdrop()
    } else {
        null
    }
    val chatInputBackdrop = if (chatWallpaperBackdrop != null && chatContentBackdrop != null) {
        rememberCombinedBackdrop(chatWallpaperBackdrop, chatContentBackdrop)
    } else {
        null
    }
    
    ChatWallpaperHost(
        chromeBackdropSource = chatChromeSource,
        hazeState = chatHazeState,
        wallpaperBackdrop = chatWallpaperBackdrop,
    ) {
    AppScaffold(
        containerColor = Color.Transparent,
        topBarSurfaceColor = AppSurfaceTokens.chromeBackground(),
        preferProgressiveTopBlur = chatThemeConfig.progressiveTopBlurEnabled,
        chromeBackdropSource = chatChromeSource,
        externalHazeState = chatHazeState,
        blurContentReady = !uiState.isLoading,
        topBar = {
            AppTopBar(
                title = userName,
                navigationIcon = {
                    AppIconButton(onClick = onBack) {
                        AppIcon(rememberAppBackIcon(), contentDescription = "返回")
                    }
                },
                actions = {
                    ChatSessionControlMenu(
                        sessionType = sessionType,
                        controlInfo = uiState.sessionControlInfo,
                        isUpdating = uiState.isSessionControlUpdating,
                        onToggleDnd = viewModel::toggleDnd,
                        onTogglePush = viewModel::togglePushMuted,
                        onToggleIntercept = {
                                if (uiState.sessionControlInfo.isIntercept == true) {
                                    viewModel.toggleIntercept()
                                } else {
                                    showInterceptConfirm = true
                                }
                        },
                        onRefresh = viewModel::loadSessionControlInfo,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(chatContentBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
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
                            AppText(uiState.error ?: "加载失败")
                            Spacer(modifier = Modifier.height(8.dp))
                            AppButton(onClick = { viewModel.loadMessages() }) {
                                AppText("重试")
                            }
                        }
                    }
                    uiState.messages.isEmpty() -> {
                        AppText(
                            text = "暂无消息",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 加载更多按钮
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
                                            AppTextButton(onClick = { viewModel.loadMoreMessages() }) {
                                                AppText("加载更多")
                                            }
                                        }
                                    }
                                }
                            }
                            items(
                                items = uiState.messages,
                                key = { it.msg_key }
                            ) { message ->
                                MessageBubble(
                                    message = message,
                                    isOwnMessage = message.sender_uid == viewModel.currentUserMid,
                                    emoteInfos = uiState.emoteInfos,
                                    videoPreviews = uiState.videoPreviews,
                                    canWithdraw = message.sender_uid == viewModel.currentUserMid && message.msg_status != 1,
                                    onLongPress = {
                                        pendingWithdrawMessage = message
                                    },
                                    onVideoClick = { bvid ->
                                        onNavigateToVideo(bvid)
                                    },
                                    onLinkClick = { link ->
                                        onOpenBilibiliLink(link)
                                    }
                                )
                            }
                        }
                    }
                }

                // 发送错误提示
                uiState.sendError?.let { error ->
                    AppSnackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            AppTextButton(onClick = { viewModel.clearSendError() }) {
                                AppText("知道了")
                            }
                        }
                    ) {
                        AppText(error)
                    }
                }
            }

            ChatInputBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                onPickImage = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                isSending = uiState.isSending,
                isUploadingImage = uiState.isUploadingImage,
                backdrop = chatInputBackdrop,
            )
        }
    }
    }

    pendingWithdrawMessage?.let { targetMessage ->
        AppAlertDialog(
            onDismissRequest = {
                if (uiState.withdrawingMessageKey == null) {
                    pendingWithdrawMessage = null
                }
            },
            title = { AppText("撤回消息") },
            text = { AppText("要撤回这条消息吗？") },
            confirmButton = {
                AppTextButton(
                    enabled = uiState.withdrawingMessageKey == null,
                    onClick = {
                        viewModel.withdrawMessage(targetMessage)
                    }
                ) {
                    AppText(if (uiState.withdrawingMessageKey == targetMessage.msg_key) "撤回中..." else "确认")
                }
            },
            dismissButton = {
                AppTextButton(
                    enabled = uiState.withdrawingMessageKey == null,
                    onClick = {
                        pendingWithdrawMessage = null
                    }
                ) {
                    AppText("取消")
                }
            }
        )
    }

    LaunchedEffect(uiState.withdrawingMessageKey) {
        if (uiState.withdrawingMessageKey == null) {
            pendingWithdrawMessage = null
        }
    }

    if (showInterceptConfirm) {
        AppAlertDialog(
            onDismissRequest = { showInterceptConfirm = false },
            title = { AppText("移入拦截") },
            text = { AppText("后续这类会话会进入拦截分类，仍可在拦截列表中查看和恢复。") },
            confirmButton = {
                AppTextButton(
                    onClick = {
                        viewModel.toggleIntercept()
                        showInterceptConfirm = false
                    }
                ) {
                    AppText("移入")
                }
            },
            dismissButton = {
                AppTextButton(onClick = { showInterceptConfirm = false }) {
                    AppText("取消")
                }
            }
        )
    }
}

/**
 * Chat is a retained navigation entry, so it needs its own opaque visual root. Otherwise a
 * transparent chat scaffold composites with the inbox entry underneath instead of with the
 * same wallpaper that HomeScreen uses.
 */
@Composable
private fun ChatWallpaperHost(
    chromeBackdropSource: ChromeBackdropSource?,
    hazeState: HazeState?,
    wallpaperBackdrop: LayerBackdrop?,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val configuredHomeWallpaperUri by SettingsManager.getHomeWallpaperUri(context)
        .collectAsStateWithLifecycle(initialValue = "")
    val splashWallpaperUri by SettingsManager.getSplashWallpaperUri(context)
        .collectAsStateWithLifecycle(initialValue = "")
    val wallpaperEffectMode by SettingsManager.getHomeWallpaperEffectMode(context)
        .collectAsStateWithLifecycle(initialValue = HomeWallpaperEffectMode.SOFT_BLUR)
    val wallpaperUri = remember(configuredHomeWallpaperUri, splashWallpaperUri) {
        resolveHomeWallpaperUri(
            homeWallpaperUri = configuredHomeWallpaperUri,
            splashWallpaperUri = splashWallpaperUri,
        )
    }
    val wallpaperPalette by WallpaperPaletteStore.currentPalette.collectAsStateWithLifecycle()
    LaunchedEffect(wallpaperUri) {
        WallpaperPaletteStore.loadWallpaperPalette(
            context = context,
            uri = wallpaperUri,
            scope = this,
        )
    }

    val baseColor = AppSurfaceTokens.chromeBackground()
    val isLightBackground = remember(baseColor) { baseColor.luminance() > 0.5f }
    val isDataSaverActive = remember(context) {
        SettingsManager.isDataSaverActive(context)
    }
    val wallpaperAppearance = remember(
        wallpaperUri,
        wallpaperEffectMode,
        isLightBackground,
        isDataSaverActive,
    ) {
        resolveHomeWallpaperBackdropAppearance(
            hasWallpaper = wallpaperUri.isNotBlank(),
            effectMode = wallpaperEffectMode,
            isDarkTheme = !isLightBackground,
            isDataSaverActive = isDataSaverActive,
        )
    }
    val wallpaperVisible = wallpaperAppearance.visible && wallpaperUri.isNotBlank()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(chromeBackdropSource?.modifier ?: Modifier)
                .then(hazeState?.let { Modifier.hazeSourceCompat(it) } ?: Modifier)
                .then(wallpaperBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
        ) {
            HomeWallpaperBackdrop(
                wallpaperUri = wallpaperUri,
                appearance = wallpaperAppearance,
                baseColor = baseColor,
                isDataSaverActive = isDataSaverActive,
                modifier = Modifier.fillMaxSize(),
            )
        }
        CompositionLocalProvider(
            LocalGlobalWallpaperBackdropVisible provides wallpaperVisible,
            LocalWallpaperPalette provides wallpaperPalette,
        ) {
            content()
        }
    }
}

@Composable
private fun ChatSessionControlMenu(
    sessionType: Int,
    controlInfo: MessageSessionControlInfo,
    isUpdating: Boolean,
    onToggleDnd: () -> Unit,
    onTogglePush: () -> Unit,
    onToggleIntercept: () -> Unit,
    onRefresh: () -> Unit
) {
    val limitLabel = when {
        controlInfo.isLimit == true && controlInfo.reportLimit == true -> "会话受限，举报也受限"
        controlInfo.isLimit == true -> "会话受限"
        controlInfo.reportLimit == true -> "举报受限"
        else -> null
    }
    AppWindowActionMenu(
        groups = listOf(
            buildList {
                add(
                    AppWindowAction(
                        label = if (controlInfo.isDnd == true) "关闭免打扰" else "开启免打扰",
                        enabled = !isUpdating,
                        onClick = onToggleDnd,
                    )
                )
                if (sessionType == 1 && controlInfo.showPushSetting) {
                    add(
                        AppWindowAction(
                            label = if (controlInfo.pushMuted == true) "接收推送" else "关闭推送",
                            enabled = !isUpdating,
                            onClick = onTogglePush,
                        )
                    )
                }
                if (sessionType == 1) {
                    add(
                        AppWindowAction(
                            label = if (controlInfo.isIntercept == true) "移出拦截" else "移入拦截",
                            enabled = !isUpdating,
                            onClick = onToggleIntercept,
                        )
                    )
                }
                limitLabel?.let { add(AppWindowAction(label = it, enabled = false)) }
                add(
                    AppWindowAction(
                        label = "刷新状态",
                        enabled = !isUpdating,
                        onClick = onRefresh,
                    )
                )
            }
        ),
    ) {
        AppIcon(Icons.Default.MoreVert, contentDescription = "会话设置")
    }
}

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onPickImage: () -> Unit,
    isSending: Boolean,
    isUploadingImage: Boolean,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier,
) {
    val showSendAction = text.isNotBlank()
    val isBusy = isSending || isUploadingImage

    val liquidGlassEnabled = LocalAppThemeConfig.current.liquidGlassEnabled
    val dockShape = resolveSharedBottomBarCapsuleShape()
    val inputHeight = AppSpacingTokens.TripleExtraLarge + AppSpacingTokens.Small
    val shellLensIntensity = resolveFloatingDockGeometryScale(inputHeight.value)
    val panelColor = com.android.purebilibili.core.ui.globalWallpaperAwareChromeColor(
        AppSurfaceTokens.surface()
    )
    val keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send)
    val keyboardActions = KeyboardActions(onSend = { onSend() })

    Row(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BottomBarMatchedReusableLiquidDock(
            shape = dockShape,
            modifier = Modifier
                .weight(1f)
                .height(inputHeight)
                .then(
                    if (!liquidGlassEnabled) {
                        Modifier
                            .clip(dockShape)
                            .background(panelColor)
                    } else {
                        Modifier
                    }
                ),
            backdrop = backdrop,
            reuseEnabled = liquidGlassEnabled,
            drawShellLens = true,
            shellLensIntensity = shellLensIntensity,
        ) { liquidChromeActive ->
            val fieldColor = if (liquidChromeActive) Color.Transparent else panelColor
            val fieldTextColor = MaterialTheme.colorScheme.onSurface
            val placeholderColor = if (liquidChromeActive) {
                fieldTextColor.copy(alpha = 0.82f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            AppOutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxSize(),
                placeholderText = "输入消息...",
                maxLines = 4,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                shape = dockShape,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = fieldTextColor),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = fieldColor,
                    unfocusedContainerColor = fieldColor,
                    disabledContainerColor = fieldColor,
                    focusedBorderColor = if (liquidChromeActive) {
                        Color.Transparent
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    unfocusedBorderColor = if (liquidChromeActive) {
                        Color.Transparent
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    disabledBorderColor = Color.Transparent,
                    focusedTextColor = fieldTextColor,
                    unfocusedTextColor = fieldTextColor,
                    disabledTextColor = fieldTextColor.copy(alpha = 0.72f),
                    focusedPlaceholderColor = placeholderColor,
                    unfocusedPlaceholderColor = placeholderColor,
                    disabledPlaceholderColor = placeholderColor,
                    cursorColor = fieldTextColor,
                ),
            )
        }

        BottomBarMatchedReusableLiquidDock(
            shape = CircleShape,
            modifier = Modifier
                .size(inputHeight)
                .then(
                    if (!liquidGlassEnabled) {
                        Modifier
                            .clip(CircleShape)
                            .background(panelColor)
                    } else {
                        Modifier
                    }
                ),
            backdrop = backdrop,
            reuseEnabled = liquidGlassEnabled,
            drawShellLens = true,
            shellLensIntensity = shellLensIntensity,
        ) {
            AppIconButton(
                onClick = {
                    if (showSendAction) {
                        onSend()
                    } else {
                        onPickImage()
                    }
                },
                modifier = Modifier.fillMaxSize(),
                enabled = !isBusy,
            ) {
                if (isBusy) {
                    com.android.purebilibili.core.ui.CutePersonLoadingIndicator(
                        size = 24.dp,
                        strokeWidth = 2.dp,
                    )
                } else {
                    AppIcon(
                        imageVector = if (showSendAction) {
                            Icons.AutoMirrored.Filled.Send
                        } else {
                            Icons.Filled.AddCircle
                        },
                        contentDescription = if (showSendAction) "发送" else "图片",
                        tint = if (showSendAction) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: PrivateMessageItem,
    isOwnMessage: Boolean,
    emoteInfos: List<EmoteInfo> = emptyList(),
    videoPreviews: Map<String, VideoPreviewInfo> = emptyMap(),
    canWithdraw: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onVideoClick: ((String) -> Unit)? = null,
    onLinkClick: ((String) -> Unit)? = null
) {
    val bubbleShape = AppShapes.container(ContainerLevel.Card)
    val fallbackContainerColor = resolveMessageBubbleFallbackContainerColor(
        isOwnMessage = isOwnMessage,
        primary = MaterialTheme.colorScheme.primary,
        surfaceVariant = MaterialTheme.colorScheme.surfaceVariant,
    )
    val fallbackContentColor = resolveMessageBubbleFallbackContentColor(
        isOwnMessage = isOwnMessage,
        onPrimary = MaterialTheme.colorScheme.onPrimary,
        onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val textColor = fallbackContentColor
    
    // BV号正则匹配
    val bvPattern = remember { Regex("BV[a-zA-Z0-9]{10}") }
    
    // 从消息内容中提取BV号
    val detectedBvids = remember(message.content) {
        if (message.msg_type == 1) {
            val content = parseTextContent(message.content)
            bvPattern.findAll(content).map { it.value }.toList()
        } else {
            emptyList()
        }
    }
    val parsedCard = remember(message.content, message.msg_type) {
        MessagePreviewParser.parseMessageCard(message.content, message.msg_type)
    }
    val linkedVideoPreviews = remember(detectedBvids, videoPreviews) {
        detectedBvids.distinct().mapNotNull { bvid ->
            videoPreviews[bvid]?.let { preview -> bvid to preview }
        }
    }
    val shouldUseLargeVideoLinkCard = message.msg_type == 1 && linkedVideoPreviews.isNotEmpty()
    val isLargeVideoMessage = message.msg_status != 1 && parsedCard?.kind == MessageCardKind.Video
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
    ) {
        if (isLargeVideoMessage) {
            Spacer(modifier = Modifier.height(2.dp))
            AppText(
                text = formatMessageTime(message.timestamp),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
        }

        if (isLargeVideoMessage && parsedCard != null) {
            MessageLargeVideoCard(
                preview = parsedCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            when {
                                parsedCard.bvid.isNotBlank() -> onVideoClick?.invoke(parsedCard.bvid)
                                parsedCard.targetUrl.isNotBlank() -> onLinkClick?.invoke(parsedCard.targetUrl)
                            }
                        },
                        onLongClick = if (canWithdraw) onLongPress else null,
                    ),
            )
        } else if (!shouldUseLargeVideoLinkCard) {
            // 消息气泡
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = if (isOwnMessage) Alignment.TopEnd else Alignment.TopStart,
            ) {
                AppSurface(
                    modifier = Modifier
                        .widthIn(max = resolveMessageBubbleMaxWidth(maxWidth))
                        .then(
                            if (canWithdraw && onLongPress != null) {
                                Modifier.combinedClickable(
                                    onClick = {},
                                    onLongClick = onLongPress
                                )
                            } else {
                                Modifier
                            }
                        ),
                    shape = bubbleShape,
                    color = fallbackContainerColor,
                    contentColor = fallbackContentColor,
                ) {
                    Box(
                        modifier = Modifier.padding(
                            horizontal = AppSpacingTokens.Medium,
                            vertical = AppSpacingTokens.Small,
                        )
                    ) {
                        when {
                        message.msg_status == 1 -> {
                            // 已撤回消息
                            AppText(
                                text = "[消息已撤回]",
                                color = textColor.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        message.msg_type == 1 -> {
                            // 文字消息 - 支持表情渲染
                            val content = parseTextContent(message.content)
                            EmoteText(
                                text = content,
                                emoteInfos = emoteInfos,
                                color = textColor,
                                linkColor = textColor,
                                style = MaterialTheme.typography.bodyLarge,
                                onLinkClick = onLinkClick
                            )
                        }
                        message.msg_type == 2 -> {
                            // 图片消息
                            val imageUrl = parseImageUrl(message.content)
                            if (imageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "图片",
                                    modifier = Modifier
                                        .widthIn(max = 200.dp)
                                        .heightIn(max = 300.dp)
                                        .clip(AppShapes.container(ContainerLevel.Chip)),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                AppText(
                                    text = "[图片]",
                                    color = textColor,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        message.msg_type == 6 -> {
                            // 表情消息 (大表情)
                            val emoteUrl = parseEmoteUrl(message.content)
                            if (emoteUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = emoteUrl,
                                    contentDescription = "表情",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(AppShapes.container(ContainerLevel.Chip)),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                AppText(
                                    text = "[表情]",
                                    color = textColor,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        message.msg_type == 10 -> {
                            // 通知消息
                            AppText(
                                text = parseNotificationContent(message.content),
                                color = textColor,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        message.msg_type == 11 -> {
                            parsedCard?.let { card ->
                                MessageCardPreviewCard(
                                    preview = card,
                                    onClick = {
                                        when {
                                            card.bvid.isNotBlank() -> onVideoClick?.invoke(card.bvid)
                                            card.targetUrl.isNotBlank() -> onLinkClick?.invoke(card.targetUrl)
                                        }
                                    }
                                )
                            } ?: AppText(
                                text = "[视频]",
                                color = textColor,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        message.msg_type in setOf(7, 12, 13, 14) -> {
                            parsedCard?.let { card ->
                                MessageCardPreviewCard(
                                    preview = card,
                                    onClick = {
                                        when {
                                            card.bvid.isNotBlank() -> onVideoClick?.invoke(card.bvid)
                                            card.targetUrl.isNotBlank() -> onLinkClick?.invoke(card.targetUrl)
                                        }
                                    }
                                )
                            } ?: AppText(
                                text = "[${getMessageTypeName(message.msg_type)}]",
                                color = textColor,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        else -> {
                            AppText(
                                text = "[${getMessageTypeName(message.msg_type)}]",
                                color = textColor.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        }
                    }
                }
            }
        }
        
        // 视频链接预览卡片
        linkedVideoPreviews.forEach { (bvid, preview) ->
            Spacer(modifier = Modifier.height(AppSpacingTokens.ExtraSmall))
            VideoLinkPreviewCard(
                preview = preview,
                onClick = { onVideoClick?.invoke(bvid) }
            )
        }
        
        if (!isLargeVideoMessage) {
            // 时间
            Spacer(modifier = Modifier.height(2.dp))
            AppText(
                text = formatMessageTime(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

/**
 * Full-width video message card matching the large stacked presentation used by the chat design.
 * It keeps the cover ratio, duration treatment, typography and theme-aware surface from the feed.
 */
@Composable
private fun MessageLargeVideoCard(
    preview: MessageCardPreview,
    modifier: Modifier = Modifier,
) {
    val cardShape = AppShapes.container(ContainerLevel.ProminentCard)
    val cardCorner = AppShapes.containerCornerDp(ContainerLevel.ProminentCard)
    val contentTypography = feedContentTypography(FeedTitleHierarchy.Standard)
    val glassContentColors = rememberMessageGlassContentColors(
        defaultOnSurface = MaterialTheme.colorScheme.onSurface,
        defaultOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Column(
        modifier = modifier
            .clip(cardShape)
            .messageGlassContainer(
                defaultContainerColor = AppSurfaceTokens.cardContainer(),
                shape = cardShape,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(MESSAGE_LARGE_VIDEO_COVER_ASPECT_RATIO)
                .clip(AppShapes.topRounded(cardCorner))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (preview.cover.isNotBlank()) {
                AsyncImage(
                    model = preview.cover,
                    contentDescription = preview.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            if (preview.duration > 0) {
                VideoCardCoverDurationText(
                    text = formatDuration(preview.duration),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(AppSpacingTokens.Small),
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(AppSpacingTokens.Small)
                    .size(AppSpacingTokens.TripleExtraLarge),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(
                    imageVector = resolveAppPlayIcon(),
                    contentDescription = "播放",
                    modifier = Modifier.fillMaxSize(),
                    tint = MediaContrastPalette.Foreground,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.bottomRounded(cardCorner))
                .background(AppSurfaceTokens.cardContainer())
                .padding(
                    horizontal = AppSpacingTokens.Medium,
                    vertical = AppSpacingTokens.Small,
                ),
            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall),
        ) {
            AppText(
                text = preview.title.ifBlank { preview.kind.label },
                style = contentTypography.title,
                color = glassContentColors.titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            preview.subtitle.takeIf { it.isNotBlank() }?.let { subtitle ->
                AppText(
                    text = subtitle,
                    style = contentTypography.author,
                    color = glassContentColors.subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Shared horizontal video-card presentation for private-message previews.
 * The frame and typography intentionally follow the related-video cards.
 */
@Composable
private fun MessageHorizontalVideoCard(
    coverUrl: String,
    title: String,
    duration: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    overlineText: String? = null,
    supportingText: String? = null,
) {
    val cardShape = AppShapes.container(ContainerLevel.Card)
    val contentTypography = feedContentTypography(FeedTitleHierarchy.Standard)
    val glassContentColors = rememberMessageGlassContentColors(
        defaultOnSurface = MaterialTheme.colorScheme.onSurface,
        defaultOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Box(
        modifier = modifier
            .widthIn(max = 280.dp)
            .fillMaxWidth()
            .clip(cardShape)
            .messageGlassContainer(
                defaultContainerColor = AppSurfaceTokens.cardContainer(),
                shape = cardShape,
            )
            .clickable(onClick = onClick),
    ) {
        HorizontalVideoCardFrame(
            coverContent = {
                if (coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            },
            coverOverlayContent = {
                if (duration > 0) {
                    VideoCardCoverDurationText(
                        text = formatDuration(duration),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp),
                    )
                }
            },
            infoContent = {
                overlineText?.takeIf { it.isNotBlank() }?.let {
                    AppText(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AppText(
                    text = title,
                    style = contentTypography.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = glassContentColors.titleColor,
                    modifier = Modifier.fillMaxWidth(),
                )
                supportingText?.takeIf { it.isNotBlank() }?.let {
                    AppText(
                        text = it,
                        style = contentTypography.author,
                        color = glassContentColors.subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
        )
    }
}

@Composable
fun VideoLinkPreviewCard(
    preview: VideoPreviewInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val supportingText = buildString {
        if (preview.ownerName.isNotBlank()) append(preview.ownerName)
        if (preview.viewCount > 0) {
            if (isNotEmpty()) append(" · ")
            append(FormatUtils.formatStat(preview.viewCount))
            append("播放")
        }
        if (preview.danmakuCount > 0) {
            if (isNotEmpty()) append(" · ")
            append(FormatUtils.formatStat(preview.danmakuCount))
            append("弹幕")
        }
    }
    MessageLargeVideoCard(
        preview = MessageCardPreview(
            kind = MessageCardKind.Video,
            title = preview.title,
            subtitle = supportingText,
            cover = preview.cover,
            bvid = preview.bvid,
            duration = preview.duration,
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

@Composable
fun MessageCardPreviewCard(
    preview: MessageCardPreview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MessageHorizontalVideoCard(
        coverUrl = preview.cover,
        title = preview.title.ifBlank { preview.kind.label },
        duration = preview.duration,
        overlineText = preview.kind.label,
        supportingText = preview.subtitle,
        onClick = onClick,
        modifier = modifier,
    )
}

/**
 * 格式化时长
 */
private fun formatDuration(seconds: Long): String {
    return FormatUtils.formatDuration(seconds.coerceAtLeast(0L).toInt())
}

/**
 * 支持表情和链接渲染的富文本组件
 */
@Composable
fun RichMessageText(
    text: String,
    emoteInfos: List<EmoteInfo>,
    color: Color,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    linkColor: Color = MaterialTheme.colorScheme.primary,  // 使用主题色
    onLinkClick: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    
    // 构建表情映射 (text -> EmoteInfo)
    val emoteMap = remember(emoteInfos) {
        emoteInfos.associateBy { it.text }
    }
    
    // 匹配模式
    val emotePattern = remember { "\\[([^\\[\\]]+)\\]".toRegex() }
    val urlPattern = remember { 
        "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)".toRegex() 
    }
    
    // 扫描所有特殊内容 (表情和URL)
    data class ContentMatch(val range: IntRange, val type: String, val value: String)
    
    val allMatches = remember(text) {
        val matches = mutableListOf<ContentMatch>()
        
        // 收集表情
        emotePattern.findAll(text).forEach { match ->
            matches.add(ContentMatch(match.range, "emote", match.value))
        }
        
        // 收集 URL
        urlPattern.findAll(text).forEach { match ->
            matches.add(ContentMatch(match.range, "url", match.value))
        }
        
        // 按位置排序
        matches.sortedBy { it.range.first }
    }
    
    // 如果没有特殊内容，直接显示文本
    if (allMatches.isEmpty()) {
        AppText(text = text, color = color, style = style, fontSize = fontSize)
        return
    }
    
    // 构建 AnnotatedString
    val annotatedString = buildAnnotatedString {
        var lastEnd = 0
        
        allMatches.forEach { match ->
            // 添加前面的普通文本
            if (match.range.first > lastEnd) {
                append(text.substring(lastEnd, match.range.first))
            }
            
            when (match.type) {
                "emote" -> {
                    val emote = emoteMap[match.value]
                    if (emote != null && emote.url.isNotEmpty()) {
                        appendInlineContent(match.value, match.value)
                    } else {
                        append(match.value)
                    }
                }
                "url" -> {
                    // 添加链接样式和注解
                    pushStringAnnotation(tag = "URL", annotation = match.value)
                    withStyle(SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    )) {
                        append(match.value)
                    }
                    pop()
                }
            }
            
            lastEnd = match.range.last + 1
        }
        
        // 添加剩余文本
        if (lastEnd < text.length) {
            append(text.substring(lastEnd))
        }
    }
    
    // 构建 InlineContent 映射 (表情图片)
    val inlineContentMap = remember(emoteInfos) {
        emoteInfos.filter { it.url.isNotEmpty() }.associate { emote ->
            emote.text to InlineTextContent(
                placeholder = Placeholder(
                    width = 1.4.em,
                    height = 1.4.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                )
            ) {
                AsyncImage(
                    model = emote.url,
                    contentDescription = emote.text,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
    
    // 用于检测点击位置
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    
    AppText(
        text = annotatedString,
        color = color,
        style = style,
        fontSize = fontSize,
        inlineContent = inlineContentMap,
        onTextLayout = { layoutResult = it },
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                layoutResult?.let { layout ->
                    val position = layout.getOffsetForPosition(offset)
                    annotatedString.getStringAnnotations(
                        tag = "URL",
                        start = position,
                        end = position
                    ).firstOrNull()?.let { annotation ->
                        val url = annotation.item
                        if (onLinkClick != null) {
                            onLinkClick(url)
                        } else {
                            // 默认: 用浏览器打开
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.util.Log.e("RichMessageText", "Failed to open URL: $url", e)
                            }
                        }
                    }
                }
            }
        }
    )
}

// 保留旧函数名的兼容性别名
@Composable
fun EmoteText(
    text: String,
    emoteInfos: List<EmoteInfo>,
    color: Color,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    linkColor: Color = MaterialTheme.colorScheme.primary,
    onLinkClick: ((String) -> Unit)? = null
) {
    RichMessageText(
        text = text,
        emoteInfos = emoteInfos,
        color = color,
        style = style,
        fontSize = fontSize,
        linkColor = linkColor,
        onLinkClick = onLinkClick
    )
}


/**
 * 解析文字消息内容
 */
private fun parseTextContent(content: String): String {
    if (content.isBlank()) return ""
    if (!content.trim().startsWith("{")) return content

    return try {
        val json = Json.parseToJsonElement(content)
        json.jsonObject["content"]?.jsonPrimitive?.content ?: content
    } catch (e: Exception) {
        content
    }
}

/**
 * 解析图片URL
 */
private fun parseImageUrl(content: String): String {
    return try {
        val json = Json.parseToJsonElement(content)
        json.jsonObject["url"]?.jsonPrimitive?.content ?: ""
    } catch (e: Exception) {
        ""
    }
}

/**
 * 解析表情URL
 */
private fun parseEmoteUrl(content: String): String {
    return try {
        val json = Json.parseToJsonElement(content)
        json.jsonObject["url"]?.jsonPrimitive?.content ?: ""
    } catch (e: Exception) {
        ""
    }
}

/**
 * 解析通知消息
 */
private fun parseNotificationContent(content: String): String {
    return try {
        val json = Json.parseToJsonElement(content)
        val title = json.jsonObject["title"]?.jsonPrimitive?.content ?: ""
        val text = json.jsonObject["text"]?.jsonPrimitive?.content ?: ""
        if (title.isNotEmpty()) "$title\n$text" else text
    } catch (e: Exception) {
        "[通知]"
    }
}

/**
 * 获取消息类型名称
 */
private fun getMessageTypeName(msgType: Int): String {
    return when (msgType) {
        1 -> "文字"
        2 -> "图片"
        5 -> "撤回"
        6 -> "表情"
        7 -> "分享"
        10 -> "通知"
        11 -> "视频"
        12 -> "专栏"
        13 -> "图片卡片"
        14 -> "分享"
        else -> "消息"
    }
}

/**
 * 格式化消息时间
 */
private fun formatMessageTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    
    val now = Calendar.getInstance()
    val msgTime = Calendar.getInstance().apply { timeInMillis = timestamp * 1000 }
    
    val sameDay = now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR)
    
    val pattern = if (sameDay) "HH:mm" else "MM-dd HH:mm"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp * 1000))
}
