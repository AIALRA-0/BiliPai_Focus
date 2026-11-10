package com.android.purebilibili.feature.video.ui.overlay

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.PathParser
import com.android.purebilibili.danmaku.parser.bas.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

/** Native retained scene: geometry and text layouts survive frame updates and seeks. */
internal class BasScenePainter(val item: BasDanmaku) : BasMeasurer {
    private val timeline = BasTimeline(item.program)
    private val nodes = timeline.states.map { Node(it) }
    private val nodeByState = java.util.IdentityHashMap<BasElementState, Node>().apply {
        for (node in nodes) put(node.state, node)
    }
    private val drawOrder = buildDrawOrder()
    private var userScale = 1f
    private var userWeight = 5
    private var opacity = 1f
    private val point = FloatArray(2)
    private val projection = FloatArray(9)
    private var perspective = 1f

    private fun buildDrawOrder(): List<Int> {
        val sorted = nodes.indices.sortedWith(compareBy<Int> {
            nodes[it].state.element.attributes.number("zIndex")
        }.thenBy { it })
        val children = sorted.groupBy { nodes[it].state.element.parentName }
        val result = ArrayList<Int>(nodes.size)
        val visited = BooleanArray(nodes.size)
        fun append(parent: String?) {
            for (index in children[parent].orEmpty()) {
                if (visited[index]) continue
                visited[index] = true
                result.add(index)
                append(nodes[index].state.element.name)
            }
        }
        append(null)
        return result
    }

    private class Node(val state: BasElementState) {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var layout: StaticLayout? = null
        var lastContent: String? = null
        var lastSize = -1f
        var lastWeight = -1
        val local = FloatArray(16)
        val world = FloatArray(16)
        val screen = Matrix()
        val inverse = Matrix()
        val svgMatrix = Matrix()
        val bounds = RectF()
        val path: Path? = if (state.element.type == BasElementType.PATH) {
            try { PathParser.createPathFromPathData(state.element.attributes.text("d")) }
            catch (_: IllegalArgumentException) { null }
            catch (_: RuntimeException) { null }
        } else null
        val viewBox: FloatArray? = state.element.attributes.text("viewBox").trim()
            .split(Regex("[\\s,]+")) .mapNotNull { it.toFloatOrNull() }
            .takeIf { it.size == 4 && it.all(Float::isFinite) && it[2] > 0f && it[3] > 0f }
            ?.toFloatArray()
        var worldReady = false
        var inheritedAlpha = 0f
        var hittable = false
        init { path?.computeBounds(bounds, true) }
    }

