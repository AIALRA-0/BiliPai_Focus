package com.android.bilipai.tv.ui

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.android.purebilibili.core.player.SharedPlaybackSession
import com.android.purebilibili.core.player.SharedPlaybackState
import com.android.purebilibili.danmaku.engine.DanmakuRenderConfig
import com.android.purebilibili.danmaku.engine.DanmakuRenderView
import com.android.purebilibili.danmaku.engine.DanmakuWindow
import com.android.purebilibili.danmaku.parser.DanmakuParser
import com.android.purebilibili.data.repository.DanmakuContentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/** 盒子友好的默认密度：半屏显示带，降低低性能设备的渲染与轨道压力。 */
internal const val TV_DANMAKU_DEFAULT_DISPLAY_AREA = 0.5f

internal const val TV_DANMAKU_BASE_TEXT_SIZE_DP = 20f

private const val TV_DANMAKU_LINE_HEIGHT_MULTIPLIER = 1.6f
private const val TV_DANMAKU_STROKE_WIDTH_PX = 1.5f
private const val TV_DANMAKU_DEFAULT_OPACITY = 0.85f
private const val TV_DANMAKU_SCROLL_DURATION_MS = 7_000L
private const val TV_DANMAKU_PINNED_DURATION_MS = 4_000L
private const val TV_DANMAKU_SEEK_THRESHOLD_MS = 2_000L
private const val TV_DANMAKU_DRIFT_SYNC_INTERVAL_MS = 5_000L

/**
 * TV 弹幕渲染设置：遥控器档位选择（不提供连续滑杆），持久化于 [com.android.bilipai.tv.TvPreferences]。
 * 默认值即此前的写死值，行为对老用户不变。public：经 public 的 TvAppViewModel/TvUiState 暴露。
 */
data class TvDanmakuSettings(
    val displayArea: Float = TV_DANMAKU_DEFAULT_DISPLAY_AREA,
    val textSizeDp: Float = TV_DANMAKU_BASE_TEXT_SIZE_DP,
    val opacity: Float = TV_DANMAKU_DEFAULT_OPACITY,
    val speedScale: Float = 1f,
) {
    companion object {
        val TEXT_SIZE_OPTIONS = listOf(16f, 20f, 26f)
        val AREA_OPTIONS = listOf(0.25f, 0.5f, 0.75f)
        val OPACITY_OPTIONS = listOf(0.6f, 0.85f, 1f)
        val SPEED_OPTIONS = listOf(0.8f, 1f, 1.25f)
    }
}

internal fun tvDanmakuSizeLabel(textSizeDp: Float): String = when (textSizeDp) {
    in 0f..17f -> "小"
    in 17f..23f -> "标准"
    else -> "大"
}

internal fun tvDanmakuAreaLabel(displayArea: Float): String = when {
    displayArea <= 0.25f -> "紧凑（1/4 屏）"
    displayArea <= 0.5f -> "标准（半屏）"
    else -> "宽松（3/4 屏）"
}

internal fun tvDanmakuOpacityLabel(opacity: Float): String = when {
    opacity <= 0.7f -> "较淡"
    opacity <= 0.9f -> "标准"
    else -> "不透明"
}

internal fun tvDanmakuSpeedLabel(speedScale: Float): String = when {
    speedScale <= 0.9f -> "慢 0.8x"
    speedScale <= 1.05f -> "标准 1.0x"
    else -> "快 1.25x"
}

internal fun resolveTvDanmakuTextSizePx(density: Float, textSizeDp: Float = TV_DANMAKU_BASE_TEXT_SIZE_DP): Float =
    textSizeDp * density

/** TV 弹幕行数下限（与手机端 resolveDanmakuMinimumVisibleLines 对齐）。 */
internal fun resolveTvDanmakuMinimumLines(displayArea: Float): Int = when {
    displayArea <= 0.25f -> 2
    displayArea <= 0.5f -> 3
    displayArea <= 0.75f -> 5
    else -> 6
}

