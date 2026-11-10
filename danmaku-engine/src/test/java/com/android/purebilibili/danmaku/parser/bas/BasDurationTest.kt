package com.android.purebilibili.danmaku.parser.bas

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BasDurationTest {
    private fun assertDuration(expected: Long, source: String) {
        assertEquals(expected, BasScriptParser.parseDurationMs(source))
        assertEquals(expected, BasScriptParser.parse(source).durationMs)
    }

    @Test
    fun `template parameters and explicit overrides determine lifetime`() {
        assertDuration(7500L, """
            def path T(lifetime=2s image="M0 0 L1 1") { duration=lifetime d=image }
            let a=T(5s)
            let b=(T(lifetime=9s) {duration=7500ms})
            set a {} 30s
            set b {} 40s
        """.trimIndent())
        assertDuration(2000L, """
            def text T(lifetime=2s) {duration=lifetime}
            let a=T()
        """.trimIndent())
    }

    @Test
    fun `nested serial and parallel groups propagate cross object offsets`() {
        assertDuration(8000L, """
            def text a {} def text b {} def text c {}
            {set a {} 1s then set b {} 2s
             {set c {} 500ms set a {} 4s}}
            then {set b {} 3s then set c {} 1s}
            set c {} 500ms
        """.trimIndent())
        assertDuration(6500L, """
            def text a {duration=0ms} def text b {}
            set a {} 5s then {set a {} 3s set b {} 1500ms}
        """.trimIndent())
    }

    @Test
    fun `copied and redefined objects retain normal lifetime semantics`() {
        assertDuration(10000L, """
            def text original {duration=12s}
            let copy=(original {duration=3s})
            set copy {} 20s
            def text original {}
            set original {} 10s
        """.trimIndent())
        assertDuration(4000L, """
            def text old {}
            set old {} 20s
            def text old(label="unused") {content=label}
            let fresh=old()
        """.trimIndent())
        assertDuration(9000L, """
            def text a {}
            set a {} 9s
            let b=(a {})
            def text a {}
        """.trimIndent())
    }

    @Test
    fun `empty defaults zero and maximum object lifetime are exact`() {
        assertDuration(0L, "")
        assertDuration(0L, "def text T(label=\"unused\") {content=label}")
        assertDuration(4000L, "def path p {d=\"M0 0\"}")
        assertDuration(0L, "def text a {duration=0ms} set a {} 99s")
        assertDuration(0L, "def text a {} set a {} 0ms")
        assertDuration(4000L, "def text a {} set a {} 0ms def text b {}")
        assertDuration(3930250L, "def text a {duration=1h5m30s250ms} def text b {duration=1s}")
        assertDuration(21000L, "def text a {} set a {} 20.5s500ms")
    }

    @Test
    fun `escaped strings cannot inject fake lifetime or animation syntax`() {
        val source = """
            def path T(image="M0 0 \" duration=90s set x {} 99s \x41\u4e2d\n\\") {
                d=image duration=250ms
            }
            let a=T("duration=30s \" set a {} 88s // \t")
            set a {} 1s,"linear"
        """.trimIndent()
        assertDuration(250L, source)
        assertEquals("duration=30s \" set a {} 88s // \t",
            BasScriptParser.parse(source).elements.single().attributes.text("d"))
    }

    @Test
    fun `malformed structure escapes and numerical timing are rejected`() {
        val malformed = listOf(
            "def text a {duration=1s",
            "def text a {duration=1s2h}",
            "def text a {duration=-1}",
            "def text a {duration=1000}",
            "def text a {duration=\"1s\"}",
            "def text a {duration=1e309ms}",
            "def text a {duration=9223372036854775808ms}",
            "def text a {} set a {} 1",
            "def text a {} set a {} -1s",
            "def text a {} set a {} 1s then",
            "def text a {} {}",
            "def text a {content=\"bad\\q\"}",
            "def text a {content=\"bad\\x0g\"}",
            "def text a {content=\"unterminated}",
            "def text a {content=\"line\nbreak\"}",
            "def text a {} set a {} 5000000000000000000ms then set a {} 5000000000000000000ms"
        )
        for (source in malformed) {
            assertFailsWith<BasParseException>(source) { BasScriptParser.parseDurationMs(source) }
            assertFailsWith<BasParseException>(source) { BasScriptParser.parse(source) }
        }
    }

    @Test
    fun `timing purpose does not require rendering semantics`() {
        assertEquals(5000L, BasScriptParser.parseDurationMs("""
            def button a {target=unknown {anything="ignored"} text=123 duration=5s}
            set a {fontSize="not numeric"} 20s,"not an easing"
        """.trimIndent()))
    }
}
