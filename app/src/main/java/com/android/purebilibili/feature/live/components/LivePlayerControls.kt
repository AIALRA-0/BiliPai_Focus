package com.android.purebilibili.feature.live.components

import android.media.AudioManager
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.SpeakerWave2
import io.github.alexzhirkevich.cupertino.icons.filled.SunMax
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronBackward
import io.github.alexzhirkevich.cupertino.icons.outlined.Pause
import io.github.alexzhirkevich.cupertino.icons.outlined.Play
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowUpLeftAndArrowDownRight
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowDownRightAndArrowUpLeft
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowClockwise
import io.github.alexzhirkevich.cupertino.icons.filled.TextBubble
import io.github.alexzhirkevich.cupertino.icons.filled.BubbleLeft
import android.app.Activity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.purebilibili.feature.live.rememberLiveChromePalette

@Composable
private fun PlayerIconBtn(
    icon: ImageVector,
    onClick: () -> Unit,
    active: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = rememberLiveChromePalette()
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (active) palette.accentSoft else palette.scrim.copy(alpha = 0.48f),
        modifier = modifier
            .size(38.dp)
            .border(
                1.dp,
                if (active) palette.accent.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.10f),
                CircleShape
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) palette.accentStrong else Color.White
            )
        }
    }
}

/**
 * 直播播放器控制层
 * 支持：
 * 1. 左侧亮度调节手势
 * 2. 右侧音量调节手势
 * 3. 单击显示/隐藏控制器
 * 4. 双击暂停/播放
 */
