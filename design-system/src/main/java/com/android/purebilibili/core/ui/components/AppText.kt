package com.android.purebilibili.core.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.android.purebilibili.core.ui.LocalAppThemeConfig
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

private fun Modifier.globalTextTapCopy(text: String): Modifier = composed {
    if (text.isBlank() || !LocalAppThemeConfig.current.globalTextTapCopyEnabled) {
        return@composed this
    }
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    pointerInput(text, context) {
        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Final,
            )
            val startPosition = down.position
            var gestureCanceled = down.isConsumed
            var upTimeMillis: Long? = null
            while (upTimeMillis == null && !gestureCanceled) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null) {
                    gestureCanceled = true
                    break
                }
                if (
                    change.isConsumed ||
                    (change.position - startPosition).getDistance() > viewConfiguration.touchSlop
                ) {
                    gestureCanceled = true
                }
                if (!change.pressed) {
                    upTimeMillis = change.uptimeMillis
                }
            }
            val pressDurationMillis = (upTimeMillis ?: down.uptimeMillis) - down.uptimeMillis
            if (
                shouldCopyGlobalTextTap(
                    text = text,
                    globalCopyEnabled = true,
                    gestureCanceled = gestureCanceled,
                    pressDurationMillis = pressDurationMillis,
                    longPressTimeoutMillis = viewConfiguration.longPressTimeoutMillis,
                )
            ) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("BiliPai 文本", text.trim()))
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                    Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

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
