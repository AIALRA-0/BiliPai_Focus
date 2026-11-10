package com.android.bilipai.tv.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TvChoiceDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun opensOnCurrentQualityAndDirectionDoesNotChangeSelection() {
        var chosen = 0
        compose.setContent {
            TvTheme {
                TvChoiceDialog("画质", listOf(32 to "480P", 64 to "720P", 80 to "1080P"),
                    onDismiss = {}, onChoose = { chosen = it }, selectedValue = 64)
            }
        }
        compose.onNodeWithText("当前 · 720P").assertIsFocused().assertIsSelected()
            .performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithText("1080P").assertIsFocused()
        compose.onNodeWithText("当前 · 720P").assertIsSelected()
        compose.runOnIdle { assertEquals(0, chosen) }
        compose.onNodeWithText("1080P").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals(80, chosen) }
    }

    @Test fun missingCurrentValueFallsBackToFirstOption() {
        compose.setContent {
            TvTheme {
                TvChoiceDialog("画质", listOf(32 to "480P", 64 to "720P"),
                    onDismiss = {}, onChoose = {}, selectedValue = 120)
            }
        }
        compose.onNodeWithText("480P").assertIsFocused()
    }
}
