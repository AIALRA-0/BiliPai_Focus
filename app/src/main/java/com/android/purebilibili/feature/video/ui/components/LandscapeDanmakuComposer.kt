package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.core.ui.resolveFilledButtonContainerColor
import com.android.purebilibili.core.ui.resolveFilledButtonContentColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.android.purebilibili.core.ui.components.AppButton
import androidx.compose.material3.ButtonDefaults
import com.android.purebilibili.core.ui.components.AppCheckbox
import com.android.purebilibili.core.ui.components.AppCircularProgressIndicator
import com.android.purebilibili.core.ui.components.AppFilterChip
import androidx.compose.material3.FilterChipDefaults
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Close
import kotlinx.coroutines.delay

@Composable
fun LandscapeDanmakuComposer(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSend: (message: String, color: Int, mode: Int, fontSize: Int, attentionCommand: Boolean) -> Unit,
    isSending: Boolean,
    initialColor: Int,
    initialMode: Int,
    initialFontSize: Int,
    initialText: String,
    initialAttentionCommand: Boolean,
    onDraftChange: (String, Boolean) -> Unit,
    onSelectionChange: (color: Int, mode: Int, fontSize: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorOptions = remember { danmakuSendColorOptions() }
    val modeOptions = remember { danmakuSendModeOptions() }
    val fontSizeOptions = remember { danmakuSendFontSizeOptions() }

    var text by remember { mutableStateOf(initialText) }
    var selectedColor by remember { mutableIntStateOf(initialColor) }
    var selectedMode by remember { mutableIntStateOf(initialMode) }
    var selectedFontSize by remember { mutableIntStateOf(initialFontSize) }
    var attentionCommandChecked by remember { mutableStateOf(initialAttentionCommand) }
    var showStylePanel by remember { mutableStateOf(false) }
    var showAdvancedOptions by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(
        visible,
        initialColor,
        initialMode,
        initialFontSize,
        initialText,
        initialAttentionCommand
    ) {
        if (!visible) return@LaunchedEffect
        val selection = resolveDanmakuSendSelectionState(
            initialColor = initialColor,
            initialMode = initialMode,
            initialFontSize = initialFontSize,
            colorOptions = colorOptions.map { it.value },
            modeOptions = modeOptions.map { it.value },
            fontSizeOptions = fontSizeOptions.map { it.value }
        )
        text = initialText
        selectedColor = selection.color
        selectedMode = selection.mode
        selectedFontSize = selection.fontSize
        attentionCommandChecked = initialAttentionCommand
        showStylePanel = false
        showAdvancedOptions = false
        delay(80)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    LaunchedEffect(selectedColor, selectedMode, selectedFontSize, visible) {
        if (!visible) return@LaunchedEffect
        onSelectionChange(selectedColor, selectedMode, selectedFontSize)
    }

    com.android.purebilibili.core.ui.LocalNavigationBackHandler(enabled = visible) {
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
        modifier = modifier
    ) {
        AppSurface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
            color = Color.Black.copy(alpha = 0.88f),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedVisibility(
                    visible = showStylePanel,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            colorOptions.forEach { option ->
                                val isSelected = selectedColor == option.value
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .then(
                                            if (option.value == DANMAKU_SEND_VIP_GRADUAL_COLOR) {
                                                Modifier.background(
                                                    Brush.linearGradient(
                                                        colors = listOf(
                                                            Color(0xFFDD94DA),
                                                            Color(0xFF72B2EA)
                                                        )
                                                    )
                                                )
                                            } else {
                                                Modifier.background(Color(option.value or 0xFF000000.toInt()))
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
                                        .clickable { selectedColor = option.value }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            modeOptions.forEach { option ->
                                AppFilterChip(
                                    selected = selectedMode == option.value,
                                    onClick = { selectedMode = option.value },
                                    label = { AppText(option.label, style = MaterialTheme.typography.labelMedium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            fontSizeOptions.forEach { option ->
                                AppFilterChip(
                                    selected = selectedFontSize == option.value,
                                    onClick = { selectedFontSize = option.value },
                                    label = { AppText(option.label, style = MaterialTheme.typography.labelMedium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        if (showAdvancedOptions) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AppShapes.container(ContainerLevel.Field))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable {
                                        attentionCommandChecked = !attentionCommandChecked
                                        onDraftChange(text, attentionCommandChecked)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppCheckbox(
                                    checked = attentionCommandChecked,
                                    onCheckedChange = {
                                        attentionCommandChecked = it
                                        onDraftChange(text, it)
                                    }
                                )
                                Column {
                                    AppText("内嵌关注按钮", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                    AppText(
                                        "发送视频内嵌关注按钮",
                                        color = Color.White.copy(alpha = 0.68f),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        } else {
                            AppTextButton(
                                onClick = { showAdvancedOptions = true },
                                contentPadding = PaddingValues(horizontal = 0.dp)
                            ) {
                                AppText("更多发送选项", color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppIconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        AppIcon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "关闭",
                            tint = Color.White.copy(alpha = 0.82f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(AppShapes.container(ContainerLevel.Pill))
                            .background(Color.White.copy(alpha = 0.14f))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = text,
                            onValueChange = {
                                if (it.length <= 100) {
                                    text = it
                                    onDraftChange(it, attentionCommandChecked)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("danmaku_landscape_composer_input")
                                .focusRequester(focusRequester),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (text.isNotBlank() && !isSending) {
                                        onSend(
                                            text.trim(),
                                            selectedColor,
                                            selectedMode,
                                            selectedFontSize,
                                            attentionCommandChecked
                                        )
                                    }
                                }
                            ),
                            decorationBox = { innerTextField ->
                                if (text.isEmpty()) {
                                    AppText(
                                        text = "发个友善的弹幕见证当下",
                                        color = Color.White.copy(alpha = 0.55f),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    AppIconButton(
                        onClick = { showStylePanel = !showStylePanel }
                    ) {
                        AppIcon(
                            imageVector = Icons.Outlined.Palette,
                            contentDescription = "弹幕样式",
                            tint = if (showStylePanel) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.White.copy(alpha = 0.86f)
                            }
                        )
                    }

                    AppButton(
                        onClick = {
                            if (text.isNotBlank() && !isSending) {
                                onSend(
                                    text.trim(),
                                    selectedColor,
                                    selectedMode,
                                    selectedFontSize,
                                    attentionCommandChecked
                                )
                            }
                        },
                        enabled = text.isNotBlank() && !isSending,
                        modifier = Modifier
                            .height(48.dp)
                            .width(72.dp),
                        shape = AppShapes.container(ContainerLevel.Pill),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = resolveFilledButtonContainerColor(MaterialTheme.colorScheme),

                            contentColor = resolveFilledButtonContentColor(MaterialTheme.colorScheme),
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        )
                    ) {
                        if (isSending) {
                            AppCircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            AppText(
                                text = "发送",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppText(
                        text = if (showStylePanel) "样式仅作用于本条弹幕" else "回车或点发送即可提交",
                        color = Color.White.copy(alpha = 0.62f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    AppText(
                        text = "${text.length}/100",
                        color = if (text.length > 90) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.92f)
                        } else {
                            Color.White.copy(alpha = 0.62f)
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
