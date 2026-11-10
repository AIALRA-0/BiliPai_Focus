package com.android.purebilibili.feature.video.danmaku

import coil3.request.allowHardware

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextPaint
import coil3.toBitmap
import coil3.imageLoader
import coil3.request.ImageRequest
import com.android.purebilibili.danmaku.engine.DanmakuItem
import com.android.purebilibili.feature.live.components.DanmakuEmoticonMapper

/**
 * 创建支持表情的 BitmapDanmaku
 * 
 * 修复记录:
 * - 添加 Bitmap 最大尺寸限制 (2048px) 防止 OOM
 * - 添加 try-catch 异常处理
 */
fun createBitmapDanmaku(
    context: Context,
    text: String,
    textColor: Int,
    textSize: Float,
    layerType: Int,
    showAtTime: Long,
    enableEmoticon: Boolean = true,
    typeface: Typeface = Typeface.DEFAULT,
    // 消息级表情（直播 emots、整条表情弹幕）优先于全局映射表。
    extraEmotes: Map<String, String> = emptyMap(),
    emoteAspectRatios: Map<String, Float> = emptyMap(),
    // Worker rasterization defers image callbacks until the base bitmap is fully painted.
    deferredImageRequests: MutableList<() -> Unit>? = null,
    onUpdate: () -> Unit
): DanmakuItem {
    // 1. 解析文本
    val segments = parseSegments(text, enableEmoticon, extraEmotes)
    
    // 2. 测量尺寸
    val paint = TextPaint().apply {
        this.textSize = textSize
        this.color = textColor
        this.typeface = typeface
        this.isAntiAlias = true
        setShadowLayer(
            (textSize / 8f).coerceIn(2f, 5f),
            0f,
            0f,
            Color.BLACK
        )
    }
    
    val fontMetrics = paint.fontMetrics
    val textHeight = fontMetrics.descent - fontMetrics.ascent
    // 图片高度稍微大一点 (1.3倍)
    val iconSize = (textSize * 1.3f).toInt()
    fun emoteWidth(key: String): Int {
        val ratio = emoteAspectRatios[key]?.takeIf { it.isFinite() && it > 0f } ?: 1f
        return (iconSize * ratio.coerceIn(0.25f, 4f)).toInt().coerceAtLeast(1)
    }
    val contentHeight = maxOf(textHeight, iconSize.toFloat())
    
    var totalWidth = 0f
    segments.forEach { seg ->
        if (seg.isEmoticon) {
            totalWidth += emoteWidth(seg.content) + 4 // 图片 + 间距
        } else {
            totalWidth += paint.measureText(seg.content)
        }
    }
    
    // 3. 创建 Bitmap (添加尺寸限制防止 OOM)
    val maxWidth = 2048 // 最大宽度限制
    val widthInt = totalWidth.toInt().coerceIn(1, maxWidth)
    val heightInt = (contentHeight * 1.2f).toInt().coerceIn(1, 256) // 高度也加限制
    
    // 如果文本被截断，记录日志
    if (totalWidth > maxWidth) {
        android.util.Log.w("Probe", "⚠️ Danmaku bitmap truncated: ${totalWidth.toInt()} -> $maxWidth")
    }
    
    val bitmap: Bitmap = try {
        Bitmap.createBitmap(widthInt, heightInt, Bitmap.Config.ARGB_8888)
    } catch (e: OutOfMemoryError) {
        android.util.Log.e("Probe", "❌ OOM creating bitmap, using fallback")
        // 返回一个极小的占位 Bitmap
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }
    
    val canvas = Canvas(bitmap)
    
    // 4. 绘制内容
    try {
        drawContent(context, canvas, segments, paint, iconSize, heightInt, onUpdate, bitmap, deferredImageRequests, extraEmotes, ::emoteWidth)
    } catch (e: Exception) {
        android.util.Log.e("Probe", "❌ Draw content failed: ${e.message}")
    }
    
    // 5. 创建引擎无关的位图弹幕，渲染适配器负责转换底层数据类型。
    return DanmakuItem().apply {
        this.bitmap = bitmap
        this.bitmapWidth = widthInt.toFloat()
        this.bitmapHeight = heightInt.toFloat()
        this.showAtTime = showAtTime
        this.layerType = layerType
    }
}

