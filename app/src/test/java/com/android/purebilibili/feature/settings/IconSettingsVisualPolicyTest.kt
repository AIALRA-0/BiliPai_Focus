package com.android.purebilibili.feature.settings

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class IconSettingsVisualPolicyTest {

    @Test
    fun `icon settings background is opaque to avoid previous page bleed through`() {
        val background = resolveIconSettingsContainerColor(
            background = Color(0xFF101010),
            surfaceVariant = Color(0xFF202020)
        )

        assertEquals(1f, background.alpha)
    }
}
