package com.android.purebilibili.core.theme

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class HardcodedColorMigrationGuardTest {

    @Test
    fun `migrated UI files should not reintroduce raw compose color literals`() {
        val migratedFiles = listOf(
            "src/main/java/com/android/purebilibili/feature/login/LoginComponents.kt",
            "src/main/java/com/android/purebilibili/feature/home/components/MineSideDrawer.kt",
            "src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt",
            "src/main/java/com/android/purebilibili/feature/settings/SettingsEntryVisualPolicy.kt",
            "src/main/java/com/android/purebilibili/feature/settings/ui/CacheClearAnimation.kt",
            "src/main/java/com/android/purebilibili/feature/home/components/LiquidIndicator.kt",
            "src/main/java/com/android/purebilibili/feature/home/components/cards/LiveRoomCard.kt",
            "src/main/java/com/android/purebilibili/feature/profile/ProfileScreen.kt",
            "src/main/java/com/android/purebilibili/feature/download/DownloadListScreen.kt",
            "src/main/java/com/android/purebilibili/feature/video/ui/section/VideoActionSection.kt"
        )
        val literalPattern = Regex("""Color\s*\(\s*0x[0-9A-Fa-f]+""")

        val offenders = migratedFiles.flatMap { path ->
            File(path).readLines().mapIndexedNotNull { index, line ->
                if (literalPattern.containsMatchIn(line)) "$path:${index + 1}: ${line.trim()}" else null
            }
        }

        assertTrue(
            offenders.isEmpty(),
            "Raw Compose color literals should move to theme tokens or explicit semantic policies:\n" +
                offenders.joinToString(separator = "\n")
        )
    }

    @Test
    fun `migrated resource files should reference named colors instead of raw hex attributes`() {
        val migratedResourceFiles = listOf(
            "src/main/res/drawable/ic_shortcut_search.xml",
            "src/main/res/drawable/ic_shortcut_history.xml",
            "src/main/res/drawable/ic_shortcut_dynamic.xml",
            "src/main/res/drawable/ic_shortcut_favorite.xml",
            "src/main/res/drawable/ic_notification.xml",
            "src/main/res/values-night/themes.xml"
        )
        val rawColorAttributePattern = Regex(
            """(?:android:)?(?:tint|fillColor|color)=["]#[0-9A-Fa-f]{6,8}["]"""
        )
        val rawWindowStyleItemPattern = Regex(
            """<item\s+name="android:(?:windowBackground|colorBackground|navigationBarColor)">#[0-9A-Fa-f]{6,8}</item>"""
        )

        val offenders = migratedResourceFiles.flatMap { path ->
            File(path).readLines().mapIndexedNotNull { index, line ->
                if (
                    rawColorAttributePattern.containsMatchIn(line) ||
                    rawWindowStyleItemPattern.containsMatchIn(line)
                ) {
                    "$path:${index + 1}: ${line.trim()}"
                } else {
                    null
                }
            }
        }

        assertTrue(
            offenders.isEmpty(),
            "Migrated XML resources should reference named color resources:\n" +
                offenders.joinToString(separator = "\n")
        )
    }
}
