package com.android.purebilibili.feature.audio.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MusicPlayerContentStructureTest {

    @Test
    fun `compact player exposes liquid play lyrics segmented control`() {
        val source = loadSource()
        val compactBranch = source
            .substringAfter("MusicPlayerLayout.COMPACT_PAGER ->")
            .substringBefore("MusicPlayerLayout.EXPANDED_SPLIT ->")

        assertTrue(compactBranch.contains("BottomBarLiquidSegmentedControl("))
        assertTrue(compactBranch.contains("resolveMusicPlayerPageTabs()"))
        assertTrue(compactBranch.contains("height = 48.dp"))
        assertTrue(compactBranch.contains("indicatorHeight = 36.dp"))
        assertTrue(compactBranch.contains("containerVerticalPadding = 6.dp"))
        assertTrue(compactBranch.contains("indicatorPositionProvider"))
        assertTrue(compactBranch.contains("animateScrollToPage"))
        assertTrue(compactBranch.contains("navigationBarsPadding()"))
        assertTrue(compactBranch.contains("miuixBackdrop = musicBackdrop"))
        assertTrue(compactBranch.contains("MUSIC_PLAYER_COMPACT_DOCK_BOTTOM_PADDING_DP"))
        assertTrue(!compactBranch.contains("visible = pagerState.currentPage != 1"))
        assertTrue(!compactBranch.contains("forceLiquidChrome = true"))
        assertTrue(!compactBranch.contains("containerColorOverride"))
        assertTrue(!compactBranch.contains("indicatorIdleSurfaceColorOverride"))
        assertTrue(compactBranch.contains("selectedTextColorOverride = MusicContentColor"))
        assertTrue(compactBranch.contains("unselectedTextColorOverride = MusicContentColor"))
    }

    @Test
    fun `compact landscape exposes transport below artwork`() {
        val source = loadSource()
        val compactLandscape = source
            .substringAfter("MusicPlayerLayout.COMPACT_LANDSCAPE ->")
            .substringBefore("MusicPlayerLayout.COMPACT_PAGER ->")

        assertTrue(compactLandscape.contains("MusicArtwork("))
        assertTrue(compactLandscape.contains("MusicProgress("))
        assertTrue(compactLandscape.contains("MusicPlayPauseButton("))
        assertTrue(compactLandscape.contains("onPlayPause = onPlayPause"))
        assertTrue(compactLandscape.contains("progressSeekRevision += 1"))
        assertTrue(compactLandscape.contains("if (landscapeLyrics)"))
        assertTrue(compactLandscape.contains("val landscapeHeaderHeight = 48.dp"))
        assertTrue(compactLandscape.contains("contentAlignment = if (landscapeLyrics)"))
        assertTrue(compactLandscape.contains("Alignment.TopCenter"))
        assertTrue(compactLandscape.contains("Alignment.Center"))
    }

    @Test
    fun `music backdrop records opaque page background before glass samples it`() {
        val source = loadSource()
        assertTrue(source.contains("val musicBackdrop = musicBackdropSource.backdrop"))
        assertTrue(source.contains(".then(musicBackdropSource.modifier)\n                    .background(pageBackground)"))
    }

    @Test
    fun `lyrics browsing pauses auto follow without seeking playback`() {
        val source = loadSource()
        val lyricsPage = source.substringAfter("private fun LyricsPage(")

        assertTrue(lyricsPage.contains("collectIsDraggedAsState()"))
        assertTrue(lyricsPage.contains("resolveLyricFocusScrollOffsetPx("))
        assertTrue(lyricsPage.contains("回到当前歌词"))
        assertTrue(!lyricsPage.contains("resolveDraggedLyricIndex("))
        assertTrue(!lyricsPage.contains("snapshotFlow"))
        assertTrue(!lyricsPage.contains("LYRIC_AUTO_FOLLOW_RESUME_DELAY_MS"))
        assertTrue(!lyricsPage.contains("scrollToItem(currentIndex, -160)"))
        assertTrue(!lyricsPage.contains("animateScrollToItem(currentIndex, -160)"))
        assertTrue(source.contains("progressSeekRevision"))
        assertTrue(lyricsPage.contains("LaunchedEffect(progressSeekRevision)"))
    }

    @Test
    fun `player uses bbplayer secondary transport and keeps actions in sheet`() {
        val source = loadSource()
        val playerPage = source
            .substringAfter("private fun PlayerPage(")
            .substringBefore("private fun MusicArtwork(")

        assertTrue(playerPage.contains("MusicSecondaryControls("))
        val secondaryControls = source
            .substringAfter("private fun MusicSecondaryControls(")
            .substringBefore("@Composable\nprivate fun MusicArtwork(")
        assertTrue(secondaryControls.contains("modifier = Modifier.size(48.dp)"))
        assertTrue(secondaryControls.windowed("modifier = Modifier.size(48.dp)".length).count {
            it == "modifier = Modifier.size(48.dp)"
        } >= 4)
        assertTrue(playerPage.contains("shouldRotateMusicArtwork("))
        assertTrue(source.contains("rememberMusicArtworkRotationDegrees("))
        assertTrue(source.contains("playbackSpeed = state.playbackSpeed"))
        assertTrue(playerPage.contains("onLikeClick"))
        assertTrue(source.contains("Icons.Outlined.Shuffle"))
        assertTrue(source.contains("Icons.Outlined.Repeat"))
        assertTrue(source.contains("Icons.AutoMirrored.Outlined.Comment"))
        assertTrue(source.contains("Icons.Outlined.QueueMusic"))
        assertTrue(source.contains("AppIconButton("))
        assertTrue(source.contains("MusicWavySlider("))
        assertFalse(source.contains("AppSlider("))
        assertTrue(source.contains("wavy = false"))
        assertTrue(source.contains("resolveMusicPlayerChromeSpec("))
        assertTrue(source.contains("onShuffleEnabledChange"))
        assertTrue(source.contains("usePaletteImmersiveBackdrop"))
        assertTrue(source.contains("showActions"))
        assertTrue(source.contains("播放器操作"))
        assertTrue(source.contains("缓存音频"))
        assertTrue(source.contains("onCollectionClick"))
        assertTrue(!source.contains("MusicPlayModeDock("))
        assertTrue(!source.contains("listOf(\"顺序播放\", \"随机播放\", \"单曲循环\", \"列表循环\")"))
    }

    @Test
    fun `artwork uses one reversible playback scale timeline`() {
        val source = loadSource()
        val artwork = source
            .substringAfter("private fun MusicArtwork(")
            .substringBefore("private fun MusicProgress(")

        assertTrue(artwork.contains("animateFloatAsState("))
        assertTrue(artwork.contains("APPLE_MUSIC_COVER_MOTION_STIFFNESS"))
        assertTrue(artwork.contains("resolveAppleMusicCoverShadowElevation(playbackProgress)"))
        assertTrue(artwork.contains("scaleX = artworkScale"))
        assertTrue(artwork.contains("scaleY = artworkScale"))
        assertTrue(artwork.contains("if (reduceMotion)"))
    }

    @Test
    fun `lyrics expose progress playback controls and immersive chrome`() {
        val source = loadSource()
        val lyricsPage = source.substringAfter("private fun LyricsPage(")
        val lyricsControls = source
            .substringAfter("private fun LyricsPrimaryControls(")
            .substringBefore("private fun formatLyricsOffset(")
        val topBar = source
            .substringAfter("private fun MusicTopBar(")
            .substringBefore("private fun GlassIconButton(")

        assertTrue(lyricsPage.contains("MusicProgress("))
        assertTrue(lyricsPage.contains("PlaybackControls("))
        assertTrue(lyricsPage.contains("AnimatedVisibility("))
        assertTrue(lyricsPage.contains("LyricsImmersiveProgress("))
        assertTrue(lyricsPage.contains("歌词设置"))
        assertTrue(lyricsPage.contains("收起"))
        assertTrue(lyricsPage.contains("onControlsVisibleChange(false)"))
        assertTrue(lyricsPage.contains("bottom = 260.dp"))
        assertTrue(lyricsPage.contains("歌词加载失败"))
        assertTrue(lyricsPage.contains("未找到匹配歌词"))
        assertTrue(!lyricsControls.contains("AppSurfaceTokens.surfaceContainer()"))
        assertTrue(lyricsControls.contains("val panelColor = resolveMusicImmersivePanelColor("))
        assertTrue(lyricsControls.contains("glassTintColor,"))
        assertTrue(lyricsControls.contains("MaterialTheme.colorScheme.surface"))
        assertTrue(lyricsControls.contains("if (miuixBackdrop != null) Color.Transparent else panelColor"))
        assertTrue(lyricsControls.contains("val panelShape = AppShapes.borderedContainer(ContainerLevel.Card)"))
        assertTrue(lyricsControls.contains(".biliPaiFloatingDockShell("))
        assertTrue(lyricsControls.contains("color = Color.Transparent"))
        assertTrue(lyricsControls.contains("LocalMusicPlayerMaterial provides panelMaterial"))
        assertTrue(topBar.contains("Icons.Outlined.KeyboardArrowDown"))
        assertTrue(topBar.contains("Icons.Outlined.MoreHoriz"))
        assertTrue(!source.contains("BottomBarMatchedReusableLiquidDock("))
        assertTrue(!topBar.contains("?: Spacer"))
        assertTrue(!source.contains("private fun Modifier.musicGlassSurface("))
        assertTrue(!source.contains("bottomBarMatchedLiquidDockSurface("))
        assertTrue(!source.contains("blurRadius = 20.dp"))
    }

    @Test
    fun `lyrics settings expose quarter second offset correction and reset`() {
        val source = loadSource()
        val settings = source
            .substringAfter("private fun LyricsSettingsContent(")
            .substringBefore("private fun LyricLineContent(")

        assertTrue(settings.contains("onLyricsOffsetChange(-250L)"))
        assertTrue(settings.contains("onLyricsOffsetChange(250L)"))
        assertTrue(settings.contains("onLyricsOffsetChange(-lyricsOffsetMs)"))
        assertTrue(settings.contains("formatLyricsOffset(lyricsOffsetMs)"))
    }

    @Test
    fun `glass controls reuse home search backdrop instead of shader background`() {
        val source = loadSource()
        val topButtons = source.substringAfter("private fun GlassIconButton(")

        assertTrue(source.contains("MiuixBackdrop?"))
        assertTrue(source.contains("rememberChromeBackdropSource()"))
        assertTrue(source.contains(".then(musicBackdropSource.modifier)"))
        assertTrue(topButtons.contains(".biliPaiFloatingDockShell("))
        assertTrue(topButtons.contains("backdrop = miuixBackdrop"))
        assertTrue(topButtons.contains("enabled = glassEnabled"))
        assertTrue(topButtons.contains("blurEnabled = !glassEnabled"))
        assertTrue(topButtons.contains("liquidGlassTuning = liquidGlassTuning"))
        assertTrue(source.contains("homeSettings.liquidGlassProgress"))
        assertTrue(source.contains("homeSettings.liquidGlassAdvancedSettings"))
        assertTrue(source.contains("homeSettings.liquidGlassReadabilityMode"))
        assertTrue(!source.contains("BottomBarMatchedReusableLiquidDock("))
        assertTrue(!source.contains("biliPaiMiuixFloatingDockSurface("))
        assertTrue(!source.contains("liquidGlassBackground("))
        assertTrue(!source.contains("forceLiquidChrome = true"))
        assertTrue(!source.contains("containerColorOverride"))
        assertTrue(!source.contains("indicatorIdleSurfaceColorOverride"))
        assertTrue(source.contains("MusicGlassMaterialMode.FROSTED"))
        assertTrue(source.contains("LocalMusicPlayerMaterial provides musicMaterial"))
        assertTrue(source.contains("blurEnabled = !glassEnabled"))
    }

    @Test
    fun `queue exposes 3D cover flow view and switch`() {
        val source = loadSource()
        val coverFlowSource = loadSource("app/src/main/java/com/android/purebilibili/feature/audio/screen/Music3DCoverFlow.kt")

        assertTrue(source.contains("Music3DCoverFlow("))
        assertTrue(source.contains("3D 唱片架"))
        assertTrue(coverFlowSource.contains("cameraDistance = (fittedCardSizeDp * 0.075f).coerceIn(8f, 14f) * density"))
        assertTrue(coverFlowSource.contains("rotationY = (pageOffset * -32f).coerceIn(-52f, 52f) *"))
        assertTrue(coverFlowSource.contains("(if (reduceMotion) 1f else itemEntrance)"))
        assertTrue(coverFlowSource.contains("translationY = if (reduceMotion) 0f else"))
        assertTrue(coverFlowSource.contains("scaleY = -1f"))
        assertTrue(coverFlowSource.contains("BlendMode.DstIn"))
        assertTrue(coverFlowSource.contains("rememberSaveable { mutableStateOf(false) }"))
        assertTrue(coverFlowSource.contains("AppMotionTokens.emphasizedSpec<Float>()"))
        assertTrue(coverFlowSource.contains("if (reduceMotion) 0f else"))
        assertTrue(!source.contains("preview_p2"))
        assertTrue(!source.contains("(Remix)"))
    }

    private fun loadSource(
        path: String = "app/src/main/java/com/android/purebilibili/feature/audio/screen/MusicPlayerContent.kt"
    ): String {
        val normalizedPath = path.removePrefix("app/")
        return listOf(File(path), File(normalizedPath)).firstOrNull(File::exists)?.readText()
            ?: error("Cannot locate $path from ${File(".").absolutePath}")
    }
}
