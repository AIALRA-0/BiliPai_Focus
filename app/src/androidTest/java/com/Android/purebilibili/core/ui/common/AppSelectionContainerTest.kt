package com.android.purebilibili.core.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSelectionContainerTest {
    @get:Rule val rule = createComposeRule()

    @Test fun selectionBelongsToEachContainerAndInputKeepsItsPlatformToolbar() {
        val first = SelectionState()
        val second = SelectionState()
        lateinit var platformToolbar: TextToolbar
        lateinit var inputToolbar: TextToolbar
        rule.setContent {
            MaterialTheme {
                val platform = LocalTextToolbar.current
                ProvideAppTextSelectionHost {
                    val input = LocalTextToolbar.current
                    SideEffect { platformToolbar = platform; inputToolbar = input }
                    Column {
                        AppSelectionContainer(state = first) { Text("first") }
                        AppSelectionContainer(state = second) { Text("second") }
                        BasicTextField(value = "editable", onValueChange = {})
                    }
                }
            }
        }
        rule.runOnIdle {
            assertSame(platformToolbar, inputToolbar)
            first.select(TextRange(1, 3))
            assertEquals(listOf("ir"), first.selectedTexts.map { it.text })
            assertEquals(emptyList<String>(), second.selectedTexts.map { it.text })
            first.clear()
            second.selectAll()
            assertEquals(listOf("second"), second.selectedTexts.map { it.text })
            assertEquals(emptyList<String>(), first.selectedTexts.map { it.text })
        }
    }
}
