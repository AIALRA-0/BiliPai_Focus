package com.android.purebilibili.feature.video.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoContentDeferredPaddingTest {
    @Test
    fun `one padding instance follows measured header height without losing other insets`() {
        var headerHeightPx = 92f
        val padding = videoContentAdditionalTopPadding(
            contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 20.dp, bottom = 80.dp),
            extraTopPx = { headerHeightPx },
            density = Density(2f),
        )
        assertEquals(54.dp, padding.calculateTopPadding())
        // A wrapped toolbar grows while its list and bottom-bar insets keep the same identity.
        headerHeightPx = 160f
        assertEquals(88.dp, padding.calculateTopPadding())
        assertEquals(80.dp, padding.calculateBottomPadding())
        assertEquals(12.dp, padding.calculateLeftPadding(LayoutDirection.Ltr))
        assertEquals(20.dp, padding.calculateLeftPadding(LayoutDirection.Rtl))
        headerHeightPx = -1f
        assertEquals(8.dp, padding.calculateTopPadding())
    }

    @Test
    fun `existing callers retain their explicit dp inset`() {
        val padding = videoContentAdditionalTopPadding(PaddingValues(top = 8.dp, bottom = 24.dp), 46.dp)
        assertEquals(54.dp, padding.calculateTopPadding())
        assertEquals(24.dp, padding.calculateBottomPadding())
    }
}
