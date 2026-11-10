package com.android.purebilibili.danmaku.parser.bas

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BasTimelineTest {
    private val measurer = BasMeasurer { state, _, _ ->
        state.width = state.content.length * state.fontSize
        state.height = state.fontSize
    }

    private fun timeline(source: String) = BasTimeline(BasScriptParser.parse(source.trimIndent()))
    private fun BasTimeline.frame(time: Long, width: Float = 1000f, height: Float = 500f) = update(time, width, height, measurer)
    private fun near(expected: Float, actual: Float, tolerance: Float = .001f) = assertTrue(abs(expected - actual) <= tolerance, "Expected $expected, got $actual")

    @Test
    fun `minimal public alpha animation and lifetime`() {
        val timeline = timeline("""
            def text demo {content="BAS"}
            set demo {alpha=0} 5s
        """)
        timeline.frame(2500)
        near(.5f, timeline.states.single().alpha)
        assertTrue(timeline.states.single().visible)
        timeline.frame(5000)
        assertFalse(timeline.states.single().visible)
        timeline.frame(0)
        near(1f, timeline.states.single().alpha)
        assertTrue(timeline.states.single().visible)
    }

    @Test
    fun `mixed pixel percent coordinates resolve again after resize and seek`() {
        val timeline = timeline("""
            def text a {x=100 y=10% duration=3s}
            set a {x=50% y=100} 2s
        """)
        val state = timeline.states.single()
        timeline.frame(1000)
        near(300f, state.x)
        near(75f, state.y)
        timeline.frame(1000, 2000f, 1000f)
        near(550f, state.x)
        near(100f, state.y)
        timeline.frame(0, 2000f, 1000f)
        near(100f, state.x)
        near(100f, state.y)
        timeline.frame(2500, 2000f, 1000f)
        near(1000f, state.x)
        timeline.frame(500, 1000f, 500f)
        near(200f, state.x)
    }

    @Test
    fun `content and font size change at set start after an empty delay`() {
        val timeline = timeline("""
            def text a {content="before" fontSize=20 duration=6s}
            set a {} 2s then set a {content="after" fontSize=5%} 3s
        """)
        timeline.frame(1999)
        assertEquals("before", timeline.states.single().content)
        near(20f, timeline.states.single().fontSize)
        timeline.frame(2000)
        assertEquals("after", timeline.states.single().content)
        near(50f, timeline.states.single().fontSize)
        timeline.frame(1000)
        assertEquals("before", timeline.states.single().content)
        near(20f, timeline.states.single().fontSize)
    }

    @Test
    fun `nested groups preserve cross target delays and serial baselines`() {
        val timeline = timeline("""
            def text a {duration=10s} def text b {duration=10s}
            {set a {x=100} 1s then set b {y=100} 2s set a {} 4s}
            then {set a {x=200} 2s then set b {y=200} 1s}
        """)
        timeline.frame(500)
        near(50f, timeline.states[0].x)
        near(0f, timeline.states[1].y)
        timeline.frame(2000)
        near(100f, timeline.states[0].x)
        near(50f, timeline.states[1].y)
        timeline.frame(5000)
        near(150f, timeline.states[0].x)
        near(100f, timeline.states[1].y)
        timeline.frame(6500)
        near(200f, timeline.states[0].x)
        near(150f, timeline.states[1].y)
        timeline.frame(500)
        near(50f, timeline.states[0].x)
        near(0f, timeline.states[1].y)
    }

    @Test
    fun `parent measurement precedes child coordinates and percent font is stage based`() {
        val timeline = timeline("""
            def text child {parent="parent" content="C" x=50% y=50% fontSize=10% duration=6s}
            def text parent {content="AB" fontSize=20 x=100 alpha=.5 duration=4s}
            set parent {} 1s then set parent {content="ABCD" fontSize=40} 2s
        """)
        timeline.frame(500)
        val child = timeline.states[0]
        val parent = timeline.states[1]
        assertEquals(1, child.parentIndex)
        near(20f, child.x)
        near(10f, child.y)
        near(100f, child.fontSize)
        near(1f, child.alpha)
        near(.5f, parent.alpha)
        timeline.frame(1500)
        near(80f, child.x)
        near(20f, child.y)
        timeline.frame(4000)
        assertFalse(parent.visible)
        assertFalse(child.visible)
        timeline.frame(500)
        assertTrue(child.visible)
        near(20f, child.x)
    }

    @Test
    fun `missing parents hide descendants without promoting them to root`() {
        val timeline = timeline("""
            def text a {parent="missing"}
            def text b {parent="a"}
            def text c {}
            def path shape {}
            def text invalidParent {parent="shape"}
        """)
        timeline.frame(0)
        assertFalse(timeline.states[0].visible)
        assertFalse(timeline.states[1].visible)
        assertTrue(timeline.states[2].visible)
        assertFalse(timeline.states[4].visible)
        timeline.frame(-1)
        assertTrue(timeline.states.none { it.visible })
    }

    @Test
    fun `last overlapping property wins but unrelated channels survive`() {
        val timeline = timeline("""
            def text a {duration=5s}
            set a {x=100 alpha=0 color=0xff0000} 2s
            set a {y=200} 2s
            set a {alpha=.5} 2s
        """)
        timeline.frame(1000)
        val state = timeline.states.single()
        near(0f, state.x)
        near(100f, state.y)
        near(.75f, state.alpha)
        assertEquals(0xff8080, state.color)
    }

    @Test
    fun `overlap suppression applies to only conflicting interval not whole set`() {
        val timeline = timeline("""
            def text a {duration=8s}
            set a {x=100 alpha=0} 4s
            set a {} 1s then set a {rotateZ=90} 2s
        """)
        timeline.frame(500)
        near(0f, timeline.states.single().x)
        near(.875f, timeline.states.single().alpha)
        timeline.frame(2000)
        near(45f, timeline.states.single().rotateZ)
        near(.5f, timeline.states.single().alpha)
        timeline.frame(500)
        near(0f, timeline.states.single().rotateZ)
    }

    @Test
    fun `serial touching endpoints do not conflict and fill forwards`() {
        val timeline = timeline("""
            def text a {duration=6s}
            set a {x=100} 1s then set a {x=200} 1s then set a {} 1s
        """)
        timeline.frame(500)
        near(50f, timeline.states.single().x)
        timeline.frame(1500)
        near(150f, timeline.states.single().x)
        timeline.frame(4000)
        near(200f, timeline.states.single().x)
    }

    @Test
    fun `zero duration set is instantaneous and remains seek independent`() {
        val timeline = timeline("""
            def text a {duration=5s}
            set a {} 1s then set a {x=80 content="instant"} 0ms then set a {} 2s
        """)
        timeline.frame(999)
        near(0f, timeline.states.single().x)
        timeline.frame(1000)
        near(80f, timeline.states.single().x)
        assertEquals("instant", timeline.states.single().content)
        timeline.frame(0)
        near(0f, timeline.states.single().x)
    }

    @Test
    fun `property easing overrides block easing`() {
        val timeline = timeline("""
            def text a {duration=4s}
            set a {x=[100,"ease-in"] y=100 alpha=[0,"steps(2, end)"]} 2s, "linear"
        """)
        timeline.frame(500)
        assertTrue(timeline.states.single().x < 25f)
        near(25f, timeline.states.single().y)
        near(1f, timeline.states.single().alpha)
        timeline.frame(1000)
        near(.5f, timeline.states.single().alpha)
    }

    @Test
    fun `all CSS easing curves and steps positions have real semantics`() {
        fun alpha(easing: String, time: Long): Float {
            val timeline = timeline("def text a {duration=3s} set a {alpha=0} 2s, \"$easing\"")
            timeline.frame(time)
            return timeline.states.single().alpha
        }
        near(.75f, alpha("linear", 500))
        assertTrue(alpha("ease", 500) < .75f)
        assertTrue(alpha("ease-in", 500) > .75f)
        assertTrue(alpha("ease-out", 500) < .75f)
        near(.5f, alpha("ease-in-out", 1000))
        near(.75f, alpha("cubic-bezier(0,0,1,1)", 500))
        near(0f, alpha("step-start", 0))
        near(1f, alpha("step-end", 1999))
        near(0f, alpha("step-end", 2000))
        near(.5f, alpha("steps(2, jump-start)", 0))
        near(1f, alpha("steps(2, jump-end)", 0))
        near(1f, alpha("steps(2, jump-none)", 500))
        near(0f, alpha("steps(2, jump-none)", 1000))
        near(2f / 3f, alpha("steps(2, jump-both)", 0))
        near(1f / 3f, alpha("steps(2, jump-both)", 1000))
    }


    @Test
    fun `button and path retain fixed scale and only position animates`() {
        val timeline = timeline("""
            def button b {text="before" scale=2 alpha=.2 rotateZ=45 duration=4s}
            def path p {scale=3 duration=4s}
            set b {x=100 scale=8 alpha=0 text="after" fontSize=30} 2s
            set p {y=100 scale=8} 2s
        """)
        timeline.frame(1000)
        near(50f, timeline.states[0].x)
        near(2f, timeline.states[0].scale)
        near(1f, timeline.states[0].alpha)
        near(0f, timeline.states[0].rotateZ)
        near(30f, timeline.states[0].fontSize)
        assertEquals("after", timeline.states[0].content)
        near(50f, timeline.states[1].y)
        near(3f, timeline.states[1].scale)
    }
}
