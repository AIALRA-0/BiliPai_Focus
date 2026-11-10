package com.android.purebilibili.feature.video.ui.overlay

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoPlayerOverlayDexContractTest {
    private fun registerSlots(types: Array<Class<*>>): Int = types.sumOf {
        if (it == java.lang.Long.TYPE || it == java.lang.Double.TYPE) 2 else 1
    }

    @Test
    fun backdropSlot_capturesOnlyStateAndActions() {
        val projectRoot = listOf(File("."), File(".."))
            .first { File(it, "app/src/main").exists() }
        val source = File(
            projectRoot,
            "app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/VideoPlayerOverlay.kt",
        ).readText()
        val entry = source.substringAfter("fun VideoPlayerOverlay(")
            .substringBefore("private fun VideoPlayerOverlayContent(")
        assertTrue(entry.contains("enabled = true,"))
        val slot = entry.substringAfter("modifier = Modifier.fillMaxSize(),")
            .substringBefore("@Composable")
        assertEquals("{\n        VideoPlayerOverlayContent(state, actions)\n    }\n}", slot.trim().removePrefix(") "))
        val content = source.substringAfter("private fun VideoPlayerOverlayContent(")
        assertTrue(
            !content.contains("PlayerControlsBackdropHost("),
            "The backdrop must wrap the small entry point, not capture the screen's locals",
        )
    }

    @Test
    fun compiledOverlayEntryPoint_fitsDexInvokeRange() {
        val owner = Class.forName(
            "com.android.purebilibili.feature.video.ui.overlay.VideoPlayerOverlayKt",
            false,
            javaClass.classLoader,
        )
        val entries = owner.declaredMethods.filter {
            it.name == "VideoPlayerOverlay" || it.name.startsWith("VideoPlayerOverlay-")
        }
        assertEquals(1, entries.size)
        for (method in entries) {
            val slots = registerSlots(method.parameterTypes)
            assertTrue(slots <= 255, "${method.name} uses $slots argument registers; DEX permits at most 255")
            // Leave room for Compose's generated composer/change-mask parameters.
            assertTrue(method.parameterCount <= 8, "The overlay entry point must retain its small contract")
        }
    }

    @Test
    fun compiledContracts_defaultConstructorsAndCopyMethodsFitDexInvokeRange() {
        for (owner in listOf(VideoPlayerOverlayState::class.java, VideoPlayerOverlayActions::class.java)) {
            for (constructor in owner.declaredConstructors) {
                // invoke-direct also passes the constructed object's receiver.
                val slots = registerSlots(constructor.parameterTypes) + 1
                assertTrue(slots <= 255, "${owner.simpleName} constructor uses $slots argument registers")
            }
            for (method in owner.declaredMethods.filter { it.name == "copy" || it.name == "copy\$default" }) {
                val receiver = if (java.lang.reflect.Modifier.isStatic(method.modifiers)) 0 else 1
                val slots = registerSlots(method.parameterTypes) + receiver
                assertTrue(slots <= 255, "${owner.simpleName}.${method.name} uses $slots argument registers")
            }
        }
    }
}
