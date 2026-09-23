package com.android.purebilibili.core.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DefaultPlaybackSpeedControlStructureTest {

    @Test
    fun playbackSpeedControlUsesAppSliderAndText() {
        val source = File(
            "src/main/java/com/android/purebilibili/core/ui/components/DefaultPlaybackSpeedPreferenceControl.kt"
        ).readText()
        assertTrue(source.contains("AppSlider("))
        assertTrue(source.contains("AppText("))
        assertTrue(source.contains("AppSurface("))
        assertFalse(source.contains("androidx.compose.material3.Slider"))
        assertFalse(source.contains("androidx.compose.material3.Text"))
    }
}
