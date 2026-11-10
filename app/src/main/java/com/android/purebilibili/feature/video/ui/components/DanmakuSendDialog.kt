// 文件路径: feature/video/ui/components/DanmakuSendDialog.kt
package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.core.ui.AppContentColors

import com.android.purebilibili.core.ui.components.AppColorPicker
import androidx.compose.ui.graphics.toArgb
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppCheckbox
import com.android.purebilibili.core.ui.components.AppCircularProgressIndicator
import com.android.purebilibili.core.ui.components.AppFilterChip
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppTextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import kotlinx.coroutines.delay
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel

internal data class DanmakuSendDialogLayoutPolicy(
    val fillMaxWidthFraction: Float,
    val bottomAligned: Boolean,
    val bottomLiftDp: Int
)

internal data class DanmakuSendSelectionState(
    val color: Int,
    val mode: Int,
    val fontSize: Int
)

internal fun resolveDanmakuSendDialogLayoutPolicy(): DanmakuSendDialogLayoutPolicy {
    return DanmakuSendDialogLayoutPolicy(
        fillMaxWidthFraction = 1f,
        bottomAligned = true,
        bottomLiftDp = 14
    )
}

internal fun resolveDanmakuDialogBottomLiftDp(
    defaultBottomLiftDp: Int,
    imeBottomPx: Int
): Int {
    return if (imeBottomPx > 0) 0 else defaultBottomLiftDp
}

internal fun resolveDanmakuSendSelectionState(
    initialColor: Int,
    initialMode: Int,
    initialFontSize: Int,
    colorOptions: List<Int>,
    modeOptions: List<Int>,
    fontSizeOptions: List<Int>
): DanmakuSendSelectionState {
    val fallbackColor = 16777215.takeIf { it in colorOptions } ?: colorOptions.firstOrNull() ?: 16777215
    val fallbackMode = 1.takeIf { it in modeOptions } ?: modeOptions.firstOrNull() ?: 1
    val fallbackFontSize = 25.takeIf { it in fontSizeOptions } ?: fontSizeOptions.firstOrNull() ?: 25
    return DanmakuSendSelectionState(
        color = initialColor.takeIf {
            it in colorOptions || it in 0..0xFFFFFF
        } ?: fallbackColor,
        mode = initialMode.takeIf { it in modeOptions } ?: fallbackMode,
        fontSize = initialFontSize.takeIf { it in fontSizeOptions } ?: fallbackFontSize
    )
}

