package com.android.purebilibili.feature.video.note

import android.content.ClipboardManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.AndroidClipboard
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** RSS and video notes use the same rich editor in both app UI styles. */
@RunWith(AndroidJUnit4::class)
class RichTextClipboardCompatibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun basicEditorPreservesNativeClipboardForMiuixNotes() {
        assertEditorClipboardContract(materialEditor = false)
    }

    @Test
    fun materialEditorPreservesNativeClipboardForMaterialNotes() {
        assertEditorClipboardContract(materialEditor = true)
    }

    private fun assertEditorClipboardContract(materialEditor: Boolean) {
        var nativeManager: ClipboardManager? = null
        var editorClipboard: Clipboard? = null
        composeRule.setContent {
            MaterialTheme {
                val platformClipboard = LocalClipboard.current as AndroidClipboard
                SideEffect { nativeManager = platformClipboard.clipboardManager }
                val state = rememberRichTextState()
                if (materialEditor) {
                    // Icons are composed inside BasicRichTextEditor's clipboard provider.
                    RichTextEditor(
                        state = state,
                        trailingIcon = {
                            val clipboard = LocalClipboard.current
                            SideEffect { editorClipboard = clipboard }
                        },
                    )
                } else {
                    BasicRichTextEditor(
                        state = state,
                        decorationBox = { innerTextField ->
                            val clipboard = LocalClipboard.current
                            SideEffect { editorClipboard = clipboard }
                            innerTextField()
                        },
                    )
                }
            }
        }
        composeRule.runOnIdle {
            assertNotNull(editorClipboard)
            // Compose 1.12's native selection/paste path requires this exact contract.
            // The old rc14 wrapper fails here regardless of release obfuscation.
            assertTrue(editorClipboard is AndroidClipboard)
            assertSame(nativeManager, (editorClipboard as AndroidClipboard).clipboardManager)
        }
    }
}
