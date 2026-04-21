// 文件路径: feature/video/ui/components/VideoSettingsPanel.kt
package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//  Cupertino Icons
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhotoCamera
import com.android.purebilibili.core.store.LONG_PRESS_SPEED_OPTIONS
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.LocalAndroidNativeVariant
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.ui.rememberAppChevronForwardIcon
import com.android.purebilibili.core.ui.rememberAppCodecIcon
import com.android.purebilibili.core.ui.rememberAppDownloadIcon
import com.android.purebilibili.core.ui.rememberAppFlipHorizontalIcon
import com.android.purebilibili.core.ui.rememberAppFlipVerticalIcon
import com.android.purebilibili.core.ui.rememberAppGestureTapIcon
import com.android.purebilibili.core.ui.rememberAppHeadphonesIcon
import com.android.purebilibili.core.ui.rememberAppSettingsIcon
import com.android.purebilibili.core.ui.rememberAppMusicIcon
import com.android.purebilibili.core.ui.rememberAppPhotoIcon
import com.android.purebilibili.core.ui.rememberAppQualityIcon
import com.android.purebilibili.core.ui.rememberAppRefreshIcon
import com.android.purebilibili.core.ui.rememberAppSpeedIcon
import com.android.purebilibili.core.ui.rememberAppTimerIcon
import com.android.purebilibili.core.ui.rememberAppWifiIcon
import com.android.purebilibili.core.ui.components.DefaultPlaybackSpeedPreferenceControl
import com.android.purebilibili.core.ui.components.formatDefaultPlaybackSpeed
import com.android.purebilibili.data.model.response.AiAudioInfo
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class VideoSettingsPanelVisualSpec(
    val rowHorizontalPadding: androidx.compose.ui.unit.Dp,
    val rowVerticalPadding: androidx.compose.ui.unit.Dp,
    val rowMinHeight: androidx.compose.ui.unit.Dp,
    val iconSize: androidx.compose.ui.unit.Dp,
    val iconGap: androidx.compose.ui.unit.Dp,
    val dividerHorizontalPadding: androidx.compose.ui.unit.Dp,
    val dividerAlpha: Float,
    val chipHeight: androidx.compose.ui.unit.Dp,
    val chipCornerRadius: androidx.compose.ui.unit.Dp,
    val chipHorizontalPadding: androidx.compose.ui.unit.Dp,
    val chipSpacing: androidx.compose.ui.unit.Dp
)

private fun resolveVideoSettingsPanelVisualSpec(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant
): VideoSettingsPanelVisualSpec {
    return if (uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX) {
        VideoSettingsPanelVisualSpec(
            rowHorizontalPadding = 16.dp,
            rowVerticalPadding = 12.dp,
            rowMinHeight = 56.dp,
            iconSize = 20.dp,
            iconGap = 12.dp,
            dividerHorizontalPadding = 16.dp,
            dividerAlpha = 0.18f,
            chipHeight = 34.dp,
            chipCornerRadius = 17.dp,
            chipHorizontalPadding = 13.dp,
            chipSpacing = 7.dp
        )
    } else {
        VideoSettingsPanelVisualSpec(
            rowHorizontalPadding = 16.dp,
            rowVerticalPadding = 14.dp,
            rowMinHeight = 52.dp,
            iconSize = 24.dp,
            iconGap = 16.dp,
            dividerHorizontalPadding = 16.dp,
            dividerAlpha = 0.5f,
            chipHeight = 32.dp,
            chipCornerRadius = 16.dp,
            chipHorizontalPadding = 12.dp,
            chipSpacing = 8.dp
        )
    }
}

@Composable
private fun rememberVideoSettingsPanelVisualSpec(): VideoSettingsPanelVisualSpec {
    val uiPreset = LocalUiPreset.current
    val androidNativeVariant = LocalAndroidNativeVariant.current
    return remember(uiPreset, androidNativeVariant) {
        resolveVideoSettingsPanelVisualSpec(uiPreset, androidNativeVariant)
    }
}

