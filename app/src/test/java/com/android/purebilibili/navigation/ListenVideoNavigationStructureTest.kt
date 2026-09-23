package com.android.purebilibili.navigation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ListenVideoNavigationStructureTest {

    @Test
    fun `listen video page exposes liquid segmented pager`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/audio/screen/ListenVideoScreen.kt"
        )

        assertTrue(source.contains("BottomBarLiquidSegmentedControl("))
        assertTrue(source.contains("HorizontalPager("))
        assertTrue(source.contains("indicatorPositionProvider"))
        assertTrue(source.contains("listOf(\"播放列表\", \"专辑\", \"歌手\")"))
        assertTrue(source.contains("collectAsStateWithLifecycle()"))
        assertTrue(source.contains("LaunchedEffect(viewModel)"))
    }

    @Test
    fun `main host wires listen tracks to external audio playlist`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt"
        )
        val listenVideoBranch = source
            .substringAfter("BiliPaiNavEntryContentRole.LISTEN_VIDEO")
            .substringBefore("BiliPaiNavEntryContentRole.HISTORY")

        assertTrue(listenVideoBranch.contains("ListenVideoRoute("))
        assertTrue(listenVideoBranch.contains("PlaylistManager.setExternalPlaylist("))
        assertTrue(listenVideoBranch.contains("ExternalPlaylistSource.FAVORITE"))
        assertTrue(listenVideoBranch.contains("BiliPaiNavKey.AudioMode("))
        assertFalse(listenVideoBranch.contains("startAudio = true"))
        assertFalse(listenVideoBranch.contains("BiliPaiNavKey.VideoDetail("))
        assertFalse(listenVideoBranch.contains("Box(modifier = Modifier.fillMaxSize())"))
    }

    @Test
    fun `now playing card reopens current audio item`() {
        val screenSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/audio/screen/ListenVideoScreen.kt"
        )
        val navigationSource = loadSource(
            "app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt"
        )

        assertTrue(screenSource.contains("onNowPlayingClick"))
        assertTrue(screenSource.contains("item.bvid"))
        assertTrue(navigationSource.contains("onNowPlayingClick ="))
        assertTrue(navigationSource.contains("BiliPaiNavKey.AudioMode(sourceBvid = bvid)"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(File(path), File(normalizedPath)).firstOrNull(File::exists)
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
