package com.android.purebilibili.feature.video.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailScreenMethodSizeStructureTest {

    @Test
    fun videoDetailScreenDelegatesLargeDialogsAndMenusToChildComposables() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt")
        val videoDetailBody = source
            .substringAfter("fun VideoDetailScreen(")
            .substringBefore("@OptIn(ExperimentalHazeMaterialsApi::class)")

        assertTrue(videoDetailBody.contains("VideoDetailFollowGroupDialog(viewModel = viewModel)"))
        assertTrue(videoDetailBody.contains("VideoDetailPlaybackEndedDialog("))
        assertTrue(videoDetailBody.contains("VideoDetailQualitySwitchFailureDialog("))
        assertTrue(videoDetailBody.contains("VideoDetailDanmakuContextMenu("))
        assertTrue(source.contains("private fun VideoDetailFollowGroupDialog("))
        assertTrue(source.contains("private fun VideoDetailPlaybackEndedDialog("))
        assertTrue(source.contains("private fun VideoDetailQualitySwitchFailureDialog("))
        assertTrue(source.contains("private fun VideoDetailDanmakuContextMenu("))
    }

    @Test
    fun videoDetailPlayerContainerUsesHomeSharedTransitionPolicy() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt")
        val playerContainerSource = source
            .substringAfter("val playerContainerModifier = if (")
            .substringBefore("//  播放器容器包含状态栏高度")

        assertTrue(source.contains("resolveVideoCardSharedTransitionMotionSpec("))
        assertTrue(source.contains("resolveVideoSharedTransitionVisualSpec("))
        assertTrue(playerContainerSource.contains("homeSharedTransitionMotionSpec.enabled"))
        assertTrue(playerContainerSource.contains("val duration = if (isFullscreenTarget)"))
        assertTrue(playerContainerSource.contains("homeSharedTransitionMotionSpec.durationMillis"))
        assertTrue(playerContainerSource.contains("durationMillis = duration"))
        assertTrue(playerContainerSource.contains("activeVideoSharedTransitionVisualSpec.targetCornerDp.dp"))
    }

    @Test
    fun videoDetailScreenUsesCentralSharedTransitionEnablePolicy() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt")

        assertTrue(
            source.contains("import com.android.purebilibili.core.ui.transition.shouldEnableVideoCoverSharedTransition")
        )
        assertFalse(
            source.contains("internal fun shouldEnableVideoCoverSharedTransition(")
        )
    }

    @Test
    fun videoDetailShellDoesNotAddEnterSettleRebound() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt")

        assertFalse(source.contains("resolveVideoDetailEnterSettleSpec("))
        assertFalse(source.contains(".videoDetailEnterSettle("))
    }

    @Test
    fun phoneAutoRotateDoesNotUseGlobalCandidateStabilizationDelay() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt")

        assertFalse(source.contains("PHONE_AUTO_ROTATE_STABILIZATION_DELAY_MS"))
        assertFalse(source.contains("PhoneAutoRotatePendingTarget"))
        assertFalse(source.contains("resolveStablePhoneAutoRotateTarget"))
        assertTrue(source.contains("PHONE_AUTO_ROTATE_LANDSCAPE_SETTLE_MS"))
        assertTrue(source.contains("resolvePhoneAutoRotateTargetToApply("))
    }

    private fun loadSource(path: String): String {
        val candidates = listOf(
            File(path),
            File("app", path.removePrefix("app/")),
            File(path.removePrefix("app/")),
            File("..", path)
        )
        return candidates.first { it.exists() }.readText()
    }
}
