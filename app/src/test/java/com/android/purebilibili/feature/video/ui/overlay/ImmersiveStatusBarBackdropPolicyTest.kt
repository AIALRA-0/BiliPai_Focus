package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImmersiveStatusBarBackdropPolicyTest {

    @Test
    fun `ambient haze keeps captured video colors free of theme tint`() {
        val style = resolveVideoStatusBarAmbientHazeStyle()

        assertEquals(Color.Black, style.backgroundColor)
        assertTrue(style.colorEffects.isEmpty())
        assertEquals(24.dp, style.blurRadius)
        assertEquals(0f, style.noiseFactor)
    }

    @Test
    fun `ambient capture balances visual freshness with low cpu and battery overhead`() {
        assertTrue(VIDEO_STATUS_BAR_AMBIENT_CAPTURE_INTERVAL_MS >= 500L)
        assertEquals(96, VIDEO_STATUS_BAR_AMBIENT_SAMPLE_WIDTH_PX)
        assertEquals(54, VIDEO_STATUS_BAR_AMBIENT_SAMPLE_HEIGHT_PX)
    }

    @Test
    fun `ambient letterbox clips its blur and does not depend on global header blur`() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/video/ui/overlay/ImmersiveStatusBarBackdrop.kt"
        ).readText()
        val letterboxBody = source.substringAfter("internal fun ImmersiveAmbientLetterboxBackdrop(")
            .substringBefore("internal fun resolvePortraitLetterboxBarHeightPx(")

        assertTrue(letterboxBody.contains(".clipToBounds()"))
        assertTrue(letterboxBody.contains("surfaceType = BlurSurfaceType.GENERIC"))
    }
}
