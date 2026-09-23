package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class DiagnosticsMd3AlignmentStructureTest {

    @Test
    fun `export logs description uses subtitle only in md3`() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt",
        ).readText()

        assertTrue(
            source.contains(
                "useMd3ExportLogsDescription = LocalAppUiStyle.current == AppUiStyle.MATERIAL3",
            ),
        )
        assertTrue(
            source.contains("subtitle = exportLogsDescription.takeIf { useMd3ExportLogsDescription }"),
        )
        assertTrue(
            source.contains("value = exportLogsDescription.takeUnless { useMd3ExportLogsDescription }"),
        )
    }
}