@Composable
private fun videoSettingsChipContainerColor(isSelected: Boolean): Color {
    val uiPreset = LocalUiPreset.current
    val androidNativeVariant = LocalAndroidNativeVariant.current
    return if (uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX) {
        if (isSelected) MiuixTheme.colorScheme.secondaryContainer else MiuixTheme.colorScheme.surfaceContainerHigh
    } else {
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    }
}

@Composable
private fun videoSettingsChipContentColor(isSelected: Boolean): Color {
    val uiPreset = LocalUiPreset.current
    val androidNativeVariant = LocalAndroidNativeVariant.current
    return if (uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX) {
        if (isSelected) MiuixTheme.colorScheme.onSecondaryContainer else MiuixTheme.colorScheme.onSurfaceVariantSummary
    } else {
        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    }
}

/**
 *  视频设置面板 - 竖屏模式下的高级设置底部弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSettingsPanel(
    // 定时关闭
    sleepTimerMinutes: Int?,
    onSleepTimerChange: (Int?) -> Unit,
    
    // 视频控制
    onReload: () -> Unit,
    
    // 画质 - 内联选择
    currentQualityLabel: String,
    qualityLabels: List<String> = emptyList(),
    qualityIds: List<Int> = emptyList(),
    switchableQualityIds: List<Int> = emptyList(),
    isLoggedIn: Boolean = false,
    isVip: Boolean = false,
    onQualitySelected: (Int) -> Unit = {},
    
    // 倍速
    currentSpeed: Float = 1.0f,
    onSpeedChange: (Float) -> Unit = {},
    
    // 镜像翻转
    isFlippedHorizontal: Boolean = false,
    isFlippedVertical: Boolean = false,
    onFlipHorizontal: () -> Unit = {},
    onFlipVertical: () -> Unit = {},
    
    // 音频模式
    isAudioOnly: Boolean = false,
    onAudioOnlyToggle: () -> Unit = {},
    
    //  CDN 线路切换
    currentCdnIndex: Int = 0,
    cdnCount: Int = 1,
    onSwitchCdn: () -> Unit = {},
    onSwitchCdnTo: (Int) -> Unit = {},

    // [New] Codec & Audio Quality
    // Passed from PlayerViewModel/SettingsManager
    currentCodec: String = "hev1", 
    onCodecChange: (String) -> Unit = {},
    currentSecondCodec: String = "avc1",
    onSecondCodecChange: (String) -> Unit = {},
    currentAudioQuality: Int = -1,
    onAudioQualityChange: (Int) -> Unit = {},
    // [New] 音频语言 (AI Translation)
    aiAudioInfo: AiAudioInfo? = null,
    currentAudioLang: String? = null,
    onAudioLangChange: (String) -> Unit = {},
    
    // 资源下载
    onSaveCover: () -> Unit = {},
    onCaptureScreenshot: () -> Unit = {},
    onDownloadAudio: () -> Unit = {},
    
    // 关闭面板
    onDismiss: () -> Unit
) {
    fun hasPermissionForQuality(qualityId: Int): Boolean {
        return when {
            qualityId >= 112 -> isVip
            qualityId >= 80 -> isLoggedIn
            else -> true
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val panelSpec = rememberVideoSettingsPanelVisualSpec()
    val context = androidx.compose.ui.platform.LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val actionPolicy = remember(configuration.screenWidthDp) {
        resolveVideoSettingsPanelActionPolicy(
            widthDp = configuration.screenWidthDp
        )
    }
    val scope = rememberCoroutineScope()
    val seekForwardSeconds by com.android.purebilibili.core.store.SettingsManager
        .getSeekForwardSeconds(context)
        .collectAsState(initial = 10)
    val seekBackwardSeconds by com.android.purebilibili.core.store.SettingsManager
        .getSeekBackwardSeconds(context)
        .collectAsState(initial = 10)
    val longPressSpeed by com.android.purebilibili.core.store.SettingsManager
        .getLongPressSpeed(context)
        .collectAsState(initial = 2.0f)
    val twoFingerVerticalSpeedEnabled by com.android.purebilibili.core.store.SettingsManager
        .getTwoFingerVerticalSpeedEnabled(context)
        .collectAsState(initial = false)
    val twoFingerHorizontalSpeedEnabled by com.android.purebilibili.core.store.SettingsManager
        .getTwoFingerHorizontalSpeedEnabled(context)
        .collectAsState(initial = false)
    val defaultPlaybackSpeed by com.android.purebilibili.core.store.SettingsManager
        .getDefaultPlaybackSpeed(context)
        .collectAsState(initial = 1.0f)
    val rememberLastPlaybackSpeed by com.android.purebilibili.core.store.SettingsManager
        .getRememberLastPlaybackSpeed(context)
        .collectAsState(initial = false)
    val timerIcon = rememberAppTimerIcon()
    val refreshIcon = rememberAppRefreshIcon()
    val photoIcon = rememberAppPhotoIcon()
    val downloadIcon = rememberAppDownloadIcon()
    val musicIcon = rememberAppMusicIcon()
    val flipHorizontalIcon = rememberAppFlipHorizontalIcon()
    val flipVerticalIcon = rememberAppFlipVerticalIcon()
    val headphonesIcon = rememberAppHeadphonesIcon()
    val qualityIcon = rememberAppQualityIcon()
    val codecIcon = rememberAppCodecIcon()
    val wifiIcon = rememberAppWifiIcon()
    val speedIcon = rememberAppSpeedIcon()
    val gestureTapIcon = rememberAppGestureTapIcon()
    val settingsIcon = rememberAppSettingsIcon()
    
    com.android.purebilibili.core.ui.IOSModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(
                if (LocalAndroidNativeVariant.current == AndroidNativeVariant.MIUIX) 4.dp else 8.dp
            )
        ) {
            //  定时关闭 - 垂直布局，选项在下一行
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = timerIcon,
                            contentDescription = null,
                            tint = if (LocalAndroidNativeVariant.current == AndroidNativeVariant.MIUIX) {
                                MiuixTheme.colorScheme.onSurfaceVariantActions
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(panelSpec.iconSize)
                        )
                        Spacer(modifier = Modifier.width(panelSpec.iconGap))
                        Text(
                            text = "定时关闭",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // 定时选项按钮组 - 支持横向滚动
                    SleepTimerOptions(
                        currentMinutes = sleepTimerMinutes,
                        onSelect = onSleepTimerChange
                    )
                }
                SettingsDivider()
            }

            item {
                //  重载视频
                SettingsItem(
                    icon = refreshIcon,
                    title = "重载视频",
                    onClick = {
                        onReload()
                        onDismiss()
                    }
                )
                SettingsDivider()
            }

            // [New] 资源下载
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = downloadIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "资源下载",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(actionPolicy.rowItemSpacingDp.dp)
                    ) {
                        SettingsActionPill(
                            icon = photoIcon,
                            label = "保存封面",
                            onClick = {
                                onSaveCover()
                                onDismiss()
                            },
                            policy = actionPolicy
                        )
                        SettingsActionPill(
                            icon = Icons.Filled.PhotoCamera,
                            label = "视频截图",
                            onClick = {
                                onCaptureScreenshot()
                                onDismiss()
                            },
                            policy = actionPolicy
                        )
                        SettingsActionPill(
                            icon = musicIcon,
                            label = "下载音频",
                            onClick = {
                                onDownloadAudio()
                                onDismiss()
                            },
                            policy = actionPolicy
                        )
                    }
                }
                SettingsDivider()
            }

            item {
                //  镜像翻转按钮组
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(actionPolicy.rowItemSpacingDp.dp)
                ) {
                    // 左右翻转
                    FlipButton(
                        icon = flipHorizontalIcon,
                        label = "左右翻转",
                        isActive = isFlippedHorizontal,
                        onClick = {
                            onFlipHorizontal()
                            onDismiss()
                        },
                        policy = actionPolicy
                    )
                    
                    // 上下翻转
                    FlipButton(
                        icon = flipVerticalIcon,
                        label = "上下翻转",
                        isActive = isFlippedVertical,
                        onClick = {
                            onFlipVertical()
                            onDismiss()
                        },
                        policy = actionPolicy
                    )
                    
                    // 听视频（音频模式）
                    FlipButton(
                        icon = headphonesIcon,
                        label = "听视频",
                        isActive = isAudioOnly,
                        onClick = {
                            onAudioOnlyToggle()
                            onDismiss()
                        },
                        policy = actionPolicy
                    )
                }
                SettingsDivider()
            }

            //  选择画质 - 内联选择
            if (qualityLabels.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = qualityIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "选择画质",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "当前 $currentQualityLabel",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // 画质选项
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(panelSpec.chipSpacing)
                ) {
                            qualityLabels.forEachIndexed { index, label ->
                                val isSelected = label == currentQualityLabel
                                val qualityId = qualityIds.getOrNull(index) ?: 0
                                val isSwitchable = qualityId in switchableQualityIds
                                val hasPermission = hasPermissionForQuality(qualityId)
                                val isEnabled = isSwitchable && hasPermission
                                val containerColor = when {
                                    isSelected -> videoSettingsChipContainerColor(true)
                                    isEnabled -> videoSettingsChipContainerColor(false)
                                    else -> videoSettingsChipContainerColor(false).copy(alpha = 0.55f)
                                }
                                val contentColor = when {
                                    isSelected -> videoSettingsChipContentColor(true)
                                    isEnabled -> videoSettingsChipContentColor(false)
                                    else -> videoSettingsChipContentColor(false).copy(alpha = 0.45f)
                                }

                                Surface(
                                    onClick = {
                                        if (!isSelected && isEnabled) {
                                            onQualitySelected(index)
                                        }
                                    },
                                    enabled = isSelected || isEnabled,
                                    shape = RoundedCornerShape(panelSpec.chipCornerRadius),
                                    color = containerColor,
                                    modifier = Modifier.height(panelSpec.chipHeight)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = panelSpec.chipHorizontalPadding)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 13.sp,
                                            color = contentColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                    SettingsDivider()
                }
            }
            
            // [New] 编码格式选择
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = codecIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "编码格式",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        val codecLabel = when(currentCodec) {
                            "avc1" -> "AVC (兼容)"
                            "hev1" -> "HEVC (推荐)"
                            "av01" -> "AV1 (极致)"
                            else -> "未知"
                        }
                        Text(
                            text = codecLabel,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val codecs = listOf("avc1" to "AVC (H.264)", "hev1" to "HEVC (H.265)", "av01" to "AV1")
                        codecs.forEach { (codec, label) ->
                            val isSelected = currentCodec == codec
                            Surface(
                                onClick = { onCodecChange(codec) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.height(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                SettingsDivider()
            }

            // [New] 次选编码格式选择
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = codecIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "次选编码",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        val secondCodecLabel = when(currentSecondCodec) {
                            "avc1" -> "AVC (兼容)"
                            "hev1" -> "HEVC (高效)"
                            "av01" -> "AV1 (高压缩)"
                            else -> "未知"
                        }
                        Text(
                            text = secondCodecLabel,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val codecs = listOf("avc1" to "AVC (H.264)", "hev1" to "HEVC (H.265)", "av01" to "AV1")
                        codecs.forEach { (codec, label) ->
                            val isSelected = currentSecondCodec == codec
                            Surface(
                                onClick = { onSecondCodecChange(codec) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.height(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                SettingsDivider()
            }

            // [New] 音频画质选择
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = musicIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "音频音质",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        val audioLabel = when(currentAudioQuality) {
                            -1 -> "自动"
                            30280 -> "192K"
                            30232 -> "132K"
                            30216 -> "64K"
                            30250 -> "杜比全景声"
                            30251 -> "Hi-Res无损"
                            else -> "其他"
                        }
                        Text(
                            text = audioLabel,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val audios = listOf(
                            -1 to "自动", 
                            30280 to "192K", 
                            30250 to "杜比", 
                            30251 to "Hi-Res"
                        )
                        audios.forEach { (code, label) ->
                            val isSelected = currentAudioQuality == code
                            Surface(
                                onClick = { onAudioQualityChange(code) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.height(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                SettingsDivider()
            }
            item { SettingsDivider() }

             // [New] 音频语言选择 (AI Translation)
            if (aiAudioInfo?.items?.isNotEmpty() == true) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AI原生翻译",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            val currentLangItem = aiAudioInfo.items.find { it.langCode == currentAudioLang }
                            val langLabel = currentLangItem?.langDoc ?: "原声"
                            
                            Text(
                                text = langLabel,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            aiAudioInfo.items.forEach { item ->
                                val isSelected = currentAudioLang == item.langCode
                                Surface(
                                    onClick = { onAudioLangChange(item.langCode) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                                        Text(
                                            text = item.langDoc,
                                            fontSize = 13.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    SettingsDivider()
                }
            }

            //  播放线路 (CDN) - 仅在有多个线路时显示
            if (cdnCount > 1) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = wifiIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "播放线路",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "当前 线路${currentCdnIndex + 1}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // CDN 线路选项
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            repeat(cdnCount) { index ->
                                val isSelected = index == currentCdnIndex
                                Surface(
                                    onClick = { 
                                        if (!isSelected) {
                                            onSwitchCdnTo(index)
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    ) {
                                        Text(
                                            text = "线路${index + 1}",
                                            fontSize = 13.sp,
                                            color = if (isSelected) 
                                                MaterialTheme.colorScheme.onPrimary 
                                            else 
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    SettingsDivider()
                }
            }

            //  播放倍速 - 内联选择
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = speedIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "播放倍速",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentSpeed == 1.0f) "正常" else "${currentSpeed}x",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // 倍速选项
                    SpeedOptions(
                        currentSpeed = currentSpeed,
                        onSelect = onSpeedChange
                    )
                }
                SettingsDivider()
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = settingsIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "默认播放速度",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (rememberLastPlaybackSpeed) {
                                    "已开启记忆上次速度（当前优先）"
                                } else {
                                    "当前默认 ${formatDefaultPlaybackSpeed(defaultPlaybackSpeed)}"
                                },
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = rememberLastPlaybackSpeed,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    com.android.purebilibili.core.store.SettingsManager
                                        .setRememberLastPlaybackSpeed(context, checked)
                                }
                            },
                            modifier = Modifier.scale(0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    DefaultPlaybackSpeedPreferenceControl(
                        currentSpeed = defaultPlaybackSpeed,
                        onSpeedChange = { speed ->
                            scope.launch {
                                com.android.purebilibili.core.store.SettingsManager
                                    .setDefaultPlaybackSpeed(context, speed)
                            }
                        },
                        title = null,
                        subtitle = null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                SettingsDivider()
            }

            //  [新增] 双击跳转秒数设置 (带开关)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp) // 优化：减少垂直间距
                ) {
                    val doubleTapSeekEnabled by com.android.purebilibili.core.store.SettingsManager
                        .getDoubleTapSeekEnabled(context)
                        .collectAsState(initial = true)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = speedIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "双击跳转",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (doubleTapSeekEnabled) "快进 ${seekForwardSeconds}s / 后退 ${seekBackwardSeconds}s" else "已关闭",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // 开关
                        Switch(
                            checked = doubleTapSeekEnabled,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    com.android.purebilibili.core.store.SettingsManager.setDoubleTapSeekEnabled(context, checked)
                                }
                            },
                            modifier = Modifier.scale(0.8f) // 优化：开关稍微缩小
                        )
                    }
                    
                    // 仅当开启时显示秒数选项
                    AnimatedVisibility(
                        visible = doubleTapSeekEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 快进秒数选择
                            Text(
                                text = "快进秒数（双击右侧）",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            SeekSecondsOptions(
                                currentSeconds = seekForwardSeconds,
                                onSelect = { seconds ->
                                    scope.launch {
                                        com.android.purebilibili.core.store.SettingsManager.setSeekForwardSeconds(context, seconds)
                                    }
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 后退秒数选择
                            Text(
                                text = "后退秒数（双击左侧）",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            SeekSecondsOptions(
                                currentSeconds = seekBackwardSeconds,
                                onSelect = { seconds ->
                                    scope.launch {
                                        com.android.purebilibili.core.store.SettingsManager.setSeekBackwardSeconds(context, seconds)
                                    }
                                }
                            )
                        }
                    }
                }
                SettingsDivider()
            }

            //  [新增] 长按倍速设置
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp) // 优化：减少垂直间距
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = gestureTapIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "长按倍速",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "当前 ${longPressSpeed}x",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 长按倍速选项
                    LongPressSpeedOptions(
                        currentSpeed = longPressSpeed,
                        onSelect = { speed ->
                            scope.launch {
                                com.android.purebilibili.core.store.SettingsManager.setLongPressSpeed(context, speed)
                            }
                        }
                    )
                }
                SettingsDivider()
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = flipVerticalIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "双指上下滑动调倍速",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "仅全屏生效，开启一项时会关闭另一项",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = twoFingerVerticalSpeedEnabled,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    com.android.purebilibili.core.store.SettingsManager
                                        .setTwoFingerVerticalSpeedEnabled(context, checked)
                                }
                            },
                            modifier = Modifier.scale(0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = flipHorizontalIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "双指左右滑动调倍速",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "仅全屏生效，开启一项时会关闭另一项",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = twoFingerHorizontalSpeedEnabled,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    com.android.purebilibili.core.store.SettingsManager
                                        .setTwoFingerHorizontalSpeedEnabled(context, checked)
                                }
                            },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 设置项组件
 */
