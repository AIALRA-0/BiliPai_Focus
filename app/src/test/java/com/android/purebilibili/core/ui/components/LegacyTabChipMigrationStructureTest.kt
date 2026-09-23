package com.android.purebilibili.core.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LegacyTabChipMigrationStructureTest {
    @Test
    fun `known page selectors cannot regress to filter chips`() {
        val migratedScreens = listOf(
            "app/src/main/java/com/android/purebilibili/feature/home/HomeComponents.kt",
            "app/src/main/java/com/android/purebilibili/feature/list/FavoriteCategoryScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/message/InboxScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/watchlater/WatchLaterScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/plugin/js/BiliPaiJsPluginContentScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/video/ui/components/DanmakuSettingsPanel.kt",
        )

        migratedScreens.forEach { path ->
            val source = File(path).readText()
            assertTrue(source.contains("AppThemeAdaptiveTabRow("), path)
            assertFalse(source.contains("AppFilterChip("), path)
        }
    }
}
