package com.android.purebilibili.core.ui.wallpaper

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.android.purebilibili.feature.home.isStaticHomeWallpaperUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Legacy imports use .img; inspect their headers before admitting them to static glass. */
@Composable
internal fun rememberIsStaticWallpaper(uri: String): Boolean {
    val context = LocalContext.current
    val needsMetadata = uri.startsWith("content:") ||
        uri.substringBefore('?').substringAfterLast('.').equals("img", ignoreCase = true)
    val result by produceState(
        initialValue = !needsMetadata && isStaticHomeWallpaperUri(uri),
        key1 = uri,
    ) {
        value = if (!needsMetadata) isStaticHomeWallpaperUri(uri) else withContext(Dispatchers.IO) {
            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(Uri.parse(uri))?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
                options.outWidth > 0 && options.outHeight > 0 &&
                    options.outMimeType !in setOf("image/gif", "image/webp")
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
        }
    }
    return result
}