@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    val chevronIcon = rememberAppChevronForwardIcon()
    val uiPreset = LocalUiPreset.current
    val androidNativeVariant = LocalAndroidNativeVariant.current
    val spec = rememberVideoSettingsPanelVisualSpec()
    if (uiPreset == UiPreset.MD3 && androidNativeVariant == AndroidNativeVariant.MIUIX) {
        BasicComponent(
            title = title,
            summary = subtitle,
            onClick = onClick,
            insideMargin = PaddingValues(
                horizontal = spec.rowHorizontalPadding,
                vertical = spec.rowVerticalPadding
            ),
            startAction = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    modifier = Modifier.size(spec.iconSize)
                )
            },
            endActions = {
                if (trailing != null) {
                    trailing()
                } else {
                    Icon(
                        imageVector = chevronIcon,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        )
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = spec.rowMinHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = spec.rowHorizontalPadding, vertical = spec.rowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(spec.iconSize)
        )
        
        Spacer(modifier = Modifier.width(spec.iconGap))
        
        // 标题和副标题
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 右侧内容
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = chevronIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    val spec = rememberVideoSettingsPanelVisualSpec()
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = spec.dividerHorizontalPadding),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = spec.dividerAlpha)
    )
}

/**
 * 定时关闭选项
 */
@Composable
private fun SleepTimerOptions(
    currentMinutes: Int?,
    onSelect: (Int?) -> Unit
) {
    val spec = rememberVideoSettingsPanelVisualSpec()
    val options = listOf(
        null to "关闭",
        15 to "15分钟",
        30 to "30分钟",
        60 to "1小时",
        90 to "1.5小时"
    )
    
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spec.chipSpacing)
    ) {
        options.forEach { (minutes, label) ->
            val isSelected = currentMinutes == minutes
            Surface(
                onClick = { onSelect(minutes) },
                shape = RoundedCornerShape(spec.chipCornerRadius),
                color = videoSettingsChipContainerColor(isSelected),
                modifier = Modifier.height(spec.chipHeight)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = spec.chipHorizontalPadding)
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        color = videoSettingsChipContentColor(isSelected)
                    )
                }
            }
        }
    }
}