@Composable
fun LivePlayerControls(
    isPlaying: Boolean,
    isFullscreen: Boolean,
    showTopBar: Boolean = true,
    title: String,
    subtitle: String = "",
    onPlayPause: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isChatVisible: Boolean = true,
    onToggleChat: () -> Unit = {},
    showChatToggle: Boolean = false,
    // 新增：弹幕开关
    isDanmakuEnabled: Boolean = true,
    onToggleDanmaku: () -> Unit = {},
    onOpenDanmakuSettings: () -> Unit = {},
    onOpenBlockSettings: () -> Unit = {},
    // [新增] 刷新
    onRefresh: () -> Unit = {},
    isAudioOnly: Boolean = false,
    onToggleAudioOnly: () -> Unit = {},
    isBackgroundPlaybackEnabled: Boolean = true,
    onToggleBackgroundPlayback: () -> Unit = {},
    onOpenShutdownTimer: () -> Unit = {},
    onOpenPlayerInfo: () -> Unit = {},
    videoFitDesc: String = "",
    onVideoFitClick: () -> Unit = {},
    currentQualityDesc: String = "",
    onQualityClick: () -> Unit = {},
    showPipButton: Boolean = false,
    onEnterPip: () -> Unit = {}
) {
    var isControlsVisible by remember { mutableStateOf(true) }
    val palette = rememberLiveChromePalette()
    
    // 自动隐藏控制器
    LaunchedEffect(isControlsVisible, isPlaying) {
        if (isControlsVisible && isPlaying) {
            kotlinx.coroutines.delay(3000)
            isControlsVisible = false
        }
    }
    
    // 手势调节状态
    var gestureIcon by remember { mutableStateOf<androidx.compose.ui.graphics.vector.ImageVector?>(null) }
    var gestureText by remember { mutableStateOf("") }
    var isGestureVisible by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { isControlsVisible = !isControlsVisible },
                    onDoubleTap = { onPlayPause() }
                )
            }
            .pointerInput(Unit) {
                val screenHeight = size.height.toFloat()
                val screenWidth = size.width.toFloat()
                
                // 使用 Float 累积变化量，解决"不跟手"问题
                var volumeAccumulator = 0f
                var brightnessAccumulator = 0f
                
                var maxVolume = 0
                
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        if (offset.x < screenWidth / 2) {
                            // 左侧：亮度
                            val windowAttr = activity?.window?.attributes?.screenBrightness ?: -1f
                            brightnessAccumulator = if (windowAttr >= 0) {
                                windowAttr
                            } else {
                                try {
                                    val sysBrightness = android.provider.Settings.System.getInt(
                                        context.contentResolver,
                                        android.provider.Settings.System.SCREEN_BRIGHTNESS
                                    )
                                    sysBrightness / 255f
                                } catch (e: Exception) {
                                    0.5f
                                }
                            }
                        } else {
                            // 右侧：音量
                            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            volumeAccumulator = currentVol.toFloat()
                        }
                        isGestureVisible = true
                    },
                    onDragEnd = {
                        isGestureVisible = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        // 灵敏度基于屏幕高度: 拖动全屏高度 = 100% 调整
                        val sensitivity = screenHeight 
                        val delta = -dragAmount / sensitivity
                        
                        if (change.position.x < screenWidth / 2) {
                            // 调节亮度
                            // 亮度范围 0.0 ~ 1.0 (增加拖动系数使调节稍快一点，比如 1.5 倍)
                            val targetBrightness = (brightnessAccumulator + delta * 1.5f).coerceIn(0.01f, 1f)
                            brightnessAccumulator = targetBrightness // 更新累积值以保持连续性
                            
                            val lp = activity?.window?.attributes
                            lp?.screenBrightness = targetBrightness
                            activity?.window?.attributes = lp
                            
                            gestureIcon = CupertinoIcons.Filled.SunMax
                            gestureText = "${(targetBrightness * 100).toInt()}%"
                        } else {
                            // 调节音量 (maxVolume 比如 15)
                            if (maxVolume > 0) {
                                // 音量需要映射到 0~maxVolume
                                val targetVolFloat = (volumeAccumulator + delta * maxVolume * 1.2f).coerceIn(0f, maxVolume.toFloat())
                                volumeAccumulator = targetVolFloat
                                
                                val newVolInt = targetVolFloat.toInt()
                                val currentVolInt = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                
                                if (newVolInt != currentVolInt) {
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolInt, 0)
                                    // 注意：不要在这里重置 volumeAccumulator，否则会丢失小数部分导致卡顿
                                }
                                
                                gestureIcon = CupertinoIcons.Filled.SpeakerWave2
                                gestureText = "${(newVolInt * 100 / maxVolume)}%"
                            }
                        }
                    }
                )
            }
    ) {
        // 1. 中间手势提示
        androidx.compose.animation.AnimatedVisibility(
            visible = isGestureVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = palette.scrim.copy(alpha = 0.68f),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    palette.border
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (gestureIcon != null) {
                        Icon(gestureIcon!!, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(gestureText, color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        
        // 2. 顶部栏 (返回 + 标题)
        AnimatedVisibility(
            visible = showTopBar && isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                palette.scrim.copy(alpha = 0.92f),
                                palette.scrim.copy(alpha = 0.48f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerIconBtn(
                    icon = CupertinoIcons.Default.ChevronBackward,
                    onClick = onBack
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (subtitle.isNotBlank() && isFullscreen) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.76f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showPipButton) {
                        PlayerIconBtn(
                            icon = Icons.Outlined.PictureInPictureAlt,
                            onClick = onEnterPip,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    PlayerIconBtn(
                        icon = Icons.Outlined.MusicNote,
                        onClick = onToggleAudioOnly,
                        active = isAudioOnly,
                        modifier = Modifier.size(34.dp)
                    )
                    PlayerIconBtn(
                        icon = Icons.Outlined.PlayCircleOutline,
                        onClick = onToggleBackgroundPlayback,
                        active = isBackgroundPlaybackEnabled,
                        modifier = Modifier.size(34.dp)
                    )
                    PlayerIconBtn(
                        icon = Icons.Outlined.Timer,
                        onClick = onOpenShutdownTimer,
                        modifier = Modifier.size(34.dp)
                    )
                    PlayerIconBtn(
                        icon = Icons.Outlined.Info,
                        onClick = onOpenPlayerInfo,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
        
        // 3. 底部栏 (播放暂停 + 进度(直播无进度) + 全屏)
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                palette.scrim.copy(alpha = 0.44f),
                                palette.scrim.copy(alpha = 0.92f)
                            )
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 播放/暂停
                PlayerIconBtn(
                    icon = if (isPlaying) CupertinoIcons.Default.Pause else CupertinoIcons.Default.Play,
                    onClick = onPlayPause
                )
                
                Spacer(Modifier.width(16.dp))
                
                // [新增] 刷新按钮
                PlayerIconBtn(
                    icon = CupertinoIcons.Outlined.ArrowClockwise,
                    onClick = onRefresh
                )
                
                Spacer(Modifier.width(12.dp))
                
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerIconBtn(
                        icon = Icons.Outlined.Block,
                        onClick = onOpenBlockSettings
                    )
                    
                        Surface(
                            onClick = onToggleDanmaku,
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDanmakuEnabled) palette.accentSoft else palette.scrim.copy(alpha = 0.34f),
                            modifier = Modifier.height(32.dp)
                        ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                                Icon(
                                    imageVector = CupertinoIcons.Filled.TextBubble,
                                    contentDescription = null,
                                    tint = if (isDanmakuEnabled) palette.accentStrong else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(14.dp)
                                )
                            Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (isDanmakuEnabled) "弹幕 开" else "弹幕 关",
                                    color = if (isDanmakuEnabled) palette.accentStrong else Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                    }
                    
                    PlayerIconBtn(
                        icon = Icons.Outlined.Settings,
                        onClick = onOpenDanmakuSettings
                    )
                    
                    if (showChatToggle) {
                        Surface(
                            onClick = {
                                com.android.purebilibili.core.util.Logger.d("LivePlayerControls", "Chat toggle clicked, current visible: $isChatVisible")
                                onToggleChat()
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isChatVisible) palette.accentSoft else palette.scrim.copy(alpha = 0.34f),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = CupertinoIcons.Filled.BubbleLeft,
                                    contentDescription = null,
                                    tint = if (isChatVisible) palette.accentStrong else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "互动区",
                                    color = if (isChatVisible) palette.accentStrong else Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    if (videoFitDesc.isNotBlank()) {
                        Surface(
                            onClick = onVideoFitClick,
                            shape = RoundedCornerShape(16.dp),
                            color = palette.scrim.copy(alpha = 0.42f),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AspectRatio,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = videoFitDesc,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    if (currentQualityDesc.isNotBlank()) {
                        Surface(
                            onClick = onQualityClick,
                            shape = RoundedCornerShape(16.dp),
                            color = palette.scrim.copy(alpha = 0.42f),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = currentQualityDesc,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))
                
                // 全屏
                PlayerIconBtn(
                    icon = if (isFullscreen) CupertinoIcons.Default.ArrowDownRightAndArrowUpLeft else CupertinoIcons.Default.ArrowUpLeftAndArrowDownRight,
                    onClick = onToggleFullscreen
                )
            }
        }
    }
}
