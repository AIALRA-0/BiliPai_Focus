package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.currentAppTextStyle
import com.android.purebilibili.core.ui.renderer.material3.AppMaterial3Text
import com.android.purebilibili.core.ui.renderer.miuix.AppMiuixText

internal fun shouldCopyGlobalTextTap(
    text: String,
    globalCopyEnabled: Boolean,
    gestureCanceled: Boolean,
    pressDurationMillis: Long,
    longPressTimeoutMillis: Long,
): Boolean = globalCopyEnabled &&
    text.isNotBlank() &&
    !gestureCanceled &&
    pressDurationMillis in 0 until longPressTimeoutMillis.coerceAtLeast(1L)

@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    autoSize: TextAutoSize? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = currentAppTextStyle(),
    tapToCopyEnabled: Boolean = true,
) {
    if (appTextSearchMatchRanges(text, LocalAppTextSearchQuery.current).isNotEmpty()) {
        AppText(
            text = appSearchHighlightedText(text), modifier = modifier, color = color,
            autoSize = autoSize, fontSize = fontSize, fontStyle = fontStyle, fontWeight = fontWeight,
            fontFamily = fontFamily, letterSpacing = letterSpacing, textDecoration = textDecoration,
            textAlign = textAlign, lineHeight = lineHeight, overflow = overflow, softWrap = softWrap,
            maxLines = maxLines, minLines = minLines, onTextLayout = onTextLayout ?: {}, style = style,
            tapToCopyEnabled = tapToCopyEnabled,
        )
        return
    }
    val effectiveModifier = if (tapToCopyEnabled) modifier.globalTextTapCopy(text) else modifier
    when (LocalAppUiStyle.current) {
        AppUiStyle.MATERIAL3 -> AppMaterial3Text(
            text, effectiveModifier, color, autoSize, fontSize, fontStyle, fontWeight, fontFamily,
            letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap, maxLines,
            minLines, onTextLayout, style,
        )
        AppUiStyle.MIUIX -> AppMiuixText(
            text, effectiveModifier, color, autoSize, fontSize, fontStyle, fontWeight, fontFamily,
            letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap, maxLines,
            minLines, onTextLayout, style,
        )
    }
}

@Composable
fun AppText(
    text: String,
    color: ColorProducer,
    modifier: Modifier = Modifier,
    autoSize: TextAutoSize? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = currentAppTextStyle(),
    tapToCopyEnabled: Boolean = true,
) {
    if (appTextSearchMatchRanges(text, LocalAppTextSearchQuery.current).isNotEmpty()) {
        AppText(
            text = appSearchHighlightedText(text), color = color, modifier = modifier,
            autoSize = autoSize, fontSize = fontSize, fontStyle = fontStyle, fontWeight = fontWeight,
            fontFamily = fontFamily, letterSpacing = letterSpacing, textDecoration = textDecoration,
            textAlign = textAlign, lineHeight = lineHeight, overflow = overflow, softWrap = softWrap,
            maxLines = maxLines, minLines = minLines, onTextLayout = onTextLayout ?: {}, style = style,
            tapToCopyEnabled = tapToCopyEnabled,
        )
        return
    }
    val effectiveModifier = if (tapToCopyEnabled) modifier.globalTextTapCopy(text) else modifier
    when (LocalAppUiStyle.current) {
        AppUiStyle.MATERIAL3 -> AppMaterial3Text(
            text, color, effectiveModifier, autoSize, fontSize, fontStyle, fontWeight, fontFamily,
            letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap, maxLines,
            minLines, onTextLayout, style,
        )
        AppUiStyle.MIUIX -> AppMiuixText(
            text, color, effectiveModifier, autoSize, fontSize, fontStyle, fontWeight, fontFamily,
            letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap, maxLines,
            minLines, onTextLayout, style,
        )
    }
}

@Composable
fun AppText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    autoSize: TextAutoSize? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = currentAppTextStyle(),
    tapToCopyEnabled: Boolean = true,
) {
    val effectiveModifier = if (tapToCopyEnabled) modifier.globalTextTapCopy(text.text) else modifier
    when (LocalAppUiStyle.current) {
        AppUiStyle.MATERIAL3 -> AppMaterial3Text(
            text, effectiveModifier, color, autoSize, fontSize, fontStyle, fontWeight, fontFamily,
            letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap, maxLines,
            minLines, inlineContent, onTextLayout, style,
        )
        AppUiStyle.MIUIX -> AppMiuixText(
            text, effectiveModifier, color, autoSize, fontSize, fontStyle, fontWeight, fontFamily,
            letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap, maxLines,
            minLines, inlineContent, onTextLayout, style,
        )
    }
}

@Composable
fun AppText(
    text: AnnotatedString,
    color: ColorProducer,
    modifier: Modifier = Modifier,
    autoSize: TextAutoSize? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = currentAppTextStyle(),
    tapToCopyEnabled: Boolean = true,
) {
    val effectiveModifier = if (tapToCopyEnabled) modifier.globalTextTapCopy(text.text) else modifier
    when (LocalAppUiStyle.current) {
        AppUiStyle.MATERIAL3 -> AppMaterial3Text(
            text, color, effectiveModifier, autoSize, fontSize, fontStyle, fontWeight, fontFamily,
            letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap, maxLines,
            minLines, inlineContent, onTextLayout, style,
        )
        AppUiStyle.MIUIX -> AppMiuixText(
            text, color, effectiveModifier, autoSize, fontSize, fontStyle, fontWeight, fontFamily,
            letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap, maxLines,
            minLines, inlineContent, onTextLayout, style,
        )
    }
}
