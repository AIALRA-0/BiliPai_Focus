package com.android.purebilibili.feature.dynamic.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class DynamicVideoCardSharedTransitionStructureTest {

    @Test
    fun dynamicVideoCard_usesSeparateCoverAndTitleSharedElements() {
        val source = File("src/main/java/com/android/purebilibili/feature/dynamic/components/VideoCards.kt")
            .readText()

        assertTrue(source.contains("videoCoverSharedElementKey(archive.bvid"))
        assertTrue(source.contains("videoTitleSharedElementKey(archive.bvid)"))
        assertTrue(source.contains("VideoCardLargeCover("))
        assertTrue(source.contains("titleModifier = titleModifier"))
    }
}
