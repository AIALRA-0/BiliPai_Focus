package com.android.purebilibili.baselineprofile

import android.os.SystemClock
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device-only danmaku gates. Content ids are supplied by instrumentation arguments so recordings
 * can be pinned to fixtures with at least 6,000 items/segment and 60,000 items/video respectively.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMetricApi::class)
class BiliPaiDanmakuFrameTimingBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun denseSegment6000_frameTiming() = benchmarkVideoFixture("benchmark.danmakuDenseBvid") {
        SystemClock.sleep(DANMAKU_SAMPLE_DURATION_MS)
    }

    @Test
    fun longVideo60000_windowedPlayback() = benchmarkVideoFixture("benchmark.danmakuLongBvid") {
        repeat(3) {
            swipePlayerSeek(forward = true)
            SystemClock.sleep(750L)
        }
    }

    @Test
    fun continuousSeek_discardsLateSegmentWindows() = benchmarkVideoFixture("benchmark.danmakuLongBvid") {
        repeat(6) { index ->
            swipePlayerSeek(forward = index % 2 == 0)
        }
    }

    @Test
    fun landscapeDenseSegment_frameTiming() = benchmarkVideoFixture("benchmark.danmakuDenseBvid") {
        enterLandscapePlayer()
        SystemClock.sleep(DANMAKU_SAMPLE_DURATION_MS)
    }

    @Test
    fun landscapeProgressScrub_frameTiming() = benchmarkVideoFixture("benchmark.danmakuLongBvid") {
        enterLandscapePlayer()
        repeat(6) { index ->
            scrubLandscapeProgress(forward = index % 2 == 0)
            SystemClock.sleep(750L)
        }
    }

    @Test
    fun liveBurst_appendOnlyFrameTiming() {
        val roomId = argument("benchmark.danmakuLiveRoomId")
        assumeTrue("Set benchmark.danmakuLiveRoomId to a burst fixture room", roomId.isNotBlank())
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE_NAME,
            metrics = danmakuMetrics(),
            compilationMode = CompilationMode.Partial(),
            iterations = DANMAKU_BENCHMARK_ITERATIONS,
            setupBlock = { killProcess() }
        ) {
            device.executeShellCommand(
                "am start -W -n $TARGET_PACKAGE_NAME/.MainActivity " +
                    "-a android.intent.action.VIEW -d https://live.bilibili.com/$roomId"
            )
            device.wait(Until.findObject(By.pkg(TARGET_PACKAGE_NAME)), DANMAKU_UI_WAIT_TIMEOUT_MS)
            SystemClock.sleep(DANMAKU_SAMPLE_DURATION_MS)
        }
    }

    private fun benchmarkVideoFixture(
        argumentName: String,
        measure: MacrobenchmarkScope.() -> Unit
    ) {
        val bvid = argument(argumentName)
        assumeTrue("Set $argumentName to a pinned danmaku fixture", bvid.isNotBlank())
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE_NAME,
            metrics = danmakuMetrics(),
            compilationMode = CompilationMode.Partial(),
            iterations = DANMAKU_BENCHMARK_ITERATIONS,
            setupBlock = { killProcess() }
        ) {
            device.executeShellCommand(
                "am start -W -n $TARGET_PACKAGE_NAME/.feature.video.VideoActivity --es bvid $bvid"
            )
            device.wait(Until.findObject(By.textContains("评论")), DANMAKU_UI_WAIT_TIMEOUT_MS)
            SystemClock.sleep(1_000L)
            measure()
        }
    }

    private fun MacrobenchmarkScope.swipePlayerSeek(forward: Boolean) {
        val y = (device.displayHeight * 22) / 100
        val fromX = if (forward) (device.displayWidth * 25) / 100 else (device.displayWidth * 75) / 100
        val toX = if (forward) (device.displayWidth * 75) / 100 else (device.displayWidth * 25) / 100
        device.swipe(fromX, y, toX, y, 18)
    }

    private fun MacrobenchmarkScope.enterLandscapePlayer() {
        var fullscreen = device.findObject(By.desc("横屏"))
        if (fullscreen == null) {
            device.click(device.displayWidth / 2, device.displayHeight * 22 / 100)
            fullscreen = device.wait(Until.findObject(By.desc("横屏")), DANMAKU_UI_WAIT_TIMEOUT_MS)
        }
        checkNotNull(fullscreen) { "Landscape control was not found" }.click()
        check(device.wait(Until.hasObject(By.desc("退出横屏")), DANMAKU_UI_WAIT_TIMEOUT_MS)) {
            "Player did not enter landscape fullscreen"
        }
        check(device.displayWidth > device.displayHeight) { "Benchmark requires landscape display bounds" }
    }

    private fun MacrobenchmarkScope.scrubLandscapeProgress(forward: Boolean) {
        var track = device.findObject(By.res(TARGET_PACKAGE_NAME, "player_progress"))
        if (track == null) {
            device.click(device.displayWidth / 2, device.displayHeight / 2)
            track = device.wait(Until.findObject(By.res(TARGET_PACKAGE_NAME, "player_progress")), DANMAKU_UI_WAIT_TIMEOUT_MS)
        }
        val bounds = checkNotNull(track) { "Landscape progress track was not found" }.visibleBounds
        // The chapter is above the track; scrub the lower touch area, not the player surface.
        val y = bounds.bottom - (bounds.height() / 5).coerceAtLeast(1)
        val left = bounds.left + bounds.width() / 4
        val right = bounds.right - bounds.width() / 4
        check(device.swipe(if (forward) left else right, y, if (forward) right else left, y, 40)) {
            "Progress scrub injection failed"
        }
    }

    private fun argument(name: String): String =
        InstrumentationRegistry.getArguments().getString(name).orEmpty().trim()

    private fun danmakuMetrics() = listOf(
        FrameTimingMetric(),
        MemoryUsageMetric(MemoryUsageMetric.Mode.Last),
        TraceSectionMetric(
            sectionName = "GC%",
            mode = TraceSectionMetric.Mode.Count,
            label = "danmakuGc",
            targetPackageOnly = true
        ),
        TraceSectionMetric(
            sectionName = "BiliPaiDanmakuSetData",
            mode = TraceSectionMetric.Mode.Count,
            label = "danmakuFullReplace",
            targetPackageOnly = true
        ),
        TraceSectionMetric(
            sectionName = "BiliPaiDanmakuAppend",
            mode = TraceSectionMetric.Mode.Count,
            label = "danmakuAppendBatch",
            targetPackageOnly = true
        )
    )

    private companion object {
        const val DANMAKU_BENCHMARK_ITERATIONS = 5
        const val DANMAKU_SAMPLE_DURATION_MS = 8_000L
        const val DANMAKU_UI_WAIT_TIMEOUT_MS = 12_000L
    }
}
