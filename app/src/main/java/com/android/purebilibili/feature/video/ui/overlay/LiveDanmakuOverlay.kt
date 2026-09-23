package com.android.purebilibili.feature.video.ui.overlay

import android.graphics.Color as AndroidColor
import android.os.SystemClock
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.android.purebilibili.core.store.DanmakuSettings
import com.android.purebilibili.danmaku.engine.DanmakuEngine
import com.android.purebilibili.danmaku.engine.DanmakuItem
import com.android.purebilibili.danmaku.engine.DanmakuRenderConfig
import com.android.purebilibili.danmaku.engine.DanmakuRenderView
import com.android.purebilibili.feature.live.LiveDanmakuItem
import com.android.purebilibili.feature.video.danmaku.DanmakuTypeFilterSettings
import com.android.purebilibili.feature.video.danmaku.DEFAULT_DANMAKU_TEXT_SIZE_PX
import com.android.purebilibili.feature.video.danmaku.createBitmapDanmaku
import com.android.purebilibili.feature.video.danmaku.resolveDanmakuRenderLayerType
import com.android.purebilibili.feature.video.danmaku.resolveDanmakuPinnedDurationMillis
import com.android.purebilibili.feature.video.danmaku.resolveDanmakuScrollDurationMillis
import com.android.purebilibili.feature.video.danmaku.resolveDanmakuTypeface
import com.android.purebilibili.feature.video.danmaku.resolveDanmakuVisibleLineCount
import com.android.purebilibili.feature.video.danmaku.shouldBlockDanmakuByRules
import com.android.purebilibili.feature.video.danmaku.shouldDisplayStandardDanmaku
import java.util.ArrayDeque
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive

private const val LIVE_BATCH_INTERVAL_MS = 100L
private const val LIVE_HISTORY_MS = 20_000L
private const val MAX_ACTIVE_LIVE_DANMAKU = 160
private const val MAX_PENDING_LIVE_DANMAKU = 80
private const val MAX_PENDING_ITEMS_BEFORE_START = 48

