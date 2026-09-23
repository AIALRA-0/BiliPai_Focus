package com.android.purebilibili.feature.video.ui.section

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoDetailActionRowPolicyTest {

    @Test
    fun shareActionTextUsesFormattedCountOrShareLabel() {
        assertEquals("999", resolveVideoDetailShareActionText(999))
        assertEquals("1.2万", resolveVideoDetailShareActionText(12_000))
        assertEquals("分享", resolveVideoDetailShareActionText(0))
    }

    @Test
    fun actionRowSpacingTightensForSixOrMoreActions() {
        assertEquals(2.dp, resolveVideoDetailActionRowItemSpacing(5))
        assertEquals(0.dp, resolveVideoDetailActionRowItemSpacing(6))
        assertEquals(4.dp, resolveVideoDetailActionButtonHorizontalPadding(5))
        assertEquals(2.dp, resolveVideoDetailActionButtonHorizontalPadding(6))
    }
}
