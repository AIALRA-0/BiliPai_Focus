// File: feature/video/ui/components/DanmakuSettingsPanel.kt
package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.core.store.DanmakuPanelWidthMode
import com.android.purebilibili.core.store.DanmakuSettingsScope
import com.android.purebilibili.feature.video.danmaku.DanmakuBlockRuleSections
import com.android.purebilibili.feature.video.danmaku.DanmakuCloudSyncStatus
import com.android.purebilibili.feature.video.danmaku.DanmakuCloudSyncUiState
import com.android.purebilibili.feature.video.danmaku.FaceOcclusionModuleState
import com.android.purebilibili.feature.video.danmaku.mergeDanmakuBlockRuleSections
import com.android.purebilibili.feature.video.danmaku.parseDanmakuBlockRules
import com.android.purebilibili.feature.video.danmaku.partitionDanmakuBlockRules
import com.android.purebilibili.feature.video.danmaku.resolveFaceOcclusionModuleUiState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

// 主题色将在 Composable 内通过 MaterialTheme.colorScheme.primary 获取
private val PanelBackground = Color(0xFF1E1E1E)
private val CardBackground = Color(0xFF2A2A2A)
private const val FULLSCREEN_DANMAKU_PANEL_WIDTH_FRACTION = 0.25f
private const val FULLSCREEN_DANMAKU_PANEL_MIN_WIDTH_DP = 220
private const val WIDE_INLINE_DANMAKU_PANEL_MAX_WIDTH_DP = 640
private const val WIDE_INLINE_DANMAKU_PANEL_SCREEN_WIDTH_DP = 840

internal fun resolveDanmakuSyncStatusBadgeText(syncUiState: DanmakuCloudSyncUiState): String {
    return when (syncUiState.status) {
        DanmakuCloudSyncStatus.IDLE -> "未同步"
        DanmakuCloudSyncStatus.PENDING -> "待同步"
        DanmakuCloudSyncStatus.SYNCING -> "同步中"
        DanmakuCloudSyncStatus.SUCCESS -> "已同步"
        DanmakuCloudSyncStatus.FAILURE -> "同步失败"
    }
}

internal fun shouldShowDanmakuSyncRetry(status: DanmakuCloudSyncStatus): Boolean {
    return status == DanmakuCloudSyncStatus.FAILURE
}

internal fun resolveDanmakuBlockManagerSections(blockRulesRaw: String): DanmakuBlockRuleSections {
    return partitionDanmakuBlockRules(parseDanmakuBlockRules(blockRulesRaw))
}

internal fun persistDanmakuBlockManagerSections(sections: DanmakuBlockRuleSections): String {
    return mergeDanmakuBlockRuleSections(
        keywordRules = sections.keywordRules,
        regexRules = sections.regexRules,
        userHashRules = sections.userHashRules
    ).joinToString(separator = "\n")
}

internal fun resolveDanmakuBlockRuleCount(sections: DanmakuBlockRuleSections): Int {
    return sections.keywordRules.size + sections.regexRules.size + sections.userHashRules.size
}

internal fun resolveDanmakuBlockRuleBadgeText(count: Int): String {
    if (count <= 0) return "0"
    return if (count > 99) "99+" else count.toString()
}

internal fun resolveDanmakuBlockManagerTabLabel(label: String, count: Int): String {
    return if (count > 0) "$label $count" else label
}

enum class DanmakuSettingsPanelPresentation {
    CenteredDialog,
    BottomSheet
}

data class DanmakuSettingsPanelLayoutPolicy(
    val presentation: DanmakuSettingsPanelPresentation,
    val horizontalPaddingDp: Int,
    val bottomPaddingDp: Int,
    val minWidthDp: Int,
    val maxWidthDp: Int,
    val maxHeightDp: Int
)