/**
 * 弹幕发送对话框
 *
 * 提供弹幕输入、颜色选择、位置/大小设置功能
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DanmakuSendDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSend: (message: String, color: Int, mode: Int, fontSize: Int, attentionCommand: Boolean) -> Unit,
    isSending: Boolean = false,
    initialColor: Int = 16777215,
    initialMode: Int = 1,
    initialFontSize: Int = 25,
    initialText: String = "",
    initialAttentionCommand: Boolean = false,
    attentionCommandAvailable: Boolean = true,
    onDraftChange: (String, Boolean) -> Unit = { _, _ -> },
    onSelectionChange: (color: Int, mode: Int, fontSize: Int) -> Unit = { _, _, _ -> },
    topReservedSpace: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    var closeRequested by remember { mutableStateOf(false) }
    var nativeClosing by remember { mutableStateOf(false) }
    val latestVisible by rememberUpdatedState(visible)
    val latestDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(visible) {
        closeRequested = false
        nativeClosing = false
    }
    val inputActive = visible && !closeRequested
    com.android.purebilibili.core.ui.AppModalBottomSheet(
        visible = inputActive,
        onDismissStarted = { nativeClosing = true },
        onDismissRequest = { if (latestVisible) latestDismiss() },
        modifier = modifier.widthIn(max = 450.dp),
        presentationOverride = com.android.purebilibili.core.ui.AppModalPresentation.BottomSheet,
        sheetSurfaceModifier = Modifier.imePadding().widthIn(max = 450.dp),
    ) {
        DanmakuSendDialogContent(
            visible = inputActive && !nativeClosing,
            onDismiss = { closeRequested = true },
            onSend = { message, color, mode, size, attention ->
                if (inputActive && !nativeClosing) onSend(message, color, mode, size, attention)
            },
            isSending = isSending,
            initialColor = initialColor,
            initialMode = initialMode,
            initialFontSize = initialFontSize,
            initialText = initialText,
            initialAttentionCommand = initialAttentionCommand,
            attentionCommandAvailable = attentionCommandAvailable,
            onDraftChange = onDraftChange,
            onSelectionChange = onSelectionChange,
            topReservedSpace = topReservedSpace,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun DanmakuSendDialogContent(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSend: (message: String, color: Int, mode: Int, fontSize: Int, attentionCommand: Boolean) -> Unit,
    isSending: Boolean = false,
    initialColor: Int = 16777215,
    initialMode: Int = 1,
    initialFontSize: Int = 25,
    initialText: String = "",
    initialAttentionCommand: Boolean = false,
    attentionCommandAvailable: Boolean = true,
    onDraftChange: (String, Boolean) -> Unit = { _, _ -> },
    onSelectionChange: (color: Int, mode: Int, fontSize: Int) -> Unit = { _, _, _ -> },
    topReservedSpace: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val colorOptions = remember { danmakuSendColorOptions().map { it.value to it.label } }
    val modeOptions = remember { danmakuSendModeOptions().map { it.value to it.label } }
    val fontSizeOptions = remember { danmakuSendFontSizeOptions().map { it.value to it.label } }

    // 状态
    var text by rememberSaveable { mutableStateOf(initialText) }
    var selectedColor by rememberSaveable { mutableIntStateOf(initialColor) }
    var selectedMode by rememberSaveable { mutableIntStateOf(initialMode) }
    var selectedFontSize by rememberSaveable { mutableIntStateOf(initialFontSize) }
    var attentionCommandChecked by rememberSaveable { mutableStateOf(initialAttentionCommand) }
    var showSettings by remember { mutableStateOf(false) }
    var showCustomColorPicker by remember { mutableStateOf(false) }
    var lastCustomColor by remember { mutableIntStateOf(0x66CCFF) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    androidx.compose.runtime.DisposableEffect(keyboardController, focusManager) {
        onDispose {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    // 重置状态
    var inputWasVisible by rememberSaveable { mutableStateOf(false) }
    // 草稿和样式会随输入回流，只在开始新会话时初始化，避免反复重置焦点与设置面板。
    LaunchedEffect(visible, showSettings, showCustomColorPicker) {
        if (!visible) {
            inputWasVisible = false
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            return@LaunchedEffect
        }
        if (!inputWasVisible) {
            inputWasVisible = true
            val selection = resolveDanmakuSendSelectionState(
                initialColor = initialColor,
                initialMode = initialMode,
                initialFontSize = initialFontSize,
                colorOptions = colorOptions.map { it.first },
                modeOptions = modeOptions.map { it.first },
                fontSizeOptions = fontSizeOptions.map { it.first }
            )
            text = initialText
            selectedColor = selection.color
            selectedMode = selection.mode
            selectedFontSize = selection.fontSize
            attentionCommandChecked = initialAttentionCommand
            showSettings = false
        }
        if (showCustomColorPicker) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            return@LaunchedEffect
        }
        delay(100)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    LaunchedEffect(selectedColor, selectedMode, selectedFontSize, visible) {
        if (!visible) return@LaunchedEffect
        onSelectionChange(selectedColor, selectedMode, selectedFontSize)
    }



    Column(
        modifier = modifier
            .heightIn(max = (LocalConfiguration.current.screenHeightDp.dp - topReservedSpace).coerceAtLeast(1.dp))
            .padding(20.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 输入和选项按剩余高度滚动；发送按钮独立固定，键盘展开后仍可操作。
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(
                    text = "发送弹幕",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = AppContentColors.primaryText()
                )

                AppIconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    AppIcon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "关闭",
                        tint = AppContentColors.secondaryText(),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 输入框
            com.android.purebilibili.core.ui.components.AppOutlinedTextField(
                value = text,
                onValueChange = {
                    if (it.length <= 100) {
                        text = it
                        onDraftChange(it, attentionCommandChecked)
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("danmaku_compact_input"),
                focusRequester = focusRequester,
                enabled = visible && !isSending,
                placeholderText = "发个友善的弹幕见证当下",
                singleLine = true,
                trailingIcon = {
                    if (text.isNotBlank() && !isSending) {
                        AppIconButton(onClick = {
                            text = ""
                            onDraftChange("", attentionCommandChecked)
                        }) { AppIcon(Icons.Outlined.Close, contentDescription = "清空") }
                    }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = {
                    if (visible && text.isNotBlank() && !isSending) {
                        onSend(text.trim(), selectedColor, selectedMode, selectedFontSize, attentionCommandChecked)
                    }
                }),
            )

            // 字数统计
            AppText(
                text = "${text.length}/100",
                style = MaterialTheme.typography.labelSmall,
                color = if (text.length > 90) {
                    AppContentColors.error()
                } else {
                    AppContentColors.secondaryText()
                },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )

            AppTextButton(
                onClick = { showSettings = !showSettings },
                modifier = Modifier.align(Alignment.End)
            ) {
                AppText(if (showSettings) "收起弹幕设置" else "颜色、位置与大小")
            }

            if (showSettings) {
            // 颜色选择
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppText(
                    text = "颜色",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                    color = AppContentColors.primaryText()
                )

                DanmakuColorOptions(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it },
                    onCustomColor = { showCustomColorPicker = true },
                )
            }

            if (attentionCommandAvailable) Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppShapes.container(ContainerLevel.Card))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
                    .clickable {
                        attentionCommandChecked = !attentionCommandChecked
                        onDraftChange(text, attentionCommandChecked)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppCheckbox(
                    checked = attentionCommandChecked,
                    onCheckedChange = {
                        attentionCommandChecked = it
                        onDraftChange(text, it)
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    AppText(
                        text = "内嵌关注按钮",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = AppContentColors.primaryText()
                    )
                    AppText(
                        text = "发送一个视频内嵌关注按钮",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppContentColors.secondaryText()
                    )
                }
            }

            // 位置和大小选择 - 垂直布局避免拥挤
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 位置选择
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppText(
                        text = "位置",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                        color = AppContentColors.primaryText()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        modeOptions.forEach { (modeValue, label) ->
                            val isSelected = selectedMode == modeValue
                            AppFilterChip(
                                selected = isSelected,
                                onClick = { selectedMode = modeValue },
                                label = {
                                    AppText(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }
                }

                // 大小选择
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppText(
                        text = "大小",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                        color = AppContentColors.primaryText()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        fontSizeOptions.forEach { (sizeValue, label) ->
                            val isSelected = selectedFontSize == sizeValue
                            AppFilterChip(
                                selected = isSelected,
                                onClick = { selectedFontSize = sizeValue },
                                label = {
                                    AppText(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }
                }
            }
            }

        }

        // 发送按钮
        AppButton(
            onClick = {
                if (visible && text.isNotBlank() && !isSending) {
                    onSend(
                        text.trim(),
                        selectedColor,
                        selectedMode,
                        selectedFontSize,
                        attentionCommandChecked && attentionCommandAvailable
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("danmaku_compact_send"),
            enabled = visible && text.isNotBlank() && !isSending,
            // 玻璃面板的主操作读取语义主题色，禁用态保持中性；原生按钮继续使用各自默认色板。
            colors = if (com.android.purebilibili.core.ui.LocalAppThemeConfig.current.liquidGlassEnabled) {
                androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = AppContentColors.accent(),
                    contentColor = AppContentColors.onAccent(),
                    disabledContainerColor = AppContentColors.primaryText().copy(alpha = 0.12f),
                    disabledContentColor = AppContentColors.primaryText().copy(alpha = 0.38f),
                )
            } else null,
            shape = AppShapes.container(ContainerLevel.Card)
        ) {
            if (isSending) {
                AppCircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = androidx.compose.material3.LocalContentColor.current
                )
            } else {
                AppText(
                    text = "发送",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
    }

    if (visible && showCustomColorPicker) {
        DanmakuCustomColorPickerDialog(
            initialColor = lastCustomColor,
            onConfirm = { picked ->
                lastCustomColor = picked
                selectedColor = picked
                showCustomColorPicker = false
            },
            onDismiss = { showCustomColorPicker = false }
        )
    }
}

/**
 * 自定义弹幕颜色：Miuix HSV 取色器 / Material 原生滑杆，确认时保存 RGB。
 */
@Composable
internal fun DanmakuCustomColorPickerDialog(
    initialColor: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var pendingColor by remember(initialColor) { mutableStateOf(Color(initialColor or 0xFF000000.toInt())) }
    val pickerRgb = pendingColor.toArgb() and 0xFFFFFF

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("自定义弹幕颜色") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppColorPicker(
                    color = pendingColor,
                    onColorChanged = { pendingColor = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                AppText(
                    text = "#%06X".format(pickerRgb),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppContentColors.secondaryText()
                )
            }
        },
        confirmButton = {
            AppTextButton(onClick = { onConfirm(pickerRgb) }) {
                AppText("确定")
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) {
                AppText("取消")
            }
        }
    )
}
