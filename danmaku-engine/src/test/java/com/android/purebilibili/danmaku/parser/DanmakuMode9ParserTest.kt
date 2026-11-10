package com.android.purebilibili.danmaku.parser

import com.android.purebilibili.danmaku.parser.bas.BasMeasurer
import com.android.purebilibili.danmaku.parser.bas.BasTimeline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DanmakuMode9ParserTest {
    @Test
    fun `mode 9 compiles a scene and can seek into its animation without affecting mode 7`() {
        val parsed = DanmakuParser.parseProtobuf(listOf(segment(
            1 to "普通弹幕",
            7 to """[0.5, 0.3, "1-0", 2, "定位弹幕", 0, 0]""",
            9 to """def text title { content = "BAS" } set title { alpha = 0 } 1s"""
        )))

        assertEquals(listOf("普通弹幕"), parsed.standardList.map { it.text })
        assertEquals(listOf("定位弹幕"), parsed.advancedList.map { it.content })
        val script = parsed.basList.single()
        val timeline = BasTimeline(script.program)
        timeline.update(500, 1000f, 500f, BasMeasurer { state, _, _ ->
            state.width = 60f
            state.height = 25f
        })
        val state = timeline.states.single()
        assertTrue(state.visible)
        assertEquals("BAS", state.content)
        assertEquals(0.5f, state.alpha, 0.0001f)
        timeline.update(1000, 1000f, 500f, BasMeasurer { state, _, _ ->
            state.width = 60f
            state.height = 25f
        })
        assertEquals(false, state.visible)
    }

    @Test
    fun `bad mode 9 scripts are isolated and never displayed as scrolling source`() {
        val parsed = DanmakuParser.parseProtobuf(listOf(segment(
            9 to "def text broken { content = \"missing closing brace\"",
            9 to """[0.5, 0.3, "1-0", 2, "not a BAS script"]""",
            8 to "window.alert('not BAS')",
            1 to "后续正常弹幕",
            9 to """def text valid { content = "后续 BAS" duration = 2s }"""
        )))

        assertEquals(listOf("后续正常弹幕"), parsed.standardList.map { it.text })
        assertTrue(parsed.advancedList.isEmpty())
        assertEquals(listOf("后续 BAS"), parsed.basList.map { it.content })
        assertEquals(2000L, parsed.basList.single().durationMs)
    }

    private fun segment(vararg items: Pair<Int, String>): ByteArray = buildList<Byte> {
        items.forEachIndexed { index, (mode, content) ->
            val element = buildList<Byte> {
                fieldNumber(1, index + 1L)
                fieldNumber(2, 1000L)
                fieldNumber(3, mode.toLong())
                fieldNumber(5, 0xFFFFFFL)
                fieldBytes(7, content.encodeToByteArray())
            }.toByteArray()
            fieldBytes(1, element)
        }
    }.toByteArray()

    private fun MutableList<Byte>.fieldNumber(field: Int, value: Long) {
        varint((field shl 3).toLong())
        varint(value)
    }

    private fun MutableList<Byte>.fieldBytes(field: Int, value: ByteArray) {
        varint(((field shl 3) or 2).toLong())
        varint(value.size.toLong())
        for (byte in value) add(byte)
    }

    private fun MutableList<Byte>.varint(value: Long) {
        var remaining = value
        do {
            var next = (remaining and 0x7F).toInt()
            remaining = remaining ushr 7
            if (remaining != 0L) next = next or 0x80
            add(next.toByte())
        } while (remaining != 0L)
    }
}
