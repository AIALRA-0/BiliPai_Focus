package com.android.purebilibili.feature.video.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class BottomInputBarStructureTest {

    @Test
    fun bottomInputBar_usesSolidSurfaceColor() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/ui/components/BottomInputBar.kt")
            .readText()

        assertTrue(source.contains("MaterialTheme.colorScheme.surface"))
        assertTrue(!source.contains("HazeState"))
        assertTrue(!source.contains("liquidGlassBackground"))
    }
}
