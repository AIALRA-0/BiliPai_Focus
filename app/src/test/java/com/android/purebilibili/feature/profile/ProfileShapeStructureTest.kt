package com.android.purebilibili.feature.profile

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ProfileShapeStructureTest {

    @Test
    fun `profile surfaces use semantic theme shapes`() {
        val screenSource = loadSource("ProfileScreen.kt")
        val skeletonSource = loadSource("ProfileLoadingSkeleton.kt")

        assertTrue(screenSource.contains("AppShapes.borderedContainer(ContainerLevel.Sheet)"))
        assertTrue(screenSource.contains("AppShapes.container(ContainerLevel.Pill)"))
        assertTrue(screenSource.contains("AppShapes.borderedContainer(ContainerLevel.Card)"))
        assertTrue(screenSource.contains("AppThemeAdaptiveTabRow("))
        assertTrue(screenSource.contains("scrollable = true"))
        assertTrue(screenSource.contains("minTabWidth = 72.dp"))
        assertTrue(!screenSource.contains("forceLiquidChrome = true"))
        assertTrue(skeletonSource.contains("AppShapes.container(ContainerLevel.Sheet)"))
        assertTrue(!screenSource.contains("RoundedCornerShape("))
        assertTrue(!skeletonSource.contains("RoundedCornerShape("))
    }

    private fun loadSource(fileName: String): String {
        val path = "src/main/java/com/android/purebilibili/feature/profile/$fileName"
        return File(path).readText()
    }
}
