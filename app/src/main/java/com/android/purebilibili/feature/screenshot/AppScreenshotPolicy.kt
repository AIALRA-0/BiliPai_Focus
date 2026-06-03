package com.android.purebilibili.feature.screenshot

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private const val TOP_RIGHT_LONG_PRESS_MILLIS = 600L
private val APP_SCREENSHOT_FILE_TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
private val ILLEGAL_FILE_CHAR_REGEX = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")

enum class AppScreenshotGestureMode(val value: Int, val label: String, val description: String) {
    TOP_RIGHT_TWO_FINGER_LONG_PRESS(
        value = 0,
        label = "右上角双指长按",
        description = "双指按住右上角约 0.6 秒，避免和系统三指截图冲突"
    ),
    THREE_FINGER_SWIPE_DOWN(
        value = 1,
        label = "三指下滑",
        description = "可能与部分国产系统截图手势冲突"
    ),
    DISABLED(
        value = 2,
        label = "关闭",
        description = "关闭手势触发，仅保留设置开关"
    );

    companion object {
        fun fromValue(value: Int): AppScreenshotGestureMode =
            entries.find { it.value == value } ?: TOP_RIGHT_TWO_FINGER_LONG_PRESS
    }
}

enum class AppScreenshotResult {
    Success,
    CaptureFailed,
    SaveFailed,
    Blocked
}

fun shouldOfferAppScreenshotShare(
    isLandscape: Boolean,
    result: AppScreenshotResult,
    hasShareUri: Boolean
): Boolean {
    return isLandscape && result == AppScreenshotResult.Success && hasShareUri
}

data class AppScreenshotPointerPosition(
    val x: Float,
    val y: Float
)

fun shouldTriggerTopRightTwoFingerLongPress(
    enabled: Boolean,
    blocked: Boolean,
    pointerCount: Int,
    pointerPositions: List<AppScreenshotPointerPosition>,
    elapsedMillis: Long,
    screenWidthPx: Float,
    hotZoneSizePx: Float,
    requiredLongPressMillis: Long = TOP_RIGHT_LONG_PRESS_MILLIS
): Boolean {
    if (!enabled || blocked) return false
    if (pointerCount != 2 || pointerPositions.size != 2) return false
    if (elapsedMillis < requiredLongPressMillis) return false
    if (screenWidthPx <= 0f || hotZoneSizePx <= 0f) return false

    val hotZoneLeft = screenWidthPx - hotZoneSizePx
    return pointerPositions.all { position ->
        position.x >= hotZoneLeft &&
            position.x <= screenWidthPx &&
            position.y >= 0f &&
            position.y <= hotZoneSizePx
    }
}

fun shouldTriggerThreeFingerSwipeDown(
    enabled: Boolean,
    blocked: Boolean,
    mode: AppScreenshotGestureMode,
    pointerCount: Int,
    totalDragX: Float,
    totalDragY: Float,
    triggerDistancePx: Float,
    maxHorizontalToVerticalRatio: Float
): Boolean {
    if (!enabled || blocked) return false
    if (mode != AppScreenshotGestureMode.THREE_FINGER_SWIPE_DOWN) return false
    if (pointerCount != 3) return false
    if (triggerDistancePx <= 0f || maxHorizontalToVerticalRatio < 0f) return false
    if (totalDragY < triggerDistancePx) return false

    return abs(totalDragX) <= abs(totalDragY) * maxHorizontalToVerticalRatio
}

fun buildAppScreenshotFileName(
    timestampMs: Long = System.currentTimeMillis(),
    prefix: String = "BiliPai"
): String {
    val safePrefix = prefix
        .replace(ILLEGAL_FILE_CHAR_REGEX, "_")
        .trim()
        .ifEmpty { "BiliPai" }
        .take(64)
    val timePart = APP_SCREENSHOT_FILE_TIME_FORMAT.format(
        Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault())
    )
    return "${safePrefix}_$timePart.png"
}