fun resolveDanmakuSettingsPanelLayoutPolicy(
    isFullscreen: Boolean,
    screenWidthDp: Int,
    screenHeightDp: Int,
    fullscreenWidthMode: DanmakuPanelWidthMode = DanmakuPanelWidthMode.THIRD
): DanmakuSettingsPanelLayoutPolicy {
    if (isFullscreen) {
        val availableWidthDp = (screenWidthDp - 32).coerceAtLeast(0)
        val resolvedMaxWidth = (
            availableWidthDp *
                FULLSCREEN_DANMAKU_PANEL_WIDTH_FRACTION
            )
            .roundToInt()
            .coerceAtLeast(FULLSCREEN_DANMAKU_PANEL_MIN_WIDTH_DP)
        return DanmakuSettingsPanelLayoutPolicy(
            presentation = DanmakuSettingsPanelPresentation.CenteredDialog,
            horizontalPaddingDp = 16,
            bottomPaddingDp = 0,
            minWidthDp = FULLSCREEN_DANMAKU_PANEL_MIN_WIDTH_DP,
            maxWidthDp = resolvedMaxWidth,
            maxHeightDp = 480
        )
    }

    if (
        screenWidthDp >= WIDE_INLINE_DANMAKU_PANEL_SCREEN_WIDTH_DP &&
        screenWidthDp > screenHeightDp
    ) {
        return DanmakuSettingsPanelLayoutPolicy(
            presentation = DanmakuSettingsPanelPresentation.CenteredDialog,
            horizontalPaddingDp = 24,
            bottomPaddingDp = 0,
            minWidthDp = 520,
            maxWidthDp = minOf(
                WIDE_INLINE_DANMAKU_PANEL_MAX_WIDTH_DP,
                (screenWidthDp - 48).coerceAtLeast(520)
            ),
            maxHeightDp = (screenHeightDp - 96).coerceIn(420, 560)
        )
    }

    val horizontalPaddingDp = if (screenWidthDp >= 600) 24 else 16
    val maxHeightDp = (screenHeightDp - 72).coerceIn(420, 560)

    return DanmakuSettingsPanelLayoutPolicy(
        presentation = DanmakuSettingsPanelPresentation.BottomSheet,
        horizontalPaddingDp = horizontalPaddingDp,
        bottomPaddingDp = 20,
        minWidthDp = 0,
        maxWidthDp = maxOf(520, screenWidthDp - horizontalPaddingDp * 2),
        maxHeightDp = maxHeightDp
    )
}

internal fun shouldDismissDanmakuSettingsPanelFromBackdropGesture(
    maxDragDistancePx: Float,
    touchSlopPx: Float
): Boolean {
    return maxDragDistancePx <= touchSlopPx
}

/**
 * Danmaku Settings Panel
 * 
 * A modern, visually appealing panel for configuring danmaku settings:
 * - Opacity
 * - Font scale
 * - Speed
 * - Display area
 * 
 * Requirement Reference: AC2.4 - Reusable DanmakuSettingsPanel
 */
