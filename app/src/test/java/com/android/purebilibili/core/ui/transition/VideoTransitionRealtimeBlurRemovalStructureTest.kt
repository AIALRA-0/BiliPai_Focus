package com.android.purebilibili.core.ui.transition

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse

class VideoTransitionRealtimeBlurRemovalStructureTest {

    @Test
    fun mainSourcesDoNotContainRemovedVideoTransitionRealtimeBlurImplementation() {
        val source = mainKotlinSources()
        val bannedTokens = listOf(
            "VideoSharedTransitionBackdropHost",
            "fallbackEntryBlurEnabled",
            "transitionMaxBlurRadiusPx"
        )

        bannedTokens.forEach { token ->
            assertFalse(source.contains(token), "主源码仍包含旧视频转场实时模糊 token: $token")
        }
    }

    private fun mainKotlinSources(): String {
        val root = listOf(
            File("app/src/main/java"),
            File("src/main/java")
        ).firstOrNull { it.exists() }
        require(root != null) { "Cannot locate main source root from ${File(".").absolutePath}" }
        return root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString(separator = "\n") { it.readText() }
    }
}
