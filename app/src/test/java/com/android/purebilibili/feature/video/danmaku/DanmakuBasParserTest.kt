package com.android.purebilibili.feature.video.danmaku

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 完整 BAS（Bilibili Animation Script）高级弹幕解析测试
 *
 * 官方指令格式:
 * [beginX, beginY, alphaRange, duration, content, rotateZ, rotateY,
 *  endX, endY, translationDuration, delay, noStroke, font, easing, pathData]
 */
class DanmakuBasParserTest {

    @Test
    fun `parse full bas format with alpha range translation and path`() {
        val json = """
            [0.5, 0.3, "1-0.5", 3, "BAS 弹幕", 45, 90,
             0.8, 0.6, 2, 500, "true", "", "0", "M0,0L100,100"]
        """.trimIndent()

        val result = DanmakuParser.parseAdvancedDanmaku(json, startTimeMs = 10_000, color = 0xFF0000)

        assertNotNull(result)
        assertEquals("BAS 弹幕", result.content)
        assertEquals(10_000L, result.startTimeMs)
        assertEquals(3_000L, result.durationMs)
        // 百分比坐标直接保留为 0~1 相对值
        assertEquals(0.5f, result.startX)
        assertEquals(0.3f, result.startY)
        // alphaRange "1-0.5"
        assertEquals(1.0f, result.alphaStart)
        assertEquals(0.5f, result.alphaEnd)
        // 旋转
        assertEquals(45f, result.rotateZ)
        assertEquals(90f, result.rotateY)
        // 位移
        assertEquals(0.8f, result.endX)
        assertEquals(0.6f, result.endY)
        assertEquals(2_000L, result.translationDurationMs)
        assertEquals(500L, result.translationDelayMs)
        // noStroke / easing
        assertTrue(result.noStroke)
        assertEquals(BasEasing.QUADRATIC_EASE_OUT, result.easing)
        // pathData: M0,0L100,100 → 像素归一化为 0~1；首点与 begin(0.5,0.3) 不一致时前置起点
        assertEquals(3, result.path.size)
        assertEquals(0.5f, result.path[0].x)
        assertEquals(0.3f, result.path[0].y)
        assertEquals(0f, result.path[1].x)
        assertEquals(0f, result.path[1].y)
        assertEquals(100f / 672f, result.path[2].x, absoluteTolerance = 0.0001f)
        assertEquals(100f / 438f, result.path[2].y, absoluteTolerance = 0.0001f)
    }

    @Test
    fun `normalize integer pixel coordinates to relative`() {
        // 整数视为像素：336 = 50% 宽 (672)，219 = 50% 高 (438)
        val json = """[336, 219, "", 2, "像素定位", 0, 0]"""

        val result = DanmakuParser.parseAdvancedDanmaku(json, startTimeMs = 0, color = 0xFFFFFF)

        assertNotNull(result)
        assertEquals(0.5f, result.startX)
        assertEquals(0.5f, result.startY)
    }

    @Test
    fun `parse legacy simplified format remains compatible`() {
        // 旧版简化格式（现有实现支持的最小集合）
        val json = """[0.5, 0.1, "", 5, "简化格式", 30, 0]"""

        val result = DanmakuParser.parseAdvancedDanmaku(json, startTimeMs = 1_000, color = 0x00FF00)

        assertNotNull(result)
        assertEquals(5_000L, result.durationMs)
        assertEquals(30f, result.rotateZ)
        assertEquals(0.5f, result.startX)
        assertEquals(0.1f, result.startY)
        // 缺省位移 = 起点 (beginY = 0.1)
        assertEquals(0.5f, result.endX)
        assertEquals(0.1f, result.endY)
        // 缺省透明度 = 1.0
        assertEquals(1.0f, result.alphaStart)
        assertEquals(1.0f, result.alphaEnd)
        // 缺省 easing = Linear
        assertEquals(BasEasing.LINEAR, result.easing)
    }

