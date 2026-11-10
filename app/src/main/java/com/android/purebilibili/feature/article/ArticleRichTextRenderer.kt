// File: feature/article/ArticleRichTextRenderer.kt
package com.android.purebilibili.feature.article

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.theme.resolveAccessibleContainerColors

internal fun resolveArticleTextColor(candidate: Color?, defaultColor: Color, backgroundColor: Color): Color {
    val requested = candidate ?: defaultColor
    if (backgroundColor == Color.Unspecified) return requested
    return resolveAccessibleContainerColors(
        containerColor = backgroundColor,
        contentColor = requested,
        backgroundColor = backgroundColor,
        fallbackContentColors = listOf(defaultColor, Color.Black, Color.White),
    ).contentColor
}

/**
 * 专栏正文的行内富文本渲染：把解析出的字号/颜色/加粗/斜体/删除线
 * span 转成 AnnotatedString。无样式 span 时退化为纯文本。
 */
internal fun buildArticleAnnotatedString(
    spans: List<ArticleTextSpan>,
    defaultColor: Color,
    baseFontSize: TextUnit,
    linkListener: androidx.compose.ui.text.LinkInteractionListener? = null,
    explicitFontScale: Float = 1f,
    backgroundColor: Color = Color.Unspecified,
): AnnotatedString {
    if (spans.none { span ->
            span.fontSizeSp != null || span.colorArgb != null ||
                span.bold || span.italic || span.strikethrough || span.link != null
        }) {
        return AnnotatedString(spans.joinToString(separator = "") { it.text })
    }
    return buildAnnotatedString {
        spans.forEach { span ->
            val text = span.text
            if (text.isEmpty()) return@forEach
            val style = SpanStyle(
                color = resolveArticleTextColor(span.colorArgb?.let(::Color), defaultColor, backgroundColor),
                fontSize = span.fontSizeSp?.let { (it * explicitFontScale).sp } ?: baseFontSize,
                fontWeight = if (span.bold) FontWeight.Bold else null,
                fontStyle = if (span.italic) FontStyle.Italic else null,
                textDecoration = if (span.strikethrough) TextDecoration.LineThrough else null
            )
            if (span.link != null) {
                withLink(androidx.compose.ui.text.LinkAnnotation.Url(span.link, linkInteractionListener = linkListener)) {
                    withStyle(style.copy(textDecoration = TextDecoration.Underline)) { append(text) }
                }
            } else {
                withStyle(style) { append(text) }
            }
        }
    }
}
