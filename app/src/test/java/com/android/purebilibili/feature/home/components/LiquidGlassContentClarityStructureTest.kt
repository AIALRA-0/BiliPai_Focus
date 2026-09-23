package com.android.purebilibili.feature.home.components

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.readText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidGlassContentClarityStructureTest {

    @Test
    fun `zero content distortion reaches every shared floating glass shell`() {
        val componentsDir = workspaceRoot().resolve(
            "app/src/main/java/com/android/purebilibili/feature/home/components"
        )
        val floatingDockChrome = componentsDir.resolve("FloatingDockChrome.kt").readText()
        val floatingBottomBar = componentsDir.resolve("FloatingBottomBar.kt").readText()
        assertTrue(
            floatingDockChrome.contains("lensIntensity.coerceIn(0f, 1f) *") &&
                floatingDockChrome.contains(
                    "liquidGlassTuning.contentDistortionScale.coerceIn(0f, 1.8f)"
                )
        )
        assertTrue(floatingDockChrome.contains("val shouldDrawLens = distortionScale > 0.001f"))

        assertTrue(floatingBottomBar.contains("val shellRefractionHeightDp = shellLensDp *"))
        assertTrue(floatingBottomBar.contains("val shellRefractionAmountDp = shellLensDp *"))
        assertEquals(2, Regex("refractionHeight = shellRefractionHeightPx").findAll(floatingBottomBar).count())
        assertEquals(2, Regex("refractionAmount = shellRefractionAmountPx").findAll(floatingBottomBar).count())
    }

    private fun workspaceRoot() = generateSequence(
        Paths.get(System.getProperty("user.dir")).toAbsolutePath()
    ) { current -> current.parent }
        .first { candidate ->
            Files.exists(
                candidate.resolve(
                    "app/src/main/java/com/android/purebilibili/feature/home/components/FloatingDockChrome.kt"
                )
            )
        }
}
