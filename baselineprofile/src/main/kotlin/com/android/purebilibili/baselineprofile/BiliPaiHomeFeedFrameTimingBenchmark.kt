package com.android.purebilibili.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode.WARM
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Ignore("pixel6Api31 managed devices do not emit stable home-feed RenderThread slices; keep video detail and startup benchmarks as the active perf gate.")
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
            clickBottomTab("首页")
        }
    ) {
        // Pair the feed swipe with deterministic tab transitions so the trace
        // always contains concrete UI redraw work on clean managed devices.
        repeat(2) {
            swipeVertical(down = true)
            swipeVertical(down = false)
            clickBottomTab("动态")
            clickBottomTab("首页")
        }
    }

    private fun MacrobenchmarkScope.clickBottomTab(label: String) {
        val byDesc = device.wait(Until.findObject(By.desc(label)), 2_000)
        if (byDesc != null) {
            byDesc.click()
            device.waitForIdle()
            return
        }

        val byText = device.wait(Until.findObject(By.text(label)), 2_000)
        if (byText != null) {
            byText.click()
            device.waitForIdle()
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
