package com.android.purebilibili.feature.video.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

internal fun resolveModalRemainingScrollConsumption(availableY: Float): Float = availableY

@Composable
internal fun rememberModalChildScrollConnection(): NestedScrollConnection {
    return remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source != NestedScrollSource.UserInput || available.y == 0f) {
                    return Offset.Zero
                }

                // 子列表先正常消费手势，仅截断到达边界后的剩余位移，避免详情页跟随滚动。
                return Offset(
                    x = 0f,
                    y = resolveModalRemainingScrollConsumption(available.y)
                )
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                return Velocity(x = 0f, y = available.y)
            }
        }
    }
}
