package com.android.purebilibili.danmaku.parser.bas

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BasPathFrameTest {
    @Test
    fun pathAlphaSwitchesFramesAtZeroDurationSetsAndRestoresAfterBackwardSeek() {
        val program = BasScriptParser.parse("""
            let first=path {d="M0 0H10V10H0Z" alpha=0 fillColor=0xFF0000}
            set first {} 0ms then set first {alpha=1} 0ms
            then set first {} 200ms then set first {alpha=0} 0ms
            let second=path {d="M0 0H10V10H0Z" alpha=0 fillColor=0x0000FF}
            set second {} 200ms then set second {alpha=1} 0ms
            then set second {} 200ms then set second {alpha=0} 0ms
        """.trimIndent())
        val timeline = BasTimeline(program)
        fun shown(time: Long): List<String> {
            timeline.update(time, 100f, 100f, BasMeasurer { state, _, _ ->
                state.width = 10f
                state.height = 10f
            })
            return timeline.states.filter { it.visible && it.alpha > 0f }.map { it.element.name }
        }
        assertEquals(listOf("first"), shown(0))
        assertEquals(listOf("first"), shown(199))
        assertEquals(listOf("second"), shown(200))
        assertEquals(listOf("second"), shown(399))
        assertEquals(emptyList(), shown(400))
        assertEquals(listOf("first"), shown(100))
    }

    @Test
    fun pathAlphaIsNumericAndInterpolatesWhileFillAlphaRemainsAnIndependentStyle() {
        val program = BasScriptParser.parse("""
            def path shape {d="M0 0L10 10" alpha=0 fillAlpha=.25}
            set shape {alpha=1} 2s
        """.trimIndent())
        val timeline = BasTimeline(program)
        timeline.update(1_000, 100f, 100f, BasMeasurer { _, _, _ -> })
        assertEquals(.5f, timeline.states.single().alpha)
        assertEquals(.25, program.elements.single().attributes.number("fillAlpha"))
        assertFailsWith<BasParseException> { BasScriptParser.parse("def path shape {alpha=\"hidden\"}") }
    }
}
