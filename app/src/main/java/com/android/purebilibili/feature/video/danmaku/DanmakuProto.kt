// 文件路径: feature/video/danmaku/DanmakuProto.kt
package com.android.purebilibili.feature.video.danmaku

import android.util.Log
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.nio.charset.StandardCharsets

/**
 * B站弹幕 Protobuf 手动解析器
 * 
 * 协议格式 (Protobuf Wire Format):
 * - DmSegMobileReply { repeated DanmakuElem elems = 1; }
 * - DanmakuElem 字段:
 *   - id (field 1, int64)
 *   - progress (field 2, int32) - 弹幕出现时间(ms)
 *   - mode (field 3, int32) - 弹幕类型
 *   - fontsize (field 4, int32) - 字体大小
 *   - color (field 5, uint32) - 颜色 RGB
 *   - midHash (field 6, string) - 用户 hash
 *   - content (field 7, string) - 弹幕内容
 *   - ctime (field 8, int64) - 发送时间戳
 *   - weight (field 9, int32) - 权重
     *   - action (field 10, string)
     *   - pool (field 11, int32) - 弹幕池
     *   - idStr (field 12, string)
     *   - attr (field 13, int32) - 属性
     *   - like (field 15, int64)
     *   - colorful (field 24, enum) - 60001=大会员渐变彩色弹幕
     *   - count (field 28, int32) - 服务端合并计数
     *   - isSelf (field 29, bool) - 当前账号发送
     */
object DanmakuProto {
    
    private const val TAG = "DanmakuProto"
    
    /**
     * 弹幕元素数据类
     */
    /**
     * 弹幕元素数据类
     */
    data class DanmakuElem(
        val id: Long = 0,
        val progress: Int = 0,      // 时间戳 (毫秒)
        val mode: Int = 1,          // 弹幕类型: 1-3滚动, 4底部, 5顶部
        val fontsize: Int = 25,     // 字体大小
        val color: Int = 0xFFFFFF,  // 颜色 RGB
        val midHash: String = "",   // 用户 hash（可用于过滤）
        val content: String = "",   // 弹幕内容
        val weight: Int = 0,        // 权重 (AI过滤)
        val pool: Int = 0,          // 弹幕池: 0普通, 1字幕, 2特殊
        val like: Long = 0L,
        val colorful: Int = DmColorfulTypeNone,
        val count: Int = 0,
        val isSelf: Boolean = false
    )

    const val DmColorfulTypeNone: Int = 0
    const val DmColorfulTypeVipGradualColor: Int = 60001

    /**
     * 弹幕元数据响应 (x/v2/dm/web/view)
     */
    data class DmWebViewReply(
        val state: Int = 0,
        val textSide: String = "",
        val dmSge: DmSegConfig? = null,
        val flag: DanmakuFlagConfig? = null,
        val specialDms: List<String> = emptyList(), // 高级弹幕/特殊弹幕 URL 列表
        val checkBox: Boolean = true,
        val count: Long = 0,
        val commandDms: List<CommandDm> = emptyList(), // 互动弹幕指令
        val dmSetting: DmSetting? = null
    )

    data class DmSegConfig(
        val pageSize: Long = 0,
        val total: Long = 0
    )

    data class DanmakuFlagConfig(
        val recFlag: Int = 0,
        val recText: String = "",
        val recSwitch: Int = 0
    )

    /**
     * 互动弹幕指令
     */
    data class CommandDm(
        val id: Long = 0,
        val oid: Long = 0,
        val mid: Long = 0,
        val command: String = "",
        val content: String = "",
        val progress: Int = 0,
        val ctime: String = "",
        val mtime: String = "",
        val extra: String = "",
        val idStr: String = ""
    )

