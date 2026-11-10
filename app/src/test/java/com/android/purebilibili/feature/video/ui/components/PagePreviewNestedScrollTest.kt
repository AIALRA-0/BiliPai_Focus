package com.android.purebilibili.feature.video.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PagePreviewNestedScrollTest {
    @Test
    fun edgeDragStopsAtPartListAndLeavesVerticalScrollForParent() {
        for (remainingX in listOf(-24f, 0f, 24f)) {
            assertEquals(
                Offset(remainingX, 0f),
                PagePreviewNestedScrollConnection.onPostScroll(
                    consumed = Offset.Zero,
                    available = Offset(remainingX, 18f),
                    source = NestedScrollSource.UserInput,
                ),
            )
        }
    }

    @Test
    fun remainingFlingDoesNotReachPagerInEitherDirection() = runTest {
        for (remainingX in listOf(-1200f, 0f, 1200f)) {
            assertEquals(
                Velocity(remainingX, 0f),
                PagePreviewNestedScrollConnection.onPostFling(
                    consumed = Velocity(300f, 0f),
                    available = Velocity(remainingX, -600f),
                ),
            )
        }
    }

    @Test
    fun childRetainsItsOwnScrollAndFlingBeforeBoundaryConsumption() = runTest {
        assertEquals(
            Offset.Zero,
            PagePreviewNestedScrollConnection.onPreScroll(
                available = Offset(-24f, 18f),
                source = NestedScrollSource.UserInput,
            ),
        )
        assertEquals(
            Velocity.Zero,
            PagePreviewNestedScrollConnection.onPreFling(Velocity(-1200f, -600f)),
        )
    }
}
