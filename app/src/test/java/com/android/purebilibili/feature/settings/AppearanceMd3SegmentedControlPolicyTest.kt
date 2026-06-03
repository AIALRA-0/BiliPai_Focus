package com.android.purebilibili.feature.settings

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.AndroidNativeVariant
import kotlin.test.Test
import kotlin.test.assertEquals

class AppearanceMd3SegmentedControlPolicyTest {

    @Test
    fun `material3 segmented control follows material primary roles`() {
        val tokens = resolveMd3SegmentedControlColorTokens(
            androidNativeVariant = AndroidNativeVariant.MATERIAL3,
            materialPrimaryContainer = Color(0xFFFFD8E5),
            materialOnPrimaryContainer = Color(0xFF5F1130),
            materialSurfaceContainerHigh = Color(0xFF281D22),
            materialOnSurfaceVariant = Color(0xFFE6BFCB),
            miuixSecondaryContainer = Color(0xFF7A4828),
            miuixOnSecondaryContainer = Color(0xFFFFE0D1),
            miuixSurfaceContainerHigh = Color(0xFF302322),
            miuixOnSurfaceVariantSummary = Color(0xFFEAD0CD)
        )

        assertEquals(Color(0xFF281D22), tokens.outerContainerColor)
        assertEquals(Color(0xFFFFD8E5), tokens.activeContainerColor)
        assertEquals(Color(0xFF5F1130), tokens.activeContentColor)
        assertEquals(Color(0xFFE6BFCB), tokens.inactiveContentColor)
    }

    @Test
    fun `miuix segmented control keeps miuix secondary roles`() {
        val tokens = resolveMd3SegmentedControlColorTokens(
            androidNativeVariant = AndroidNativeVariant.MIUIX,
            materialPrimaryContainer = Color(0xFFFFD8E5),
            materialOnPrimaryContainer = Color(0xFF5F1130),
            materialSurfaceContainerHigh = Color(0xFF281D22),
            materialOnSurfaceVariant = Color(0xFFE6BFCB),
            miuixSecondaryContainer = Color(0xFF7A4828),
            miuixOnSecondaryContainer = Color(0xFFFFE0D1),
            miuixSurfaceContainerHigh = Color(0xFF302322),
            miuixOnSurfaceVariantSummary = Color(0xFFEAD0CD)
        )

        assertEquals(Color(0xFF302322), tokens.outerContainerColor)
        assertEquals(Color(0xFF7A4828), tokens.activeContainerColor)
        assertEquals(Color(0xFFFFE0D1), tokens.activeContentColor)
        assertEquals(Color(0xFFEAD0CD), tokens.inactiveContentColor)
    }
}
