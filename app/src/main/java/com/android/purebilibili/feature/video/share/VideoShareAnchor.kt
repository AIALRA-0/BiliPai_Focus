package com.android.purebilibili.feature.video.share

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.ui.components.AppGlassMenuSource
import com.android.purebilibili.core.ui.components.appGlassMenuAvailable
import com.android.purebilibili.core.ui.components.appGlassMenuSource
import com.android.purebilibili.core.ui.components.rememberAppGlassMenuSource

/** 同一详情页的多个分享按钮各自测量，在点击时选择真实来源。 */
@Stable
internal class VideoShareAnchorState {
    var source by mutableStateOf<AppGlassMenuSource?>(null)
}

internal val LocalVideoShareAnchorState = compositionLocalOf<VideoShareAnchorState?> { null }

@Composable
internal fun VideoShareActionAnchor(
    onClick: () -> Unit,
    content: @Composable (onClick: () -> Unit) -> Unit,
) {
    val state = LocalVideoShareAnchorState.current
    if (state == null || !appGlassMenuAvailable()) {
        content(onClick)
        return
    }
    val source = rememberAppGlassMenuSource()
    val open = { state.source = source; onClick() }
    Box(Modifier.appGlassMenuSource(source, onOpen = open)) { content(open) }
}
