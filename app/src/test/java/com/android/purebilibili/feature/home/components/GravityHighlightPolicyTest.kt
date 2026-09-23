package com.android.purebilibili.feature.home.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GravityHighlightPolicyTest {

    @Test
    fun `nearby gravity samples collapse onto the same highlight direction`() {
        val base = quantizeGravityHighlightDirection(gravityX = 0.20f, gravityY = -0.98f)
        val nearby = quantizeGravityHighlightDirection(gravityX = 0.22f, gravityY = -0.97f)

        assertEquals(base.first, nearby.first, 0.0001f)
        assertEquals(base.second, nearby.second, 0.0001f)
    }

    @Test
    fun `missing gravity falls back to straight up`() {
        val direction = quantizeGravityHighlightDirection(gravityX = 0f, gravityY = 0f)

        assertEquals(0f, direction.first, 0.0001f)
        assertEquals(-1f, direction.second, 0.0001f)
    }

    @Test
    fun `sensor state is deferred to the draw phase through a shared state holder`() {
        val root = listOf(File("."), File("..")).first { File(it, "app/src/main").exists() }
        val shared = File(
            root,
            "app/src/main/java/com/android/purebilibili/feature/home/components/" +
                "FloatingDockChrome.kt",
        ).readText()
        val floatingBar = File(
            root,
            "app/src/main/java/com/android/purebilibili/feature/home/components/" +
                "FloatingBottomBar.kt",
        ).readText()
        val legacyBar = File(
            root,
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt",
        ).readText()

        assertTrue(shared.contains("): State<Highlight>"))
        assertTrue(shared.contains("quantizedDirection.value"))
        assertFalse(shared.contains("val tilt by rememberDeviceTilt()"))
        assertFalse(floatingBar.contains("fun rememberGravityRotatedHighlight("))
        assertFalse(legacyBar.contains("fun rememberGravityRotatedHighlight("))
    }
}
