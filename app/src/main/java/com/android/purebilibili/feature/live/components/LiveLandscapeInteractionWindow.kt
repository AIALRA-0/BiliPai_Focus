package com.android.purebilibili.feature.live.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.platform.LocalConfiguration
import com.android.purebilibili.core.ui.components.AppGlassMenu
import com.android.purebilibili.core.ui.components.AppGlassMenuSource
import com.android.purebilibili.core.ui.components.LocalAppGlassPanelDismissAction
import com.android.purebilibili.core.ui.components.LocalAppGlassMenuBackdrop
import com.android.purebilibili.core.ui.components.LocalAppGlassMenuDarkTheme
import com.android.purebilibili.core.ui.components.appGlassMenuAvailable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppContentColors
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppText
import top.yukonga.miuix.kmp.blur.Backdrop
import kotlin.math.roundToInt

/** 标题拖动与消息滚动分区；位置随可用窗口夹紧，收起后卸载消息内容。 */
@Composable
internal fun LiveLandscapeInteractionWindow(
    roomKey: String,
    glass: Boolean,
    backdrop: Backdrop?,
    source: AppGlassMenuSource?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onCollapsedChange: (Boolean) -> Unit,
    headerAction: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    var collapsed by rememberSaveable(roomKey) { mutableStateOf(false) }
    var dragX by rememberSaveable(roomKey) { mutableFloatStateOf(0f) }
    var dragY by rememberSaveable(roomKey) { mutableFloatStateOf(0f) }
    val latestCollapsedChange by rememberUpdatedState(onCollapsedChange)
    LaunchedEffect(collapsed) { latestCollapsedChange(collapsed) }
    DisposableEffect(roomKey) { onDispose { latestCollapsedChange(false) } }
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val glassPanel = glass && appGlassMenuAvailable() && source?.isAvailable == true
    if (glassPanel) {
        val panelHeight = if (collapsed) 48.dp else minOf(280.dp, (configuration.screenHeightDp * 0.6f).dp)
        val panelWidth = minOf(260.dp, (configuration.screenWidthDp * 0.32f).dp)
        var glassDragX by rememberSaveable(roomKey) { mutableFloatStateOf(0f) }
        var glassDragY by rememberSaveable(roomKey) { mutableFloatStateOf(0f) }
        CompositionLocalProvider(
            LocalAppGlassMenuBackdrop provides backdrop,
            LocalAppGlassMenuDarkTheme provides true,
        ) {
            AppGlassMenu(
                    expanded = true,
                    source = source,
                    // 布局锚点放在右上安全边距；形变仍捕获真实按钮来源。
                    anchorBounds = with(density) {
                        val right = configuration.screenWidthDp.dp.toPx() - 16.dp.toPx()
                        val top = 56.dp.toPx()
                        Rect(right, top, right, top)
                    },
                    onDismissRequest = onClose,
                    offset = DpOffset(glassDragX.dp, glassDragY.dp),
                    minWidth = panelWidth,
                    maxWidth = panelWidth,
                    maxHeight = (configuration.screenHeightDp * 0.85f).dp,
                    interactiveContent = true,
                    useAvailableHeight = true,
                    content = {
                        val dismiss = LocalAppGlassPanelDismissAction.current
                        Box(Modifier.fillMaxWidth().height(panelHeight)) {
                            LiveInteractionWindowBody(
                                collapsed = collapsed,
                                onToggleCollapsed = { collapsed = !collapsed },
                                onClose = { if (dismiss != null) dismiss(onClose) else onClose() },
                                onDrag = { amount ->
                                    // 宿主夹紧面板边界；限制累计位移，避免拖到边缘后积累空行程。
                                    glassDragX = (glassDragX + with(density) { amount.x.toDp().value })
                                        .coerceIn(-configuration.screenWidthDp * 0.5f, 0f)
                                    glassDragY = (glassDragY + with(density) { amount.y.toDp().value })
                                        .coerceIn(-configuration.screenHeightDp * 0.5f, configuration.screenHeightDp * 0.5f)
                                },
                                headerAction = headerAction,
                                content = content,
                            )
                        }
                    },
                )
        }
        return
    }
    BoxWithConstraints(modifier.safeDrawingPadding().padding(8.dp)) {
        val width = minOf(260.dp, maxWidth * 0.32f)
        val height = if (collapsed) 48.dp else minOf(280.dp, maxHeight * 0.6f)
        val density = LocalDensity.current
        val maxX = with(density) { (maxWidth - width).coerceAtLeast(0.dp).toPx() }
        val maxY = with(density) { (maxHeight - height).coerceAtLeast(0.dp).toPx() }
        val shape = RoundedCornerShape(20.dp)
        val panelModifier = Modifier
            .offset { IntOffset((maxX + dragX).coerceIn(0f, maxX).roundToInt(), dragY.coerceIn(0f, maxY).roundToInt()) }
            .size(width, height)
            .clip(shape)
            .pointerInput(Unit) { detectTapGestures { } }
        val body: @Composable () -> Unit = {
            LiveInteractionWindowBody(
                collapsed = collapsed,
                onToggleCollapsed = { collapsed = !collapsed },
                onClose = onClose,
                onDrag = { amount ->
                    dragX = ((maxX + dragX).coerceIn(0f, maxX) + amount.x).coerceIn(0f, maxX) - maxX
                    dragY = (dragY.coerceIn(0f, maxY) + amount.y).coerceIn(0f, maxY)
                },
                headerAction = headerAction,
                content = content,
            )
        }
        Box(panelModifier.background(AppSurfaceTokens.surfaceContainer())) { body() }
    }
}

/** 仅标题接收拖动，正文继续处理消息滚动、长按菜单与输入。 */
@Composable
private fun LiveInteractionWindowBody(
    collapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    onClose: () -> Unit,
    onDrag: (Offset) -> Unit,
    headerAction: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val latestDrag by rememberUpdatedState(onDrag)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .pointerInput(Unit) {
                        detectDragGestures { change, amount ->
                            change.consume()
                            latestDrag(amount)
                        }
                    }.padding(start = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) { AppText("弹幕互动", color = AppContentColors.primaryText()) }
            if (!collapsed) headerAction()
            AppIconButton(onClick = onToggleCollapsed, modifier = Modifier.size(40.dp)) {
                AppIcon(if (collapsed) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess,
                    contentDescription = if (collapsed) "展开互动窗" else "收起互动窗")
            }
            AppIconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                AppIcon(Icons.Outlined.Close, contentDescription = "关闭互动窗")
            }
        }
        if (!collapsed) Box(Modifier.weight(1f).fillMaxWidth()) { content() }
    }
}
