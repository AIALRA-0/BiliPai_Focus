package com.android.purebilibili.feature.video.ui.components.glass

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.android.purebilibili.feature.video.ui.components.PlayerListPopupPlacement
import com.android.purebilibili.feature.video.ui.components.PlayerSelectionPopup
import top.yukonga.miuix.kmp.basic.ListPopupDefaults

/** 保留玻璃布局的调用契约；弹窗本身仅由主题选择原生实现。 */
@Composable
fun PlayerMiuixListPopup(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    placement: PlayerListPopupPlacement = PlayerListPopupPlacement.CENTER,
    maxHeight: Dp? = 440.dp,
    minWidth: Dp = ListPopupDefaults.MinWidth,
    anchorBounds: IntRect? = null,
    content: @Composable () -> Unit,
) {
    PlayerSelectionPopup(
        title = title,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        placement = placement,
        maxHeight = maxHeight,
        minWidth = minWidth,
        anchorBounds = anchorBounds,
        content = content,
    )
}