/** Live danmaku renderer backed by an append-only, bounded session timeline. */
@Composable
fun LiveDanmakuOverlay(
    danmakuFlow: SharedFlow<LiveDanmakuItem>,
    displayArea: Float = 1f,
    danmakuSettings: DanmakuSettings = DanmakuSettings(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val safeDisplayArea = displayArea.takeIf(Float::isFinite)?.coerceIn(0.25f, 1f) ?: 1f
    val latestDanmakuSettings by rememberUpdatedState(danmakuSettings)
    var renderView by remember { mutableStateOf<DanmakuRenderView?>(null) }
    var engine by remember { mutableStateOf<DanmakuEngine?>(null) }
    var startTime by remember { mutableLongStateOf(0L) }
    var isStarted by remember { mutableStateOf(false) }
    val activeItems = remember { ArrayDeque<DanmakuItem>() }
    val pendingItems = remember { ArrayDeque<DanmakuItem>() }
    val pendingItemsBeforeStart = remember { ArrayDeque<LiveDanmakuItem>() }

    AndroidView(
        factory = { viewContext ->
            DanmakuRenderView(viewContext).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                renderView = this
                engine = this.engine
                startTime = SystemClock.elapsedRealtime()
                this.engine.start(0L)
                isStarted = true
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(safeDisplayArea),
        update = { view ->
            val textSize = DEFAULT_DANMAKU_TEXT_SIZE_PX *
                danmakuSettings.fontScale.coerceIn(0.3f, 2f)
            val strokeWidth = danmakuSettings.strokeWidth.coerceAtLeast(0f)
            view.engine.updateConfig(
                DanmakuRenderConfig(
                    alpha = (danmakuSettings.opacity.coerceIn(0f, 1f) * 255).toInt(),
                    textSizePx = textSize,
                    typeface = resolveDanmakuTypeface(danmakuSettings.fontWeight),
                    strokeWidthPx = strokeWidth,
                    scrollDurationMs = resolveDanmakuScrollDurationMillis(
                        scrollDurationSeconds = danmakuSettings.scrollDurationSeconds,
                        speedFactor = danmakuSettings.speed,
                        scrollFixedVelocity = danmakuSettings.scrollFixedVelocity,
                        viewportWidthPx = view.width
                    ),
                    lineHeightPx = textSize * danmakuSettings.lineHeight.coerceIn(0.8f, 2.2f),
                    lineCount = resolveDanmakuVisibleLineCount(
                        visibleHeightPx = view.height.toFloat(),
                        areaRatioHint = safeDisplayArea,
                        fontSize = textSize,
                        strokeWidth = strokeWidth,
                        strokeEnabled = strokeWidth > 0f,
                        lineHeight = danmakuSettings.lineHeight,
                        massiveMode = danmakuSettings.massiveMode
                    ),
                    pinnedDurationMs = resolveDanmakuPinnedDurationMillis(
                        danmakuSettings.staticDurationSeconds
                    )
                )
            )
        }
    )

    LaunchedEffect(engine, isStarted) {
        while (isActive) {
            val currentEngine = engine
            if (currentEngine != null && isStarted) {
                val currentTime = SystemClock.elapsedRealtime() - startTime
                val settings = latestDanmakuSettings
                val textSize = DEFAULT_DANMAKU_TEXT_SIZE_PX *
                    settings.fontScale.coerceIn(0.3f, 2f)

                while (pendingItemsBeforeStart.isNotEmpty()) {
                    pendingItems.addLast(
                        createLiveDanmakuItem(
                            item = pendingItemsBeforeStart.removeFirst(),
                            currentTime = currentTime,
                            context = context,
                            engine = currentEngine,
                            textSize = textSize,
                            fontWeight = settings.fontWeight,
                            staticDanmakuToScroll = settings.staticDanmakuToScroll
                        )
                    )
                }

                if (pendingItems.isNotEmpty()) {
                    val batch = ArrayList<DanmakuItem>(pendingItems.size)
                    while (pendingItems.isNotEmpty()) {
                        pendingItems.removeFirst().also {
                            batch += it
                            activeItems.addLast(it)
                        }
                    }
                    appendLiveDanmakuBatch(batch, currentEngine::append)
                }

                var trimBefore = currentTime - LIVE_HISTORY_MS
                while (activeItems.isNotEmpty() &&
                    (activeItems.first.showAtTime < trimBefore || activeItems.size > MAX_ACTIVE_LIVE_DANMAKU)
                ) {
                    val removed = activeItems.removeFirst()
                    trimBefore = maxOf(trimBefore, removed.showAtTime + 1L)
                }
                currentEngine.trimBefore(trimBefore)
            }
            delay(LIVE_BATCH_INTERVAL_MS)
        }
    }

    LaunchedEffect(danmakuFlow) {
        danmakuFlow.collect { item ->
            val settings = latestDanmakuSettings
            val typeFilter = DanmakuTypeFilterSettings(
                allowScroll = settings.allowScroll,
                allowTop = settings.allowTop,
                allowBottom = settings.allowBottom,
                allowColorful = settings.allowColorful,
                allowSpecial = settings.allowSpecial
            )
            if (item.isSuperChat && !settings.allowSpecial) {
                return@collect
            }
            if (!item.isSuperChat && !shouldDisplayStandardDanmaku(item.mode, item.color, typeFilter)) {
                return@collect
            }
            val userHash = item.uid.takeIf { it > 0L }?.toString().orEmpty()
            if (shouldBlockDanmakuByRules(item.text, settings.blockRules, userHash)) {
                return@collect
            }
            val currentEngine = engine
            if (!isStarted || currentEngine == null || startTime == 0L) {
                if (pendingItemsBeforeStart.size >= MAX_PENDING_ITEMS_BEFORE_START) {
                    pendingItemsBeforeStart.removeFirst()
                }
                pendingItemsBeforeStart.addLast(item)
                return@collect
            }

            val currentTime = SystemClock.elapsedRealtime() - startTime
            val textSize = DEFAULT_DANMAKU_TEXT_SIZE_PX * settings.fontScale.coerceIn(0.3f, 2f)
            val renderItem = createLiveDanmakuItem(
                item = item,
                currentTime = currentTime,
                context = context,
                engine = currentEngine,
                textSize = textSize,
                fontWeight = settings.fontWeight,
                staticDanmakuToScroll = settings.staticDanmakuToScroll
            )
            if (pendingItems.size >= MAX_PENDING_LIVE_DANMAKU) {
                releaseLiveDanmakuItem(
                    pendingItems.removeFirst(),
                    LiveDanmakuBitmapOwnership.APP_QUEUE_ONLY
                )
            }
            pendingItems.addLast(renderItem)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            engine?.close()
            renderView?.releaseRenderer()
            activeItems.forEach { item ->
                releaseLiveDanmakuItem(item, LiveDanmakuBitmapOwnership.TIMELINE_DISCARDED)
            }
            pendingItems.forEach { item ->
                releaseLiveDanmakuItem(item, LiveDanmakuBitmapOwnership.APP_QUEUE_ONLY)
            }
            activeItems.clear()
            pendingItems.clear()
            pendingItemsBeforeStart.clear()
            isStarted = false
            engine = null
            renderView = null
        }
    }
}

private fun createLiveDanmakuItem(
    item: LiveDanmakuItem,
    currentTime: Long,
    context: android.content.Context,
    engine: DanmakuEngine,
    textSize: Float,
    fontWeight: Int,
    staticDanmakuToScroll: Boolean
): DanmakuItem {
    val layerType = resolveDanmakuRenderLayerType(item.mode, staticDanmakuToScroll)
    val textColor = if (item.color == 0) AndroidColor.WHITE else (0xFF000000 or item.color.toLong()).toInt()
    val showAtTime = currentTime + 50L

    if (!shouldRenderLiveDanmakuAsBitmap(item.isSuperChat, item.emoticonUrl)) {
        return DanmakuItem().apply {
            text = item.text
            this.textColor = textColor
            this.layerType = layerType
            this.showAtTime = showAtTime
            isSelf = item.isSelf
        }
    }

    return createBitmapDanmaku(
        context = context,
        text = item.text,
        textColor = textColor,
        textSize = textSize,
        layerType = layerType,
        showAtTime = showAtTime,
        enableEmoticon = !item.emoticonUrl.isNullOrBlank(),
        typeface = resolveDanmakuTypeface(fontWeight),
        onUpdate = engine::invalidate
    ).apply { isSelf = item.isSelf }
}

private fun releaseLiveDanmakuItem(
    item: DanmakuItem,
    ownership: LiveDanmakuBitmapOwnership
) {
    if (!shouldManuallyRecycleLiveDanmakuBitmap(ownership)) return
    item.bitmap?.takeUnless { it.isRecycled }?.recycle()
    item.bitmap = null
}
