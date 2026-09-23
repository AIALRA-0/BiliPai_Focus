package com.android.purebilibili.core.ui.common

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CopySelectionDialogStructureTest {

    @Test
    fun copySelectionDialogUpgradesToTextSelectionBottomSheet() {
        val source = File(
            "src/main/java/com/android/purebilibili/core/ui/common/CopySelectionDialog.kt"
        ).readText()
        assertTrue(source.contains("TextSelectionBottomSheet("))
        assertFalse(source.contains("AppAlertDialog("))
        assertFalse(source.contains("heightIn(max = 280.dp)"))
    }
}
