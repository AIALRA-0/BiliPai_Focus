package com.android.purebilibili.danmaku.parser.bas

import kotlin.math.roundToInt

/** Compiles channels once. update is allocation-free and has no dependence on playback history. */
class BasTimeline(val program: BasProgram) {
    val states: List<BasElementState> = program.elements.map(::BasElementState)
    private val nodes: Array<Node>
    private val layoutOrder: IntArray

    init {
        val names = program.elements.withIndex().associate { it.value.name to it.index }
        nodes = Array(states.size) { index -> Node(states[index], program.transitions.filter { it.elementName == states[index].element.name }) }
        for (index in states.indices) {
            val state = states[index]
            val parent = state.element.parentName?.let { names[it] } ?: -1
            state.parentIndex = if (parent >= 0 && states[parent].element.type == BasElementType.TEXT) parent else -1
        }
        val order = ArrayList<Int>(states.size)
        val visited = BooleanArray(states.size)
        val visiting = BooleanArray(states.size)
        fun visit(index: Int) {
            if (visited[index]) return
            require(!visiting[index]) { "Cyclic BAS parent" }
            visiting[index] = true
            val parent = states[index].parentIndex
            if (parent >= 0) visit(parent)
            visiting[index] = false
            visited[index] = true
            order.add(index)
        }
        for (index in states.indices) visit(index)
        layoutOrder = order.toIntArray()
    }

    fun update(timeMs: Long, stageWidthPx: Float, stageHeightPx: Float, measurer: BasMeasurer) {
        for (index in layoutOrder) {
            val node = nodes[index]
            val state = states[index]
            val parentIndex = state.parentIndex
            val parent = if (parentIndex >= 0) states[parentIndex] else null
            val missingParent = state.element.parentName != null && parent == null
            state.visible = timeMs >= 0 && timeMs < state.element.durationMs && !missingParent && (parent == null || parent.visible)
            if (!state.visible) continue
            val width = parent?.width ?: stageWidthPx
            val height = parent?.height ?: stageHeightPx
            state.x = node.x.evaluate(timeMs, width.toDouble()).toFloat()
            state.y = node.y.evaluate(timeMs, height.toDouble()).toFloat()
            // Non-spatial percentage tokens retain their literal value, as in BAS attribute conversion.
            state.scale = node.scale.evaluate(timeMs, 100.0).toFloat()
            state.alpha = node.alpha.evaluate(timeMs, 100.0).toFloat()
            state.color = node.color.evaluate(timeMs, 100.0).roundToInt() and 0xFFFFFF
            state.rotateX = node.rotateX.evaluate(timeMs, 100.0).toFloat()
            state.rotateY = node.rotateY.evaluate(timeMs, 100.0).toFloat()
            state.rotateZ = node.rotateZ.evaluate(timeMs, 100.0).toFloat()
            // Official BAS uses stage width for all font percentages, including nested text.
            state.fontSize = node.fontSize.evaluate(timeMs, stageWidthPx.toDouble()).toFloat()
            state.content = node.content.evaluate(timeMs)
            measurer.measure(state, width, height)
        }
    }

    private class Node(state: BasElementState, transitions: List<BasTransition>) {
        val x: NumericTrack
        val y: NumericTrack
        val scale: NumericTrack
        val alpha: NumericTrack
        val color: NumericTrack
        val rotateX: NumericTrack
        val rotateY: NumericTrack
        val rotateZ: NumericTrack
        val fontSize: NumericTrack
        val content: TextTrack

        init {
            val attrs = state.element.attributes
            val events = ArrayList<Event>()
            for (transition in transitions) {
                for ((key, raw) in transition.properties) {
                    if (key !in BasScriptParser.mutableProperties(state.element.type)) continue
                    val array = raw as? BasValue.Array
                    val value = array?.values?.firstOrNull() ?: raw
                    val easingName = (array?.values?.getOrNull(1) as? BasValue.Text)?.value ?: transition.easing
                    events.add(Event(key, value, transition.startTimeMs, transition.durationMs, transition.order, BasEasing.parse(easingName)))
                }
            }
            // Conflict is a channel, not a whole set: unrelated alpha/color survive a transform conflict.
            // Source-last wins only for overlapping intervals; touching serial endpoints are not overlap.
            val surviving = events.filter { earlier ->
                events.none { later -> later.order > earlier.order && sameChannel(earlier.key, later.key) && overlaps(earlier, later) }
            }.sortedWith(compareBy<Event> { it.start }.thenBy { it.order })
            val channels = surviving.groupBy { it.key }
            fun numeric(key: String, default: Double, instant: Boolean = false, rgb: Boolean = false, textOnly: Boolean = false): NumericTrack {
                val value = if (!textOnly || state.element.type == BasElementType.TEXT) attrs[key] as? BasValue.Number else null
                return NumericTrack(value ?: BasValue.Number(default), channels[key].orEmpty(), instant, rgb)
            }
            x = numeric("x", 0.0)
            y = numeric("y", 0.0)
            scale = numeric("scale", 1.0)
            alpha = numeric("alpha", 1.0, textOnly = state.element.type != BasElementType.PATH)
            color = numeric("color", 16777215.0, rgb = true, textOnly = true)
            rotateX = numeric("rotateX", 0.0, textOnly = true)
            rotateY = numeric("rotateY", 0.0, textOnly = true)
            rotateZ = numeric("rotateZ", 0.0, textOnly = true)
            fontSize = numeric("fontSize", 25.0, instant = true)
            val key = if (state.element.type == BasElementType.BUTTON) "text" else "content"
            content = TextTrack(attrs.text(key), channels[key].orEmpty())
        }
    }

