package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.android.purebilibili.core.ui.components.AppGlassMenuSource
import com.android.purebilibili.core.ui.components.appGlassMenuAvailable
import com.android.purebilibili.core.ui.components.appGlassMenuSource
import com.android.purebilibili.core.ui.components.rememberAppGlassMenuSource

internal enum class FavoriteFolderMenuControl { PLAYER, DETAIL }

/** 来源图层随页面保留，控制栏临时隐藏时仍可完成返回动画。 */
@Stable
internal class FavoriteFolderMenuAnchor(
    val player: AppGlassMenuSource,
    val detail: AppGlassMenuSource,
) {
    var source: AppGlassMenuSource? by mutableStateOf(null)
}

internal val LocalFavoriteFolderMenuAnchor = compositionLocalOf<FavoriteFolderMenuAnchor?> { null }

@Composable
internal fun rememberFavoriteFolderMenuAnchor(): FavoriteFolderMenuAnchor {
    val player = rememberAppGlassMenuSource()
    val detail = rememberAppGlassMenuSource()
    return remember(player, detail) { FavoriteFolderMenuAnchor(player, detail) }
}

@Composable
internal fun Modifier.favoriteFolderMenuSource(
    control: FavoriteFolderMenuControl = FavoriteFolderMenuControl.PLAYER,
): Modifier {
    val anchor = LocalFavoriteFolderMenuAnchor.current ?: return this
    if (!appGlassMenuAvailable()) return this
    val source = when (control) {
        FavoriteFolderMenuControl.PLAYER -> anchor.player
        FavoriteFolderMenuControl.DETAIL -> anchor.detail
    }
    // 仅记录实际触碰的入口，不消费事件，保留收藏点击与长按手势。
    return appGlassMenuSource(source).pointerInput(anchor, source) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            anchor.source = source
            waitForUpOrCancellation(pass = PointerEventPass.Initial)
        }
    }
}