/**
 * 播放倍速选项
 */
@Composable
private fun SpeedOptions(
    currentSpeed: Float,
    onSelect: (Float) -> Unit
) {
    val spec = rememberVideoSettingsPanelVisualSpec()
    val options = listOf(
        0.5f to "0.5x",
        0.75f to "0.75x",
        1.0f to "正常",
        1.25f to "1.25x",
        1.3f to "1.3x",
        1.5f to "1.5x",
        2.0f to "2x"
    )
    
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spec.chipSpacing)
    ) {
        options.forEach { (speed, label) ->
            val isSelected = currentSpeed == speed
            Surface(
                onClick = { onSelect(speed) },
                shape = RoundedCornerShape(spec.chipCornerRadius),
                color = videoSettingsChipContainerColor(isSelected),
                modifier = Modifier.height(spec.chipHeight)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = spec.chipHorizontalPadding)
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        color = videoSettingsChipContentColor(isSelected)
                    )
                }
            }
        }
    }
}

/**
 * 翻转/模式切换按钮
 */
@Composable
private fun FlipButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    policy: VideoSettingsPanelActionPolicy
) {
    val spec = rememberVideoSettingsPanelVisualSpec()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(policy.pillHeightDp.dp),
        color = if (isActive) videoSettingsChipContainerColor(true) else videoSettingsChipContainerColor(false).copy(alpha = 0.78f),
        border = if (isActive) null else null,
        modifier = Modifier
            .height(policy.pillHeightDp.dp)
            .defaultMinSize(minWidth = policy.pillMinWidthDp.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = policy.pillHorizontalPaddingDp.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) 
                    videoSettingsChipContentColor(true)
                else 
                    videoSettingsChipContentColor(false),
                modifier = Modifier.size(policy.pillIconSizeDp.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                color = if (isActive) 
                    videoSettingsChipContentColor(true)
                else 
                    videoSettingsChipContentColor(false)
            )
        }
    }
}

