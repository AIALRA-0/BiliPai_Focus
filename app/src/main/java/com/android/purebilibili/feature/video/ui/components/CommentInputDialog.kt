// 文件路径: feature/video/ui/components/CommentInputDialog.kt
package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.core.theme.opaqueCompositeOver
import com.android.purebilibili.core.ui.resolveFilledButtonContainerColor
import com.android.purebilibili.core.ui.resolveFilledButtonContentColor
import com.android.purebilibili.core.ui.components.AppSegmentOption
import com.android.purebilibili.core.ui.components.AppThemeAdaptiveTabRow
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppHorizontalDivider

import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppCircularProgressIndicator
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppOutlinedTextField
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.core.ui.components.appDesktopFocusableItemVisuals

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.android.purebilibili.core.util.PickMultipleGalleryVisualMedia
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.android.purebilibili.core.ui.motion.resolveCommentVerticalContentRevealMotionSpec
import com.android.purebilibili.core.ui.motion.verticalContentRevealEnterTransition
import com.android.purebilibili.core.ui.motion.verticalContentRevealExitTransition
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.data.model.response.MentionSearchUser
import kotlinx.coroutines.delay
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel

private const val COMMENT_INPUT_FOCUS_RETRY_COUNT = 3
private const val COMMENT_INPUT_FOCUS_RETRY_DELAY_MS = 80L

internal data class CommentInputDialogLayoutPolicy(
    val inputBoxMinHeightDp: Int,
    val inputBoxMaxHeightDp: Int,
    val emojiPanelHeightDp: Int,
    val sheetHorizontalPaddingDp: Int,
    val toolbarToolButtonSizeDp: Int,
    val toolbarToolSpacingDp: Int,
    val sendButtonHorizontalPaddingDp: Int
)

/**
 * 评论输入弹层尺寸策略。
 *
 * 平板在 48dp 触控下限之上再抬高输入舒适区（对标 BiliPai isTablet ? 300 : 170 的加高思路），
 * 横屏仍相对竖屏略压缩，避免遮挡过多视频区域。
 */
internal fun resolveCommentInputDialogLayoutPolicy(
    isLandscape: Boolean,
    isTablet: Boolean = false
): CommentInputDialogLayoutPolicy {
    return when {
        isTablet && isLandscape -> CommentInputDialogLayoutPolicy(
            inputBoxMinHeightDp = 96,
            inputBoxMaxHeightDp = 168,
            emojiPanelHeightDp = 240,
            sheetHorizontalPaddingDp = 20,
            toolbarToolButtonSizeDp = 44,
            toolbarToolSpacingDp = 8,
            sendButtonHorizontalPaddingDp = 20
        )
        isTablet -> CommentInputDialogLayoutPolicy(
            inputBoxMinHeightDp = 120,
            inputBoxMaxHeightDp = 200,
            emojiPanelHeightDp = 280,
            sheetHorizontalPaddingDp = 20,
            toolbarToolButtonSizeDp = 44,
            toolbarToolSpacingDp = 8,
            sendButtonHorizontalPaddingDp = 18
        )
        isLandscape -> CommentInputDialogLayoutPolicy(
            inputBoxMinHeightDp = 64,
            inputBoxMaxHeightDp = 112,
            emojiPanelHeightDp = 196,
            sheetHorizontalPaddingDp = 16,
            toolbarToolButtonSizeDp = 40,
            toolbarToolSpacingDp = 6,
            sendButtonHorizontalPaddingDp = 18
        )
        else -> CommentInputDialogLayoutPolicy(
            inputBoxMinHeightDp = 84,
            inputBoxMaxHeightDp = 136,
            emojiPanelHeightDp = 220,
            sheetHorizontalPaddingDp = 16,
            toolbarToolButtonSizeDp = 40,
            toolbarToolSpacingDp = 6,
            sendButtonHorizontalPaddingDp = 16
        )
    }
}

internal fun shouldAutoShowCommentKeyboard(
    visible: Boolean,
    canInputComment: Boolean,
    showEmojiPanel: Boolean
): Boolean {
    return visible && canInputComment && !showEmojiPanel
}

internal fun resolveCommentProgressInsertText(positionMs: Long): String {
    return " ${FormatUtils.formatDuration(positionMs.coerceAtLeast(0L))} "
}

