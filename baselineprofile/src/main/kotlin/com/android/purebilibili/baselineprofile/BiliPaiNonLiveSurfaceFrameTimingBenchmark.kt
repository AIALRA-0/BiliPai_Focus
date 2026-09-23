package com.android.purebilibili.baselineprofile

import android.os.SystemClock
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

/** Frame-timing CUJs for non-live image-heavy surfaces missing from the original profile. */
@RunWith(AndroidJUnit4::class)
class BiliPaiNonLiveSurfaceFrameTimingBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun searchVideoResultsScroll_compilationPartial() = measureNonLiveSurface(
        setup = {
            startExplicitDeepLink("bilipai://search")
            device.executeShellCommand("input text android")
            device.pressEnter()
            SystemClock.sleep(NETWORK_CONTENT_SETTLE_MS)
            // Pin the CUJ to video results; never enter live/live-user cards.
            device.wait(Until.findObject(By.text("视频")), 3_000)?.click()
            device.waitForIdle()
        },
    ) {
        repeat(3) {
            swipeFeed(down = true)
            swipeFeed(down = false)
        }
    }

    @Test
    fun spaceFeedScroll_compilationPartial() = measureNonLiveSurface(
        setup = {
            startExplicitDeepLink("https://space.bilibili.com/${resolveBenchmarkMid()}")
            SystemClock.sleep(NETWORK_CONTENT_SETTLE_MS)
        },
    ) {
        repeat(3) {
            swipeFeed(down = true)
            swipeFeed(down = false)
        }
    }

    @Test
    fun webContentScroll_compilationPartial() = measureNonLiveSurface(
        setup = {
            startExplicitDeepLink(resolveBenchmarkWebUrl())
            SystemClock.sleep(WEB_CONTENT_SETTLE_MS)
        },
    ) {
        repeat(3) {
            swipeFeed(down = true, steps = 90)
            swipeFeed(down = false, steps = 90)
        }
    }

    private fun measureNonLiveSurface(
        setup: MacrobenchmarkScope.() -> Unit,
        measure: MacrobenchmarkScope.() -> Unit,
    ) = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = FRAME_TIMING_BENCHMARK_ITERATIONS,
        startupMode = WARM,
        setupBlock = {
            pressHome()
            setup()
        },
        measureBlock = measure,
    )

    private fun MacrobenchmarkScope.startExplicitDeepLink(uri: String) {
        val component = "$TARGET_PACKAGE_NAME/.MainActivity"
        device.executeShellCommand(
            "am start -W -n $component -a android.intent.action.VIEW -d ${shellQuote(uri)}"
        )
        requireNotNull(
            device.wait(Until.findObject(By.pkg(TARGET_PACKAGE_NAME)), UI_WAIT_TIMEOUT_MS)
        )
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.swipeFeed(down: Boolean, steps: Int = 36) {
        val x = device.displayWidth / 2
        val fromY = if (down) (device.displayHeight * 78) / 100 else (device.displayHeight * 32) / 100
        val toY = if (down) (device.displayHeight * 32) / 100 else (device.displayHeight * 78) / 100
        device.swipe(x, fromY, x, toY, steps)
        device.waitForIdle()
    }

    private fun resolveBenchmarkMid(): Long = InstrumentationRegistry.getArguments()
        .getString("benchmark.mid")
        ?.toLongOrNull()
        ?.takeIf { it > 0L }
        ?: DEFAULT_BENCHMARK_MID

    private fun resolveBenchmarkWebUrl(): String = InstrumentationRegistry.getArguments()
        .getString("benchmark.webUrl")
        .orEmpty()
        .trim()
        .ifBlank { DEFAULT_BENCHMARK_WEB_URL }

    private fun shellQuote(value: String): String =
        "'${value.replace("'", "'\"'\"'")}'"

    private companion object {
        const val DEFAULT_BENCHMARK_WEB_URL = "https://example.com"
        const val NETWORK_CONTENT_SETTLE_MS = 1_500L
        const val WEB_CONTENT_SETTLE_MS = 1_000L
        const val UI_WAIT_TIMEOUT_MS = 8_000L
    }
}
