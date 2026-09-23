package com.android.purebilibili.core.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdaptiveNativeControlAdoptionStructureTest {
    @Test
    fun `standard feature controls use shared component entry points`() {
        val login = File(
            "src/main/java/com/android/purebilibili/feature/login/LoginComponents.kt"
        ).readText()
        val liveArea = File(
            "src/main/java/com/android/purebilibili/feature/live/LiveAreaDetailScreen.kt"
        ).readText()
        val timeline = File(
            "src/main/java/com/android/purebilibili/feature/bangumi/BangumiTimelineScreen.kt"
        ).readText()

        val loginButton = login.substringAfter("fun ModernButton(").substringBefore("\n}\n\n@Composable\nfun TopBar")
        val liveSortChip = liveArea.substringAfter("private fun LiveSortChip(").substringBefore("\n}\n\nprivate fun LiveRoom")
        val dayChip = timeline.substringAfter("private fun DayChip(").substringBefore("\n}\n\n@Composable")

        assertTrue(loginButton.contains("AppButton("))
        assertFalse(loginButton.contains(".clickable("))
        assertTrue(liveSortChip.contains("AppFilterChip("))
        assertFalse(liveSortChip.contains("AppSurface("))
        assertTrue(dayChip.contains("AppFilterChip("))
        assertFalse(dayChip.contains("AppSurface("))
    }
}
