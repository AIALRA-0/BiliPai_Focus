package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.SponsorCategory
import com.android.purebilibili.data.model.response.SponsorProgressMarker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PlaybackDebugInfo(
    val resolution: String = "",
    val videoBitrate: String = "",
    val audioBitrate: String = "",
    val videoCodec: String = "",
    val audioCodec: String = "",
    val frameRate: String = "",
    val videoDecoder: String = "",
    val audioDecoder: String = "",
    val playbackState: String = "",
    val playWhenReady: String = "",
    val isPlaying: String = "",
    val firstFrame: String = "",
    val droppedFrames: String = "",
    val bandwidthEstimate: String = "",
    val lastVideoEvent: String = "",
    val lastAudioEvent: String = ""
)

data class DebugStatRow(
    val label: String,
    val value: String
)

data class CenterPlaybackButtonStyle(
    val containerColor: Color,
    val innerColor: Color,
    val borderColor: Color,
    val iconTint: Color
)

data class ProgressBarMarkerUiState(
    val segmentId: String,
    val category: String,
    val startFraction: Float,
    val endFraction: Float,
    val color: Color
)

internal enum class PlaybackIssueType {
    STUTTER,
    BLACK_SCREEN,
    NO_RESPONSE
}

enum class PlaybackUserActionType {
    PLAY,
    PAUSE
}

internal data class PlaybackIssueSignal(
    val type: PlaybackIssueType,
    val title: String,
    val message: String
)

private const val PLAYBACK_STUTTER_THRESHOLD_MS = 10_000L
private const val PLAYBACK_BLACK_SCREEN_THRESHOLD_MS = 5_000L
private const val PLAYBACK_NO_RESPONSE_THRESHOLD_MS = 2_000L

internal fun resolvePlaybackDebugRows(
    info: PlaybackDebugInfo
): List<DebugStatRow> {
    val candidates = listOf(
        DebugStatRow("Resolution", info.resolution),
        DebugStatRow("Video bitrate", info.videoBitrate),
        DebugStatRow("Audio bitrate", info.audioBitrate),
        DebugStatRow("Video codec", info.videoCodec),
        DebugStatRow("Audio codec", info.audioCodec),
        DebugStatRow("Frame rate", info.frameRate),
        DebugStatRow("Video decoder", info.videoDecoder),
        DebugStatRow("Audio decoder", info.audioDecoder),
        DebugStatRow("Playback state", info.playbackState),
        DebugStatRow("Play when ready", info.playWhenReady),
        DebugStatRow("Is playing", info.isPlaying),
        DebugStatRow("First frame", info.firstFrame),
        DebugStatRow("Dropped frames", info.droppedFrames),
        DebugStatRow("Bandwidth", info.bandwidthEstimate),
        DebugStatRow("Last video event", info.lastVideoEvent),
        DebugStatRow("Last audio event", info.lastAudioEvent)
    )
    return candidates.filter { it.value.isNotBlank() }
}

internal fun appendPlaybackDiagnosticEvent(
    current: List<String>,
    event: String,
    maxEntries: Int = 40
): List<String> {
    val normalizedEvent = event.trim()
    if (normalizedEvent.isBlank()) return current

    val next = buildList {
        addAll(current.takeLast((maxEntries - 1).coerceAtLeast(0)))
        if (current.lastOrNull() != normalizedEvent) {
            add(normalizedEvent)
        }
    }
    return next.takeLast(maxEntries)
}

internal fun resolvePlaybackDiagnosticEvents(
    current: List<String>,
    event: String,
    diagnosticsEnabled: Boolean,
    maxEntries: Int = 40
): List<String> {
    if (!diagnosticsEnabled) return current
    return appendPlaybackDiagnosticEvent(
        current = current,
        event = event,
        maxEntries = maxEntries
    )
}

internal fun shouldMonitorPlaybackIssues(
    diagnosticsEnabled: Boolean,
    bufferingStartedAtMs: Long,
    waitingFirstFrameStartedAtMs: Long,
    hasPendingUserAction: Boolean
): Boolean {
    return diagnosticsEnabled &&
        (bufferingStartedAtMs > 0L || waitingFirstFrameStartedAtMs > 0L || hasPendingUserAction)
}

