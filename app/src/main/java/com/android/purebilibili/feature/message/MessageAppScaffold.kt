package com.android.purebilibili.feature.message

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.ui.ImmersiveAppScaffold

/** Capture the same opaque wallpaper + content composite that the message page displays. */
@Composable
internal fun MessageAppScaffold(
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    blurContentReady: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    // The scaffold's content source replays the root-owned static wallpaper. Dynamic
    // backgrounds stay root-owned too; do not create a second media renderer in each list.
    ImmersiveAppScaffold(
        modifier = modifier,
        topBar = topBar,
        blurContentReady = blurContentReady,
        content = content,
    )
}
