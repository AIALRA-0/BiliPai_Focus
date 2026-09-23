// File: feature/video/ui/overlay/TopControlBar.kt
package com.android.purebilibili.feature.video.ui.overlay
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.currentStateAsState
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import com.android.purebilibili.core.ui.AppIcons
import com.android.purebilibili.core.ui.components.AppIconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Comment
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun shouldShowDislikeInTopControlBar(widthDp: Int): Boolean = widthDp >= 980
internal fun shouldShowInteractiveActionsInTopControlBar(
    showFullscreenActionItems: Boolean
): Boolean = showFullscreenActionItems
/**
 * 顶栏是否加状态栏 padding。
 *
 * 以 [statusBarVisible] 为准；全屏时系统栏通常已隐藏，应传 false。
 * [isFullscreen] 仅作兼容旧调用：未显式传 statusBarVisible 时，全屏仍 pad
 *（隐藏栏时 inset 为 0，无视觉影响）。
 */
internal fun shouldApplyStatusBarPaddingToTopControlBar(
    isFullscreen: Boolean = false,
    statusBarVisible: Boolean = isFullscreen,
): Boolean = com.android.purebilibili.feature.video.screen
    .shouldApplyStatusBarPaddingToVideoPlayerChrome(statusBarVisible = statusBarVisible)
internal fun shouldPollTopControlBarClock(
    showCurrentTime: Boolean,
    hostLifecycleStarted: Boolean
): Boolean = showCurrentTime && hostLifecycleStarted
internal fun shouldPollTopControlBarBattery(
    showBatteryLevel: Boolean,
    hostLifecycleStarted: Boolean
): Boolean = showBatteryLevel && hostLifecycleStarted

internal enum class BatteryChargeTone {
    CRITICAL,
    LOW,
    NORMAL
}

internal data class LandscapeBatteryVisualState(
    val percent: Int,
    val fillFraction: Float,
    val chargeTone: BatteryChargeTone
) {
    val displayText: String
        get() = "$percent%"
}

internal fun resolveLandscapeBatteryVisualState(percent: Int?): LandscapeBatteryVisualState? {
    val normalizedPercent = percent?.coerceIn(0, 100) ?: return null
    val chargeTone = when {
        normalizedPercent <= 15 -> BatteryChargeTone.CRITICAL
        normalizedPercent <= 35 -> BatteryChargeTone.LOW
        else -> BatteryChargeTone.NORMAL
    }
    return LandscapeBatteryVisualState(
        percent = normalizedPercent,
        fillFraction = normalizedPercent / 100f,
        chargeTone = chargeTone
    )
}

internal data class LandscapeTopStatusInfo(
    val battery: LandscapeBatteryVisualState? = null,
    val currentTimeText: String? = null
) {
    val isVisible: Boolean
        get() = battery != null || !currentTimeText.isNullOrBlank()
}

internal fun resolveLandscapeTopStatusInfo(
    showBatteryLevel: Boolean,
    batteryLevelPercent: Int?,
    showCurrentTime: Boolean,
    currentTimeText: String
): LandscapeTopStatusInfo {
    return LandscapeTopStatusInfo(
        battery = if (showBatteryLevel) resolveLandscapeBatteryVisualState(batteryLevelPercent) else null,
        currentTimeText = currentTimeText.takeIf { showCurrentTime }
    )
}

internal fun shouldShowLandscapeMetaRow(
    hasStatusInfo: Boolean,
    onlineCount: String
): Boolean = hasStatusInfo || onlineCount.isNotBlank()

