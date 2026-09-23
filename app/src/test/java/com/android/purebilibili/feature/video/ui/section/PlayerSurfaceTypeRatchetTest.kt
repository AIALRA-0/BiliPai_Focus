package com.android.purebilibili.feature.video.ui.section

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class PlayerSurfaceTypeRatchetTest {

    @Test
    fun navigationTransformPlayerViewUsesTextureViewSoItCanFollowSharedBounds() {
        val layout = listOf(
            "src/main/res/layout/view_player_texture.xml",
            "app/src/main/res/layout/view_player_texture.xml"
        )
            .map(::File)
            .firstOrNull(File::exists)
            ?: error("找不到 PlayerView 布局")

        // Dedicated texture layout stays texture_view for morph/flip only.
        // Default PlayerView(ctx) path uses SurfaceView so HDR can reach the display.
        assertTrue(layout.readText().contains("app:surface_type=\"texture_view\""))
    }

    @Test
    fun hdrPlaybackForcesSurfaceViewOverNavigationTexture() {
        assertTrue(
            !shouldUseTextureSurfaceForFlip(
                isFlippedHorizontal = false,
                isFlippedVertical = false,
                navigationTransformEnabled = true,
                requiresHdrSurfaceOutput = true
            )
        )
        assertTrue(requiresHdrSurfaceOutput(currentQualityId = 125))
        assertTrue(requiresHdrSurfaceOutput(currentQualityId = 126))
    }
}
