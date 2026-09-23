package com.android.purebilibili.feature.home

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HomeFeedSkeletonVisualSpecTest {
    @Test
    fun splitSkeletonSurfaces_useNamedComplementaryShapes() {
        val coverShape = assertIs<RoundedCornerShape>(resolveHomeSkeletonCoverShape(12.dp))
        val infoShape = assertIs<RoundedCornerShape>(resolveHomeSkeletonInfoShape(12.dp))

        assertEquals(
            RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = 0.dp,
                bottomEnd = 0.dp,
            ),
            coverShape,
        )
        assertEquals(
            RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 0.dp,
                bottomStart = 12.dp,
                bottomEnd = 12.dp,
            ),
            infoShape,
        )
    }
}
