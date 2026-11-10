package com.android.purebilibili.danmaku.parser.bas

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

internal fun interface BasEasing {
    fun map(progress: Double): Double

    companion object {
        private val linear = BasEasing { it }
        private val function = Regex("([a-z-]+)\\s*\\((.*)\\)")

        fun parse(source: String): BasEasing {
            val text = source.trim().lowercase()
            return when (text) {
                "linear" -> linear
                "ease" -> bezier(.25, .1, .25, 1.0)
                "ease-in" -> bezier(.42, 0.0, 1.0, 1.0)
                "ease-out" -> bezier(0.0, 0.0, .58, 1.0)
                "ease-in-out" -> bezier(.42, 0.0, .58, 1.0)
                "step-start" -> steps(1, "jump-start")
                "step-end" -> steps(1, "jump-end")
                else -> {
                    val match = function.matchEntire(text) ?: invalid(source)
                    val args = match.groupValues[2].split(',').map { it.trim() }
                    when (match.groupValues[1]) {
                        "cubic-bezier" -> {
                            if (args.size != 4) invalid(source)
                            val numbers = args.map { it.toDoubleOrNull()?.takeIf(Double::isFinite) ?: invalid(source) }
                            if (numbers[0] !in 0.0..1.0 || numbers[2] !in 0.0..1.0) invalid(source)
                            bezier(numbers[0], numbers[1], numbers[2], numbers[3])
                        }
                        "steps" -> {
                            if (args.size !in 1..2) invalid(source)
                            val count = args[0].toIntOrNull()?.takeIf { it > 0 } ?: invalid(source)
                            val position = if (args.size == 1) "jump-end" else args[1]
                            if (position !in setOf("start", "end", "jump-start", "jump-end", "jump-none", "jump-both")) invalid(source)
                            if (position == "jump-none" && count == 1) invalid(source)
                            steps(count, position)
                        }
                        else -> invalid(source)
                    }
                }
            }
        }

        private fun steps(count: Int, position: String): BasEasing = BasEasing { progress ->
            var step = floor(progress * count).toInt()
            val start = position == "start" || position == "jump-start" || position == "jump-both"
            if (start) step++
            val jumps = when (position) { "jump-none" -> count - 1; "jump-both" -> count + 1; else -> count }
            min(jumps, max(0, step)).toDouble() / jumps
        }

        private fun bezier(x1: Double, y1: Double, x2: Double, y2: Double): BasEasing = BasEasing { x ->
            if (x <= 0.0) 0.0 else if (x >= 1.0) 1.0 else {
                // Bisection also handles flat derivatives and curves with overshooting y handles.
                var low = 0.0
                var high = 1.0
                repeat(36) {
                    val t = (low + high) * .5
                    if (curve(t, x1, x2) < x) low = t else high = t
                }
                curve((low + high) * .5, y1, y2)
            }
        }

        private fun curve(t: Double, a: Double, b: Double): Double {
            val u = 1.0 - t
            return 3.0 * u * u * t * a + 3.0 * u * t * t * b + t * t * t
        }

        private fun invalid(source: String): Nothing = throw IllegalArgumentException("Invalid CSS easing '$source'")
    }
}