/** 半屏带内的可见行数：估算行距并受视口行预算约束（镜像手机端算法）。 */
internal fun resolveTvDanmakuLineCount(viewportHeightPx: Int, displayArea: Float, textSizePx: Float): Int {
    val area = displayArea.coerceIn(0.25f, 1f)
    val visibleHeightPx = viewportHeightPx * area
    if (visibleHeightPx <= 0f || textSizePx <= 0f) return 0
    val lineHeightPx = textSizePx * TV_DANMAKU_LINE_HEIGHT_MULTIPLIER
    val estimatedLineHeight = (textSizePx + TV_DANMAKU_STROKE_WIDTH_PX + 12f) * TV_DANMAKU_LINE_HEIGHT_MULTIPLIER
    val estimated = if (estimatedLineHeight > 0f) (visibleHeightPx / estimatedLineHeight).toInt() else 0
    val budget = if (visibleHeightPx >= lineHeightPx) {
        ((visibleHeightPx - lineHeightPx) / lineHeightPx).toInt() + 1
    } else {
        0
    }
    return estimated.coerceAtLeast(resolveTvDanmakuMinimumLines(area)).coerceAtMost(budget).coerceAtLeast(0)
}

private fun buildTvDanmakuRenderConfig(
    viewportHeightPx: Int,
    density: Float,
    playSpeedPercent: Int,
    settings: TvDanmakuSettings = TvDanmakuSettings(),
): DanmakuRenderConfig {
    val textSizePx = resolveTvDanmakuTextSizePx(density, settings.textSizeDp)
    return DanmakuRenderConfig(
        alpha = (settings.opacity * 255).toInt(),
        textSizePx = textSizePx,
        strokeWidthPx = TV_DANMAKU_STROKE_WIDTH_PX,
        strokeColor = android.graphics.Color.BLACK,
        // 速度档位越大滚动越快：时长按 speedScale 反比缩放，钳制在引擎可接受范围。
        scrollDurationMs = (TV_DANMAKU_SCROLL_DURATION_MS / settings.speedScale).toLong().coerceIn(2_000L, 30_000L),
        lineHeightPx = textSizePx * TV_DANMAKU_LINE_HEIGHT_MULTIPLIER,
        lineMarginPx = 0f,
        lineCount = resolveTvDanmakuLineCount(viewportHeightPx, settings.displayArea, textSizePx),
        topMarginPx = 0f,
        bottomMarginPx = 0f,
        pinnedDurationMs = TV_DANMAKU_PINNED_DURATION_MS,
        playSpeedPercent = playSpeedPercent.coerceIn(10, 400),
    )
}

/**
 * TV 弹幕覆盖层。渲染器、解析器与读取缓存全部来自共享模块（:danmaku-engine、:core-data），
 * 这里只做遥控器语境的编排：被动覆盖层不抢焦点，播放/暂停/seek/倍速同步到引擎。
 * 高级弹幕（BAS）、指令弹幕与屏蔽词规则是手机端 Overlay 能力，TV 暂不渲染。
 */