private fun parseSegments(text: String, enableEmoticon: Boolean, extraEmotes: Map<String, String>): List<DanmakuContentSegment> =
    resolveDanmakuContentSegments(text, enableEmoticon, extraEmotes, DanmakuEmoticonMapper.emoticonMap.value)

private fun drawContent(
    context: Context,
    canvas: Canvas,
    segments: List<DanmakuContentSegment>,
    paint: TextPaint,
    iconSize: Int,
    canvasHeight: Int,
    onUpdate: () -> Unit,
    targetBitmap: Bitmap, // 用于重绘
    deferredImageRequests: MutableList<() -> Unit>?,
    extraEmotes: Map<String, String> = emptyMap(),
    emoteWidth: (String) -> Int,
) {
    val strokePaint = TextPaint(paint).apply {
        style = Paint.Style.STROKE
        strokeWidth = (paint.textSize / 8f).coerceIn(2.2f, 4.5f)
        color = Color.BLACK
        isAntiAlias = true
    }
    // 清除画布 (如果重绘)
    // 但 BitmapData 可能持有引用，所以我们最好重绘到同一个 Bitmap 上
    // 注意: 在 Coil 回调中，我们会在同一个 Canvas 上绘制吗？ No, create new Canvas for the bitmap
    
    val fontMetrics = paint.fontMetrics
    // 基线计算: 垂直居中
    // Icon 在 centerY, Text 基线在 centerY + (descent-ascent)/2 - descent ?
    // 简单点: 
    val centerY = canvasHeight / 2f
    val textBaseLine = centerY - (fontMetrics.descent + fontMetrics.ascent) / 2f
    
    var xOffset = 0f
    var emoticonCount = 0
    val maxEmoticonPerDanmaku = 2
    val imageLoader = context.imageLoader
    
    segments.forEach { seg ->
        if (seg.isEmoticon) {
            if (emoticonCount >= maxEmoticonPerDanmaku) {
                // 超出数量上限的表情回退为文字；无括号键补回括号，与未命中分支一致。
                val fallback = if (seg.content.startsWith("[")) seg.content else "[${seg.content}]"
                canvas.drawText(fallback, xOffset, textBaseLine, strokePaint)
                canvas.drawText(fallback, xOffset, textBaseLine, paint)
                xOffset += paint.measureText(fallback)
                return@forEach
            }
            val url = extraEmotes[seg.content] ?: DanmakuEmoticonMapper.emoticonMap.value[seg.content]
            if (url != null) {
                // 尝试加载
                val startX = xOffset
                val imageWidth = emoteWidth(seg.content)
                emoticonCount++
                
                // 加载图片
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .target { result ->
                        if (targetBitmap.isRecycled) return@target
                        val loadedBitmap = result.toBitmap()
                        // 重绘！
                        // 注意：这里是异步回调。我们需要重新获取 Canvas (因为 Bitmap 是同一个)
                        val asyncCanvas = Canvas(targetBitmap)
                        // 擦除这块区域？比较难。
                        // 简单策略：直接覆盖绘制。
                        // 计算位置
                        val top = centerY - iconSize / 2f
                        val dstRect = Rect(startX.toInt(), top.toInt(), (startX + imageWidth).toInt(), (top + iconSize).toInt())
                        asyncCanvas.drawBitmap(loadedBitmap, null, dstRect, null)

                        // 通知 UI 刷新
                        onUpdate()
                    }
                    .build()
                if (deferredImageRequests == null) imageLoader.enqueue(request)
                else deferredImageRequests.add {
                    imageLoader.enqueue(request)
                    Unit
                }
                
                xOffset += imageWidth + 4
            } else {
                 canvas.drawText("[${seg.content}]", xOffset, textBaseLine, strokePaint)
                 canvas.drawText("[${seg.content}]", xOffset, textBaseLine, paint)
                 xOffset += paint.measureText("[${seg.content}]")
            }
        } else {
            canvas.drawText(seg.content, xOffset, textBaseLine, strokePaint)
            canvas.drawText(seg.content, xOffset, textBaseLine, paint)
            xOffset += paint.measureText(seg.content)
        }
    }
}
