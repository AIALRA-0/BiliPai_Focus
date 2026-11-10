package com.android.purebilibili.feature.dynamic.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import coil3.size.Scale
import com.android.purebilibili.feature.dynamic.resolveDynamicSidebarUserAvatarUrl
import com.android.purebilibili.feature.dynamic.resolveDynamicUserAvatarDecodeSizePx

@Composable
internal fun rememberDynamicUserAvatarDecodeSizePx(): Int =
    resolveDynamicUserAvatarDecodeSizePx(LocalDensity.current.density)

@Composable
internal fun rememberDynamicUserAvatarRequest(face: String): ImageRequest {
    val context = LocalContext.current
    val decodeSizePx = rememberDynamicUserAvatarDecodeSizePx()
    return remember(context, face, decodeSizePx) {
        buildDynamicUserAvatarRequest(context, face, decodeSizePx)
    }
}

/** Display and prefetch share bounded decoding and cache sizing in every panel material. */
internal fun buildDynamicUserAvatarRequest(context: Context, face: String, decodeSizePx: Int): ImageRequest =
    ImageRequest.Builder(context.applicationContext)
        .data(resolveDynamicSidebarUserAvatarUrl(face).ifEmpty { null })
        .size(decodeSizePx.coerceIn(48, 256))
        .precision(Precision.EXACT)
        .scale(Scale.FILL)
        .crossfade(false)
        .build()
