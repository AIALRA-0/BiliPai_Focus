package com.android.purebilibili.feature.audio.bgm

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.BgmSongHeat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun BgmHeatChart(rawPoints: List<BgmSongHeat>, modifier: Modifier = Modifier) {
    val points = remember(rawPoints) { resolveBgmHeatPoints(rawPoints) }
    if (points.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val measurer = rememberTextMeasurer()
    val dates = remember(points) {
        val formatter = SimpleDateFormat("MM-dd", Locale.getDefault())
        points.map { formatter.format(Date(it.date * 1000L)) }
    }
    val minimum = points.minOf { it.heat }.toDouble()
    val maximum = points.maxOf { it.heat }.toDouble()
    val range = resolveBgmHeatRange(points)
    val lower = range.minimum
    val upper = range.maximum
    val summary = "近${points.size}日热度趋势，${dates.first()}至${dates.last()}，最低${FormatUtils.formatStat(minimum.toLong())}，最高${FormatUtils.formatStat(maximum.toLong())}"
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppText("近${points.size}日热度趋势", style = MaterialTheme.typography.titleMedium)
        Canvas(Modifier.fillMaxWidth().height(240.dp).semantics { contentDescription = summary }) {
            val labels = (0..3).map { i ->
                measurer.measure(FormatUtils.formatStat((upper - (upper - lower) * i / 3).toLong()), labelStyle)
            }
            val left = labels.maxOf { it.size.width }.toFloat() + 8.dp.toPx()
            val top = labels.maxOf { it.size.height }.toFloat() / 2
            val bottom = size.height - labels.maxOf { it.size.height } - 12.dp.toPx()
            val width = (size.width - left - 16.dp.toPx()).coerceAtLeast(1f)
            val height = (bottom - top).coerceAtLeast(1f)
            for (i in 0..3) {
                val y = top + height * i / 3
                drawText(labels[i], topLeft = Offset(left - labels[i].size.width - 6.dp.toPx(), y - labels[i].size.height / 2))
                drawLine(colors.outlineVariant, Offset(left, y), Offset(left + width, y), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())))
            }
            val indices = (0 until minOf(5, points.size)).map { i ->
                if (points.size == 1) 0 else i * (points.size - 1) / (minOf(5, points.size) - 1)
            }
            indices.forEach { index ->
                val x = left + width * index / (points.size - 1).coerceAtLeast(1)
                drawLine(colors.outlineVariant, Offset(x, top), Offset(x, bottom), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())))
                val text = measurer.measure(dates[index], labelStyle)
                drawText(text, topLeft = Offset((x - text.size.width / 2).coerceIn(0f, (size.width - text.size.width).coerceAtLeast(0f)), bottom + 8.dp.toPx()))
            }
            val line = Path()
            points.forEachIndexed { index, point ->
                val x = left + width * index / (points.size - 1).coerceAtLeast(1)
                val y = bottom - ((point.heat - lower) / (upper - lower)).toFloat() * height
                if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
            }
            val area = Path().apply { addPath(line); lineTo(left + if (points.size > 1) width else 0f, bottom); lineTo(left, bottom); close() }
            drawPath(area, Brush.verticalGradient(listOf(colors.primary.copy(alpha = .35f), colors.primary.copy(alpha = .03f)), top, bottom))
            drawPath(line, colors.primary, style = Stroke(2.dp.toPx()))
            if (points.size == 1) drawCircle(colors.primary, 3.dp.toPx(), Offset(left, bottom - ((points.first().heat - lower) / (upper - lower)).toFloat() * height))
            drawRect(colors.outline, topLeft = Offset(left, top), size = Size(width, height), style = Stroke(1.dp.toPx()))
        }
    }
}