@Composable
fun DanmakuSettingsPanel(
    isFullscreen: Boolean = true,
    settingsScope: DanmakuSettingsScope = DanmakuSettingsScope.PORTRAIT,
    opacity: Float,
    fontScale: Float,
    showAdvancedSection: Boolean = false,
    fontWeight: Int = 5,
    speed: Float,
    displayArea: Float = 0.5f,
    strokeWidth: Float = 1.5f,
    lineHeight: Float = 1.6f,
    scrollDurationSeconds: Float = 7.0f,
    staticDurationSeconds: Float = 4.0f,
    scrollFixedVelocity: Boolean = false,
    staticDanmakuToScroll: Boolean = false,
    massiveMode: Boolean = false,
    mergeDuplicates: Boolean = true,
    allowScroll: Boolean = true,
    allowTop: Boolean = true,
    allowBottom: Boolean = true,
    allowColorful: Boolean = true,
    allowSpecial: Boolean = true,
    showBlockRuleEditor: Boolean = false,
    showSmartOcclusionSection: Boolean = true,
    showSyncSection: Boolean = false,
    blockRulesRaw: String = "",
    smartOcclusion: Boolean = true,
    fullscreenWidthMode: DanmakuPanelWidthMode = DanmakuPanelWidthMode.THIRD,
    syncUiState: DanmakuCloudSyncUiState = DanmakuCloudSyncUiState(),
    smartOcclusionModuleState: FaceOcclusionModuleState = FaceOcclusionModuleState.Checking,
    smartOcclusionDownloadProgress: Int? = null,
    onOpacityChange: (Float) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onFontWeightChange: (Int) -> Unit = {},
    onSpeedChange: (Float) -> Unit,
    onDisplayAreaChange: (Float) -> Unit = {},
    onStrokeWidthChange: (Float) -> Unit = {},
    onLineHeightChange: (Float) -> Unit = {},
    onScrollDurationSecondsChange: (Float) -> Unit = {},
    onStaticDurationSecondsChange: (Float) -> Unit = {},
    onScrollFixedVelocityChange: (Boolean) -> Unit = {},
    onStaticDanmakuToScrollChange: (Boolean) -> Unit = {},
    onMassiveModeChange: (Boolean) -> Unit = {},
    onMergeDuplicatesChange: (Boolean) -> Unit = {},
    onAllowScrollChange: (Boolean) -> Unit = {},
    onAllowTopChange: (Boolean) -> Unit = {},
    onAllowBottomChange: (Boolean) -> Unit = {},
    onAllowColorfulChange: (Boolean) -> Unit = {},
    onAllowSpecialChange: (Boolean) -> Unit = {},
    onBlockRulesRawChange: (String) -> Unit = {},
    onSmartOcclusionChange: (Boolean) -> Unit = {},
    onFullscreenWidthModeChange: (DanmakuPanelWidthMode) -> Unit = {},
    onSyncNowClick: () -> Unit = {},
    onSmartOcclusionDownloadClick: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var showBlockManager by remember { mutableStateOf(false) }
    val blockManagerSections = remember(blockRulesRaw) {
        resolveDanmakuBlockManagerSections(blockRulesRaw)
    }
    val totalBlockRuleCount = remember(blockManagerSections) {
        resolveDanmakuBlockRuleCount(blockManagerSections)
    }
    val configuration = LocalConfiguration.current
    val viewConfiguration = LocalViewConfiguration.current
    val layoutPolicy = remember(
        isFullscreen,
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        fullscreenWidthMode
    ) {
        resolveDanmakuSettingsPanelLayoutPolicy(
            isFullscreen = isFullscreen,
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp,
            fullscreenWidthMode = fullscreenWidthMode
        )
    }
    val moduleUiState = remember(smartOcclusionModuleState, smartOcclusionDownloadProgress) {
        resolveFaceOcclusionModuleUiState(
            state = smartOcclusionModuleState,
            progressPercent = smartOcclusionDownloadProgress
        )
    }
    val overlayContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = if (
                layoutPolicy.presentation == DanmakuSettingsPanelPresentation.BottomSheet
            ) {
                Alignment.BottomCenter
            } else {
                Alignment.Center
            }
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .pointerInput(onDismiss, viewConfiguration.touchSlop) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var maxDragDistancePx = 0f
                            var active = true
                            while (active) {
                                val event = awaitPointerEvent(PointerEventPass.Final)
                                event.changes.forEach { change ->
                                    val distance = hypot(
                                        (change.position.x - down.position.x).toDouble(),
                                        (change.position.y - down.position.y).toDouble()
                                    ).toFloat()
                                    maxDragDistancePx = max(maxDragDistancePx, distance)
                                    if (change.pressed || change.previousPressed) {
                                        change.consume()
                                    }
                                }
                                active = event.changes.any { it.pressed }
                            }
                            if (
                                shouldDismissDanmakuSettingsPanelFromBackdropGesture(
                                    maxDragDistancePx = maxDragDistancePx,
                                    touchSlopPx = viewConfiguration.touchSlop
                                )
                            ) {
                                onDismiss()
                            }
                        }
                    }
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = layoutPolicy.horizontalPaddingDp.dp,
                        end = layoutPolicy.horizontalPaddingDp.dp,
                        bottom = layoutPolicy.bottomPaddingDp.dp
                    )
                    .widthIn(
                        min = layoutPolicy.minWidthDp.dp,
                        max = layoutPolicy.maxWidthDp.dp
                    )
                    .heightIn(max = layoutPolicy.maxHeightDp.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { },
                color = PanelBackground,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 16.dp,
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "弹幕设置",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(999.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = settingsScope.badgeLabel,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = settingsScope.subtitle,
                                    color = Color.White.copy(alpha = 0.58f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White.copy(0.1f), CircleShape)
                        ) {
                            Icon(
                                CupertinoIcons.Default.Xmark,
                                contentDescription = "关闭",
                                tint = Color.White.copy(0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    if (showSyncSection) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = CardBackground,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "账号同步",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = syncUiState.message
                                            ?: when (syncUiState.status) {
                                                DanmakuCloudSyncStatus.SUCCESS -> "当前基础弹幕设置已同步到账号"
                                                DanmakuCloudSyncStatus.SYNCING -> "正在同步当前弹幕设置"
                                                DanmakuCloudSyncStatus.PENDING -> "检测到设置变更，等待同步"
                                                DanmakuCloudSyncStatus.FAILURE -> "最近一次同步失败，可立即重试"
                                                DanmakuCloudSyncStatus.IDLE -> "当前设备本地设置尚未触发同步"
                                            },
                                        color = Color.White.copy(0.6f),
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = resolveDanmakuSyncStatusBadgeText(syncUiState),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = onSyncNowClick,
                                        enabled = syncUiState.status != DanmakuCloudSyncStatus.SYNCING,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = if (shouldShowDanmakuSyncRetry(syncUiState.status)) {
                                                "重试同步"
                                            } else {
                                                "立即同步"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Settings Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = CardBackground,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            DanmakuSliderItem(
                                label = "透明度",
                                value = opacity,
                                valueRange = 0.3f..1f,
                                displayValue = { "${(it * 100).toInt()}%" },
                                onValueChange = onOpacityChange
                            )
                            
                            DanmakuSliderItem(
                                label = "字体大小",
                                value = fontScale,
                                valueRange = 0.3f..2f,
                                displayValue = { "${(it * 100).toInt()}%" },
                                onValueChange = onFontScaleChange
                            )
                            
                            DanmakuSliderItem(
                                label = "弹幕速度",
                                value = speed,
                                valueRange = 0.5f..2f,
                                displayValue = { v ->
                                    when {
                                        v >= 1.5f -> "慢"
                                        v <= 0.7f -> "快"
                                        else -> "中"
                                    }
                                },
                                onValueChange = onSpeedChange
                            )
                        }
                    }

                    if (showAdvancedSection) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = CardBackground,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                Text(
                                    text = "高级渲染",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "更细的弹幕渲染控制",
                                    color = Color.White.copy(0.5f),
                                    fontSize = 11.sp
                                )
                                DanmakuSliderItem(
                                    label = "字体粗细",
                                    value = fontWeight.toFloat(),
                                    valueRange = 1f..9f,
                                    steps = 7,
                                    displayValue = { "${it.roundToInt()}" },
                                    onValueChange = { onFontWeightChange(it.roundToInt()) }
                                )
                                DanmakuSliderItem(
                                    label = "描边粗细",
                                    value = strokeWidth,
                                    valueRange = 0f..4f,
                                    displayValue = { String.format("%.1f", it) },
                                    onValueChange = onStrokeWidthChange
                                )
                                DanmakuSliderItem(
                                    label = "行高",
                                    value = lineHeight,
                                    valueRange = 0.8f..2.2f,
                                    displayValue = { String.format("%.1f", it) },
                                    onValueChange = onLineHeightChange
                                )
                                DanmakuSliderItem(
                                    label = "滚动时长",
                                    value = scrollDurationSeconds,
                                    valueRange = 2f..15f,
                                    displayValue = { "${it.roundToInt()}s" },
                                    onValueChange = onScrollDurationSecondsChange
                                )
                                DanmakuSliderItem(
                                    label = "静态停留",
                                    value = staticDurationSeconds,
                                    valueRange = 2f..15f,
                                    displayValue = { "${it.roundToInt()}s" },
                                    onValueChange = onStaticDurationSecondsChange
                                )
                                DanmakuFilterSwitchRow(
                                    label = "固定滚动速度",
                                    checked = scrollFixedVelocity,
                                    onCheckedChange = onScrollFixedVelocityChange
                                )
                                DanmakuFilterSwitchRow(
                                    label = "固定弹幕转滚动",
                                    checked = staticDanmakuToScroll,
                                    onCheckedChange = onStaticDanmakuToScrollChange
                                )
                                DanmakuFilterSwitchRow(
                                    label = "海量弹幕模式",
                                    checked = massiveMode,
                                    onCheckedChange = onMassiveModeChange,
                                    showDivider = false
                                )
                            }
                        }
                    }
                
                    Spacer(modifier = Modifier.height(16.dp))
                
                    DanmakuAreaSelector(
                        currentArea = displayArea,
                        onAreaChange = onDisplayAreaChange
                    )
                
                    Spacer(modifier = Modifier.height(16.dp))
                
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = CardBackground,
                        shape = RoundedCornerShape(16.dp),
                        onClick = { onMergeDuplicatesChange(!mergeDuplicates) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "合并重复弹幕",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "减少刷屏干扰，将重复内容合并显示",
                                    color = Color.White.copy(0.5f),
                                    fontSize = 11.sp
                                )
                            }
                            
                            Switch(
                                checked = mergeDuplicates,
                                onCheckedChange = onMergeDuplicatesChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.White.copy(0.1f)
                                )
                            )
                        }
                    }
    
                    Spacer(modifier = Modifier.height(16.dp))

                    if (showSmartOcclusionSection) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = CardBackground,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "人脸模型",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = moduleUiState.statusText,
                                        color = Color.White.copy(0.65f),
                                        fontSize = 11.sp
                                    )
                                }
                                if (moduleUiState.showAction) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Button(
                                        onClick = onSmartOcclusionDownloadClick,
                                        enabled = moduleUiState.isActionEnabled,
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = moduleUiState.actionText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
    
                        Spacer(modifier = Modifier.height(16.dp))
    
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = CardBackground,
                            shape = RoundedCornerShape(16.dp),
                            onClick = { onSmartOcclusionChange(!smartOcclusion) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "智能避脸遮挡",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "实时识别人脸并避让弹幕轨道",
                                        color = Color.White.copy(0.5f),
                                        fontSize = 11.sp
                                    )
                                }
    
                                Switch(
                                    checked = smartOcclusion,
                                    onCheckedChange = onSmartOcclusionChange,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = Color.White.copy(0.1f)
                                    )
                                )
                            }
                        }
    
                        Spacer(modifier = Modifier.height(16.dp))
                    }
    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = CardBackground,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "屏蔽类型",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "关闭对应开关即可屏蔽",
                                color = Color.White.copy(0.5f),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
    
                            DanmakuFilterSwitchRow(
                                label = "滚动弹幕",
                                checked = allowScroll,
                                onCheckedChange = onAllowScrollChange
                            )
                            DanmakuFilterSwitchRow(
                                label = "顶部弹幕",
                                checked = allowTop,
                                onCheckedChange = onAllowTopChange
                            )
                            DanmakuFilterSwitchRow(
                                label = "底部弹幕",
                                checked = allowBottom,
                                onCheckedChange = onAllowBottomChange
                            )
                            DanmakuFilterSwitchRow(
                                label = "彩色弹幕",
                                checked = allowColorful,
                                onCheckedChange = onAllowColorfulChange
                            )
                            DanmakuFilterSwitchRow(
                                label = "高级弹幕",
                                checked = allowSpecial,
                                onCheckedChange = onAllowSpecialChange,
                                showDivider = false
                            )
                        }
                    }
    
                    Spacer(modifier = Modifier.height(16.dp))

                    if (showBlockRuleEditor) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = CardBackground,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "自定义屏蔽词",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { showBlockManager = true },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "屏蔽管理",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (totalBlockRuleCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = resolveDanmakuBlockRuleBadgeText(totalBlockRuleCount),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (totalBlockRuleCount > 0) {
                                        "已维护 $totalBlockRuleCount 条规则，修改后立即生效"
                                    } else {
                                        "每行一个，支持关键词、正则与 UID(hash)：regex:xxx / re:xxx / /xxx/ / uid:xxx"
                                    },
                                    color = Color.White.copy(0.5f),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = blockRulesRaw,
                                    onValueChange = onBlockRulesRawChange,
                                    placeholder = {
                                        Text(
                                            text = "例如：剧透\\nregex:第\\\\d+集\\n/哈{3,}/",
                                            color = Color.White.copy(0.35f),
                                            fontSize = 12.sp
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    maxLines = 6,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                        focusedContainerColor = Color.White.copy(alpha = 0.02f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isFullscreen) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = false
            )
        ) {
            overlayContent()
        }
    } else {
        overlayContent()
    }

    if (showBlockManager) {
        DanmakuBlockManagerDialog(
            rawRules = blockRulesRaw,
            onRulesSave = { onBlockRulesRawChange(it) },
            onDismiss = { showBlockManager = false }
        )
    }
}

