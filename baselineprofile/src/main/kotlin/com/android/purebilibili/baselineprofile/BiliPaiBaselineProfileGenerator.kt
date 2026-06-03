package com.android.purebilibili.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BiliPaiBaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() {
        baselineProfileRule.collect(
            packageName = TARGET_PACKAGE_NAME,
            includeInStartupProfile = true,
            maxIterations = 8
        ) {
            pressHome()
            startActivityAndWait()
            device.waitForIdle()

            // Core startup and navigation hotspots.
            cycleMainTabs()

            // Feed-heavy screens for Compose + Haze + list rendering paths.
            repeat(2) {
                clickBottomTab("动态")
                scrollFeedOnce()
                scrollFeedOnce(reverse = true)

                clickBottomTab("首页")
                scrollFeedOnce()

                clickBottomTab("历史")
                scrollFeedOnce()
            }

            startVideoDetailActivity()
            scrollVideoDetailContent()
            swipeVideoPlayerSeek()
            device.pressBack()
            device.waitForIdle()

            clickBottomTab("首页")
        }
    }

    private fun MacrobenchmarkScope.cycleMainTabs() {
        clickBottomTab("首页")
        clickBottomTab("动态")
        clickBottomTab("历史")
        clickBottomTab("我的")
        clickBottomTab("首页")
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

    private fun MacrobenchmarkScope.scrollFeedOnce(reverse: Boolean = false) {
        val x = device.displayWidth / 2
        val fromY = if (reverse) device.displayHeight / 3 else (device.displayHeight * 3) / 4
        val toY = if (reverse) (device.displayHeight * 3) / 4 else device.displayHeight / 3
        device.swipe(x, fromY, x, toY, 24)
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.startVideoDetailActivity() {
        val component = "$TARGET_PACKAGE_NAME/.feature.video.VideoActivity"
        device.executeShellCommand("am start -W -n $component --es bvid ${resolveBenchmarkBvid()}")
        device.wait(Until.findObject(By.pkg(TARGET_PACKAGE_NAME)), 8_000)
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.scrollVideoDetailContent() {
        val x = device.displayWidth / 2
        val fromY = (device.displayHeight * 88) / 100
        val toY = (device.displayHeight * 45) / 100
        device.swipe(x, fromY, x, toY, 24)
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.swipeVideoPlayerSeek() {
        val y = (device.displayHeight * 22) / 100
        val fromX = (device.displayWidth * 24) / 100
        val toX = (device.displayWidth * 76) / 100
        device.swipe(fromX, y, toX, y, 24)
        device.waitForIdle()
    }

    private fun resolveBenchmarkBvid(): String {
        val configured = InstrumentationRegistry.getArguments()
            .getString("benchmark.bvid")
            .orEmpty()
            .trim()
        return if (configured.isNotBlank()) configured else DEFAULT_BENCHMARK_BVID
    }
}
