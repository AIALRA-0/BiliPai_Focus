package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.core.util.HapticType
import kotlin.test.Test
import kotlin.test.assertEquals

class ImagePreviewFeedbackPolicyTest {

    @Test
    fun longPressSaveStartFeedback_usesMediumHaptic() {
        assertEquals(HapticType.MEDIUM, resolveImagePreviewLongPressSaveStartFeedback())
    }

    @Test
    fun saveFeedback_usesLightHapticOnSuccess() {
        assertEquals(HapticType.LIGHT, resolveImagePreviewSaveFeedback(success = true))
    }

    @Test
    fun saveFeedback_usesHeavyHapticOnFailure() {
        assertEquals(HapticType.HEAVY, resolveImagePreviewSaveFeedback(success = false))
    }

    @Test
    fun imageShareMimeType_preservesAnimatedAndStaticFormats() {
        assertEquals("image/gif", resolveImageShareMimeType("https://i0.hdslb.com/a.gif"))
        assertEquals("image/webp", resolveImageShareMimeType("https://i0.hdslb.com/a.webp@640w"))
        assertEquals("image/png", resolveImageShareMimeType("https://i0.hdslb.com/a.png"))
        assertEquals("image/jpeg", resolveImageShareMimeType("https://i0.hdslb.com/a.jpg"))
    }
}
