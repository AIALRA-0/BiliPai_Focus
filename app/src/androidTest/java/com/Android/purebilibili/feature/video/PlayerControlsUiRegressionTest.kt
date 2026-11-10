package com.android.purebilibili.feature.video

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.store.PlayerProgressPlacement
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.AppThemeConfig
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.feature.video.ui.overlay.BottomControlBar
import com.android.purebilibili.feature.video.ui.overlay.CenterPlaybackControl
import com.android.purebilibili.feature.video.ui.overlay.PlayerProgress
import com.android.purebilibili.feature.video.ui.overlay.TopControlBar
import com.android.purebilibili.feature.video.ui.overlay.glass.BottomControlBar as GlassBottomControlBar
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import top.yukonga.miuix.kmp.theme.MiuixTheme

@RunWith(AndroidJUnit4::class)
class PlayerControlsUiRegressionTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun castButton_respectsVisibilitySetting() {
        val showCastButton = mutableStateOf(false)
        rule.setContent {
            MaterialTheme {
                TopControlBar(
                    title = "测试视频",
                    isFullscreen = false,
                    showCurrentTime = false,
                    showInteractiveActions = true,
                    showCastButton = showCastButton.value,
                    likeStatusKnown = false,
                    onCastClick = {},
                    onBack = {}
                )
            }
        }
        rule.onNodeWithContentDescription("投屏").assertDoesNotExist()

