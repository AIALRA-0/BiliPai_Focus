package com.android.purebilibili.core.util

import com.android.purebilibili.feature.video.ui.section.requiresHdrSurfaceOutput
import com.android.purebilibili.feature.video.ui.section.resolveAllowLivePlayerSharedElementForMorph
import com.android.purebilibili.feature.video.ui.section.shouldUseTextureSurfaceForFlip
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoSurfaceCompatibilityTest {
    @Test
    fun android13AffectedVendorsUseCompatibilityIncludingManufacturerAndBrandAliases() {
        for ((manufacturer, brand) in listOf(
            "Xiaomi" to "Redmi",
            "xiaomi" to "POCO",
            "other" to "XIAOMI",
            null to "Redmi",
            "other" to "poco",
            "vivo" to "other",
            "other" to "VIVO",
            null to "iQOO",
        )) {
            assertTrue(prefersTextureVideoSurface(33, manufacturer, brand), "$manufacturer / $brand")
        }
    }

    @Test
    fun unaffectedDevicesAndAndroidVersionsKeepOriginalSurfaceSelection() {
        for (sdk in listOf(29, 30, 31, 32, 34, 35, 36)) {
            assertFalse(prefersTextureVideoSurface(sdk, "Xiaomi", "Redmi"))
            assertFalse(prefersTextureVideoSurface(sdk, "vivo", "iQOO"))
        }
        assertFalse(prefersTextureVideoSurface(33, "Samsung", "Samsung"))
        assertFalse(prefersTextureVideoSurface(33, null, null))
    }

    @Test
    fun affectedDeviceRemainsSafeAcrossSdrHdrAndSdrOutputChangesWithoutEnablingMorph() {
        val compatible = prefersTextureVideoSurface(33, "Xiaomi", "Redmi")
        for ((quality, transfer) in listOf(80 to 0, 125 to 0, 126 to 0, 80 to 6, 80 to 7, 80 to 0)) {
            val hdr = requiresHdrSurfaceOutput(quality, transfer)
            val texture = shouldUseTextureSurfaceForFlip(
                isFlippedHorizontal = false,
                isFlippedVertical = false,
                navigationTransformEnabled = false,
                requiresHdrSurfaceOutput = hdr,
                preferCompatibleTextureSurface = compatible,
            )
            if (hdr) assertFalse(texture) else assertTrue(texture)
            assertFalse(resolveAllowLivePlayerSharedElementForMorph(
                cardTransitionEnabled = true,
                liveSurfaceCardTransitionEnabled = false,
                requiresHdrSurfaceOutput = hdr,
            ))
        }
    }
}
