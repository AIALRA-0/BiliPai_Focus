package com.android.purebilibili.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode.WARM
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BiliPaiSettingsReturnFrameTimingBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun settingsSubpageReturn_compilationPartial() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = FRAME_TIMING_BENCHMARK_ITERATIONS,
        startupMode = WARM,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            device.waitForIdle()
            clickTextOrDescription("我的")
            clickTextOrDescription("设置", "Settings")
            clickTextOrDescription("外观设置")
        }
    ) {
        repeat(4) {
            device.pressBack()
            device.waitForIdle()
            clickTextOrDescription("外观设置")
        }
    }

    private fun MacrobenchmarkScope.clickTextOrDescription(vararg labels: String) {
        labels.forEach { label ->
            val byDescription = device.wait(Until.findObject(By.desc(label)), 1_000)
            if (byDescription != null) {
                byDescription.click()
                device.waitForIdle()
                return
            }
            val byText = device.wait(Until.findObject(By.text(label)), 1_000)
            if (byText != null) {
                byText.click()
                device.waitForIdle()
                return
            }
        }
        error("Unable to find any of: ${labels.joinToString()}")
    }
}