    data class DmSetting(
        val dmSwitch: Boolean = true,
        val aiSwitch: Boolean = true,
        val aiLevel: Int = 0,
        val blocktop: Boolean = false,
        val blockscroll: Boolean = false,
        val blockbottom: Boolean = false,
        val blockcolor: Boolean = false,
        val blockspecial: Boolean = false,
        val preventeshading: Boolean = false,
        val dmask: Boolean = false,
        val opacity: Float = 1.0f,
        val dmarea: Int = 0,
        val speedplus: Float = 1.0f,
        val fontsize: Float = 1.0f,
        val screensync: Boolean = false,
        val speedsync: Boolean = false,
        val fontfamily: String = "",
        val bold: Boolean = false,
        val fontborder: Int = 0,
        val drawType: String = ""
    )
    
    /**
     * 解析 DmWebViewReply 消息
     */
    fun parseWebViewReply(data: ByteArray): DmWebViewReply {
        if (data.isEmpty()) return DmWebViewReply()
        
        var state = 0
        var textSide = ""
        var dmSge: DmSegConfig? = null
        var flag: DanmakuFlagConfig? = null
        val specialDms = mutableListOf<String>()
        var checkBox = true
        var count = 0L
        val commandDms = mutableListOf<CommandDm>()
        var dmSetting: DmSetting? = null

        try {
            val input = ProtoInput(data)
            while (!input.isAtEnd()) {
                val tag = input.readTag()
                val fieldNumber = tag ushr 3
                val wireType = tag and 0x07

                when (fieldNumber) {
                    1 -> state = input.readVarint().toInt()
                    2 -> input.skipField(wireType)
                    // 兼容旧/新 web/view 字段：旧版 dmSge=3、flag=4；新版 dmSge=4、flag=5
                    3 -> {
                        if (wireType == 2) {
                            val bytes = input.readBytes()
                            val dmSegCandidate = parseDmSegConfig(bytes)
                            if (isLikelyDmSegConfig(dmSegCandidate)) {
                                dmSge = dmSegCandidate
                            } else {
                                textSide = String(bytes, StandardCharsets.UTF_8)
                            }
                        } else {
                            input.skipField(wireType)
                        }
                    }
                    4 -> {
                        if (wireType == 2) {
                            val bytes = input.readBytes()
                            val dmSegCandidate = parseDmSegConfig(bytes)
                            if (isLikelyDmSegConfig(dmSegCandidate)) {
                                dmSge = dmSegCandidate
                            } else {
                                parseDanmakuFlagConfig(bytes).takeIf(::isLikelyDanmakuFlagConfig)?.let { flag = it }
                            }
                        } else {
                            input.skipField(wireType)
                        }
                    }
                    // 旧版 specialDms=5，新版 flag=5
                    5 -> {
                        if (wireType == 2) {
                            val bytes = input.readBytes()
                            val flagCandidate = parseDanmakuFlagConfig(bytes)
                            if (isLikelyDanmakuFlagConfig(flagCandidate)) {
                                flag = flagCandidate
                            } else {
                                decodeSpecialDmUrl(bytes)?.let { specialDms.add(it) }
                            }
                        } else {
                            input.skipField(wireType)
                        }
                    }
                    6 -> {
                        if (wireType == 2) {
                            decodeSpecialDmUrl(input.readBytes())?.let { specialDms.add(it) }
                        } else {
                            input.skipField(wireType)
                        }
                    }
                    // 旧版 count=7，新版 count=8；旧版 commandDms=8，新版 commandDms=9
                    7 -> {
                        when (wireType) {
                            0 -> {
                                val value = input.readVarint()
                                if (value > 1L) count = value else checkBox = value != 0L
                            }
                            2 -> parseCommandDm(input.readBytes())?.let { commandDms.add(it) }
                            else -> input.skipField(wireType)
                        }
                    }
                    8 -> {
                        when (wireType) {
                            0 -> count = input.readVarint()
                            2 -> {
                                val bytes = input.readBytes()
                                parseCommandDm(bytes)?.let { commandDms.add(it) }
                            }
                            else -> input.skipField(wireType)
                        }
                    }
                    // 旧版 dmSetting=9，新版 dmSetting=10
                    9 -> {
                        if (wireType == 2) {
                            val bytes = input.readBytes()
                            val command = parseCommandDm(bytes)
                            if (command != null) {
                                commandDms.add(command)
                            } else {
                                dmSetting = parseDmSetting(bytes)
                            }
                        } else {
                            input.skipField(wireType)
                        }
                    }
                    10 -> {
                        if (wireType == 2) {
                            dmSetting = parseDmSetting(input.readBytes())
                        } else {
                            input.skipField(wireType)
                        }
                    }
                    else -> input.skipField(wireType)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, " Parse DmWebViewReply error: \${e.message}", e)
        }

        return DmWebViewReply(state, textSide, dmSge, flag, specialDms, checkBox, count, commandDms, dmSetting)
    }

    private fun isLikelyDmSegConfig(config: DmSegConfig): Boolean {
        return config.pageSize >= 1_000L &&
            config.total in 1L..10_000L
    }

    private fun isLikelyDanmakuFlagConfig(config: DanmakuFlagConfig): Boolean {
        return config.recFlag != 0 ||
            config.recSwitch != 0 ||
            config.recText.isNotEmpty()
    }

    private fun decodeSpecialDmUrl(data: ByteArray): String? {
        if (data.isEmpty()) return null
        val text = String(data, StandardCharsets.UTF_8).trim()
        return text.takeIf { it.startsWith("http://") || it.startsWith("https://") || it.startsWith("//") }
    }

    private fun parseDmSegConfig(data: ByteArray): DmSegConfig {
        var pageSize = 0L
        var total = 0L
        try {
            val input = ProtoInput(data)
            while (!input.isAtEnd()) {
                val tag = input.readTag()
                val fieldNumber = tag ushr 3
                val wireType = tag and 0x07
                when(fieldNumber) {
                    1 -> pageSize = input.readVarint()
                    2 -> total = input.readVarint()
                    else -> input.skipField(wireType)
                }
            }
        } catch (e: Exception) {}
        return DmSegConfig(pageSize, total)
    }

    private fun parseDanmakuFlagConfig(data: ByteArray): DanmakuFlagConfig {
        var recFlag = 0
        var recText = ""
        var recSwitch = 0
        try {
            val input = ProtoInput(data)
            while (!input.isAtEnd()) {
                val tag = input.readTag()
                val wireType = tag and 0x07
                when(tag ushr 3) {
                    1 -> recFlag = input.readVarint().toInt()
                    2 -> recText = input.readString()
                    3 -> recSwitch = input.readVarint().toInt()
                    else -> input.skipField(wireType)
                }
            }
        } catch (e: Exception) {}
        return DanmakuFlagConfig(recFlag, recText, recSwitch)
    }

    private fun parseCommandDm(data: ByteArray): CommandDm? {
        if (data.isEmpty()) return null
        var id = 0L
        var oid = 0L
        var mid = 0L
        var command = ""
        var content = ""
        var progress = 0
        var ctime = ""
        var mtime = ""
        var extra = ""
        var idStr = ""
        try {
           val input = ProtoInput(data)
           while(!input.isAtEnd()) {
               val tag = input.readTag()
               val wireType = tag and 0x07
               when(tag ushr 3) {
                   1 -> id = input.readVarint()
                   2 -> oid = input.readVarint()
                   3 -> mid = input.readVarint()
                   4 -> command = input.readString()
                   5 -> content = input.readString()
                   6 -> progress = input.readVarint().toInt()
                   7 -> ctime = input.readString()
                   8 -> mtime = input.readString()
                   9 -> extra = input.readString()
                   10 -> idStr = input.readString()
                   else -> input.skipField(wireType)
               }
           }
        } catch(e: Exception) { return null }
        if (command.isBlank() && content.isBlank() && extra.isBlank()) return null
        return CommandDm(id, oid, mid, command, content, progress, ctime, mtime, extra, idStr)
    }

    private fun parseDmSetting(data: ByteArray): DmSetting {
        var dmSwitch = true
        var aiSwitch = true
        var aiLevel = 0
        var blocktop = false
        var blockscroll = false
        var blockbottom = false
        var blockcolor = false
        var blockspecial = false
        var preventeshading = false
        var dmask = false
        var opacity = 1.0f
        var dmarea = 0
        var speedplus = 1.0f
        var fontsize = 1.0f
        var screensync = false
        var speedsync = false
        var fontfamily = ""
        var bold = false
        var fontborder = 0
        var drawType = ""
        try {
            val input = ProtoInput(data)
            while (!input.isAtEnd()) {
                val tag = input.readTag()
                val wireType = tag and 0x07
                when (tag ushr 3) {
                    1 -> dmSwitch = input.readVarint() != 0L
                    2 -> aiSwitch = input.readVarint() != 0L
                    3 -> aiLevel = input.readVarint().toInt()
                    4 -> blocktop = input.readVarint() != 0L
                    5 -> blockscroll = input.readVarint() != 0L
                    6 -> blockbottom = input.readVarint() != 0L
                    7 -> blockcolor = input.readVarint() != 0L
                    8 -> blockspecial = input.readVarint() != 0L
                    9 -> preventeshading = input.readVarint() != 0L
                    10 -> dmask = input.readVarint() != 0L
                    11 -> opacity = input.readFloat()
                    12 -> dmarea = input.readVarint().toInt()
                    13 -> speedplus = input.readFloat()
                    14 -> fontsize = input.readFloat()
                    15 -> screensync = input.readVarint() != 0L
                    16 -> speedsync = input.readVarint() != 0L
                    17 -> fontfamily = input.readString()
                    18 -> bold = input.readVarint() != 0L
                    19 -> fontborder = input.readVarint().toInt()
                    20 -> drawType = input.readString()
                    else -> input.skipField(wireType)
                }
            }
        } catch (_: Exception) {
        }
        return DmSetting(
            dmSwitch = dmSwitch,
            aiSwitch = aiSwitch,
            aiLevel = aiLevel,
            blocktop = blocktop,
            blockscroll = blockscroll,
            blockbottom = blockbottom,
            blockcolor = blockcolor,
            blockspecial = blockspecial,
            preventeshading = preventeshading,
            dmask = dmask,
            opacity = opacity,
            dmarea = dmarea,
            speedplus = speedplus,
            fontsize = fontsize,
            screensync = screensync,
            speedsync = speedsync,
            fontfamily = fontfamily,
            bold = bold,
            fontborder = fontborder,
            drawType = drawType
        )
    }
    
    /**
     * 解析 DmSegMobileReply 消息
     * 
     * @param data 原始 Protobuf 字节数组
     * @return 弹幕元素列表
     */
    fun parse(data: ByteArray): List<DanmakuElem> {
        val result = mutableListOf<DanmakuElem>()
        
        if (data.isEmpty()) {
            Log.w(TAG, " Empty data received")
            return result
        }
        
        try {
            val input = ProtoInput(data)
            
            // DmSegMobileReply 的 field 1 是 repeated DanmakuElem
            while (!input.isAtEnd()) {
                val tag = input.readTag()
                val fieldNumber = tag ushr 3
                val wireType = tag and 0x07
                
                when (fieldNumber) {
                    1 -> {
                        // DanmakuElem 是 length-delimited (wireType = 2)
                        if (wireType == 2) {
                            val elemData = input.readBytes()
                            val elem = parseDanmakuElem(elemData)
                            if (elem != null && elem.content.isNotEmpty()) {
                                result.add(elem)
                            }
                        } else {
                            input.skipField(wireType)
                        }
                    }
                    else -> input.skipField(wireType)
                }
            }
            
            Log.d(TAG, " Parsed ${result.size} danmakus from protobuf")
            
        } catch (e: Exception) {
            Log.e(TAG, " Parse protobuf error: ${e.message}", e)
        }
        
        return result
    }
    
    /**
     * 解析单个 DanmakuElem 消息
     */
    private fun parseDanmakuElem(data: ByteArray): DanmakuElem? {
        if (data.isEmpty()) return null
        
        var id = 0L
        var progress = 0
        var mode = 1
        var fontsize = 25
        var color = 0xFFFFFF
        var midHash = ""
        var content = ""
        var weight = 0
        var pool = 0
        var like = 0L
        var colorful = DmColorfulTypeNone
        var count = 0
        var isSelf = false
        
        try {
            val input = ProtoInput(data)
            
            while (!input.isAtEnd()) {
                val tag = input.readTag()
                val fieldNumber = tag ushr 3
                val wireType = tag and 0x07
                
                when (fieldNumber) {
                    1 -> id = input.readVarint()           // id
                    2 -> progress = input.readVarint().toInt()  // progress (ms)
                    3 -> mode = input.readVarint().toInt()      // mode
                    4 -> fontsize = input.readVarint().toInt()  // fontsize
                    5 -> color = input.readVarint().toInt()     // color
                    6 -> midHash = input.readString()           // midHash
                    7 -> content = input.readString()           // content
                    8 -> input.readVarint()                     // ctime (skip)
                    9 -> weight = input.readVarint().toInt()    // weight
                    10 -> input.readString()                    // action (skip)
                    11 -> pool = input.readVarint().toInt()     // pool
                    12 -> input.readString()                    // idStr (skip)
                    13 -> input.readVarint()                    // attr (skip)
                    15 -> like = input.readVarint()              // like
                    22 -> input.readString()                    // animation (skip)
                    23 -> input.readString()                    // extra (skip)
                    24 -> colorful = input.readVarint().toInt() // colorful
                    25 -> input.readVarint()                    // type (skip)
                    26 -> input.readVarint()                    // oid (skip)
                    27 -> input.readVarint()                    // dmFrom (skip)
                    28 -> count = input.readVarint().toInt()    // count
                    29 -> isSelf = input.readVarint() != 0L     // isSelf
                    else -> input.skipField(wireType)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, " Parse elem error: ${e.message}")
            return null
        }
        
        return DanmakuElem(
            id = id,
            progress = progress,
            mode = mode,
            fontsize = fontsize,
            color = color,
            midHash = midHash,
            content = content,
            weight = weight,
            pool = pool,
            like = like,
            colorful = colorful,
            count = count,
            isSelf = isSelf
        )
    }
    
    /**
     * 简易 Protobuf 输入流读取器
     */
    private class ProtoInput(private val data: ByteArray) {
        private var position = 0
        
        fun isAtEnd(): Boolean = position >= data.size
        
        fun readTag(): Int = readVarint().toInt()
        
        /**
         * 读取 Varint (变长整数)
         */
        fun readVarint(): Long {
            var result = 0L
            var shift = 0
            
            while (position < data.size) {
                val byte = data[position++].toInt() and 0xFF
                result = result or ((byte and 0x7F).toLong() shl shift)
                
                if ((byte and 0x80) == 0) {
                    break
                }
                shift += 7
                
                if (shift >= 64) {
                    throw RuntimeException("Varint too long")
                }
            }
            
            return result
        }
        
        /**
         * 读取 length-delimited 字节数组
         */
        fun readBytes(): ByteArray {
            val length = readVarint().toInt()
            if (length <= 0 || position + length > data.size) {
                return ByteArray(0)
            }
            val result = data.copyOfRange(position, position + length)
            position += length
            return result
        }
        
        /**
         * 读取字符串 (UTF-8)
         */
        fun readString(): String {
            val bytes = readBytes()
            return String(bytes, StandardCharsets.UTF_8)
        }

        fun readFloat(): Float {
            if (position + 4 > data.size) return 0f
            val bits = (data[position].toInt() and 0xFF) or
                ((data[position + 1].toInt() and 0xFF) shl 8) or
                ((data[position + 2].toInt() and 0xFF) shl 16) or
                ((data[position + 3].toInt() and 0xFF) shl 24)
            position += 4
            return java.lang.Float.intBitsToFloat(bits)
        }
        
        /**
         * 跳过未知字段
         */
        fun skipField(wireType: Int) {
            when (wireType) {
                0 -> readVarint()           // Varint
                1 -> position += 8          // 64-bit
                2 -> {                      // Length-delimited
                    val length = readVarint().toInt()
                    position += length
                }
                5 -> position += 4          // 32-bit
                else -> { /* 未知类型，忽略 */ }
            }
        }
    }
}