        rule.runOnIdle { showCastButton.value = true }
        rule.onNodeWithContentDescription("投屏").assertExists()
    }

    @Test
    fun progressPlacement_movesProgressAcrossControlRow() {
        val progressPlacement = mutableStateOf(PlayerProgressPlacement.ABOVE_CONTROLS)
        rule.setContent {
            MaterialTheme {
                MiuixTheme {
                    Box(Modifier.size(width = 800.dp, height = 180.dp).background(Color.Black)) {
                        GlassBottomControlBar(
                            isPlaying = true,
                            progress = PlayerProgress(current = 30_000, duration = 120_000),
                            isFullscreen = false,
                            progressPlacement = progressPlacement.value,
                            onPlayPauseClick = {},
                            onSeek = {},
                            onToggleFullscreen = {},
                        )
                    }
                }
            }
        }
        val progressAbove = rule.onNodeWithTag("player_progress").fetchSemanticsNode().boundsInRoot
        val controlsAbove = rule.onNodeWithTag("player_control_row").fetchSemanticsNode().boundsInRoot
        assertTrue(progressAbove.bottom <= controlsAbove.top)

        rule.runOnIdle { progressPlacement.value = PlayerProgressPlacement.BOTTOM_EDGE }
        val progressBelow = rule.onNodeWithTag("player_progress").fetchSemanticsNode().boundsInRoot
        val controlsBelow = rule.onNodeWithTag("player_control_row").fetchSemanticsNode().boundsInRoot
        assertTrue(progressBelow.top >= controlsBelow.bottom)
    }

    @Test
    fun seekRegionRemainsGestureSafeWithSinglePlaybackIdentityAcrossMaterialsAndWidths() {
        val width = mutableStateOf(960)
        val fullscreen = mutableStateOf(false)
        val glass = mutableStateOf(false)
        rule.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = LocalDensity.current.density * maxWidth.value / 960f
                CompositionLocalProvider(
                    LocalDensity provides Density(density),
                    LocalAppThemeConfig provides AppThemeConfig(liquidGlassEnabled = glass.value),
                ) {
                    MaterialTheme {
                        Box(Modifier.width(width.value.dp)) {
                            BottomControlBar(
                                progress = PlayerProgress(30_000, 120_000),
                                isFullscreen = fullscreen.value,
                                viewportWidthDpOverride = width.value,
                                currentQualityLabel = "1080P",
                                onSeek = {}, onToggleFullscreen = {},
                            )
                        }
                    }
                }
            }
        }
        for (widthDp in listOf(320, 500, 720, 960)) {
            for (isFullscreen in listOf(false, true)) {
                rule.runOnIdle { width.value = widthDp; fullscreen.value = isFullscreen; glass.value = false }
                for (useGlass in listOf(false, true)) {
                    rule.runOnIdle { glass.value = useGlass }
                    val tags = listOf("player_progress", "player_progress_seek_area", "player_control_row", "player_fullscreen_toggle")
                    val bounds = tags.associateWith { rule.onNodeWithTag(it).assertIsDisplayed().fetchSemanticsNode().boundsInRoot }
                    val controls = bounds.getValue("player_control_row")
                    val seek = bounds.getValue("player_progress_seek_area")
                    assertTrue(bounds.getValue("player_progress").bottom <= controls.top)
                    val pxPerDp = controls.width / (widthDp - 20f)
                    // The control row has 10dp outer padding; seek keeps at least 20dp window-edge exclusion.
                    assertTrue(seek.left >= controls.left + 10f * pxPerDp - 1f)
                    assertTrue(seek.right <= controls.right - 10f * pxPerDp + 1f)
                }
            }
        }
    }

    @Test
    fun playbackTargetAndGlyphStayAtViewportCenterAcrossAspectRatiosAndMaterials() {
        val viewport = mutableStateOf(320 to 180)
        val fullscreen = mutableStateOf(false)
        val glass = mutableStateOf(false)
        val style = mutableStateOf(AppUiStyle.MATERIAL3)
        rule.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = LocalDensity.current.density * maxWidth.value / 960f
                CompositionLocalProvider(
                    LocalDensity provides Density(density),
                    LocalAppUiStyle provides style.value,
                    LocalAppThemeConfig provides AppThemeConfig(liquidGlassEnabled = glass.value),
                ) {
                    MaterialTheme { MiuixTheme {
                        Box(Modifier.size(viewport.value.first.dp, viewport.value.second.dp).testTag("player_viewport")) {
                            BottomControlBar(
                                isFullscreen = fullscreen.value,
                                viewportIsLandscape = viewport.value.first > viewport.value.second,
                                viewportWidthDpOverride = viewport.value.first,
                                progress = PlayerProgress(30_000, 120_000),
                                onSeek = {}, onToggleFullscreen = {},
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                            CenterPlaybackControl(
                                visible = true,
                                isPlaying = true,
                                onPlayPause = {},
                                diameter = if (fullscreen.value) 72.dp else 56.dp,
                                viewportTopInset = if (fullscreen.value) 0.dp else 24.dp,
                            )
                        }
                    } }
                }
            }
        }
        for (uiStyle in AppUiStyle.entries) {
            for (useGlass in listOf(false, true)) {
                for (isFullscreen in listOf(false, true)) {
                    for (dimensions in listOf(320 to 180, 320 to 640, 900 to 400)) {
                        rule.runOnIdle {
                            style.value = uiStyle
                            glass.value = useGlass
                            fullscreen.value = isFullscreen
                            viewport.value = dimensions
                        }
                        rule.onAllNodesWithTag("player_play_pause").assertCountEquals(1)
                        val host = rule.onNodeWithTag("player_viewport").fetchSemanticsNode().boundsInRoot
                        val target = rule.onNodeWithTag("player_play_pause").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                        val glyph = rule.onNodeWithContentDescription("暂停播放", useUnmergedTree = true)
                            .fetchSemanticsNode().boundsInRoot
                        val expectedCenterY = host.center.y +
                            if (isFullscreen) 0f else 12f * host.width / dimensions.first
                        val mode = "$uiStyle glass=$useGlass fullscreen=$isFullscreen viewport=$dimensions"
                        assertEquals("$mode target x", host.center.x, target.center.x, 1f)
                        assertEquals("$mode target y", expectedCenterY, target.center.y, 1f)
                        assertEquals("$mode glyph x", host.center.x, glyph.center.x, 1f)
                        assertEquals("$mode glyph y", expectedCenterY, glyph.center.y, 1f)
                    }
                }
            }
        }
    }
}
