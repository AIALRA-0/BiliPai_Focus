package com.android.purebilibili.core.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeEffectSnapshotStructureTest {

    @Test
    fun navigationDestinationsReuseTheRootThemeEffectSnapshot() {
        val paths = listOf(
            "app/src/main/java/com/android/purebilibili/feature/search/SearchScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/dynamic/components/DynamicTopBar.kt",
            "app/src/main/java/com/android/purebilibili/feature/partition/PartitionScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/list/CommonListScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/profile/ProfileScreen.kt",
        )

        paths.forEach { path ->
            val source = loadSource(path)
            assertTrue(source.contains("LocalAppThemeConfig.current"), path)
            assertFalse(source.contains("getHeaderBlurEnabled(context)"), path)
            assertFalse(source.contains("getProgressiveTopBlurEnabled(context)"), path)
            assertFalse(source.contains("getBottomBarBlurEnabled(context)"), path)
        }
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        return listOf(File(path), File(normalizedPath)).firstOrNull(File::exists)?.readText()
            ?: error("Cannot locate $path from ${File(".").absolutePath}")
    }
}
