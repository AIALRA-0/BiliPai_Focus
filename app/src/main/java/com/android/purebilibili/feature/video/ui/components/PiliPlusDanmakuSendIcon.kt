package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.core.ui.AppContentColors

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.R
import com.android.purebilibili.core.ui.appIconGlyphTextStyle
import com.android.purebilibili.core.ui.components.AppText

private val PiliPlusIconFont = FontFamily(Font(R.font.piliplus_custom_icons))

/** TV-and-danmaku motif with a send arrow, distinct from the settings gear and toggle mark. */
@Composable
fun PiliPlusDanmakuSendIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = AppContentColors.neutralAction(),
) {
    Box(modifier = modifier.size(size).clearAndSetSemantics { contentDescription = "发弹幕" }) {
        Canvas(Modifier.matchParentSize()) {
            scale(this.size.width / 24f, this.size.height / 24f, pivot = Offset.Zero) {
                // Balance the denser TV/character area against the lighter lower-right arrow.
                // The drawn frame spans y 2..23, so top = -0.5 puts its ink centre (12) on the
                // canvas centre; the previous 0.75 rode 5.2% low and made this glyph sit below
                // the font-drawn toggle glyph it is shown next to.
                translate(left = 0.75f, top = -0.5f) {
                val frame = Path().apply {
                    moveTo(19f, 13f)
                    lineTo(19f, 10f)
                    quadraticBezierTo(19f, 6f, 15f, 6f)
                    lineTo(6f, 6f)
                    quadraticBezierTo(2f, 6f, 2f, 10f)
                    lineTo(2f, 17f)
                    quadraticBezierTo(2f, 21f, 6f, 21f)
                    lineTo(12f, 21f)
                }
                drawPath(frame, tint, style = Stroke(1.8f))
                drawLine(tint, Offset(7f, 2f), Offset(10f, 6f), 1.8f)
                drawLine(tint, Offset(15f, 2f), Offset(12f, 6f), 1.8f)
                val arrow = Path().apply {
                    moveTo(18f, 15f)
                    lineTo(23f, 19f)
                    lineTo(18f, 23f)
                    lineTo(18f, 20f)
                    lineTo(15f, 20f)
                    lineTo(15f, 18f)
                    lineTo(18f, 18f)
                    close()
                }
                drawPath(arrow, tint)
                }
            }
        }
        Box(
            modifier = Modifier
                .offset(x = size * (2.75f / 24f), y = size * (5.5f / 24f))
                .size(width = size * (17f / 24f), height = size * (15f / 24f)),
            contentAlignment = Alignment.Center,
        ) {
            AppText(
                text = "弹",
                color = tint,
                fontSize = (size.value * 0.48f).sp,
                lineHeight = (size.value * 0.54f).sp,
                style = MaterialTheme.typography.labelSmall.copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                ),
                tapToCopyEnabled = false,
            )
        }
    }
}

@Composable
fun PiliPlusDanmakuSettingsIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = AppContentColors.accent(),
) = PiliPlusDanmakuGlyph("\uE804", "弹幕设置", tint, size, modifier)

@Composable
internal fun PiliPlusDanmakuGlyph(
    glyph: String,
    description: String,
    tint: Color,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(size).clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = glyph,
            fontFamily = PiliPlusIconFont,
            fontSize = size.value.sp,
            lineHeight = size.value.sp,
            color = tint,
            // Icon-font glyphs and the hand-drawn send glyph then share one vertical centre.
            style = appIconGlyphTextStyle(),
            tapToCopyEnabled = false,
        )
    }
}
