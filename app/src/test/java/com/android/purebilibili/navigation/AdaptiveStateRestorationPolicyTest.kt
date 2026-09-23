package com.android.purebilibili.navigation

import com.android.purebilibili.core.ui.AppSplitPane
import com.android.purebilibili.feature.home.components.BottomNavItem
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdaptiveStateRestorationPolicyTest {

    @Test
    fun `logical page identity does not include window size or fold posture`() {
        assertEquals("bottom:${ScreenRoutes.Home.route}", resolveBottomPagerSaveableStateKey(BottomNavItem.HOME))
        assertEquals("bottom:${ScreenRoutes.Dynamic.route}", resolveBottomPagerSaveableStateKey(BottomNavItem.DYNAMIC))
        assertEquals(AppSplitPane.Primary.name, "Primary")
        assertEquals(AppSplitPane.Secondary.name, "Secondary")
        assertFalse(resolveBottomPagerSaveableStateKey(BottomNavItem.HOME).contains("Compact"))
        assertFalse(resolveBottomPagerSaveableStateKey(BottomNavItem.HOME).contains("Book"))
    }

    @Test
    fun `player and navigation saveable keys stay independent of display role`() {
        val videoDetail = read("app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt")
        val bangumi = read("app/src/main/java/com/android/purebilibili/feature/bangumi/BangumiPlayerScreen.kt")
        val splitLayout = read("app/src/main/java/com/android/purebilibili/core/ui/SplitLayout.kt")
        val navigation = read("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")

        assertTrue(videoDetail.contains("var userRequestedFullscreen by rememberSaveable"))
        assertFalse(videoDetail.contains("rememberSaveable(windowSizeClass"))
        assertFalse(videoDetail.contains("rememberSaveable(displayContext"))
        assertTrue(bangumi.contains("var userRequestedFullscreen by rememberSaveable(seasonId)"))
        assertTrue(splitLayout.contains("paneStateHolder.SaveableStateProvider(AppSplitPane.Primary.name"))
        assertTrue(splitLayout.contains("paneStateHolder.SaveableStateProvider(AppSplitPane.Secondary.name"))
        assertTrue(navigation.contains("resolveBottomPagerSaveableStateKey(slotItem)"))
        assertFalse(splitLayout.contains("SaveableStateProvider(\"${'$'}width"))
    }

    private fun read(path: String): String {
        val normalized = path.removePrefix("app/")
        val file = listOf(File(path), File(normalized)).firstOrNull { it.exists() }
            ?: error("Cannot locate $path")
        return file.readText()
    }
}
