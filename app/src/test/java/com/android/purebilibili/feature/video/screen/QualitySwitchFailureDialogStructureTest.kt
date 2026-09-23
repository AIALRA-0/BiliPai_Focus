package com.android.purebilibili.feature.video.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class QualitySwitchFailureDialogStructureTest {

    @Test
    fun `once checkbox is placed at the trailing edge of its dialog row`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailOverlayHost.kt"
        )
        val onceRow = source
            .substringAfter("fun dismissQualitySwitchFailureDialogAfterUserChoice()")
            .substringBefore("confirmButton =")

        val labelIndex = onceRow.indexOf("AppText(\n                                text = \"仅提示一次\"")
        val checkboxIndex = onceRow.indexOf("AppCheckbox(")

        assertTrue(labelIndex >= 0)
        assertTrue(checkboxIndex > labelIndex)
        assertTrue(onceRow.contains("modifier = Modifier.weight(1f)"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(File(path), File(normalizedPath))
            .firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