/**
 * Top Control Bar Component
 * 
 * Redesigned to match official Bilibili landscape layout:
 * - Left: Back button, Title (Marquee), Online count
 * - Right: Action buttons (Like, Dislike, Coin, Share, Cast, More)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopControlBar(
    title: String,
    onlineCount: String = "",
    isFullscreen: Boolean,
    /** 系统状态栏是否仍显示；显示时顶栏必须 statusBarsPadding。 */
    statusBarVisible: Boolean = isFullscreen,
    showBatteryLevel: Boolean = false,
    showCurrentTime: Boolean = true,
    showInteractiveActions: Boolean = true,
    onBack: () -> Unit,
    // Interactions
    isLiked: Boolean = false,
    isCoined: Boolean = false,
    allowDislikeAction: Boolean = true,
    onLikeClick: () -> Unit = {},
    onDislikeClick: () -> Unit = {},
    onCoinClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    onCastClick: () -> Unit = {}, // Added Cast callback
    showCastButton: Boolean = true,
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()
    val hostLifecycleStarted = lifecycleState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
    val layoutPolicy = remember(configuration.screenWidthDp) {
        resolveTopControlBarLayoutPolicy(
            widthDp = configuration.screenWidthDp
        )
    }
    val showDislikeAction = remember(configuration.screenWidthDp, allowDislikeAction, showInteractiveActions) {
        allowDislikeAction &&
            showInteractiveActions &&
            shouldShowDislikeInTopControlBar(widthDp = configuration.screenWidthDp)
    }
    val showInteractiveActionGroup = remember(showInteractiveActions) {
        shouldShowInteractiveActionsInTopControlBar(
            showFullscreenActionItems = showInteractiveActions
        )
    }
    val currentTimeText by produceState(initialValue = formatCurrentTime(), showCurrentTime, hostLifecycleStarted) {
        if (!shouldPollTopControlBarClock(showCurrentTime, hostLifecycleStarted)) {
            value = formatCurrentTime()
            return@produceState
        }
        while (true) {
            value = formatCurrentTime()
            val now = System.currentTimeMillis()
            val nextMinuteDelay = (60_000L - (now % 60_000L)).coerceAtLeast(1_000L)
            delay(nextMinuteDelay)
        }
    }
    val batteryLevelPercent by produceState<Int?>(initialValue = null, showBatteryLevel, hostLifecycleStarted) {
        if (!shouldPollTopControlBarBattery(showBatteryLevel, hostLifecycleStarted)) {
            value = null
            return@produceState
        }
        while (true) {
            value = resolveBatteryLevelPercent(context)
            delay(30_000L)
        }
    }
    val statusInfo = remember(currentTimeText, batteryLevelPercent, showBatteryLevel, showCurrentTime) {
        resolveLandscapeTopStatusInfo(
            showBatteryLevel = showBatteryLevel,
            batteryLevelPercent = batteryLevelPercent,
            showCurrentTime = showCurrentTime,
            currentTimeText = currentTimeText
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (
                    shouldApplyStatusBarPaddingToTopControlBar(
                        isFullscreen = isFullscreen,
                        statusBarVisible = statusBarVisible,
                    )
                ) {
                    Modifier.statusBarsPadding()
                } else {
                    Modifier
                }
            )
            .padding(
                horizontal = layoutPolicy.horizontalPaddingDp.dp,
                vertical = layoutPolicy.verticalPaddingDp.dp
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Left Section: Back & Info ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f) // Text takes remaining space
            ) {
                // Back Button
                AppIconButton(
                    onClick = onBack,
                    modifier = Modifier.size(layoutPolicy.buttonSizeDp.dp)
                ) {
                    AppIcon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = "Back", 
                        tint = Color.White,
                        modifier = Modifier.size(layoutPolicy.iconSizeDp.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(layoutPolicy.backToTitleSpacingDp.dp))

                // 标题与右侧图标保持同一行
                AppText(
                    text = title,
                    color = Color.White,
                    fontSize = layoutPolicy.titleFontSp.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .basicMarquee() // Marquee effect for long text
                )
            }
            
            Spacer(modifier = Modifier.width(layoutPolicy.sectionGapDp.dp)) // Space between text and actions
            
            // --- Right Section: Actions ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(layoutPolicy.actionSpacingDp.dp)
            ) {
                if (showInteractiveActionGroup) {
                    // Like
                    ActionIcon(
                        icon = if (isLiked) Icons.Rounded.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "点赞",
                        isActive = isLiked,
                        onClick = onLikeClick,
                        buttonSizeDp = layoutPolicy.buttonSizeDp,
                        iconSizeDp = layoutPolicy.iconSizeDp
                    )

                    if (showDislikeAction) {
                        // Dislike
                        ActionIcon(
                            icon = Icons.Outlined.ThumbDown,
                            contentDescription = "不喜欢",
                            isActive = false,
                            onClick = onDislikeClick,
                            buttonSizeDp = layoutPolicy.buttonSizeDp,
                            iconSizeDp = layoutPolicy.iconSizeDp
                        )
                    }

                    // Coin
                    ActionIcon(
                        icon = AppIcons.BiliCoin,
                        contentDescription = "投币",
                        isActive = isCoined,
                        onClick = onCoinClick,
                        buttonSizeDp = layoutPolicy.buttonSizeDp,
                        iconSizeDp = layoutPolicy.iconSizeDp
                    )

                    // Share
                    ActionIcon(
                        icon = Icons.Outlined.Share,
                        contentDescription = "分享",
                        isActive = false,
                        onClick = onShareClick,
                        buttonSizeDp = layoutPolicy.buttonSizeDp,
                        iconSizeDp = layoutPolicy.iconSizeDp
                    )
                }

                if (showCastButton) {
                    ActionIcon(
                        icon = Icons.Outlined.Cast,
                        contentDescription = "投屏",
                        isActive = false,
                        onClick = onCastClick,
                        buttonSizeDp = layoutPolicy.buttonSizeDp,
                        iconSizeDp = layoutPolicy.iconSizeDp
                    )
                }

                ActionIcon(
                    icon = Icons.Outlined.Comment,
                    contentDescription = "评论区",
                    isActive = false,
                    onClick = onCommentClick,
                    buttonSizeDp = layoutPolicy.buttonSizeDp,
                    iconSizeDp = layoutPolicy.iconSizeDp
                )
                
                // More (Three dots)
                AppIconButton(
                    onClick = onMoreClick,
                    modifier = Modifier.size(layoutPolicy.buttonSizeDp.dp)
                ) {
                    AppIcon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "更多",
                        tint = Color.White,
                        modifier = Modifier.size(layoutPolicy.iconSizeDp.dp)
                    )
                }
            }
        }

        if (shouldShowLandscapeMetaRow(statusInfo.isVisible, onlineCount)) {
            Column(
                modifier = Modifier.padding(
                    start = layoutPolicy.onlineCountStartPaddingDp.dp,
                    top = layoutPolicy.timeBottomSpacingDp.dp
                ),
                verticalArrangement = Arrangement.spacedBy(layoutPolicy.onlineCountTopPaddingDp.dp)
            ) {
                statusInfo.battery?.let { battery ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BatteryStatusIcon(
                            state = battery,
                            modifier = Modifier.size(
                                width = (layoutPolicy.timeFontSp + 14).dp,
                                height = (layoutPolicy.timeFontSp + 4).dp
                            )
                        )
                        AppText(
                            text = battery.displayText,
                            color = resolveBatteryStatusTint(battery).copy(alpha = 0.95f),
                            fontSize = layoutPolicy.timeFontSp.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        statusInfo.currentTimeText?.let { timeText ->
                            AppText(
                                text = timeText,
                                color = Color.White.copy(alpha = 0.88f),
                                fontSize = layoutPolicy.timeFontSp.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } ?: statusInfo.currentTimeText?.let { timeText ->
                    AppText(
                        text = timeText,
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = layoutPolicy.timeFontSp.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (onlineCount.isNotEmpty()) {
                    AppText(
                        text = onlineCount,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = layoutPolicy.onlineCountFontSp.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

private fun formatCurrentTime(): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date())
}

private fun resolveBatteryLevelPercent(context: Context): Int? {
    return runCatching {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level < 0 || scale <= 0) return@runCatching null
        (level * 100f / scale).toInt().coerceIn(0, 100)
    }.getOrNull()
}

@Composable
private fun BatteryStatusIcon(
    state: LandscapeBatteryVisualState,
    modifier: Modifier = Modifier
) {
    val tint = resolveBatteryStatusTint(state)
    Canvas(modifier = modifier) {
        val strokeWidth = size.height * 0.12f
        val terminalWidth = size.width * 0.09f
        val bodyWidth = size.width - terminalWidth - strokeWidth
        val bodyHeight = size.height * 0.76f
        val bodyTop = (size.height - bodyHeight) / 2f
        val cornerRadius = CornerRadius(bodyHeight * 0.18f, bodyHeight * 0.18f)
        val innerPadding = strokeWidth * 1.1f
        val fillWidth = ((bodyWidth - innerPadding * 2f) * state.fillFraction).coerceAtLeast(0f)

        drawRoundRect(
            color = Color.White.copy(alpha = 0.92f),
            topLeft = Offset(0f, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = cornerRadius,
            style = Stroke(width = strokeWidth)
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.92f),
            topLeft = Offset(bodyWidth + strokeWidth * 0.35f, size.height * 0.34f),
            size = Size(terminalWidth, size.height * 0.28f),
            cornerRadius = CornerRadius(terminalWidth / 2f, terminalWidth / 2f)
        )
        if (fillWidth > 0f) {
            drawRoundRect(
                color = tint,
                topLeft = Offset(innerPadding, bodyTop + innerPadding),
                size = Size(fillWidth, bodyHeight - innerPadding * 2f),
                cornerRadius = CornerRadius(cornerRadius.x / 1.5f, cornerRadius.y / 1.5f)
            )
        }
    }
}

private fun resolveBatteryStatusTint(
    state: LandscapeBatteryVisualState
): Color {
    return when (state.chargeTone) {
        BatteryChargeTone.CRITICAL -> Color(0xFFFF5A5F)
        BatteryChargeTone.LOW -> Color(0xFFFFB347)
        BatteryChargeTone.NORMAL -> Color.White.copy(alpha = 0.92f)
    }
}

@Composable
private fun ActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
    buttonSizeDp: Int = 32,
    iconSizeDp: Int = 24
) {
    AppIconButton(
        onClick = onClick,
        modifier = Modifier.size(buttonSizeDp.dp)
    ) {
        AppIcon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
            modifier = Modifier.size(iconSizeDp.dp)
        )
    }
}