    override fun measure(state: BasElementState, containerWidthPx: Float, containerHeightPx: Float) {
        val node = nodeByState.getValue(state)
        val attrs = state.element.attributes
        if (state.element.type == BasElementType.PATH) {
            val box = node.viewBox
            if (box == null) {
                // Without viewBox, BAS dimensions do not resize or clip the authored path.
                state.width = max(node.bounds.right, node.bounds.width()).coerceAtLeast(0f)
                state.height = max(node.bounds.bottom, node.bounds.height()).coerceAtLeast(0f)
                node.svgMatrix.reset()
            } else {
                val hasWidth = attrs["width"] is BasValue.Number
                val hasHeight = attrs["height"] is BasValue.Number
                state.width = dimension(attrs["width"], containerWidthPx, box[2])
                state.height = dimension(attrs["height"], containerHeightPx, box[3])
                if (hasWidth && !hasHeight) state.height = state.width * box[3] / box[2]
                if (hasHeight && !hasWidth) state.width = state.height * box[2] / box[3]
                val scale = min(state.width / box[2], state.height / box[3])
                node.svgMatrix.setScale(scale, scale)
                node.svgMatrix.postTranslate((state.width - box[2] * scale) / 2f - box[0] * scale,
                    (state.height - box[3] * scale) / 2f - box[1] * scale)
            }
            return
        }
        val size = max(0.01f, state.fontSize * userScale * item.fontScale)
        if (node.lastContent != state.content || node.lastSize != size || node.lastWeight != userWeight) {
            val paint = node.textPaint
            paint.textSize = size
            val bold = attrs.number("bold", if (state.element.type == BasElementType.TEXT) 1.0 else 0.0) != 0.0
            val family = attrs.text("fontFamily", "sans-serif")
            val base = Typeface.create(family, if (bold) Typeface.BOLD else Typeface.NORMAL)
            paint.typeface = if (android.os.Build.VERSION.SDK_INT >= 28)
                Typeface.create(base, (userWeight * 100).coerceIn(if (bold) 700 else 100, 900), false)
            else Typeface.create(base, if (bold || userWeight >= 6) Typeface.BOLD else Typeface.NORMAL)
            var width = 0f
            var start = 0
            for (i in 0..state.content.length) {
                if (i == state.content.length || state.content[i] == '\n') {
                    width = max(width, paint.measureText(state.content, start, i))
                    start = i + 1
                }
            }
            node.layout = StaticLayout.Builder.obtain(state.content, 0, state.content.length, paint,
                max(1, kotlin.math.ceil(width.toDouble()).toInt()))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).build()
            node.lastContent = state.content
            node.lastSize = size
            node.lastWeight = userWeight
        }
        state.width = (node.layout?.width ?: 0).toFloat()
        state.height = (node.layout?.height ?: 0).toFloat()
        if (state.element.type == BasElementType.BUTTON) {
            state.width += 40f
            state.height += 20f
        }
        if (state.element.type == BasElementType.TEXT &&
            attrs["width"] is BasValue.Number && attrs["height"] is BasValue.Number) {
            state.width = dimension(attrs["width"], containerWidthPx, state.width)
            state.height = dimension(attrs["height"], containerHeightPx, state.height)
        }
    }

    fun update(timeMs: Long, width: Float, height: Float, opacity: Float, fontScale: Float, fontWeight: Int) {
        this.opacity = opacity.coerceIn(0f, 1f)
        userScale = fontScale
        userWeight = fontWeight
        perspective = max(1f, width / (2f * tan(Math.toRadians(27.5)).toFloat()))
        timeline.update(timeMs, width, height, this)
        for (index in nodes.indices) {
            nodes[index].worldReady = false
            nodes[index].hittable = false
        }
        for (index in nodes.indices) transform(index, width / 2f, height / 2f)
    }

    private fun transform(index: Int, centerX: Float, centerY: Float) {
        val node = nodes[index]
        if (node.worldReady) return
        // Mark first: malformed parent cycles cannot recurse indefinitely.
        node.worldReady = true
        val state = node.state
        if (!state.visible) {
            node.inheritedAlpha = 0f
            return
        }
        val parent = nodes.getOrNull(state.parentIndex)
        if (parent != null) transform(state.parentIndex, centerX, centerY)
        android.opengl.Matrix.setIdentityM(node.local, 0)
        android.opengl.Matrix.translateM(node.local, 0, state.x, state.y, 0f)
        if (state.rotateX != 0f) android.opengl.Matrix.rotateM(node.local, 0, state.rotateX, 1f, 0f, 0f)
        if (state.rotateY != 0f) android.opengl.Matrix.rotateM(node.local, 0, state.rotateY, 0f, 1f, 0f)
        if (state.rotateZ != 0f) android.opengl.Matrix.rotateM(node.local, 0, state.rotateZ, 0f, 0f, 1f)
        if (state.scale != 1f) android.opengl.Matrix.scaleM(node.local, 0, state.scale, state.scale, state.scale)
        val attrs = state.element.attributes
        android.opengl.Matrix.translateM(node.local, 0,
            -state.width * attrs.number("anchorX").toFloat(),
            -state.height * attrs.number("anchorY").toFloat(), 0f)
        if (parent != null) android.opengl.Matrix.multiplyMM(node.world, 0, parent.world, 0, node.local, 0)
        else System.arraycopy(node.local, 0, node.world, 0, 16)
        val m = node.world
        val d = perspective
        projection[0] = m[0] - centerX * m[2] / d
        projection[1] = m[4] - centerX * m[6] / d
        projection[2] = m[12] - centerX * m[14] / d
        projection[3] = m[1] - centerY * m[2] / d
        projection[4] = m[5] - centerY * m[6] / d
        projection[5] = m[13] - centerY * m[14] / d
        projection[6] = -m[2] / d
        projection[7] = -m[6] / d
        projection[8] = 1f - m[14] / d
        node.screen.setValues(projection)
        node.inheritedAlpha = if (!state.visible || (parent != null && !parent.state.visible)) 0f
            else state.alpha * (parent?.inheritedAlpha ?: opacity)
        node.hittable = state.element.type == BasElementType.BUTTON && state.element.target != null &&
            node.inheritedAlpha > 0f && node.screen.invert(node.inverse)
    }

    fun draw(canvas: Canvas) {
        for (orderIndex in drawOrder.indices) {
            val index = drawOrder[orderIndex]
            val node = nodes[index]
            if (node.inheritedAlpha <= 0f) continue
            val state = node.state
            val attrs = state.element.attributes
            val save = canvas.save()
            canvas.concat(node.screen)
            when (state.element.type) {
                BasElementType.PATH -> node.path?.let { path ->
                    if (node.viewBox != null) canvas.clipRect(0f, 0f, state.width, state.height)
                    canvas.concat(node.svgMatrix)
                    node.paint.style = Paint.Style.FILL
                    node.paint.color = color(item.colorOverride ?: attrs.number("fillColor", 0xFFFFFF.toDouble()).toInt(),
                        node.inheritedAlpha * attrs.number("fillAlpha", 1.0).toFloat())
                    canvas.drawPath(path, node.paint)
                    val border = attrs.number("borderWidth").toFloat()
                    if (border > 0f) {
                        node.paint.style = Paint.Style.STROKE
                        node.paint.strokeWidth = border
                        node.paint.color = color(attrs.number("borderColor").toInt(),
                            node.inheritedAlpha * attrs.number("borderAlpha", 1.0).toFloat())
                        canvas.drawPath(path, node.paint)
                    }
                }
                BasElementType.BUTTON -> {
                    node.paint.style = Paint.Style.FILL
                    node.paint.color = color(attrs.number("fillColor", 0xFFFFFF.toDouble()).toInt(),
                        node.inheritedAlpha * attrs.number("fillAlpha", 1.0).toFloat())
                    canvas.drawRoundRect(0f, 0f, state.width, state.height, 3f, 3f, node.paint)
                    canvas.translate(20f, 10f)
                    node.textPaint.clearShadowLayer()
                    node.textPaint.style = Paint.Style.FILL
                    node.textPaint.color = color(item.colorOverride ?: attrs.number("textColor").toInt(),
                        node.inheritedAlpha * attrs.number("textAlpha", 1.0).toFloat())
                    node.layout?.draw(canvas)
                }
                BasElementType.TEXT -> {
                    val paint = node.textPaint
                    paint.clearShadowLayer()
                    val stroke = attrs.number("strokeWidth").toFloat()
                    if (stroke > 0f) {
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = stroke
                        paint.color = color(attrs.number("strokeColor", 0xFFFFFF.toDouble()).toInt(), node.inheritedAlpha)
                        node.layout?.draw(canvas)
                    }
                    paint.style = Paint.Style.FILL
                    paint.color = color(item.colorOverride ?: state.color, node.inheritedAlpha)
                    if (attrs.number("textShadow", 1.0) != 0.0)
                        paint.setShadowLayer(2f, 1f, 1f, Color.BLACK)
                    node.layout?.draw(canvas)
                }
            }
            canvas.restoreToCount(save)
        }
    }

    fun hit(x: Float, y: Float): BasTarget? {
        for (i in drawOrder.indices.reversed()) {
            val node = nodes[drawOrder[i]]
            val target = node.state.element.target ?: continue
            if (!node.hittable || node.state.element.type != BasElementType.BUTTON) continue
            point[0] = x; point[1] = y
            node.inverse.mapPoints(point)
            if (point[0] >= 0f && point[1] >= 0f && point[0] <= node.state.width && point[1] <= node.state.height)
                return target
        }
        return null
    }

    private fun dimension(value: BasValue?, container: Float, fallback: Float): Float {
        val n = value as? BasValue.Number ?: return max(0f, fallback)
        return max(0f, if (n.unit == BasUnit.PERCENT) container * n.value.toFloat() / 100f else n.value.toFloat())
    }

    private fun color(rgb: Int, alpha: Float): Int =
        (rgb and 0xFFFFFF) or ((alpha.coerceIn(0f, 1f) * 255f + 0.5f).toInt() shl 24)
}