    @Test
    fun `parse relative path command`() {
        // 相对路径 m/l 命令
        val json = """[0.5, 0.5, "", 2, "相对路径", 0, 0, 0, 0, 0, 0, "", "", "1", "M100,100l50,0l0,50"]"""

        val result = DanmakuParser.parseAdvancedDanmaku(json, startTimeMs = 0, color = 0xFFFFFF)

        assertNotNull(result)
        // 首点与 begin(0.5,0.5) 不一致，前置起点后共 4 点
        assertEquals(4, result.path.size)
        assertEquals(0.5f, result.path[0].x)
        assertEquals(0.5f, result.path[0].y)
        // M100,100 → (100/672, 100/438)
        assertEquals(100f / 672f, result.path[1].x, absoluteTolerance = 0.0001f)
        // l50,0 → (150, 100)
        assertEquals(150f / 672f, result.path[2].x, absoluteTolerance = 0.0001f)
        // l0,50 → (150, 150)
        assertEquals(150f / 438f, result.path[3].y, absoluteTolerance = 0.0001f)
    }

    @Test
    fun `invalid json returns null`() {
        assertNull(DanmakuParser.parseAdvancedDanmaku("not json", 0, 0xFFFFFF))
        assertNull(DanmakuParser.parseAdvancedDanmaku("""{"a":1}""", 0, 0xFFFFFF))
        // 数组长度不足
        assertNull(DanmakuParser.parseAdvancedDanmaku("""[0.5, 0.5]""", 0, 0xFFFFFF))
    }

    @Test
    fun `invalid path token stops parsing without hanging`() {
        // 超长数字溢出 toDouble 时不应死循环，返回已解析的点或空列表
        val json = """[0.5, 0.5, "", 2, "路径", 0, 0, 0, 0, 0, 0, "", "", "1", "M0,0L999999999999999999999999999999999999,100"]""".trimIndent()

        val result = DanmakuParser.parseAdvancedDanmaku(json, 0, 0xFFFFFF)

        assertNotNull(result)
        // 已解析出首点 (0,0)，非法 token 处停止；前置 begin 起点后共 2 点
        assertEquals(2, result.path.size)
        assertEquals(0.5f, result.path[0].x)
        assertEquals(0f, result.path[1].x)
    }

    @Test
    fun `translation progress respects delay and duration window`() {
        val danmaku = DanmakuParser.parseAdvancedDanmaku(
            """[0.5, 0.5, "", 4, "位移", 0, 0, 0.9, 0.9, 2, 1000]""",
            startTimeMs = 0,
            color = 0xFFFFFF
        )!!

        // delay 1000ms 内不移动
        assertEquals(0f, danmaku.getTranslationProgress(500))
        // 窗口内线性
        assertEquals(0.5f, danmaku.getTranslationProgress(2000))
        // 窗口结束后停在终点
        assertEquals(1f, danmaku.getTranslationProgress(3000))
        assertEquals(1f, danmaku.getTranslationProgress(4000))
    }

    @Test
    fun `alpha animation interpolates between range`() {
        val danmaku = DanmakuParser.parseAdvancedDanmaku(
            """[0.5, 0.5, "1-0", 2, "淡出", 0, 0]""",
            startTimeMs = 0,
            color = 0xFFFFFF
        )!!

        assertEquals(1.0f, danmaku.getAlphaAt(0))
        assertEquals(0.5f, danmaku.getAlphaAt(1000))
        assertEquals(0.0f, danmaku.getAlphaAt(2000))
    }

    @Test
    fun `path point interpolation follows segment lengths`() {
        val danmaku = DanmakuParser.parseAdvancedDanmaku(
            """[0, 0, "", 2, "路径", 0, 0, 0, 0, 0, 0, "", "", "1", "M0,0L100,0L100,100"]""",
            startTimeMs = 0,
            color = 0xFFFFFF
        )!!

        // 三段等长路径：25% 进度应落在第一段中点
        val point = danmaku.getPathPointAt(0.25f)
        assertEquals(50f / 672f, point.x, absoluteTolerance = 0.001f)
        assertEquals(0f, point.y, absoluteTolerance = 0.001f)
        // 75% 进度应落在第二段中点
        val point2 = danmaku.getPathPointAt(0.75f)
        assertEquals(100f / 672f, point2.x, absoluteTolerance = 0.001f)
        assertEquals(50f / 438f, point2.y, absoluteTolerance = 0.001f)
        // 终点
        val end = danmaku.getPathPointAt(1f)
        assertEquals(100f / 672f, end.x, absoluteTolerance = 0.001f)
        assertEquals(100f / 438f, end.y, absoluteTolerance = 0.001f)
    }
}
