package com.android.purebilibili.feature.video.ui.overlay

import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.core.util.HapticType

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntRect
import com.android.purebilibili.core.store.PlaybackCompletionBehavior
import com.android.purebilibili.feature.video.ui.components.PlayerListPopupPlacement
import com.android.purebilibili.feature.video.ui.components.PlayerSelectionPopup
import com.android.purebilibili.feature.video.ui.components.PlayerSelectionItem
import com.android.purebilibili.feature.video.ui.components.playerPopupSelectedOption

@Composable
internal fun PlaybackOrderSelectionSheet(
    currentBehavior: PlaybackCompletionBehavior,
    onSelect: (PlaybackCompletionBehavior) -> Unit,
    onDismiss: () -> Unit,
    isFullscreen: Boolean = false,
    placement: PlayerListPopupPlacement = if (isFullscreen) PlayerListPopupPlacement.END else PlayerListPopupPlacement.CENTER,
    anchorBounds: IntRect? = null,
) {
    val selectionHaptic = rememberHapticFeedback(com.android.purebilibili.core.ui.AppHapticScene.MENUS)
    PlayerSelectionPopup(title = "选择播放顺序", showTitle = false,
        onDismissRequest = onDismiss, placement = placement, anchorBounds = anchorBounds) {
        playerPlaybackOrderOptions.forEachIndexed { index, behavior ->
            val selected = currentBehavior == behavior
            PlayerSelectionItem(text = behavior.label, selected = selected, enabled = !selected,
                index = index, optionCount = playerPlaybackOrderOptions.size,
                modifier = Modifier.playerPopupSelectedOption(selected),
                onClick = {
                    if (!selected) selectionHaptic(HapticType.LIGHT)
                    onSelect(behavior)
                    onDismiss()
                })
        }
    }
}
