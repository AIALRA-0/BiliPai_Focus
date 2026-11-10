package com.android.purebilibili.danmaku.parser.bas

/** BAS values retain their units until layout; a percentage is not a pixel coordinate. */
sealed interface BasValue {
    data class Number(val value: Double, val unit: BasUnit = BasUnit.NUMBER) : BasValue
    data class Text(val value: String) : BasValue
    data class Object(val type: String, val attributes: Map<String, BasValue>) : BasValue
    data class Array(val values: List<BasValue>) : BasValue
    data class Reference(val name: String) : BasValue
}

enum class BasUnit { NUMBER, PERCENT, TIME }
enum class BasElementType { TEXT, BUTTON, PATH }

data class BasElement(
    val name: String,
    val type: BasElementType,
    val attributes: Map<String, BasValue>,
    val durationMs: Long,
    val parentName: String? = null,
    val target: BasTarget? = null
)

/** A flattened animation; order preserves the source precedence of overlapping sets. */
data class BasTransition(
    val elementName: String,
    val properties: Map<String, BasValue>,
    val startTimeMs: Long,
    val durationMs: Long,
    val easing: String,
    val order: Int,
    val group: Int
)

data class BasProgram(
    val elements: List<BasElement>,
    val transitions: List<BasTransition>,
    val durationMs: Long,
    val textContent: String
)

sealed interface BasTarget {
    data class Seek(val timeMs: Long) : BasTarget
    data class Video(
        val aid: Long?,
        val bvid: String?,
        val page: Int,
        val timeMs: Long
    ) : BasTarget
    data class Bangumi(
        val seasonId: Long?,
        val episodeId: Long?,
        val timeMs: Long
    ) : BasTarget
}

/** One Mode 9 script, independent of the Mode 7 single-text array format. */
data class BasDanmaku(
    val id: Long,
    val startTimeMs: Long,
    val source: String,
    val program: BasProgram,
    val userHash: String = "",
    val weight: Int = 0,
    val isSelf: Boolean = false,
    val color: Int = 0xFFFFFF,
    val colorOverride: Int? = null,
    val fontScale: Float = 1f
) {
    val durationMs: Long get() = program.durationMs
    val content: String get() = program.textContent
}

class BasParseException(
    message: String,
    val line: Int,
    val column: Int
) : IllegalArgumentException("$message at $line:$column")

/** Reused frame state. The timeline and painter do not allocate a new scene on every frame. */
class BasElementState(val element: BasElement) {
    var visible: Boolean = false
    var x: Float = 0f
    var y: Float = 0f
    var width: Float = 0f
    var height: Float = 0f
    var scale: Float = 1f
    var alpha: Float = 1f
    var color: Int = 0xFFFFFF
    var rotateX: Float = 0f
    var rotateY: Float = 0f
    var rotateZ: Float = 0f
    var fontSize: Float = 25f
    var content: String = ""
    var parentIndex: Int = -1
}

/** Measurement writes width/height into the reused state before child coordinates are resolved. */
fun interface BasMeasurer {
    fun measure(
        state: BasElementState,
        containerWidthPx: Float,
        containerHeightPx: Float
    )
}

fun Map<String, BasValue>.number(name: String, fallback: Double = 0.0): Double =
    (get(name) as? BasValue.Number)?.value ?: fallback

fun Map<String, BasValue>.text(name: String, fallback: String = ""): String =
    when (val value = get(name)) {
        is BasValue.Text -> value.value
        is BasValue.Reference -> value.name
        else -> fallback
    }
