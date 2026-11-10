package com.android.purebilibili.core.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize

/** Places overlay content inside the first physical safe pane, with optional behind-tap dismiss. */
@Composable
fun HingeSafeOverlayHost(
    regionProvider: (IntSize, IntOffset) -> List<IntRect>,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    onDismissRequest: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier) {
        // A full-window Dialog has no platform "outside" area; handle taps behind the surface.
        if (onDismissRequest != null) {
            Box(Modifier.matchParentSize().pointerInput(onDismissRequest) {
                detectTapGestures { onDismissRequest() }
            })
        }
        WindowRegionLayout(
            modifier = Modifier.fillMaxSize(),
            regionProvider = regionProvider,
            primaryContent = {
                Box(Modifier.fillMaxSize(), contentAlignment = alignment, content = content)
            },
        )
    }
}

/**
 * 播放页输入弹层（评论、弹幕等底部输入面板）统一宿主。
 *
 * 半开折叠姿态（shouldAvoidHinge）时把整个弹层落进 [LocalHingeSafeOverlayRegions]
 * 提供的 sheet 安全区，避免输入控件跨缝或贴缝；平铺窗口退化为全窗口 BottomCenter
 * 容器，由调用方自行限宽。两条路径都在内容层之下处理"点击空白处关闭"。
 *
 * 调用方需在 [modifier] 上自带 `imePadding()` 等系统栏避让——铰链安全区域的
 * provider 依赖宿主传入真实可见窗口的尺寸与原点。
 */
@Composable
fun HingeSafeInputOverlayHost(
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.BottomCenter,
    onDismissRequest: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val sheetRegions = LocalHingeSafeOverlayRegions.current.sheet
    if (sheetRegions != null) {
        HingeSafeOverlayHost(
            regionProvider = sheetRegions,
            modifier = modifier,
            alignment = alignment,
            onDismissRequest = onDismissRequest,
            content = content,
        )
    } else {
        Box(modifier) {
            if (onDismissRequest != null) {
                Box(
                    Modifier.matchParentSize().pointerInput(onDismissRequest) {
                        detectTapGestures { onDismissRequest() }
                    }
                )
            }
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = alignment,
                content = content,
            )
        }
    }
}
