package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.data.model.response.DrawMajor
import com.android.purebilibili.data.model.response.DrawItem
import com.android.purebilibili.data.model.response.OpusMajor
import com.android.purebilibili.data.model.response.OpusPic
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ForwardedContentPreviewPolicyTest {

    @Test
    fun resolveForwardedDrawPreviewState_returnsAllImagesWithClickedIndex() {
        val draw = DrawMajor(
            items = listOf(
                DrawItem(src = "a.jpg"),
                DrawItem(src = "b.jpg"),
                DrawItem(src = "c.jpg")
            )
        )

        val state = resolveForwardedDrawPreviewState(draw, clickedIndex = 1)

        assertEquals(listOf("a.jpg", "b.jpg", "c.jpg"), state?.images)
        assertEquals(1, state?.initialIndex)
    }

    @Test
    fun resolveForwardedOpusPreviewState_returnsNullWhenIndexInvalid() {
        val opus = OpusMajor(
            pics = listOf(
                OpusPic(url = "1.jpg"),
                OpusPic(url = "2.jpg")
            )
        )

        val state = resolveForwardedOpusPreviewState(opus, clickedIndex = 2)

        assertNull(state)
    }

    @Test
    fun resolveForwardedDrawPreviewState_keepsNineImageCollageIndexes() {
        val draw = DrawMajor(
            items = (1..9).map { DrawItem(src = "$it.jpg") }
        )

        val state = resolveForwardedDrawPreviewState(draw, clickedIndex = 8)

        assertEquals((1..9).map { "$it.jpg" }, state?.images)
        assertEquals(8, state?.initialIndex)
        assertEquals(
            DYNAMIC_FEED_PREVIEW_MAX_IMAGES,
            resolveDrawGridDisplayCount(
                totalImages = draw.items.size,
                maxDisplayImages = resolveDynamicOpusPreviewImageLimit(isDetail = false)
            )
        )
        assertEquals(3, resolveDrawGridColumnCount(displayCount = DYNAMIC_FEED_PREVIEW_MAX_IMAGES))
    }

    @Test
    fun forwardedContent_doesNotHardCapGridAtFourImages() {
        val path = "app/src/main/java/com/android/purebilibili/feature/dynamic/components/ForwardedContent.kt"
        val sourceFile = listOf(File(path), File(path.removePrefix("app/"))).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        val source = sourceFile.readText()

        assertFalse(source.contains("take(4)"))
        assertTrue(source.contains("maxDisplayImages = resolveDynamicOpusPreviewImageLimit(isDetail = false)"))
    }
}