@Composable
internal fun TvDanmakuOverlay(
    session: SharedPlaybackSession,
    state: SharedPlaybackState,
    cid: Long,
    settings: TvDanmakuSettings = TvDanmakuSettings(),
    modifier: Modifier = Modifier,
) {
    var renderView by remember { mutableStateOf<DanmakuRenderView?>(null) }
    var loadedCid by remember { mutableLongStateOf(0L) }
    val player = session.player

    AndroidView(
        factory = { context ->
            DanmakuRenderView(context).apply {
                // 被动覆盖层：触摸与焦点全部让给遥控器导航
                setRendererTouchable(false)
                isFocusable = false
                isClickable = false
                isLongClickable = false
                descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                renderView = this
            }
        },
        onRelease = { view ->
            if (renderView === view) renderView = null
            view.releaseRenderer()
        },
        modifier = modifier,
    )

    // 加载当前 cid 的全部分段。duration 变化（切 P）会触发重载；仓库层有字节缓存，重载代价低。
    // segmentIndices 仅作引擎窗口簿记，TV 走 replaceWindow 全量时间线，不依赖其连续性。
    LaunchedEffect(renderView, cid, state.durationMs) {
        val view = renderView ?: return@LaunchedEffect
        if (cid <= 0L || state.durationMs <= 0L) return@LaunchedEffect
        val engine = view.engine
        if (loadedCid != cid) engine.clear()
        val segments = withContext(Dispatchers.IO) {
            DanmakuContentRepository.getDanmakuSegments(cid, state.durationMs)
        }
        val parsed = withContext(Dispatchers.Default) { DanmakuParser.parseProtobuf(segments) }
        if (parsed.serverDisabled || parsed.standardList.isEmpty()) return@LaunchedEffect
        engine.updateConfig(
            buildTvDanmakuRenderConfig(
                viewportHeightPx = view.height,
                density = view.resources.displayMetrics.density,
                playSpeedPercent = (player.playbackParameters.speed * 100).toInt(),
                settings = settings,
            )
        )
        engine.replaceWindow(
            DanmakuWindow(
                anchorSegment = 1,
                segmentIndices = (1..segments.size).toList(),
                items = parsed.standardList,
            ),
            player.currentPosition,
        )
        loadedCid = cid
        if (player.isPlaying) engine.start(player.currentPosition) else engine.pause()
    }

    // 播放/暂停联动
    LaunchedEffect(renderView, loadedCid, cid, state.playing) {
        val engine = renderView?.engine ?: return@LaunchedEffect
        if (loadedCid == 0L || loadedCid != cid) return@LaunchedEffect
        if (state.playing) {
            engine.synchronizeTo(player.currentPosition)
            engine.start(player.currentPosition)
        } else {
            engine.pause()
        }
    }

    // seek 检测：位置突跳超过阈值时重建引擎时间线锚点（首帧位置只记录不触发）
    var lastPositionMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(renderView, loadedCid, cid, state.positionMs) {
        val engine = renderView?.engine ?: return@LaunchedEffect
        if (loadedCid == 0L || loadedCid != cid) return@LaunchedEffect
        val current = state.positionMs
        if (lastPositionMs != 0L && kotlin.math.abs(current - lastPositionMs) > TV_DANMAKU_SEEK_THRESHOLD_MS) {
            engine.seekTo(current)
        }
        lastPositionMs = current
    }

    // 漂移校正与倍速配置跟进；引擎对相同配置幂等
    LaunchedEffect(renderView, loadedCid, cid, state.playing) {
        val view = renderView ?: return@LaunchedEffect
        if (!state.playing || loadedCid == 0L || loadedCid != cid) return@LaunchedEffect
        while (isActive) {
            delay(TV_DANMAKU_DRIFT_SYNC_INTERVAL_MS)
            if (!isActive) break
            view.engine.synchronizeTo(player.currentPosition)
            view.engine.updateConfig(
                buildTvDanmakuRenderConfig(
                    viewportHeightPx = view.height,
                    density = view.resources.displayMetrics.density,
                    playSpeedPercent = (player.playbackParameters.speed * 100).toInt(),
                    settings = settings,
                )
            )
        }
    }

    // 设置档位变更热更新：引擎幂等，立即重放当前渲染配置（含暂停态）。
    LaunchedEffect(renderView, loadedCid, cid, settings) {
        val view = renderView ?: return@LaunchedEffect
        if (loadedCid == 0L || loadedCid != cid) return@LaunchedEffect
        view.engine.updateConfig(
            buildTvDanmakuRenderConfig(
                viewportHeightPx = view.height,
                density = view.resources.displayMetrics.density,
                playSpeedPercent = (player.playbackParameters.speed * 100).toInt(),
                settings = settings,
            )
        )
    }
}
