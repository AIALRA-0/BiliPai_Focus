// 文件路径: feature/video/ui/components/DanmakuSendDialog.kt
package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Xmark
import kotlinx.coroutines.delay

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

internal const val DANMAKU_SEND_VIP_GRADUAL_COLOR = -1

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
        color = initialColor.takeIf { it in colorOptions } ?: fallbackColor,
        mode = initialMode.takeIf { it in modeOptions } ?: fallbackMode,
        fontSize = initialFontSize.takeIf { it in fontSizeOptions } ?: fallbackFontSize
    )
}

/**
 * 弹幕发送对话框
 *
 * 提供弹幕输入、颜色选择、位置/大小设置功能
 */
@Composable
fun DanmakuSendDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSend: (message: String, color: Int, mode: Int, fontSize: Int, encourage: Boolean) -> Unit,
    isSending: Boolean = false,
    initialColor: Int = 16777215,
    initialMode: Int = 1,
    initialFontSize: Int = 25,
    onSelectionChange: (color: Int, mode: Int, fontSize: Int) -> Unit = { _, _, _ -> },
    topReservedSpace: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val layoutPolicy = remember { resolveDanmakuSendDialogLayoutPolicy() }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val effectiveBottomLiftDp = resolveDanmakuDialogBottomLiftDp(
        defaultBottomLiftDp = layoutPolicy.bottomLiftDp,
        imeBottomPx = imeBottomPx
    )
    val maxSheetHeight = remember(configuration.screenHeightDp, topReservedSpace) {
        (configuration.screenHeightDp.dp - topReservedSpace).coerceAtLeast(1.dp)
    }

    // 弹幕预设颜色 (十进制 RGB)
    val colorOptions = listOf(
        16777215 to "白色",  // 0xFFFFFF
        16646914 to "红色",  // 0xFE0302
        16740868 to "橙色",  // 0xFF7204
        16755202 to "金色",  // 0xFFAA02
        52224 to "绿色",     // 0x00CD00
        41430 to "蓝色",     // 0x00A1D6
        13369971 to "紫色",  // 0xCC0273
        2236962 to "黑色",   // 0x222222
        DANMAKU_SEND_VIP_GRADUAL_COLOR to "会员渐变"
    )

    // 弹幕位置模式
    val modeOptions = listOf(
        1 to "滚动",
        5 to "顶部",
        4 to "底部"
    )

    // 弹幕字号
    val fontSizeOptions = listOf(
        18 to "小",
        25 to "中",
        36 to "大"
    )

    // 状态
    var text by remember { mutableStateOf("") }
    var selectedColor by remember { mutableIntStateOf(initialColor) }
    var selectedMode by remember { mutableIntStateOf(initialMode) }
    var selectedFontSize by remember { mutableIntStateOf(initialFontSize) }
    var encourageChecked by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // 重置状态
    LaunchedEffect(visible, initialColor, initialMode, initialFontSize) {
        if (visible) {
            val selection = resolveDanmakuSendSelectionState(
                initialColor = initialColor,
                initialMode = initialMode,
                initialFontSize = initialFontSize,
                colorOptions = colorOptions.map { it.first },
                modeOptions = modeOptions.map { it.first },
                fontSizeOptions = fontSizeOptions.map { it.first }
            )
            text = ""
            selectedColor = selection.color
            selectedMode = selection.mode
            selectedFontSize = selection.fontSize
            encourageChecked = false
            delay(100)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(selectedColor, selectedMode, selectedFontSize, visible) {
        if (!visible) return@LaunchedEffect
        onSelectionChange(selectedColor, selectedMode, selectedFontSize)
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 })
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topReservedSpace)
                    .padding(bottom = effectiveBottomLiftDp.dp)
                    .imePadding(),
                verticalArrangement = if (layoutPolicy.bottomAligned) Arrangement.Bottom else Arrangement.Center
            ) {
                if (layoutPolicy.bottomAligned) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = onDismiss
                            )
                    )
                }

                Surface(
                    modifier = modifier
                        .fillMaxWidth(layoutPolicy.fillMaxWidthFraction)
                        .heightIn(max = maxSheetHeight)
                        .wrapContentHeight(),
                    shape = if (layoutPolicy.bottomAligned) {
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    } else {
                        RoundedCornerShape(20.dp)
                    },
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 标题栏
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "发送弹幕",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = CupertinoIcons.Outlined.Xmark,
                                    contentDescription = "关闭",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // 输入框
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = text,
                                onValueChange = { if (it.length <= 100) text = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (text.isEmpty()) {
                                            Text(
                                                text = "发个友善的弹幕见证当下",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 15.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }

                        // 字数统计
                        Text(
                            text = "${text.length}/100",
                            fontSize = 12.sp,
                            color = if (text.length > 90) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )

                        // 颜色选择
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "颜色",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                colorOptions.forEach { (colorValue, _) ->
                                    val isSelected = selectedColor == colorValue
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .then(
                                                if (colorValue == DANMAKU_SEND_VIP_GRADUAL_COLOR) {
                                                    Modifier.background(
                                                        Brush.linearGradient(
                                                            colors = listOf(
                                                                Color(0xFFDD94DA),
                                                                Color(0xFF72B2EA)
                                                            )
                                                        )
                                                    )
                                                } else {
                                                    Modifier.background(Color(colorValue or 0xFF000000.toInt()))
                                                }
                                            )
                                            .then(
                                                if (isSelected) {
                                                    Modifier.border(
                                                        width = 2.dp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        shape = CircleShape
                                                    )
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable { selectedColor = colorValue }
                                    ) {
                                        if (colorValue == DANMAKU_SEND_VIP_GRADUAL_COLOR) {
                                            Text(
                                                text = "VIP",
                                                modifier = Modifier.align(Alignment.Center),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
                                .clickable { encourageChecked = !encourageChecked }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = encourageChecked,
                                onCheckedChange = { encourageChecked = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "关注弹幕",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "发送时附带 B 站关注/鼓励弹幕标识",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                Text(
                                    text = "位置",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    modeOptions.forEach { (modeValue, label) ->
                                        val isSelected = selectedMode == modeValue
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedMode = modeValue },
                                            label = {
                                                Text(
                                                    text = label,
                                                    fontSize = 12.sp,
                                                    maxLines = 1
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                }
                            }

                            // 大小选择
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "大小",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    fontSizeOptions.forEach { (sizeValue, label) ->
                                        val isSelected = selectedFontSize == sizeValue
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedFontSize = sizeValue },
                                            label = {
                                                Text(
                                                    text = label,
                                                    fontSize = 12.sp,
                                                    maxLines = 1
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // 发送按钮
                        Button(
                            onClick = {
                                if (text.isNotBlank() && !isSending) {
                                    onSend(
                                        text.trim(),
                                        selectedColor,
                                        selectedMode,
                                        selectedFontSize,
                                        encourageChecked
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = text.isNotBlank() && !isSending,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    text = "发送",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
