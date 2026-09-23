package com.android.purebilibili.feature.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.store.PlayerProgressPlacement
import com.android.purebilibili.feature.video.ui.overlay.BottomControlBar
import com.android.purebilibili.feature.video.ui.overlay.PlayerProgress
import com.android.purebilibili.feature.video.ui.overlay.TopControlBar
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerControlsUiRegressionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun castButton_respectsVisibilitySetting() {
        setTopBar(showCastButton = false)
        composeTestRule.onNodeWithContentDescription("投屏").assertDoesNotExist()

        setTopBar(showCastButton = true)
        composeTestRule.onNodeWithContentDescription("投屏").assertExists()
    }

    @Test
    fun progressPlacement_movesProgressAcrossControlRow() {
        setBottomBar(PlayerProgressPlacement.ABOVE_CONTROLS)
        assertProgressIsAboveControls()

        setBottomBar(PlayerProgressPlacement.BOTTOM_EDGE)
        val progress = composeTestRule.onNodeWithTag("player_progress").fetchSemanticsNode().boundsInRoot
        val controls = composeTestRule.onNodeWithTag("player_control_row").fetchSemanticsNode().boundsInRoot
        assertTrue(progress.top >= controls.bottom)
    }

    private fun setTopBar(showCastButton: Boolean) {
        composeTestRule.setContent {
            MaterialTheme {
                TopControlBar(
                    title = "测试视频",
                    isFullscreen = false,
                    showCurrentTime = false,
                    showInteractiveActions = false,
                    showCastButton = showCastButton,
                    onBack = {}
                )
            }
        }
    }

    private fun setBottomBar(progressPlacement: PlayerProgressPlacement) {
        composeTestRule.setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .size(width = 800.dp, height = 180.dp)
                        .background(Color.Black)
                ) {
                    BottomControlBar(
                        isPlaying = true,
                        progress = PlayerProgress(current = 30_000, duration = 120_000),
                        isFullscreen = false,
                        progressPlacement = progressPlacement,
                        onPlayPauseClick = {},
                        onSeek = {},
                        onToggleFullscreen = {}
                    )
                }
            }
        }
    }

    private fun assertProgressIsAboveControls() {
        val progress = composeTestRule.onNodeWithTag("player_progress").fetchSemanticsNode().boundsInRoot
        val controls = composeTestRule.onNodeWithTag("player_control_row").fetchSemanticsNode().boundsInRoot
        assertTrue(progress.bottom <= controls.top)
    }
}
