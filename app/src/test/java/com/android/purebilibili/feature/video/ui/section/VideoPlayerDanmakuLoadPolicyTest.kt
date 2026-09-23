package com.android.purebilibili.feature.video.ui.section

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoPlayerDanmakuLoadPolicyTest {

    @Test
    fun immediateLoad_startsAsSoonAsCidIsAvailable_evenWithoutDurationHint() {
        val policy = resolveVideoPlayerDanmakuLoadPolicy(
            cid = 10086L,
            danmakuEnabled = true,
            durationHintMs = 0L
        )

        assertTrue(policy.shouldEnable)
        assertTrue(policy.shouldLoadImmediately)
        assertEquals(0L, policy.durationHintMs)
    }

    @Test
    fun immediateLoad_keepsDurationHintWhenPlayerAlreadyKnowsIt() {
        val policy = resolveVideoPlayerDanmakuLoadPolicy(
            cid = 10010L,
            danmakuEnabled = true,
            durationHintMs = 360_000L
        )

        assertTrue(policy.shouldLoadImmediately)
        assertEquals(360_000L, policy.durationHintMs)
    }

    @Test
    fun immediateLoad_staysDisabledWhenCidInvalidOrDanmakuOff() {
        assertFalse(
            resolveVideoPlayerDanmakuLoadPolicy(
                cid = 0L,
                danmakuEnabled = true,
                durationHintMs = 0L
            ).shouldLoadImmediately
        )

        assertFalse(
            resolveVideoPlayerDanmakuLoadPolicy(
                cid = 10010L,
                danmakuEnabled = false,
                durationHintMs = 0L
            ).shouldLoadImmediately
        )
    }

    @Test
    fun engineSync_disablesAndClearsWhenSettingTurnsOff() {
        assertEquals(
            VideoPlayerDanmakuEngineSyncAction.DisableAndClear,
            resolveVideoPlayerDanmakuEngineSyncAction(
                danmakuEnabled = false,
                cid = 10010L
            )
        )
    }

    @Test
    fun engineSync_enablesOnlyWhenCidIsValidAndSettingIsOn() {
        assertEquals(
            VideoPlayerDanmakuEngineSyncAction.Enable,
            resolveVideoPlayerDanmakuEngineSyncAction(
                danmakuEnabled = true,
                cid = 10010L
            )
        )
        assertEquals(
            VideoPlayerDanmakuEngineSyncAction.DisableAndClear,
            resolveVideoPlayerDanmakuEngineSyncAction(
                danmakuEnabled = true,
                cid = 0L
            )
        )
    }

    @Test
    fun relatedNavigation_outgoingHostCannotBindOrLoadSharedDanmakuEngine() {
        assertFalse(
            shouldRunVideoPlayerDanmakuHostEffects(
                danmakuHostActive = false,
                hostLifecycleStarted = true,
            )
        )
        assertFalse(
            shouldRunVideoPlayerDanmakuHostEffects(
                danmakuHostActive = true,
                hostLifecycleStarted = false,
            )
        )
        assertTrue(
            shouldRunVideoPlayerDanmakuHostEffects(
                danmakuHostActive = true,
                hostLifecycleStarted = true,
            )
        )
    }

    @Test
    fun detailAndPlayerToggle_syncEngineImmediatelyAndReleaseDanmakuView() {
        val playerSource = java.io.File(
            "src/main/java/com/android/purebilibili/feature/video/ui/section/VideoPlayerSection.kt"
        ).readText()
        val detailSource = java.io.File(
            "src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailPhoneContent.kt"
        ).readText()
        val detailStateHolderSource = java.io.File(
            "src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt"
        ).readText()

        assertTrue(
            playerSource.contains("resolveVideoPlayerDanmakuEngineSyncAction("),
            "VideoPlayerSection must sync engine enable/clear via policy."
        )
        assertTrue(
            playerSource.contains("onRelease") &&
                playerSource.contains("danmakuManager.clear()") &&
                // 相关推荐 push 后旧页 dispose 不得清掉新页已接管的 view；按目标 detach。
                // hide() 经 isEnabled=false 间接触发，不要求源码直写 danmakuManager.hide()。
                playerSource.contains("danmakuManager.detachView(view)"),
            "VideoPlayerSection must safely release DanmakuView on dispose."
        )
        assertTrue(
            detailSource.contains("danmakuManager.isEnabled = newValue") ||
                detailSource.contains("rememberDanmakuManager(success.info.bvid)"),
            "Detail tab danmaku toggle must update the shared DanmakuManager immediately, not only DataStore."
        )
        assertTrue(
            playerSource.contains("danmakuManager.isEnabled = newState"),
            "Fullscreen bottom-bar danmaku toggle must update DanmakuManager immediately."
        )
        assertTrue(
            playerSource.contains("if (!runDanmakuHostEffects) return@LaunchedEffect") &&
                detailStateHolderSource.contains(
                    "danmakuHostActive = !hasCommittedRelatedVideoNavigation"
                ),
            "Outgoing related-video detail hosts must not bind or load the shared danmaku engine."
        )
        assertTrue(
            playerSource.contains("attachedPlayer?.let { danmakuManager.detachPlayer(it) }") &&
                !playerSource.contains("ON_RESUME: Re-attaching danmaku player") &&
                !playerSource.contains("danmakuManager.clearViewReference()"),
            "Player ownership must be paired by its Compose effect without lifecycle re-registration."
        )
    }
}
