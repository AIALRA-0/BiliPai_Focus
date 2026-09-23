package com.android.purebilibili.core.ui

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class AppSpacingTokensTest {

    @Test
    fun `spacing tokens follow the shared four dp scale`() {
        assertEquals(0.dp, AppSpacingTokens.None)
        assertEquals(2.dp, AppSpacingTokens.Micro)
        assertEquals(4.dp, AppSpacingTokens.ExtraSmall)
        assertEquals(8.dp, AppSpacingTokens.Small)
        assertEquals(12.dp, AppSpacingTokens.Medium)
        assertEquals(16.dp, AppSpacingTokens.Large)
        assertEquals(24.dp, AppSpacingTokens.ExtraLarge)
        assertEquals(32.dp, AppSpacingTokens.DoubleExtraLarge)
        assertEquals(48.dp, AppSpacingTokens.TripleExtraLarge)
    }
}
