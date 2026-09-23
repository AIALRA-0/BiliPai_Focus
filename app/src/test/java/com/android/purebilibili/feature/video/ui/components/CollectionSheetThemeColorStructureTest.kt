package com.android.purebilibili.feature.video.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollectionSheetThemeColorStructureTest {

    @Test
    fun `collection selected state follows theme primary color`() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/video/ui/components/CollectionSheet.kt"
        ).readText()

        assertTrue(source.contains("MaterialTheme.colorScheme.primary"))
        assertFalse(source.contains("iOSBlue"))
    }
}