@Composable
private fun DanmakuBlockManagerDialog(
    rawRules: String,
    onRulesSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialSections = remember(rawRules) {
        resolveDanmakuBlockManagerSections(rawRules)
    }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var keywordRules by remember(rawRules) { mutableStateOf(initialSections.keywordRules) }
    var regexRules by remember(rawRules) { mutableStateOf(initialSections.regexRules) }
    var userHashRules by remember(rawRules) { mutableStateOf(initialSections.userHashRules) }
    var inputValue by remember(selectedTabIndex) { mutableStateOf("") }

    fun updateCurrentRules(transform: (List<String>) -> List<String>) {
        when (selectedTabIndex) {
            0 -> keywordRules = transform(keywordRules)
            1 -> regexRules = transform(regexRules)
            else -> userHashRules = transform(userHashRules)
        }
    }

    val currentRules = when (selectedTabIndex) {
        0 -> keywordRules
        1 -> regexRules
        else -> userHashRules
    }
    val tabCounts = remember(keywordRules, regexRules, userHashRules) {
        listOf(keywordRules.size, regexRules.size, userHashRules.size)
    }
    val currentHint = when (selectedTabIndex) {
        0 -> "例如：剧透"
        1 -> "例如：regex:第\\d+集"
        else -> "例如：uid:abc123 或 abc123"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            color = PanelBackground,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 16.dp,
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "屏蔽管理",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "分类维护关键词、正则和 UID(hash) 规则",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 11.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(0.1f), CircleShape)
                    ) {
                        Icon(
                            CupertinoIcons.Default.Xmark,
                            contentDescription = "关闭",
                            tint = Color.White.copy(0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("关键词", "正则", "UID(hash)").forEachIndexed { index, label ->
                        FilterChip(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            label = {
                                Text(
                                    resolveDanmakuBlockManagerTabLabel(label, tabCounts[index]),
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    placeholder = {
                        Text(
                            text = currentHint,
                            color = Color.White.copy(alpha = 0.35f),
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = Color.White.copy(alpha = 0.02f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            val candidate = inputValue.trim()
                            if (candidate.isEmpty()) return@Button
                            updateCurrentRules { (it + candidate).distinct() }
                            inputValue = ""
                        },
                        enabled = inputValue.isNotBlank()
                    ) {
                        Text("添加")
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CardBackground,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (currentRules.isEmpty()) {
                            Text(
                                text = "当前分类还没有规则",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        } else {
                            currentRules.forEachIndexed { index, rule ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = rule,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(
                                        onClick = {
                                            updateCurrentRules { rules ->
                                                rules.filterIndexed { currentIndex, _ ->
                                                    currentIndex != index
                                                }
                                            }
                                        }
                                    ) {
                                        Text("删除")
                                    }
                                }
                                if (index != currentRules.lastIndex) {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onRulesSave(
                                persistDanmakuBlockManagerSections(
                                    DanmakuBlockRuleSections(
                                        keywordRules = keywordRules,
                                        regexRules = regexRules,
                                        userHashRules = userHashRules
                                    )
                                )
                            )
                            onDismiss()
                        }
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

@Composable
private fun DanmakuFilterSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 14.sp
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.White.copy(0.1f)
                )
            )
        }
        if (showDivider) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        }
    }
}

/**
 * 弹幕显示区域选择器
 */
@Composable
private fun DanmakuAreaSelector(
    currentArea: Float,
    onAreaChange: (Float) -> Unit
) {
    //  本地状态确保即时 UI 响应
    var localArea by remember(currentArea) { mutableFloatStateOf(currentArea) }
    
    data class AreaOption(val value: Float, val label: String, val subLabel: String)
    
    val areaOptions = listOf(
        AreaOption(0.25f, "1/4", "顶部"),
        AreaOption(0.5f, "1/2", "半屏"),
        AreaOption(0.75f, "3/4", "大部"),
        AreaOption(1.0f, "全屏", "铺满")
    )
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBackground,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "显示区域",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                areaOptions.forEach { option ->
                    //  使用本地状态判断选中状态
                    val isSelected = kotlin.math.abs(localArea - option.value) < 0.1f
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (isSelected) {
                                    Modifier.background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                            )
                                        )
                                    )
                                } else {
                                    Modifier
                                        .background(Color.White.copy(0.05f))
                                        .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(12.dp))
                                }
                            )
                            .clickable { 
                                localArea = option.value  //  即时更新 UI
                                onAreaChange(option.value) 
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = option.label,
                                color = if (isSelected) Color.White else Color.White.copy(0.9f),
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = option.subLabel,
                                color = if (isSelected) Color.White.copy(0.8f) else Color.White.copy(0.5f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DanmakuSliderItem(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    displayValue: (Float) -> String,
    onValueChange: (Float) -> Unit
) {
    var localValue by remember(value) { mutableFloatStateOf(value) }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = displayValue(localValue),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Slider(
            value = localValue,
            onValueChange = { newValue ->
                localValue = newValue
                onValueChange(newValue)
            },
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(0.15f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
