package com.android.purebilibili.core.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdaptiveLoadingIndicatorIntegrationTest {

    @Test
    fun sharedEntryPointsRouteThroughAdaptiveLoading() {
        val adaptive = loadSource(
            "design-system/src/main/java/com/android/purebilibili/core/ui/AdaptiveLoadingIndicator.kt"
        )
        val lottie = loadSource(
            "app/src/main/java/com/android/purebilibili/core/ui/LottieComponents.kt"
        )

        assertTrue(adaptive.contains("LoadingIndicator("))
        assertTrue(adaptive.contains("MiuixInfiniteProgressIndicator("))
        assertTrue(adaptive.contains("MiuixCircularProgressIndicator("))
        assertTrue(lottie.contains("AdaptiveLoadingIndicator("))
    }

    @Test
    fun iosRendererShell_isRemovedAfterMigration() {
        val iosRenderer = listOf(
            File("design-system/src/main/java/com/android/purebilibili/core/ui/IosLoadingIndicator.kt"),
            File("../design-system/src/main/java/com/android/purebilibili/core/ui/IosLoadingIndicator.kt"),
        )
        val adaptive = loadSource(
            "design-system/src/main/java/com/android/purebilibili/core/ui/AdaptiveLoadingIndicator.kt"
        )

        // 6A 迁移：iOS 渲染器壳已删除，adaptive 通道只保留双值视觉。
        assertEquals(false, iosRenderer.any { it.exists() })
        assertFalse(adaptive.contains("IosCutePersonLoadingIndicator"))
        assertFalse(adaptive.contains("IOS_CUTE_PERSON"))
    }

    @Test
    fun cutePersonEntryPointDispatchesThroughAdaptiveLoading() {
        val lottie = loadSource(
            "app/src/main/java/com/android/purebilibili/core/ui/LottieComponents.kt"
        )

        assertTrue(lottie.contains("AdaptiveLoadingIndicator("))
        assertTrue(lottie.contains("fun CutePersonLoadingIndicator("))
        assertFalse(lottie.contains("internal fun IosCutePersonLoadingIndicator("))
    }

    @Test
    fun emptyStateUsesCuteRemoteTelegramRawAnimation() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/core/ui/LottieComponents.kt"
        )

        assertTrue(
            source.contains(
                "const val EMPTY = \"https://raw.githubusercontent.com/DrKLO/Telegram/master/TMessagesProj/src/main/res/raw/utyan_empty2.json\""
            )
        )
        assertFalse(source.contains("lf20_wnqlfojb.json"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        return listOf(
            File(path),
            File("../$path"),
            File(normalizedPath),
        ).first { it.exists() }.readText()
    }
}
