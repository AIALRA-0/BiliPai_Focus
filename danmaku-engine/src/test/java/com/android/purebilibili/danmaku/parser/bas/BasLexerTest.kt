package com.android.purebilibili.danmaku.parser.bas

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BasLexerTest {
    private fun tokens(source: String): List<Pair<BasTokenKind, String>> {
        val lexer = BasLexer(source)
        return buildList {
            while (true) {
                val token = lexer.next()
                if (token.kind == BasTokenKind.END) break
                add(token.kind to token.text)
            }
        }
    }

    @Test
    fun `decimal fractions and signed exponents preserve token boundaries`() {
        assertEquals(listOf("0", "001", ".5", "0.25", "12.3e+4", "9E-2").map {
            BasTokenKind.NUMBER to it
        }, tokens("0 001 .5 0.25 12.3e+4 9E-2"))
        assertEquals(listOf(
            BasTokenKind.NUMBER to "1", BasTokenKind.SYMBOL to ".",
            BasTokenKind.NUMBER to "2", BasTokenKind.ID to "e", BasTokenKind.SYMBOL to "+",
            BasTokenKind.NUMBER to "3", BasTokenKind.ID to "E",
            BasTokenKind.SYMBOL to ".", BasTokenKind.HEX to "0xFF"
        ), tokens("1. 2e+ 3E . 0xFF"))
    }

    @Test
    fun `compound time scanning leaves following non time numbers untouched`() {
        assertEquals(listOf(
            BasTokenKind.TIME to "1h2.5m3e-1s4ms",
            BasTokenKind.NUMBER to "7", BasTokenKind.SYMBOL to "%",
            BasTokenKind.TIME to ".5s", BasTokenKind.NUMBER to "2"
        ), tokens("1h2.5m3e-1s4ms 7% .5s2"))
        for (source in listOf("1s2h", "1m2m", "1ms2s")) {
            assertFailsWith<BasParseException>(source) { tokens(source) }
        }
    }

    @Test
    fun `numeric scanning preserves positions across comments and CRLF`() {
        val lexer = BasLexer("// comment\r\n .5e2ms\r\n9")
        assertEquals(BasToken(BasTokenKind.TIME, ".5e2ms", 2, 2), lexer.next())
        assertEquals(BasToken(BasTokenKind.NUMBER, "9", 3, 1), lexer.next())
        assertEquals(BasToken(BasTokenKind.END, "", 3, 2), lexer.next())
    }
}
