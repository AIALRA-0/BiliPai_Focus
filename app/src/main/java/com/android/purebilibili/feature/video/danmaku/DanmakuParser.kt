// 文件路径: feature/video/danmaku/DanmakuParser.kt
package com.android.purebilibili.feature.video.danmaku

import android.util.Log
import android.util.Xml
import com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_BOTTOM
import com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_REVERSE
import com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_SCROLL
import com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_TOP
import com.android.purebilibili.danmaku.engine.DanmakuItem
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream

/**
 * 弹幕解析器
 * 
 * 支持两种格式：
 * 1. XML 格式 (旧版 API)
 * 2. Protobuf 格式 (新版 seg.so API)
 */
object DanmakuParser {
    
    private const val TAG = "DanmakuParser"
    
    /**
     *  [新增] 解析 Protobuf 弹幕数据 (推荐)
     * 
     * @param segments Protobuf 分段数据列表
     * @return ParsedDanmaku (标准弹幕 + 高级弹幕)
     */
    fun parseProtobuf(segments: List<ByteArray>): ParsedDanmaku {
        val standardList = mutableListOf<DanmakuItem>()
        val advancedList = mutableListOf<AdvancedDanmakuData>()
        
        if (segments.isEmpty()) {
            Log.w(TAG, " No segments to parse")
            return ParsedDanmaku(standardList, advancedList)
        }
        
        Log.d(TAG, " Parsing ${segments.size} Protobuf segments...")
        
        var totalParsed = 0
        for ((index, segment) in segments.withIndex()) {
            try {
                val elems = DanmakuProto.parse(segment)
                Log.d(TAG, " Segment ${index + 1}: parsed ${elems.size} danmakus")
                
                for (elem in elems) {
                    // 尝试解析为高级弹幕 (Mode 7 高级弹幕 / Mode 9 BAS 代码弹幕)
                    // Mode 8 是 JS 代码弹幕，移动端无 JS 沙箱，尝试按 BAS JSON 解析失败则丢弃
                    if (elem.mode == 7 || elem.mode == 9) {
                        try {
                            val advanced = parseAdvancedDanmaku(elem.content, elem.progress.toLong(), elem.color)
                            if (advanced != null) {
                                advancedList.add(advanced)
                                totalParsed++
                                continue // 成功解析为高级弹幕，跳过标准解析
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, " Failed to parse advanced danmaku: ${e.message}")
                        }
                    }

                    // Mode 8 代码弹幕：content 为 JS 代码，无法作为文本渲染
                    if (elem.mode == 8) continue

                    // 标准弹幕解析
                    val textData = createTextDataFromProto(elem)
                    if (textData != null) {
                        standardList.add(textData)
                        totalParsed++
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, " Failed to parse segment ${index + 1}: ${e.message}")
            }
        }
        
        //  [关键] 按时间排序 - DanmakuRenderEngine 需要有序数据
        standardList.sortBy { it.showAtTime }
        advancedList.sortBy { it.startTimeMs }
        
        // 统计信息
        if (totalParsed > 0) {
            val times = standardList.map { it.showAtTime }
            val minTime = times.minOrNull() ?: 0
            val maxTime = times.maxOrNull() ?: 0
            Log.w(TAG, " Parsed result: Standard=${standardList.size}, Advanced=${advancedList.size} | Time: ${minTime}ms ~ ${maxTime}ms")
        } else {
            Log.w(TAG, " No danmakus parsed from Protobuf!")
        }
        
        return ParsedDanmaku(standardList, advancedList)
    }
    
    /**
     * 从 Protobuf DanmakuElem 创建 TextData
     */
    private fun createTextDataFromProto(elem: DanmakuProto.DanmakuElem): DanmakuItem? {
        if (elem.content.isEmpty()) return null
        
        // Mode 8/9 代码弹幕目前暂不支持
        if (elem.mode >= 8) return null
        
        val layerType = mapLayerType(elem.mode)
        val colorWithAlpha = elem.color or 0xFF000000.toInt()
        
        // [API 完整利用] 使用 WeightedTextData 携带 weight 和 pool 信息
        return WeightedTextData().apply {
            this.danmakuId = elem.id
            this.userHash = elem.midHash
            this.text = formatDanmakuTextWithCount(
                content = elem.content,
                duplicateCount = elem.count
            )
            this.showAtTime = elem.progress.toLong()
            this.layerType = layerType
            this.textColor = colorWithAlpha
            this.textSizeScale = resolveBilibiliDanmakuFontScale(elem.fontsize.toFloat())
            
            // 填充 Bilibili 特有属性
            this.weight = elem.weight
            this.pool = elem.pool
            this.attr = elem.attr
            this.likeCount = elem.like
            this.isVipGradualColor = elem.colorful == DanmakuProto.DmColorfulTypeVipGradualColor
            this.duplicateCount = elem.count
            this.isSelf = elem.isSelf
        }
    }
    
    private var debugLogCount = 0  // 用于限制调试日志数量
    
    /**
     * 解析 XML 弹幕数据 (旧版 API，作为后备方案)
     * 
     * @param rawData 原始 XML 数据
     * @return ParsedDanmaku (标准弹幕 + 高级弹幕)
     */
    fun parse(rawData: ByteArray): ParsedDanmaku {
        val standardList = mutableListOf<DanmakuItem>()
        val advancedList = mutableListOf<AdvancedDanmakuData>()
        
        try {
            val parser = Xml.newPullParser()
            parser.setInput(ByteArrayInputStream(rawData), "UTF-8")
            
            var eventType = parser.eventType
            var count = 0
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "d") {
                    val pAttr = parser.getAttributeValue(null, "p")
                    parser.next()
                    val content = if (parser.eventType == XmlPullParser.TEXT) parser.text else ""
                    
                    if (pAttr != null && content.isNotEmpty()) {
                        val parts = pAttr.split(",")
                        if (parts.size >= 2) {
                            val mode = parts[1].toIntOrNull() ?: 1
                            val timeMs = ((parts[0].toFloatOrNull() ?: 0f) * 1000).toLong()
                            
                            val colorInt = (parts.getOrNull(3)?.toLongOrNull() ?: 0xFFFFFF).toInt()
                            
                            if (mode == 7 || mode == 9) {
                                val advanced = parseAdvancedDanmaku(content, timeMs, colorInt)
                                if (advanced != null) {
                                    advancedList.add(advanced)
                                    count++
                                    continue
                                }
                            }
                            
                            val danmaku = createTextData(pAttr, content)
                            if (danmaku != null) {
                                standardList.add(danmaku)
                                count++
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            
            standardList.sortBy { it.showAtTime }
            advancedList.sortBy { it.startTimeMs }
            
            Log.w(TAG, " XML Parsed: Standard=${standardList.size}, Advanced=${advancedList.size}")
            
        } catch (e: Exception) {
            Log.e(TAG, " XML parse error: ${e.message}", e)
        }
        
        return ParsedDanmaku(standardList, advancedList)
    }
    
    /**
     * 解析 DanmakuView 元数据 (x/v2/dm/web/view)
     */
    fun parseWebViewReply(data: ByteArray): DanmakuProto.DmWebViewReply {
        return DanmakuProto.parseWebViewReply(data)
    }

    /**
     * 从 JSON 格式内容解析高级弹幕 (Mode 7)
     * 格式: [startX, startY, mode, duration, content, rotateZ, rotateY]
     * 注意：部分高级弹幕的颜色可能在 JSON 中，也可以使用外层属性的颜色
     */
    /**
     * 从 JSON 格式内容解析高级弹幕 (Mode 7 / Mode 9 BAS)
     * 完整格式 (与官方引擎 BiliDanmukuParser 一致):
     * [beginX, beginY, alphaRange, duration, content, rotateZ, rotateY,
     *  endX, endY, translationDuration, delay, noStroke, font, easing, pathData]
     *
     * - beginX/beginY: 含小数点视为百分比 (0~1，基准 672x438)，整数为像素
     * - alphaRange: 透明度范围 "1-0.5"（起-止）
     * - easing: "0"=Quadratic.easeOut，其他=Linear
     * - pathData: SVG 路径 "M0,0L100,100L200,0"（像素坐标）
     */
    internal fun parseAdvancedDanmaku(jsonContent: String, startTimeMs: Long, color: Int): AdvancedDanmakuData? {
        try {
            // 简单的 JSON 数组检查
            if (!jsonContent.trim().startsWith("[")) return null

            val jsonArray = org.json.JSONArray(jsonContent)
            if (jsonArray.length() < 5) return null

            val beginX = normalizeBasCoordinate(jsonArray, 0, 672)
            val beginY = normalizeBasCoordinate(jsonArray, 1, 438)
            val alphaRange = parseBasAlphaRange(jsonArray)
            val duration = (jsonArray.optDouble(3, 1.0) * 1000).toLong()
            val content = jsonArray.optString(4, "")
            val rotateZ = jsonArray.optDouble(5, 0.0).toFloat()
            val rotateY = jsonArray.optDouble(6, 0.0).toFloat()

            // 位移终点：缺省时与起点相同（无位移）
            val endX = normalizeBasCoordinate(jsonArray, 7, 672, fallback = beginX)
            val endY = normalizeBasCoordinate(jsonArray, 8, 438, fallback = beginY)

            // 位移动画时长：缺省等于总时长
            val translationDurationMs = optBasDouble(jsonArray, 9)
                ?.let { (it * 1000).toLong() }
                ?: duration
            // delay 按官方格式为毫秒（区别于 index 9 的秒），直接取整
            val translationDelayMs = (optBasDouble(jsonArray, 10) ?: 0.0).toLong()
            val noStroke = jsonArray.optString(11, "") == "true"
            // index 12 = font，官方引擎未处理，忽略
            val easing = if (jsonArray.optString(13, "1") == "0") {
                BasEasing.QUADRATIC_EASE_OUT
            } else {
                BasEasing.LINEAR
            }
            val path = parseBasPath(jsonArray.optString(14, ""))
            // 路径首点与定位起点不一致时，前置起点，避免弹幕从画面原点跳变
            val effectivePath = if (path.isNotEmpty() && (path[0].x != beginX || path[0].y != beginY)) {
                listOf(BasPathPoint(beginX, beginY)) + path
            } else {
                path
            }

            val (alphaStart, alphaEnd) = alphaRange

            return AdvancedDanmakuData(
                content = content,
                startTimeMs = startTimeMs,
                durationMs = duration,
                startX = beginX,
                startY = beginY,
                endX = endX,
                endY = endY,
                alpha = alphaStart,
                alphaStart = alphaStart,
                alphaEnd = alphaEnd,
                rotateZ = rotateZ,
                rotateY = rotateY,
                translationDurationMs = translationDurationMs,
                translationDelayMs = translationDelayMs,
                noStroke = noStroke,
                easing = easing,
                path = effectivePath,
                color = color // 使用传入的颜色
            )
        } catch (e: Exception) {
            // Log.d(TAG, "Not a valid Mode 7 JSON: $jsonContent")
            return null
        }
    }

    /**
     * BAS 坐标归一化（与官方引擎 BiliDanmukuParser 一致）：
     * - 含小数点 → 百分比 (0~1)，直接作为相对值（官方基准 672x438 下与百分比等价）
     * - 整数 → 像素，除以官方基准 (X=672, Y=438) 得到 0~1 相对值
     * 渲染层统一按 0~1 相对值乘实际容器尺寸。
     */
    private fun normalizeBasCoordinate(
        jsonArray: org.json.JSONArray,
        index: Int,
        base: Int,
        fallback: Float = 0f
    ): Float {
        val value = optBasDouble(jsonArray, index) ?: return fallback
        return if (value % 1.0 != 0.0) {
            value.toFloat().coerceIn(0f, 1f)
        } else {
            (value / base).toFloat().coerceIn(0f, 1f)
        }
    }

    private fun optBasDouble(jsonArray: org.json.JSONArray, index: Int): Double? {
        if (index >= jsonArray.length()) return null
        return try {
            jsonArray.optDouble(index, Double.NaN).takeIf { !it.isNaN() }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 解析透明度范围 "1-0.5"（起-止，0~1）；缺省或非法时返回 (1.0, 1.0)
     */
    private fun parseBasAlphaRange(jsonArray: org.json.JSONArray): Pair<Float, Float> {
        val raw = jsonArray.optString(2, "")
        if (raw.isBlank()) return 1.0f to 1.0f
        val parts = raw.split("-")
        if (parts.size != 2) return 1.0f to 1.0f
        val start = parts[0].toFloatOrNull() ?: return 1.0f to 1.0f
        val end = parts[1].toFloatOrNull() ?: return 1.0f to 1.0f
        return start.coerceIn(0f, 1f) to end.coerceIn(0f, 1f)
    }

    /**
     * 解析 SVG 路径 (如 "M0,0L100,100L200,0") 为归一化点列表。
     * 支持 M/m (move) 与 L/l (line) 命令；坐标按 672x438 基准归一化。
     */
    private fun parseBasPath(raw: String): List<BasPathPoint> {
        if (raw.isBlank()) return emptyList()
        val points = mutableListOf<BasPathPoint>()
        var currentX = 0.0
        var currentY = 0.0
        var command = 'M'
        var isRelative = false

        val tokens = Regex("[MLml]|-?\\d+(?:\\.\\d+)?")
            .findAll(raw)
            .map { it.value }
            .toList()

        var index = 0
        while (index < tokens.size) {
            val token = tokens[index]
            if (token[0] == 'M' || token[0] == 'm' || token[0] == 'L' || token[0] == 'l') {
                command = token[0]
                isRelative = command.isLowerCase()
                index++
                continue
            }
            // 收集当前命令的参数对
            val params = mutableListOf<Double>()
            while (index < tokens.size) {
                val next = tokens[index]
                if (next[0] == 'M' || next[0] == 'm' || next[0] == 'L' || next[0] == 'l') break
                val parsed = next.toDoubleOrNull()
                // 非法 token（超长数字、乱码）停止解析避免死循环：
                // 超长数字可能在 Double 内不溢出（如 1e36），但远超 BAS 像素坐标
                // 合理范围（672x438 基准），同样视为非法。
                if (parsed == null || !parsed.isFinite() || kotlin.math.abs(parsed) > 100_000.0) {
                    return points
                }
                params.add(parsed)
                index++
            }
            var p = 0
            while (p + 1 < params.size) {
                val rawX = params[p]
                val rawY = params[p + 1]
                val absX = if (isRelative) currentX + rawX else rawX
                val absY = if (isRelative) currentY + rawY else rawY
                currentX = absX
                currentY = absY
                points.add(BasPathPoint((absX / 672.0).toFloat().coerceIn(0f, 1f), (absY / 438.0).toFloat().coerceIn(0f, 1f)))
                p += 2
            }
        }
        return points
    }

    private fun formatDanmakuTextWithCount(
        content: String,
        duplicateCount: Int
    ): String {
        return if (duplicateCount > 1) {
            "$content x$duplicateCount"
        } else {
            content
        }
    }
    
    /**
     * 从属性字符串创建 TextData
     */
    private fun createTextData(pAttr: String, content: String): DanmakuItem? {
        try {
            val parts = pAttr.split(",")
            if (parts.size < 4) return null
            
            val biliType = parts[1].toIntOrNull() ?: 1
            
            // 过滤 Mode 7/8/9
            if (biliType >= 7) return null
            
            val timeSeconds = parts[0].toFloatOrNull() ?: 0f
            val timeMs = (timeSeconds * 1000).toLong()  // 转换为毫秒
            val fontSize = parts[2].toFloatOrNull() ?: 25f
            val colorInt = parts[3].toLongOrNull() ?: 0xFFFFFF
            val pool = parts.getOrNull(5)?.toIntOrNull() ?: 0
            val userHash = parts.getOrNull(6).orEmpty()
            val danmakuId = parts.getOrNull(7)?.toLongOrNull() ?: 0L
            
            val layerType = mapLayerType(biliType)
            
            return WeightedTextData().apply {
                this.danmakuId = danmakuId
                this.userHash = userHash
                this.pool = pool
                this.text = content
                this.showAtTime = timeMs
                this.layerType = layerType
                this.textColor = (colorInt.toInt() or 0xFF000000.toInt())
                this.textSizeScale = resolveBilibiliDanmakuFontScale(fontSize)
            }
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * 映射 Bilibili 弹幕类型到 DanmakuRenderEngine LayerType
     */
    private fun mapLayerType(biliType: Int): Int = when (biliType) {
        1, 2, 3 -> DANMAKU_LAYER_SCROLL
        4 -> DANMAKU_LAYER_BOTTOM
        5 -> DANMAKU_LAYER_TOP
        6 -> DANMAKU_LAYER_REVERSE
        else -> DANMAKU_LAYER_SCROLL
    }
}
