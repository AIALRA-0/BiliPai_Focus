package com.android.purebilibili.danmaku.parser.bas

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BasScriptParserTest {
    @Test
    fun `public minimal text gets defaults and animation lifetime`() {
        val program = BasScriptParser.parse("""
            def text demo { content = "BAS" }
            set demo { alpha = 0 } 5s
        """.trimIndent())
        val element = program.elements.single()
        assertEquals(BasElementType.TEXT, element.type)
        assertEquals("BAS", program.textContent)
        assertEquals(5000L, element.durationMs)
        assertEquals(25.0, element.attributes.number("fontSize"))
        assertEquals("SimHei", element.attributes.text("fontFamily"))
        assertEquals(1.0, element.attributes.number("bold"))
        assertEquals(0L, program.transitions.single().startTimeMs)
    }

    @Test
    fun `templates bind named arguments first and recursively bind aggregates`() {
        val program = BasScriptParser.parse("""
            def button T(label="default" destination=1m position=10% curve="ease-in") {
                text=label x=position target=seek {time=destination}
                metadata=custom {label=label values=[1,curve]}
            }
            let a = T("hello", position=50%, destination=20.5s500ms,)
            let b = (a {text="copied" x=25%})
        """.trimIndent())
        assertEquals(listOf("a", "b"), program.elements.map { it.name })
        val a = program.elements[0]
        val b = program.elements[1]
        assertEquals("hello", a.attributes.text("text"))
        assertEquals(BasValue.Number(50.0, BasUnit.PERCENT), a.attributes["x"])
        assertEquals(BasTarget.Seek(21000), a.target)
        assertEquals("copied", b.attributes.text("text"))
        assertEquals(BasTarget.Seek(21000), b.target)
        val metadata = assertIs<BasValue.Object>(a.attributes["metadata"])
        assertEquals("custom", metadata.type)
        assertEquals(BasValue.Text("hello"), metadata.attributes["label"])
        assertEquals(listOf(BasValue.Number(1.0), BasValue.Text("ease-in")), assertIs<BasValue.Array>(metadata.attributes["values"]).values)
    }

    @Test
    fun `constructor and template calls are valid anonymous animation targets`() {
        val program = BasScriptParser.parse("""
            def text T(c="default") { content=c }
            set (T("animated")) { alpha=0 } 1s
            set (text {content="inline"}) {x=20%} 2s
            let copy=(T("copy") {x=30%})
        """.trimIndent())
        assertEquals(3, program.elements.size)
        assertEquals(2, program.transitions.size)
        assertEquals("copy", program.elements.single { it.name == "copy" }.attributes.text("content"))
        assertEquals(program.elements.take(2).map { it.name }, program.transitions.map { it.elementName })
    }

    @Test
    fun `recursive parallel and serial groups flatten cross object delays`() {
        val program = BasScriptParser.parse("""
            def text a {} def text b {} def text c {}
            { set a {x=10} 1s then set b {y=20} 2s
              {set c {alpha=.5} 500ms set a {} 4s} }
            then {set b {content="after"} 3s then set c {x=30} 1s}
            set c {color=0xff0000} 500ms
        """.trimIndent())
        assertEquals(listOf(0L, 1000L, 0L, 0L, 4000L, 7000L, 0L), program.transitions.map { it.startTimeMs })
        assertEquals(8000L, program.durationMs)
        assertEquals(7000L, program.elements.single { it.name == "b" }.durationMs)
        assertEquals(2, program.transitions.map { it.group }.distinct().size)
        assertEquals((0..6).toList(), program.transitions.map { it.order })
    }

    @Test
    fun `compound times scientific decimals hexadecimal and escapes retain types`() {
        val program = BasScriptParser.parse("""
            // Comment accepted through the end of the line.
            def text @a${'$'}_9 {
                content="中文\n\r\t\\\'\"\x41\u4e2d"
                duration=1h5m30s250ms x=-.5e2 % y=+1e-3 color=0Xabcdef
            }
        """.trimIndent())
        val element = program.elements.single()
        assertEquals(3930250L, element.durationMs)
        assertEquals("中文\n\r\t\\'\"A中", element.attributes.text("content"))
        assertEquals(BasValue.Number(-50.0, BasUnit.PERCENT), element.attributes["x"])
        assertEquals(.001, element.attributes.number("y"))
        assertEquals(11259375.0, element.attributes.number("color"))
    }

    @Test
    fun `typed targets preserve bvid and compound seek times`() {
        val program = BasScriptParser.parse("""
            def button seekButton {target=seek {time=30m}}
            def button videoButton {target=av {bvid="BV1test" time=1m30s}}
            def button episodeButton {target=bangumi {seasonId=1699 episodeId=80041 time=250ms}}
        """.trimIndent())
        assertEquals(BasTarget.Seek(1800000), program.elements[0].target)
        assertEquals(BasTarget.Video(null, "BV1test", 1, 90000), program.elements[1].target)
        assertEquals(BasTarget.Bangumi(1699, 80041, 250), program.elements[2].target)
    }

    @Test
    fun `explicit lifetime wins and immutable set properties are ignored`() {
        val program = BasScriptParser.parse("""
            def text a {duration=1s parent="missing"}
            def button b {} def path p {}
            set a {duration=10s parent="b" zIndex=9 fontFamily="other" anchorX=1 x=50%} 3s
            set b {alpha=0 scale=2 text="changed" x=20} 2s
            set p {d="M0 0" scale=2 y=10} 2s
        """.trimIndent())
        assertEquals(1000L, program.elements[0].durationMs)
        assertEquals("missing", program.elements[0].parentName)
        assertEquals(setOf("x"), program.transitions[0].properties.keys)
        assertEquals(setOf("text", "x"), program.transitions[1].properties.keys)
        assertEquals(setOf("y"), program.transitions[2].properties.keys)
        assertEquals(2000L, program.durationMs)
    }

    @Test
    fun `unanimated elements default to four seconds and zero is explicit`() {
        val program = BasScriptParser.parse("def text a {} def text b {duration=0ms}")
        assertEquals(listOf(4000L, 0L), program.elements.map { it.durationMs })
    }

    @Test
    fun `last repeated attributes and redefinitions win`() {
        val program = BasScriptParser.parse("def text a {content=\"old\"} def text a {x=1 x=2 content=\"new\"}")
        assertEquals(1, program.elements.size)
        assertEquals(2.0, program.elements.single().attributes.number("x"))
        assertEquals("new", program.textContent)
    }

    @Test
    fun `generated anonymous names never replace declared identifiers`() {
        val program = BasScriptParser.parse("def text @bas_0 {content=\"keep\"} let a=text {content=\"new\"}")
        assertEquals(listOf("@bas_0", "a"), program.elements.map { it.name })
        assertEquals("keep", program.elements.first().attributes.text("content"))
    }

    @Test
    fun `parent cycles are rejected including self parent`() {
        assertFailsWith<BasParseException> { BasScriptParser.parse("def text a {parent=\"b\"} def text b {parent=\"a\"}") }
        assertFailsWith<BasParseException> { BasScriptParser.parse("def text a {parent=\"a\"}") }
    }

    @Test
    fun `lexical and grammar failures carry source positions`() {
        val error = assertFailsWith<BasParseException> { BasScriptParser.parse("def text a {}\nset unknown {} 1s") }
        assertEquals(2, error.line)
        assertTrue(error.column > 1)
        for (source in listOf(
            "def text a {content='single'}", "def text a {content=\"\\b\"}",
            "def text a {content=\"\\u12\"}", "def text a {content=\"line\nline\"}",
            "/* unsupported */ def text a {}", "def text 中文 {}", "def text a {x=1.}",
            "def text a {x=1e999}", "def text a {duration=-1s}", "def text a {duration=1s1m}",
            "def text a {color=-0xff}", "let a=123", "let a=unknown {}", "let a=Missing()",
            "def unknown a {}", "def text a {} set a {}", "def text a {} set a {} 1s;",
            "def text a {} apply a(x=1)", "def text a {} clone a", "def text a {} set a {x=a.x} 1s",
            "def text T(x=1) {x=x} let a=T(y=2)", "def text T(x=1) {x=x} let a=T(1,2)",
            "def text T(x=1) {x=missing} let a=T()", "def text T(x=1,y=2) {}",
            "def button a {target=url {time=1s}}", "def button a {target=av {page=1}}",
            "def text a {} set a {x=[50%, \"unknown\"]} 1s", "def text a {} set a {} 1s, \"cubic-bezier(2,0,1,1)\"",
            "def text a {} set a {} 1s, \"steps(1,jump-none)\""
        )) {
            val failure = assertFailsWith<BasParseException>(source) { BasScriptParser.parse(source) }
            assertTrue(failure.line >= 1, source)
            assertTrue(failure.column >= 1, source)
            assertFalse(failure.message.isNullOrBlank(), source)
        }
    }

}
