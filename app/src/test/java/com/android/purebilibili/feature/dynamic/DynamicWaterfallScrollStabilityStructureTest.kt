package com.android.purebilibili.feature.dynamic

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicWaterfallScrollStabilityStructureTest {
    @Test
    fun `image bounds used for preview do not invalidate composition while scrolling`() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/DrawGrid.kt"
        ).readText()

        assertTrue(source.contains("val imageRectRef = remember"))
        assertTrue(source.contains("imageRectRef.value = coordinates.boundsInWindow()"))
        assertFalse(source.contains("var imageRect by remember { mutableStateOf<Rect?>(null) }"))
    }
}
