package com.android.purebilibili.core.ui.common

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class TextSelectionHostStructureTest {

    @Test
    fun provideAppTextSelectionHostProvidesControllerAndCustomToolbar() {
        val source = File(
            "src/main/java/com/android/purebilibili/core/ui/common/TextSelectionHost.kt"
        ).readText()

        assertTrue(source.contains("LocalTextSelectionController provides controller"))
        assertTrue(source.contains("LocalTextToolbar provides customToolbar"))
        assertTrue(source.contains("AppFloatingSelectionToolbar("))
        assertTrue(source.contains("TextSelectionBottomSheet("))
        assertTrue(source.contains("formatInPlaceCopyFeedback("))
    }
}