@Composable
private fun SettingsActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    policy: VideoSettingsPanelActionPolicy
) {
    val spec = rememberVideoSettingsPanelVisualSpec()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(policy.pillHeightDp.dp),
        color = if (LocalAndroidNativeVariant.current == AndroidNativeVariant.MIUIX) {
            MiuixTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        modifier = Modifier
            .height(policy.pillHeightDp.dp)
            .defaultMinSize(minWidth = policy.pillMinWidthDp.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = policy.pillHorizontalPaddingDp.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(policy.pillIconSizeDp.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 双击跳转秒数选项
 */
@Composable
private fun SeekSecondsOptions(
    currentSeconds: Int,
    onSelect: (Int) -> Unit
) {
    val spec = rememberVideoSettingsPanelVisualSpec()
    val options = listOf(5, 10, 15, 20, 30)
    
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spec.chipSpacing)
    ) {
        options.forEach { seconds ->
            val isSelected = currentSeconds == seconds
            Surface(
                onClick = { onSelect(seconds) },
                shape = RoundedCornerShape(spec.chipCornerRadius),
                color = videoSettingsChipContainerColor(isSelected),
                modifier = Modifier.height(spec.chipHeight)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = spec.chipHorizontalPadding)
                ) {
                    Text(
                        text = "${seconds}s",
                        fontSize = 13.sp,
                        color = videoSettingsChipContentColor(isSelected)
                    )
                }
            }
        }
    }
}

/**
 * 长按倍速选项
 */
@Composable
private fun LongPressSpeedOptions(
    currentSpeed: Float,
    onSelect: (Float) -> Unit
) {
    val spec = rememberVideoSettingsPanelVisualSpec()
    val options = LONG_PRESS_SPEED_OPTIONS
    
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spec.chipSpacing)
    ) {
        options.forEach { speed ->
            val isSelected = currentSpeed == speed
            Surface(
                onClick = { onSelect(speed) },
                shape = RoundedCornerShape(spec.chipCornerRadius),
                color = videoSettingsChipContainerColor(isSelected),
                modifier = Modifier.height(spec.chipHeight)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = spec.chipHorizontalPadding)
                ) {
                    Text(
                        text = "${speed}x",
                        fontSize = 13.sp,
                        color = videoSettingsChipContentColor(isSelected)
                    )
                }
            }
        }
    }
}