    private data class Event(val key: String, val value: BasValue, val start: Long, val duration: Long, val order: Int, val easing: BasEasing)
    private data class Coefficients(val pixels: Double, val fraction: Double) {
        fun resolved(size: Double) = pixels + fraction * size
        fun blend(other: Coefficients, p: Double) = Coefficients(pixels + (other.pixels - pixels) * p, fraction + (other.fraction - fraction) * p)
    }
    private class Segment(val start: Long, val duration: Long, val from: Coefficients, val to: Coefficients, val easing: BasEasing) {
        fun progress(time: Long): Double = if (duration == 0L || time >= start + duration) 1.0 else easing.map(((time - start).toDouble() / duration).coerceIn(0.0, 1.0))
    }

    private class NumericTrack(initial: BasValue.Number, events: List<Event>, instant: Boolean, private val rgb: Boolean) {
        private val initial = coefficients(initial)
        private val segments: Array<Segment>

        init {
            val compiled = ArrayList<Segment>(events.size)
            for (event in events) {
                val target = event.value as? BasValue.Number ?: continue
                val previous = compiled.lastOrNull()
                val from = if (previous == null) this.initial else {
                    val p = previous.progress(event.start)
                    if (rgb) Coefficients(blendColor(previous.from.pixels, previous.to.pixels, p), 0.0)
                    else previous.from.blend(previous.to, p)
                }
                compiled.add(Segment(event.start, if (instant) 0L else event.duration, from, coefficients(target), event.easing))
            }
            segments = compiled.toTypedArray()
        }

        fun evaluate(time: Long, size: Double): Double {
            var low = 0
            var high = segments.size - 1
            var selected = -1
            while (low <= high) {
                val middle = (low + high) ushr 1
                if (segments[middle].start <= time) { selected = middle; low = middle + 1 } else high = middle - 1
            }
            if (selected < 0) return initial.resolved(size)
            val segment = segments[selected]
            val p = segment.progress(time)
            if (rgb) return blendColor(segment.from.pixels, segment.to.pixels, p)
            val from = segment.from.resolved(size)
            return from + (segment.to.resolved(size) - from) * p
        }
    }

    private class TextTrack(private val initial: String, events: List<Event>) {
        private val starts = LongArray(events.size) { events[it].start }
        private val values = Array(events.size) { index ->
            when (val value = events[index].value) { is BasValue.Text -> value.value; is BasValue.Reference -> value.name; else -> initial }
        }
        fun evaluate(time: Long): String {
            var low = 0
            var high = starts.size - 1
            var selected = -1
            while (low <= high) {
                val middle = (low + high) ushr 1
                if (starts[middle] <= time) { selected = middle; low = middle + 1 } else high = middle - 1
            }
            return if (selected < 0) initial else values[selected]
        }
    }

    private companion object {
        val transforms = setOf("x", "y", "scale", "rotateX", "rotateY", "rotateZ")
        fun sameChannel(a: String, b: String) = a == b || (a in transforms && b in transforms)
        fun overlaps(a: Event, b: Event): Boolean {
            val aEnd = a.start + a.duration
            val bEnd = b.start + b.duration
            if (a.duration == 0L && b.duration == 0L) return a.start == b.start
            if (a.duration == 0L) return a.start >= b.start && a.start < bEnd
            if (b.duration == 0L) return b.start >= a.start && b.start < aEnd
            return a.start < bEnd && b.start < aEnd
        }
        fun coefficients(value: BasValue.Number) = if (value.unit == BasUnit.PERCENT) Coefficients(0.0, value.value / 100.0) else Coefficients(value.value, 0.0)
        fun blendColor(from: Double, to: Double, progress: Double): Double {
            val a = from.roundToInt()
            val b = to.roundToInt()
            var color = 0
            var shift = 16
            while (shift >= 0) {
                val start = (a ushr shift) and 255
                val end = (b ushr shift) and 255
                val value = (start + (end - start) * progress).roundToInt().coerceIn(0, 255)
                color = color or (value shl shift)
                shift -= 8
            }
            return color.toDouble()
        }
    }
}
