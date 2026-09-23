package com.android.purebilibili.baselineprofile

import android.os.SystemClock
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

            // Non-live image-heavy and navigation paths omitted by the original eight-line
            // baseline profile. These flows never select a live result or enter a live route.
            exerciseSearchResults()
            exerciseAppearanceSettings()
            exerciseSpaceFeed()
            exerciseWebSession()

            exerciseHomeCardSharedTransition()

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

    private fun MacrobenchmarkScope.exerciseSearchResults() {
        clickBottomTab("首页")
        val search = device.wait(Until.findObject(By.desc("搜索")), 3_000)
            ?: device.wait(Until.findObject(By.textContains("搜索视频")), 3_000)
            ?: return
        search.click()
        device.waitForIdle()
        // SearchScreen requests focus on entry. ASCII keeps shell input deterministic.
        device.executeShellCommand("input text android")
        device.pressEnter()
        SystemClock.sleep(NETWORK_CONTENT_SETTLE_MS)
        repeat(2) { scrollFeedOnce() }
        device.pressBack()
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.exerciseAppearanceSettings() {
        clickBottomTab("我的")
        val settings = device.wait(Until.findObject(By.desc("设置")), 3_000) ?: return
        settings.click()
        val appearance = device.wait(Until.findObject(By.text("外观设置")), 4_000) ?: run {
            device.pressBack()
            return
        }
        appearance.click()
        device.waitForIdle()
        repeat(2) {
            scrollFeedOnce()
            scrollFeedOnce(reverse = true)
        }
        device.pressBack()
        device.pressBack()
        device.waitForIdle()
        clickBottomTab("首页")
    }

    private fun MacrobenchmarkScope.exerciseSpaceFeed() {
        startExplicitDeepLink("https://space.bilibili.com/${resolveBenchmarkMid()}")
        SystemClock.sleep(NETWORK_CONTENT_SETTLE_MS)
        repeat(2) { scrollFeedOnce() }
        device.pressBack()
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.exerciseWebSession() {
        val configuredUrl = InstrumentationRegistry.getArguments()
            .getString("benchmark.webUrl")
            .orEmpty()
            .trim()
            .ifBlank { DEFAULT_BENCHMARK_WEB_URL }
        startExplicitDeepLink(configuredUrl)
        SystemClock.sleep(WEB_CONTENT_SETTLE_MS)
        scrollFeedOnce()
        device.pressBack()
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.startExplicitDeepLink(uri: String) {
        val component = "$TARGET_PACKAGE_NAME/.MainActivity"
        device.executeShellCommand(
            "am start -W -n $component -a android.intent.action.VIEW -d ${shellQuote(uri)}"
        )
        device.wait(Until.findObject(By.pkg(TARGET_PACKAGE_NAME)), 8_000)
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

    private fun MacrobenchmarkScope.exerciseHomeCardSharedTransition() {
        clickBottomTab("首页")
        device.waitForIdle()
        val videoCard = device.wait(Until.findObject(By.descContains("播放")), 3_000)
            ?: device.wait(Until.findObject(By.clazz("android.widget.ImageView")), 3_000)
        if (videoCard != null) {
            videoCard.click()
            device.waitForIdle()
            SystemClock.sleep(NETWORK_CONTENT_SETTLE_MS)
            device.pressBack()
            device.waitForIdle()
        }
    }

    private fun resolveBenchmarkBvid(): String {
        val configured = InstrumentationRegistry.getArguments()
            .getString("benchmark.bvid")
            .orEmpty()
            .trim()
        return if (configured.isNotBlank()) configured else DEFAULT_BENCHMARK_BVID
    }

    private fun resolveBenchmarkMid(): Long = InstrumentationRegistry.getArguments()
        .getString("benchmark.mid")
        ?.toLongOrNull()
        ?.takeIf { it > 0L }
        ?: DEFAULT_BENCHMARK_MID

    private fun shellQuote(value: String): String =
        "'${value.replace("'", "'\"'\"'")}'"

    private companion object {
        const val DEFAULT_BENCHMARK_WEB_URL = "https://example.com"
        const val NETWORK_CONTENT_SETTLE_MS = 1_500L
        const val WEB_CONTENT_SETTLE_MS = 1_000L
    }
}
