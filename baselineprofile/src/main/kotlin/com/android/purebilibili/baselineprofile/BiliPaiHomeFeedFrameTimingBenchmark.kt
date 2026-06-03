package com.android.purebilibili.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode.WARM
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BiliPaiHomeFeedFrameTimingBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun homeFeedScroll_compilationNone() = scrollFeed(CompilationMode.None())

    @Test
    fun homeFeedScroll_compilationPartial() = scrollFeed(CompilationMode.Partial())

    @Test
    fun homeFeedScroll_compilationFull() = scrollFeed(CompilationMode.Full())

    private fun scrollFeed(compilationMode: CompilationMode) = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = compilationMode,
        iterations = FRAME_TIMING_BENCHMARK_ITERATIONS,
        startupMode = WARM,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            device.waitForIdle()
        }
    ) {
        // Keep gesture pattern deterministic for better cross-run comparison.
        repeat(3) {
            swipeVertical(down = true)
            swipeVertical(down = false)
        }
    }

    private fun MacrobenchmarkScope.swipeVertical(down: Boolean) {
        val x = device.displayWidth / 2
        val yFrom = if (down) (device.displayHeight * 3) / 4 else device.displayHeight / 3
        val yTo = if (down) device.displayHeight / 3 else (device.displayHeight * 3) / 4
        device.swipe(x, yFrom, x, yTo, 24)
        device.waitForIdle()
    }
}
