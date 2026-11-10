package com.android.purebilibili.feature.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import kotlin.math.roundToInt

private class ProfileTabsAnchor {
    // 标签先测量，分页后测量：占位在同一轮测量中直接使用标签的实际高度。
    var heightPx = 0
    var rootTopPx by mutableFloatStateOf(0f)
    val pageTopPx = mutableStateMapOf<Int, Float>()
}

/** 玻璃栏固定挂载在分页外，只从各页占位读取纵向位置；原生栏仍由各页持有。 */
@Composable
internal fun ProfileSpaceTabsHost(
    sharedGlassTabs: Boolean,
    currentPage: () -> Int,
    tabsVisible: () -> Boolean,
    tabs: @Composable (Modifier) -> Unit,
    nativeTabs: @Composable (Int, Modifier) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (@Composable (Int, Modifier) -> Unit) -> Unit,
) {
    if (!sharedGlassTabs) {
        Box(modifier) { content(nativeTabs) }
        return
    }
    val anchor = remember { ProfileTabsAnchor() }
    Layout(
        modifier = modifier
            .clipToBounds()
            .onGloballyPositioned { anchor.rootTopPx = it.positionInRoot().y },
        content = {
            Box(Modifier.fillMaxSize()) {
                content { page, slotModifier ->
                    Spacer(
                        slotModifier.fillMaxWidth()
                            .layout { measurable, constraints ->
                                val height = anchor.heightPx.coerceIn(constraints.minHeight, constraints.maxHeight)
                                val child = measurable.measure(
                                    constraints.copy(minHeight = height, maxHeight = height),
                                )
                                layout(child.width, child.height) { child.placeRelative(0, 0) }
                            }
                            .onGloballyPositioned { anchor.pageTopPx[page] = it.positionInRoot().y },
                    )
                }
            }
            // 该实例不属于任何 page key，切页不会替换其拖动、吸附和按压状态。
            tabs(Modifier)
        },
    ) { measurables, constraints ->
        val tabBar = measurables[1].measure(constraints.copy(minHeight = 0))
        anchor.heightPx = tabBar.height
        val pager = measurables[0].measure(constraints)
        layout(pager.width, pager.height) {
            pager.placeRelative(0, 0)
            val top = anchor.pageTopPx[currentPage()]
            val y = if (top != null && tabsVisible()) {
                (top - anchor.rootTopPx).roundToInt()
            } else -tabBar.height * 2
            // 横向坐标始终属于宿主；不采纳分页补偿位移后的 x 坐标。
            tabBar.placeRelative(0, y)
        }
    }
}