internal fun buildPlaybackDiagnosticReport(
    title: String,
    bvid: String,
    cid: Long,
    currentPositionMs: Long,
    bufferedPositionMs: Long,
    debugInfo: PlaybackDebugInfo,
    recentEvents: List<String>,
    generatedAtMillis: Long = System.currentTimeMillis()
): String {
    val generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        .format(Date(generatedAtMillis))
    val debugRows = resolvePlaybackDebugRows(debugInfo)

    return buildString {
        appendLine("BiliPai Player Diagnostics")
        appendLine("Generated at: $generatedAt")
        if (title.isNotBlank()) appendLine("Title: $title")
        if (bvid.isNotBlank()) appendLine("BVID: $bvid")
        if (cid > 0L) appendLine("CID: $cid")
        appendLine("Position: ${FormatUtils.formatDuration(currentPositionMs)}")
        appendLine("Buffered: ${FormatUtils.formatDuration(bufferedPositionMs)}")
        if (debugRows.isNotEmpty()) {
            appendLine()
            appendLine("Playback stats:")
            debugRows.forEach { row ->
                appendLine("${row.label}: ${row.value}")
            }
        }
        if (recentEvents.isNotEmpty()) {
            appendLine()
            appendLine("Recent events:")
            recentEvents.forEach { event ->
                appendLine("- $event")
            }
        }
    }.trim()
}

internal fun resolvePlaybackIssueSignal(
    playbackState: Int,
    playWhenReady: Boolean,
    firstFrameRendered: Boolean,
    bufferingDurationMs: Long,
    waitingFirstFrameDurationMs: Long
): PlaybackIssueSignal? {
    if (!playWhenReady) return null

    if (playbackState == androidx.media3.common.Player.STATE_BUFFERING &&
        bufferingDurationMs >= PLAYBACK_STUTTER_THRESHOLD_MS
    ) {
        return PlaybackIssueSignal(
            type = PlaybackIssueType.STUTTER,
            title = "检测到播放卡顿",
            message = "播放器长时间处于缓冲状态，可以一键导出诊断日志。"
        )
    }

    if (playbackState == androidx.media3.common.Player.STATE_READY &&
        !firstFrameRendered &&
        waitingFirstFrameDurationMs >= PLAYBACK_BLACK_SCREEN_THRESHOLD_MS
    ) {
        return PlaybackIssueSignal(
            type = PlaybackIssueType.BLACK_SCREEN,
            title = "检测到疑似黑屏",
            message = "播放器已经准备完成但首帧迟迟未出现，可以一键导出诊断日志。"
        )
    }

    return null
}

internal fun resolvePlaybackActionNoResponseSignal(
    actionType: PlaybackUserActionType,
    actionAgeMs: Long,
    hasPlayerResponded: Boolean
): PlaybackIssueSignal? {
    if (hasPlayerResponded || actionAgeMs < PLAYBACK_NO_RESPONSE_THRESHOLD_MS) return null

    val actionLabel = when (actionType) {
        PlaybackUserActionType.PLAY -> "播放"
        PlaybackUserActionType.PAUSE -> "暂停"
    }
    return PlaybackIssueSignal(
        type = PlaybackIssueType.NO_RESPONSE,
        title = "检测到点击无响应",
        message = "用户点击${actionLabel}后播放器状态没有及时变化，可以一键导出诊断日志。"
    )
}

internal fun resolveCenterPlaybackButtonStyle(
    isDarkTheme: Boolean
): CenterPlaybackButtonStyle {
    return if (isDarkTheme) {
        CenterPlaybackButtonStyle(
            containerColor = Color.Black.copy(alpha = 0.44f),
            innerColor = Color.White.copy(alpha = 0.16f),
            borderColor = Color.White.copy(alpha = 0.22f),
            iconTint = Color.White
        )
    } else {
        CenterPlaybackButtonStyle(
            containerColor = Color.Black.copy(alpha = 0.28f),
            innerColor = Color.Black.copy(alpha = 0.18f),
            borderColor = Color.Black.copy(alpha = 0.16f),
            iconTint = Color.White
        )
    }
}

internal fun resolveSponsorProgressBarMarkers(
    durationMs: Long,
    markers: List<SponsorProgressMarker>
): List<ProgressBarMarkerUiState> {
    if (durationMs <= 0L || markers.isEmpty()) return emptyList()
    return markers.mapNotNull { marker ->
        val startFraction = (marker.startTimeMs.toFloat() / durationMs).coerceIn(0f, 1f)
        val endFraction = (marker.endTimeMs.toFloat() / durationMs).coerceIn(0f, 1f)
        if (endFraction <= startFraction) {
            null
        } else {
            ProgressBarMarkerUiState(
                segmentId = marker.segmentId,
                category = marker.category,
                startFraction = startFraction,
                endFraction = endFraction,
                color = resolveSponsorProgressMarkerColor(marker.category)
            )
        }
    }
}

private fun resolveSponsorProgressMarkerColor(category: String): Color {
    return if (category == SponsorCategory.SPONSOR) {
        Color(0xFFFF8A65)
    } else {
        Color(0xFFFDE68A).copy(alpha = 0.72f)
    }
}
