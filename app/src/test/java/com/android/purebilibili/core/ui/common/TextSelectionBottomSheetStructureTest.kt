package com.android.purebilibili.core.ui.common

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class TextSelectionBottomSheetStructureTest {

    @Test
    fun textSelectionBottomSheetUsesAppModalBottomSheetAndSelectionContainer() {
        val source = File(
            "src/main/java/com/android/purebilibili/core/ui/common/TextSelectionBottomSheet.kt"
        ).readText()

        assertTrue(source.contains("AppModalBottomSheet("))
        assertTrue(source.contains("BasicTextField("))
        assertTrue(source.contains("AppText("))
        assertTrue(source.contains("copyPlainTextToClipboard("))
        assertTrue(source.contains("TextSelectionPolicy"))
    }
}
