package com.android.purebilibili.core.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

internal fun copyPlainTextToClipboard(
    context: Context,
    text: String,
    label: String = "BiliPai"
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
