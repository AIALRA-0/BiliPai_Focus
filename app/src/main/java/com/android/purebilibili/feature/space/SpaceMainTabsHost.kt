package com.android.purebilibili.feature.space

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.IntOffset

/** 玻璃主栏只有一个指针与动画所有者；分页、吸顶副本仅保留等高占位。 */
@Composable
internal fun SpaceMainTabsHost(
    sharedGlassTabs: Boolean,
    position: () -> IntOffset?,
    modifier: Modifier = Modifier,
    tabs: @Composable () -> Unit,
    content: @Composable (mainTabsSlot: (@Composable () -> Unit)?) -> Unit,
) {
    if (!sharedGlassTabs) {
        Box(modifier) { content(null) }
        return
    }
    val measuredHeight = remember { mutableIntStateOf(0) }
    Layout(
        modifier = modifier.clipToBounds(),
        content = {
            Box(Modifier.fillMaxSize()) {
                content {
                    Spacer(Modifier.fillMaxWidth().layout { measurable, constraints ->
                        val height = measuredHeight.intValue.coerceIn(constraints.minHeight, constraints.maxHeight)
                        val child = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                        layout(child.width, child.height) { child.placeRelative(0, 0) }
                    })
                }
            }
            tabs()
        },
    ) { measurables, constraints ->
        val rail = measurables[1].measure(constraints.copy(minHeight = 0))
        measuredHeight.intValue = rail.height
        val pages = measurables[0].measure(constraints)
        layout(pages.width, pages.height) {
            pages.placeRelative(0, 0)
            // 位置只在放置阶段读取；滚动不重建主栏，离开视口时不放置也不接受触摸。
            position()?.let { rail.placeRelative(it.x, it.y) }
        }
    }
}
