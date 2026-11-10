package com.android.purebilibili.feature.article

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import com.android.purebilibili.core.ui.rememberAppMoreIcon
import com.android.purebilibili.core.ui.components.*

@Composable
internal fun ArticleMoreMenu(
    articleId: Long,
    modifier: Modifier = Modifier,
    onOpenWeb: ((String) -> Unit)? = null,
    onReadingSettings: () -> Unit,
    onReadFromStart: () -> Unit,
    outlineEnabled: Boolean,
    onOpenOutline: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var expanded by remember(articleId) { mutableStateOf(false) }
    val url = "https://www.bilibili.com/read/cv$articleId"
    AppGlassMenuAnchor(modifier = modifier, onOpen = { expanded = true }) {
        AppIconButton(onClick = { expanded = true }) {
            AppIcon(rememberAppMoreIcon(), contentDescription = "更多专栏操作")
        }
        AppDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppDropdownMenuItem(text = { AppText("正文目录") }, enabled = outlineEnabled, onClick = {
                expanded = false
                onOpenOutline()
            })
            AppDropdownMenuItem(text = { AppText("阅读设置") }, onClick = {
                expanded = false
                onReadingSettings()
            })
            AppDropdownMenuItem(text = { AppText("从头阅读") }, onClick = {
                expanded = false
                onReadFromStart()
            })
            AppDropdownMenuItem(text = { AppText("复制链接") }, onClick = {
                expanded = false
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("专栏链接", url))
                Toast.makeText(context, "已复制链接", Toast.LENGTH_SHORT).show()
            })
            AppDropdownMenuItem(text = { AppText("浏览器打开") }, onClick = {
                expanded = false
                try {
                    if (onOpenWeb != null) onOpenWeb(url) else uriHandler.openUri(url)
                } catch (_: IllegalArgumentException) {
                    Toast.makeText(context, "没有可打开链接的浏览器", Toast.LENGTH_SHORT).show()
                } catch (_: android.content.ActivityNotFoundException) {
                    Toast.makeText(context, "没有可打开链接的浏览器", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
