package com.android.purebilibili.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode.WARM
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BiliPaiVideoDetailFrameTimingBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun videoDetailContentScroll_compilationPartial() = benchmarkVideoDetailContentScroll(
        compilationMode = CompilationMode.Partial()
    )

    @Test
    fun videoDetailContentScroll_compilationFull() = benchmarkVideoDetailContentScroll(
        compilationMode = CompilationMode.Full()
    )

    @Test
    fun playerSwipeGesture_compilationPartial() = benchmarkPlayerSwipeGesture(
        compilationMode = CompilationMode.Partial()
    )

    @Test
    fun playerSwipeGesture_compilationFull() = benchmarkPlayerSwipeGesture(
        compilationMode = CompilationMode.Full()
    )

    private fun benchmarkVideoDetailContentScroll(compilationMode: CompilationMode) =
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = compilationMode,
            iterations = FRAME_TIMING_BENCHMARK_ITERATIONS,
            startupMode = WARM,
            setupBlock = {
                pressHome()
                startVideoDetailActivity()
                waitForVideoDetailReady()
            }
        ) {
            repeat(3) {
                swipeDetailContent(up = true)
                swipeDetailContent(up = false)
            }
        }

    private fun benchmarkPlayerSwipeGesture(compilationMode: CompilationMode) =
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = compilationMode,
            iterations = FRAME_TIMING_BENCHMARK_ITERATIONS,
            startupMode = WARM,
            setupBlock = {
                pressHome()
                startVideoDetailActivity()
                waitForVideoDetailReady()
            }
        ) {
            repeat(3) {
                swipePlayerSeek(forward = true)
                swipePlayerSeek(forward = false)
                swipePlayerVertical(up = true)
                swipePlayerVertical(up = false)
            }
        }

    private fun MacrobenchmarkScope.startVideoDetailActivity() {
        val benchmarkBvid = resolveBenchmarkBvid()
        val component = "$TARGET_PACKAGE_NAME/.feature.video.VideoActivity"
        device.executeShellCommand("am start -W -n $component --es bvid $benchmarkBvid")
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.waitForVideoDetailReady() {
        device.wait(Until.findObject(By.textContains("评论")), 8_000)
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.swipeDetailContent(up: Boolean) {
        val x = device.displayWidth / 2
        val fromY = if (up) (device.displayHeight * 88) / 100 else (device.displayHeight * 45) / 100
        val toY = if (up) (device.displayHeight * 45) / 100 else (device.displayHeight * 88) / 100
        device.swipe(x, fromY, x, toY, 24)
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.swipePlayerSeek(forward: Boolean) {
        val y = (device.displayHeight * 22) / 100
        val fromX = if (forward) (device.displayWidth * 22) / 100 else (device.displayWidth * 78) / 100
        val toX = if (forward) (device.displayWidth * 78) / 100 else (device.displayWidth * 22) / 100
        device.swipe(fromX, y, toX, y, 24)
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.swipePlayerVertical(up: Boolean) {
        val x = (device.displayWidth * 80) / 100
        val fromY = if (up) (device.displayHeight * 30) / 100 else (device.displayHeight * 14) / 100
        val toY = if (up) (device.displayHeight * 14) / 100 else (device.displayHeight * 30) / 100
        device.swipe(x, fromY, x, toY, 24)
        device.waitForIdle()
    }

    private fun resolveBenchmarkBvid(): String {
        val args = InstrumentationRegistry.getArguments()
        val configured = args.getString("benchmark.bvid").orEmpty().trim()
        return if (configured.isNotBlank()) configured else DEFAULT_BENCHMARK_BVID
    }
}
