package com.android.purebilibili.feature.home.components.cards

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoCardCoverOverlayTextPolicyTest {

    @Test
    fun coverOverlayTextShadowIsSoftAndReadable() {
        val shadow = resolveVideoCardCoverOverlayTextShadow()
        assertTrue(shadow.blurRadius in 2.5f..4.5f)
        assertTrue(shadow.color.alpha in 0.4f..0.7f)
        assertTrue(shadow.offset.y > 0f)
    }

    @Test
    fun elegantVideoCardAppliesCoverOverlayTextShadow() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt"
        ).readText()
        assertTrue(source.contains("resolveVideoCardCoverOverlayTextShadow()"))
        assertTrue(source.contains("style = coverOverlayTextStyle"))
    }

    @Test
    fun coverDurationTextUsesSharedOverlayShadow() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCardCoverDurationText.kt"
        ).readText()
        assertTrue(source.contains("resolveVideoCardCoverOverlayTextShadow()"))
        assertTrue(source.contains("feedContentTypography().coverBadge"))
        assertFalse(source.contains("Color.Black.copy"))
        assertFalse(source.contains("AppSurface("))
    }
}
