package com.android.purebilibili.navigation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class AudioModeNavigationStructureTest {

    @Test
    fun `navigation3 audio mode reuses the previous video detail player owner`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt"
        )
        val videoDetailBranch = source
            .substringAfter("BiliPaiNavEntryContentRole.VIDEO_DETAIL")
            .substringBefore("BiliPaiNavEntryContentRole.ONBOARDING")
        val audioModeBranch = source
            .substringAfter("BiliPaiNavEntryContentRole.AUDIO_MODE")
            .substringBefore("BiliPaiNavEntryContentRole.PARTITION")

        assertTrue(videoDetailBranch.contains("videoDetailViewModelOwners[videoKey] = videoDetailOwner"))
        assertTrue(videoDetailBranch.contains("viewModel = videoPlaybackViewModel"))
        assertTrue(audioModeBranch.contains("previousVideoKey?.let(videoDetailViewModelOwners::get)"))
        assertTrue(audioModeBranch.contains("viewModel(viewModelStoreOwner = sharedVideoOwner)"))
        assertTrue(audioModeBranch.contains("viewModel.uiState.value is"))
        assertTrue(videoDetailBranch.contains("isPlaybackSessionActive = videoDetailPlaybackSessionActive"))
        assertTrue(
            videoDetailBranch.contains(
                "navigation3BackStack.lastOrNull() !is BiliPaiNavKey.AudioMode"
            )
        )
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(File(path), File(normalizedPath)).firstOrNull(File::exists)
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