internal fun commentDraftTextFieldValue(text: String): TextFieldValue {
    return TextFieldValue(
        text = text,
        selection = TextRange(text.length)
    )
}

internal fun canPublishCommentDraft(
    text: String,
    selectedImageCount: Int,
    canInputComment: Boolean,
    isSending: Boolean
): Boolean = canInputComment && !isSending &&
    (text.isNotBlank() || selectedImageCount > 0)

/**
 * 评论输入对话框
 * 
 * 提供评论输入功能，支持回复指定评论
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentInputDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSend: (String, List<Uri>, Boolean) -> Unit,
    isSending: Boolean = false,
    replyToName: String? = null,
    inputHint: String = "进来唠会嗑呗~",
    canInputComment: Boolean = true,
    modifier: Modifier = Modifier,
    currentVideoPositionMsProvider: () -> Long = { 0L },
    mentionUsers: List<MentionSearchUser> = emptyList(),
    isMentionSearching: Boolean = false,
    mentionSearchError: String? = null,
    onMentionSearchQueryChange: (String) -> Unit = {},
    initialText: String = "",
    initialImageUris: List<Uri> = emptyList(),
    initialSyncToDynamic: Boolean = false,
    onDraftChange: (String, List<Uri>, Boolean) -> Unit = { _, _, _ -> },
    emotePackages: List<com.android.purebilibili.data.model.response.EmotePackage> = emptyList() // [新增] 表情包列表
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val isTablet = LocalWindowSizeClass.current.isTablet
    val layoutPolicy = remember(isLandscape, isTablet) {
        resolveCommentInputDialogLayoutPolicy(
            isLandscape = isLandscape,
            isTablet = isTablet
        )
    }

    // 状态
    var textFieldValue by remember { mutableStateOf(commentDraftTextFieldValue(initialText)) }
    var isForwardToDynamic by remember { mutableStateOf(initialSyncToDynamic) } // 转发到动态
    var showEmojiPanel by remember { mutableStateOf(false) }    // 表情面板
    var showMentionPanel by remember { mutableStateOf(false) }
    var mentionSearchText by remember { mutableStateOf("") }
    var currentTab by remember { mutableIntStateOf(0) } // 0=Kaomoji, 1=Emoji, 2+=API Packages
    var selectedImageUris by remember { mutableStateOf(initialImageUris) }
    val text = textFieldValue.text
    val canPublish = canPublishCommentDraft(
        text = text,
        selectedImageCount = selectedImageUris.size,
        canInputComment = canInputComment,
        isSending = isSending
    )
    
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissDialog = remember(onDismiss, keyboardController, focusManager) {
        {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
            onDismiss()
        }
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        PickMultipleGalleryVisualMedia(maxItems = 9)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris = (selectedImageUris + uris)
                .distinct()
                .take(9)
            onDraftChange(textFieldValue.text, selectedImageUris, isForwardToDynamic)
        }
    }

    fun updateTextFieldValue(nextValue: TextFieldValue) {
        if (nextValue.text.length > 1000) return
        textFieldValue = nextValue
        onDraftChange(nextValue.text, selectedImageUris, isForwardToDynamic)
        val mentionQuery = resolveActiveCommentMentionQuery(
            text = nextValue.text,
            cursor = nextValue.selection.end
        )
        if (mentionQuery != null) {
            showMentionPanel = true
            showEmojiPanel = false
            mentionSearchText = mentionQuery.query
            onMentionSearchQueryChange(mentionQuery.query)
        } else {
            showMentionPanel = false
        }
    }

    fun insertTextAtCursor(insertText: String) {
        val cursor = textFieldValue.selection.end.coerceIn(0, textFieldValue.text.length)
        val nextText = textFieldValue.text.replaceRange(cursor, cursor, insertText)
        val nextCursor = cursor + insertText.length
        updateTextFieldValue(TextFieldValue(nextText, TextRange(nextCursor)))
    }

    suspend fun requestInputFocusWithRetry() {
        repeat(COMMENT_INPUT_FOCUS_RETRY_COUNT) { index ->
            delay(COMMENT_INPUT_FOCUS_RETRY_DELAY_MS + index * 40L)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    
    // 重置状态
    LaunchedEffect(visible) {
        if (visible) {
            // 草稿更新会随每次输入回流，不能作为 effect key，否则会持续覆盖 IME 选区。
            textFieldValue = commentDraftTextFieldValue(initialText)
            isForwardToDynamic = initialSyncToDynamic
            showEmojiPanel = false
            showMentionPanel = false
            mentionSearchText = ""
            selectedImageUris = initialImageUris
        }
    }
    
    // 监听 emoji 面板开关，控制键盘
    LaunchedEffect(showEmojiPanel, visible, canInputComment) {
        if (!canInputComment) return@LaunchedEffect
        if (showEmojiPanel) {
            keyboardController?.hide()
        } else if (shouldAutoShowCommentKeyboard(visible, canInputComment, showEmojiPanel)) {
            requestInputFocusWithRetry()
        }
    }

    DisposableEffect(visible) {
        onDispose {
            if (visible) {
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
            }
        }
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it }
    ) {
        Dialog(
            onDismissRequest = dismissDialog,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false, // 允许全宽
                decorFitsSystemWindows = false   // 沉浸式：内容延伸到状态栏/导航栏下
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(), // 避让软键盘
                verticalArrangement = Arrangement.Bottom // 底部对齐
            ) {
                // 点击上半部分空白区域关闭
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = dismissDialog
                        )
                )
                
                // 输入区域
                AppSurface(
                    modifier = modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = AppShapes.container(ContainerLevel.Sheet),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(layoutPolicy.sheetHorizontalPaddingDp.dp)
                            .navigationBarsPadding() // 避让底部导航栏(手势条)
                    ) {
                        // 1. 顶部：输入框
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    min = layoutPolicy.inputBoxMinHeightDp.dp,
                                    max = layoutPolicy.inputBoxMaxHeightDp.dp
                                )
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), AppShapes.container(ContainerLevel.Chip))
                                .padding(12.dp)
                        ) {
                            BasicTextField(
                                value = textFieldValue,
                                onValueChange = ::updateTextFieldValue,
                                enabled = canInputComment && !isSending,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight() // 填满 Box
                                    .focusRequester(focusRequester),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        if (text.isEmpty()) {
                                            val fallbackHint = "进来唠会嗑呗~"
                                            val resolvedHint = inputHint.ifBlank { fallbackHint }
                                            AppText(
                                                text = if (replyToName != null) "回复 @$replyToName: $resolvedHint" else resolvedHint,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            
                            // 右上角全屏图标 (装饰)
                            AppIcon(
                                imageVector = Icons.Filled.Fullscreen,
                                contentDescription = "Expand",
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(16.dp)
                                    .alpha(0.5f),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(
                            visible = showMentionPanel && canInputComment,
                            enter = verticalContentRevealEnterTransition(resolveCommentVerticalContentRevealMotionSpec()),
                            exit = verticalContentRevealExitTransition(resolveCommentVerticalContentRevealMotionSpec())
                        ) {
                            CommentMentionSearchPanel(
                                query = mentionSearchText,
                                onQueryChange = { query ->
                                    mentionSearchText = query
                                    onMentionSearchQueryChange(query)
                                },
                                users = mentionUsers,
                                isLoading = isMentionSearching,
                                errorMessage = mentionSearchError,
                                onUserClick = { user ->
                                    val cursor = textFieldValue.selection.end
                                    val (nextText, nextSelection) = insertCommentMentionText(
                                        text = textFieldValue.text,
                                        cursor = cursor,
                                        mentionName = user.name
                                    )
                                    textFieldValue = TextFieldValue(nextText, nextSelection)
                                    showMentionPanel = false
                                    mentionSearchText = ""
                                },
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        if (selectedImageUris.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppText(
                                    text = "已选 ${selectedImageUris.size}/9 张",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(selectedImageUris, key = { it.toString() }) { uri ->
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(AppShapes.container(ContainerLevel.Chip))
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant,
                                                shape = AppShapes.container(ContainerLevel.Chip)
                                            )
                                    ) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = "已选图片",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        AppSurface(
                                            shape = AppShapes.container(ContainerLevel.Pill),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(3.dp)
                                                .clickable {
                                                    selectedImageUris = selectedImageUris.filterNot { it == uri }
                                                    onDraftChange(
                                                        textFieldValue.text,
                                                        selectedImageUris,
                                                        isForwardToDynamic
                                                    )
                                                }
                                        ) {
                                            AppIcon(
                                                imageVector = Icons.Outlined.Close,
                                                contentDescription = "移除",
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .padding(1.dp),
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // 2. 底部工具栏
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(layoutPolicy.toolbarToolSpacingDp.dp)
                            ) {
                                // 转发到动态
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(AppShapes.container(ContainerLevel.Tag))
                                        .clickable(enabled = canInputComment && !isSending) {
                                            isForwardToDynamic = !isForwardToDynamic
                                            onDraftChange(
                                                textFieldValue.text,
                                                selectedImageUris,
                                                isForwardToDynamic
                                            )
                                        }
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    // 模拟 RadioButton/Checkbox
                                    AppIcon(
                                        imageVector = if (isForwardToDynamic) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (isForwardToDynamic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    AppText(
                                        text = "转发到动态",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }

                                // 图标栏: 表情 @ 图片
                                AppIconButton(
                                    onClick = { showEmojiPanel = !showEmojiPanel },
                                    enabled = canInputComment && !isSending,
                                    modifier = Modifier.size(layoutPolicy.toolbarToolButtonSizeDp.dp)
                                ) {
                                    AppIcon(
                                        imageVector = Icons.Filled.Face,
                                        contentDescription = "Emoji",
                                        tint = if (showEmojiPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                AppIconButton(
                                    onClick = {
                                        insertTextAtCursor("@")
                                        showEmojiPanel = false
                                        showMentionPanel = true
                                        mentionSearchText = ""
                                        onMentionSearchQueryChange("")
                                    },
                                    enabled = canInputComment && !isSending,
                                    modifier = Modifier.size(layoutPolicy.toolbarToolButtonSizeDp.dp)
                                ) {
                                    AppIcon(
                                        imageVector = Icons.Filled.Email,
                                        contentDescription = "At",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                AppTextButton(
                                    onClick = {
                                        insertTextAtCursor(resolveCommentProgressInsertText(currentVideoPositionMsProvider()))
                                        showEmojiPanel = false
                                        showMentionPanel = false
                                    },
                                    enabled = canInputComment && !isSending,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    AppText(
                                        text = "进度",
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1
                                    )
                                }

                                AppIconButton(
                                    onClick = {
                                        imagePickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    enabled = canInputComment && !isSending,
                                    modifier = Modifier.size(layoutPolicy.toolbarToolButtonSizeDp.dp)
                                ) {
                                    AppIcon(
                                        imageVector = Icons.Outlined.Image,
                                        contentDescription = "图片",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // 发送按钮
                            AppButton(
                                onClick = {
                                    if (canPublish) {
                                        keyboardController?.hide()
                                        focusManager.clearFocus(force = true)
                                        android.util.Log.d("CommentInputDialog", "📤 Sending comment: $text")
                                        onSend(text.trim(), selectedImageUris, isForwardToDynamic)
                                    }
                                },
                                enabled = canPublish,
                                shape = AppShapes.container(ContainerLevel.Floating),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = resolveFilledButtonContainerColor(MaterialTheme.colorScheme),

                                    contentColor = resolveFilledButtonContentColor(MaterialTheme.colorScheme), // 应该是粉色
                                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                ),
                                contentPadding = PaddingValues(horizontal = layoutPolicy.sendButtonHorizontalPaddingDp.dp, vertical = 0.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                if (isSending) {
                                    AppCircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    AppText(
                                        text = "发布",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (!canInputComment) {
                            AppText(
                                text = "当前评论区暂不可评论",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        
                        // 3. 表情面板區域
                        val emojiPanelRevealMotion = remember {
                            resolveCommentVerticalContentRevealMotionSpec()
                        }
                        AnimatedVisibility(
                            visible = showEmojiPanel,
                            enter = verticalContentRevealEnterTransition(emojiPanelRevealMotion),
                            exit = verticalContentRevealExitTransition(emojiPanelRevealMotion)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(layoutPolicy.emojiPanelHeightDp.dp)
                                    .padding(top = 8.dp)
                            ) {
                                // 顶部标签栏 (可滚动)
                                AppThemeAdaptiveTabRow(
                                    options = buildList {
                                        add(AppSegmentOption(0, "颜文字"))
                                        add(AppSegmentOption(1, "Emoji"))
                                        emotePackages.forEachIndexed { index, pkg ->
                                            add(AppSegmentOption(index + 2, pkg.text))
                                        }
                                    },
                                    selectedValue = currentTab,
                                    onSelectionChange = { currentTab = it },
                                    height = 48.dp,
                                    scrollable = true,
                                )

                                // 内容区域
                                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)) {
                                    when (currentTab) {
                                        0 -> { // 颜文字
                                            val kaomojis = listOf(
                                                "(⌒▽⌒)", "（￣▽￣）", "(=・ω・=)", "(｀・ω・´)", 
                                                "(〜￣△￣)〜", "(･∀･)", "(°∀°)ﾉ", "(￣3￣)", 
                                                "╮(￣▽￣)╭", "( ´_ゝ｀)", "_(:3」∠)_", "(;¬_¬)",
                                                "(ﾟДﾟ≡ﾟДﾟ)", "(ノ=Д=)ノ┻━┻", "Σ( ￣□￣||)", "(´；ω；`)",
                                                "（/TДT)/", "(^・ω・^ )", "(●￣(ｴ)￣●)", "ε=ε=(ノ≧∇≦)ノ",
                                                "( >﹏<。)", "( *・ω・)✄╰ひ╯", "(╬￣皿￣)凸", "⊙__⊙"
                                            )
                                            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(80.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(kaomojis.size, key = { i -> kaomojis[i] }) { i ->
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier
                                                            .height(48.dp)
                                                            .appDesktopFocusableItemVisuals()
                                                            .clickable(role = Role.Button) {
                                                                insertTextAtCursor(kaomojis[i])
                                                            },
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(36.dp)
                                                                .clip(AppShapes.container(ContainerLevel.Tag))
                                                                .background(
                                                                    opaqueCompositeOver(
                                                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f),
                                                                        MaterialTheme.colorScheme.surface,
                                                                    )
                                                                ),
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            AppText(kaomojis[i], style = MaterialTheme.typography.bodySmall)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        1 -> { // Emoji
                                            val emojis = listOf(
                                                "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣",
                                                "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰",
                                                "😘", "😗", "😙", "😚", "😋", "😛", "😝", "😜",
                                                "🤪", "🤨", "🧐", "🤓", "😎", "🤩", "🥳", "😏",
                                                "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣",
                                                "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼"
                                            )
                                            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(48.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(emojis.size, key = { i -> emojis[i] }) { i ->
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .appDesktopFocusableItemVisuals()
                                                            .clickable(role = Role.Button) {
                                                                insertTextAtCursor(emojis[i])
                                                            }
                                                    ) {
                                                        AppText(emojis[i], style = MaterialTheme.typography.headlineSmall)
                                                    }
                                                }
                                            }
                                        }
                                        else -> { // API Package
                                            val pkgIndex = currentTab - 2
                                            if (pkgIndex < emotePackages.size) {
                                                val pkg = emotePackages[pkgIndex]
                                                val emotes = pkg.emote ?: emptyList()
                                                
                                                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                                    columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(60.dp),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    items(emotes.size, key = { i -> emotes[i].id }) { i ->
                                                        val emote = emotes[i]
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            modifier = Modifier.clickable { insertTextAtCursor(emote.text) }
                                                        ) {
                                                            AsyncImage(
                                                                model = emote.url,
                                                                contentDescription = emote.text,
                                                                modifier = Modifier.size(50.dp)
                                                            )
                                                            AppText(
                                                                text = emote.text.replace("[", "").replace("]", ""),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentMentionSearchPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    users: List<MentionSearchUser>,
    isLoading: Boolean,
    errorMessage: String?,
    onUserClick: (MentionSearchUser) -> Unit,
    modifier: Modifier = Modifier
) {
    AppSurface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp),
        shape = AppShapes.container(ContainerLevel.Chip),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            AppOutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                singleLine = true,
                leadingIcon = {
                    AppIcon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                placeholder = { AppText("搜索好友昵称") },
                textStyle = MaterialTheme.typography.bodySmall,
                shape = AppShapes.container(ContainerLevel.Card),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.56f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            when {
                isLoading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppCircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        AppText(
                            text = "正在搜索好友",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                errorMessage != null -> {
                    AppText(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }

                users.isEmpty() -> {
                    AppText(
                        text = if (query.isBlank()) "输入好友昵称搜索" else "没有找到匹配的用户",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        items(users, key = { it.uid }) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUserClick(user) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = user.face,
                                    contentDescription = user.name,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(AppShapes.container(ContainerLevel.Card))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    AppText(
                                        text = user.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    AppText(
                                        text = "${FormatUtils.formatStat(user.fans.toLong())} 粉丝",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
