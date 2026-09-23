package com.android.purebilibili.navigation3.predictiveback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.io.File

class BiliPaiMiuixNavTransitionTest {

    @Test
    fun coveredPageBlurStartsStrongAndClearsWithNavigationDepth() {
        assertEquals(0f, resolveMiuixNavCoveredBlurProgress(-1f))
        assertEquals(0f, resolveMiuixNavCoveredBlurProgress(0f))
        assertEquals(0.0625f, resolveMiuixNavCoveredBlurProgress(0.25f))
        assertEquals(0.25f, resolveMiuixNavCoveredBlurProgress(0.5f))
        assertEquals(1f, resolveMiuixNavCoveredBlurProgress(1f))
        assertEquals(1f, resolveMiuixNavCoveredBlurProgress(2f))
    }

    @Test
    fun allAnimatedStylesShareTheRealtimeCoveredBlurWrapper() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/navigation3/predictiveback/BiliPaiMiuixNavTransition.kt"),
            File("src/main/java/com/android/purebilibili/navigation3/predictiveback/BiliPaiMiuixNavTransition.kt"),
        ).first(File::exists).readText()

        assertTrue(source.contains("NONE -> return NoPredictiveBackTransition"))
        assertTrue(source.contains("else NavTransitions.MiuixDefault"))
        assertTrue(source.contains("return realtimeCoveredBlurTransition("))
        assertTrue(source.contains("baseTransition = baseTransition"))
    }

    @Test
    fun miuixStyleUsesLibraryDefaultForProgrammaticPush() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/navigation3/predictiveback/MiuixPredictiveBackProgressTransition.kt"),
            File("src/main/java/com/android/purebilibili/navigation3/predictiveback/MiuixPredictiveBackProgressTransition.kt"),
        ).first(File::exists).readText()

        assertTrue(source.contains("push = NavTransitions.MiuixDefault"))
        assertTrue(source.contains("pop = NavTransitions.MiuixDefault"))
        assertTrue(source.contains("predictivePop = pop"))
    }
}
