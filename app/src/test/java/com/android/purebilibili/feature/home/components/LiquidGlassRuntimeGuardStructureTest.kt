package com.android.purebilibili.feature.home.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiquidGlassRuntimeGuardStructureTest {

    @Test
    fun `runtime guard trims secondary effects without overriding primary glass toggle`() {
        val floatingChrome = source("FloatingDockChrome.kt")
        val floatingBar = source("FloatingBottomBar.kt")
        val progressiveTop = source("ProgressiveTopChrome.kt")
        val bottomBar = source("BottomBar.kt")
        val homeHeader = source("HomeHeader.kt")
        val videoCard = source("cards/VideoCard.kt")

        assertFalse(floatingChrome.contains("isLowBlurBudgetForced()"))
        assertTrue(floatingBar.contains("PlainMiuixFloatingBottomBar("))
        assertFalse(floatingBar.contains("isLowBlurBudgetForced()"))
        assertTrue(progressiveTop.contains("isLowBlurBudgetForced()"))
        assertFalse(bottomBar.contains("isLowBlurBudgetForced("))
        assertTrue(homeHeader.contains("!isLowBlurBudgetForced(forceLowBlurBudget)"))
        assertFalse(homeHeader.contains("val isLiquidGlassMode = false"))
        assertTrue(homeHeader.contains("renderMode == HomeTopChromeRenderMode.LIQUID_GLASS_BACKDROP"))
        assertTrue(homeHeader.contains("if (isLiquidGlassMode && !useProgressiveTopBlur)"))
        assertTrue(videoCard.contains("!isLowBlurBudgetForced()"))
    }

    private fun source(name: String): String {
        val root = listOf(File("."), File("..")).first { File(it, "app/src/main").exists() }
        return File(
            root,
            "app/src/main/java/com/android/purebilibili/feature/home/components/$name",
        ).readText()
    }
}
