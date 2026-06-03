package com.android.purebilibili.feature.home.components.cards

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoCardRememberOptimizationStructureTest {

    @Test
    fun elegantVideoCard_groupsPillColorsAndSharedTransitionSpecs() {
        val source = File("src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt")
            .readText()

        assertTrue(source.contains("val pillColors = remember(glassEnabled, blurEnabled, inlinePillBaseColor)"))
        assertFalse(source.contains("val coverPillColors = rememberHomeGlassPillColors("))
        assertFalse(source.contains("val emphasizedCoverPillColors = rememberHomeGlassPillColors("))
        assertFalse(source.contains("val inlinePillColors = rememberHomeGlassPillColors("))
        assertTrue(source.contains("val homeSharedTransitionSpecs = remember("))
        assertTrue(source.contains("VideoCardSharedTransitionSpecs("))
        assertFalse(source.contains("val homeSharedTransitionVisualSpec = remember("))
    }
}
