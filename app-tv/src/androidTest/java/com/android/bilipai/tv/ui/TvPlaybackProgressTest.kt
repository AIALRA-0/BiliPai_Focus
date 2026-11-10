package com.android.bilipai.tv.ui

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.android.bilipai.tv.ui.components.TvPlaybackProgress
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TvPlaybackProgressTest {
    @get:Rule val compose = createComposeRule()

    @Test fun previewKeepsActualProgressAndCancelKeepsFocus() {
        val preview = mutableStateOf<Long?>(null)
        var starts = 0
        compose.setContent {
            val focus = remember { FocusRequester() }
            TvTheme {
                TvPlaybackProgress(positionMsProvider = { 30_000L }, bufferedPositionMsProvider = { 60_000L },
                    durationMs = 120_000L, previewPositionMs = preview.value, canSeek = true,
                    onStartPreview = { starts++; preview.value = 30_000L }, modifier = Modifier.focusRequester(focus))
            }
            LaunchedEffect(focus) { focus.requestFocus() }
        }
        val actualProgress = SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo,
            ProgressBarRangeInfo(0.25f, 0f..1f))
        compose.onNodeWithTag("tv-seek").assertIsFocused().assert(actualProgress)
            .performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals(1, starts); preview.value = 40_000L }
        compose.onNodeWithText("跳转到 00:40 · 确认跳转 · 返回取消").assertExists()
        compose.onNodeWithText("播放位置 00:30 / 02:00").assertExists()
        compose.onNodeWithTag("tv-seek").assert(actualProgress)
        compose.runOnIdle { preview.value = null }
        compose.onNodeWithText("跳转到 00:40 · 确认跳转 · 返回取消").assertDoesNotExist()
        compose.onNodeWithTag("tv-seek").assertIsFocused().assert(actualProgress)
    }

    @Test fun unknownDurationDisablesSeekWithoutCreatingAFakeRange() {
        compose.setContent {
            TvTheme {
                TvPlaybackProgress(positionMsProvider = { 30_000L }, bufferedPositionMsProvider = { 60_000L },
                    durationMs = 0L, previewPositionMs = null, canSeek = false, onStartPreview = {})
            }
        }
        compose.onNodeWithTag("tv-seek").assertIsNotEnabled()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ProgressBarRangeInfo))
        compose.onNodeWithText("时长尚未确定，暂不能跳转").assertExists()
    }

    @Test fun knownDurationDoesNotMakeUnseekableContentInteractive() {
        compose.setContent {
            TvTheme {
                TvPlaybackProgress(positionMsProvider = { 30_000L }, bufferedPositionMsProvider = { 60_000L },
                    durationMs = 120_000L, previewPositionMs = null, canSeek = false, onStartPreview = {})
            }
        }
        compose.onNodeWithTag("tv-seek").assertIsNotEnabled()
        compose.onNodeWithText("当前内容暂不支持跳转").assertExists()
    }
}
