package com.android.purebilibili.feature.video.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.core.ui.components.AppSurface
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.window.WindowListPopup

enum class PlayerListPopupPlacement {
    CENTER,
    START,
    END,
    END_BOTTOM,
}

private val PlayerCenterPopupPositionProvider = object : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowBounds: IntRect,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
        popupMargin: IntRect,
        alignment: PopupPositionProvider.Align,
    ): IntOffset = IntOffset(
        x = windowBounds.left + (windowBounds.width - popupContentSize.width) / 2,
        y = windowBounds.top + (windowBounds.height - popupContentSize.height) / 2,
    )

    override fun getMargins(): PaddingValues = PaddingValues(0.dp)
}

private val PlayerEndBottomPopupPositionProvider = object : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowBounds: IntRect,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
        popupMargin: IntRect,
        alignment: PopupPositionProvider.Align,
    ): IntOffset = IntOffset(
        x = windowBounds.right - popupContentSize.width - popupMargin.right,
        y = windowBounds.bottom - popupContentSize.height - popupMargin.bottom,
    )

    override fun getMargins(): PaddingValues = PaddingValues(24.dp)
}

/** Shared native Miuix popup shell for player option lists. */
@Composable
fun PlayerMiuixListPopup(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    placement: PlayerListPopupPlacement = PlayerListPopupPlacement.CENTER,
    maxHeight: Dp? = 440.dp,
    minWidth: Dp = ListPopupDefaults.MinWidth,
    content: @Composable () -> Unit,
) {
    val useNativePopup = LocalAppThemeConfig.current.nativeMiuixPopupsEnabled
    val alignment = when (placement) {
        PlayerListPopupPlacement.CENTER,
        PlayerListPopupPlacement.START -> PopupPositionProvider.Align.Start
        PlayerListPopupPlacement.END,
        PlayerListPopupPlacement.END_BOTTOM -> PopupPositionProvider.Align.End
    }
    val horizontalMargin = when (placement) {
        PlayerListPopupPlacement.CENTER -> 0.dp
        PlayerListPopupPlacement.START,
        PlayerListPopupPlacement.END,
        PlayerListPopupPlacement.END_BOTTOM -> 24.dp
    }

    val popupContent: @Composable () -> Unit = {
        ListPopupColumn {
            SmallTitle(text = title)
            content()
        }
    }

    if (!useNativePopup) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest,
                    ),
                contentAlignment = when (placement) {
                    PlayerListPopupPlacement.CENTER -> Alignment.Center
                    PlayerListPopupPlacement.START -> Alignment.CenterStart
                    PlayerListPopupPlacement.END -> Alignment.CenterEnd
                    PlayerListPopupPlacement.END_BOTTOM -> Alignment.BottomEnd
                },
            ) {
                AppSurface(
                    modifier = modifier
                        .widthIn(min = minWidth, max = 320.dp)
                        .then(if (maxHeight != null) Modifier.heightIn(max = maxHeight) else Modifier),
                ) { popupContent() }
            }
        }
        return
    }

    WindowListPopup(
        show = true,
        popupModifier = modifier,
        popupPositionProvider = when (placement) {
            PlayerListPopupPlacement.CENTER -> PlayerCenterPopupPositionProvider
            PlayerListPopupPlacement.END_BOTTOM -> PlayerEndBottomPopupPositionProvider
            else -> ListPopupDefaults.dropdownPositionProvider(
                verticalMargin = 8.dp,
                horizontalMargin = horizontalMargin,
            )
        },
        alignment = alignment,
        enableWindowDim = placement == PlayerListPopupPlacement.CENTER,
        onDismissRequest = onDismissRequest,
        maxHeight = maxHeight,
        minWidth = minWidth,
        content = popupContent,
    )
}
