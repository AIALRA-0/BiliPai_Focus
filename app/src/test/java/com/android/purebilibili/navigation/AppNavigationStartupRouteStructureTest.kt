package com.android.purebilibili.navigation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppNavigationStartupRouteStructureTest {

    @Test
    fun cacheMissWaitsForDataStoreBeforeCreatingTheInitialBackStack() {
        val navigation = sourceFile("navigation/AppNavigation.kt").readText()
        val settings = sourceFile("core/store/SettingsManager.kt").readText()

        assertTrue(navigation.contains("produceState<Boolean?>"))
        assertTrue(navigation.contains("resolvedPortraitStartupRoute ?: return"))
        assertTrue(navigation.contains("resolveLaunchToPortraitFeedOnStartup(context)"))
        assertFalse(navigation.contains("isLaunchToPortraitFeedOnStartupSync(context)"))

        val resolver = settings
            .substringAfter("suspend fun resolveLaunchToPortraitFeedOnStartup")
            .substringBefore("// --- 竖屏视频判断比例")
        assertTrue(resolver.contains("settingsDataStore.data.first()"))
        assertFalse(resolver.contains("runBlocking"))
    }

    private fun sourceFile(relativePath: String): File {
        val roots = listOf(
            File("src/main/java/com/android/purebilibili"),
            File("app/src/main/java/com/android/purebilibili"),
        )
        return roots.map { File(it, relativePath) }.firstOrNull { it.exists() }
            ?: error("找不到 $relativePath")
    }
}
