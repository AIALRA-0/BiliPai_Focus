package com.android.purebilibili.feature.video.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailScrollStatePersistenceStructureTest {

    @Test
    fun `phone detail scroll state survives loading and navigation preview`() {
        val screenSource = loadSource("VideoDetailScreenStateHolder.kt")
        val phoneSource = loadSource("VideoDetailPhoneContent.kt")
        val contentSource = loadSource("VideoContentSection.kt")

        assertTrue(screenSource.contains("saver = LazyListState.Saver"))
        assertTrue(screenSource.contains("rememberSaveable(currentBvid"))
        assertTrue(screenSource.contains("introListState = introListState"))
        assertTrue(screenSource.contains("commentListState = commentListState"))
        assertTrue(screenSource.contains("videoContentPagerState = videoContentPagerState"))
        assertTrue(phoneSource.contains("introListState: LazyListState"))
        assertTrue(phoneSource.contains("commentListState: LazyListState"))
        assertTrue(contentSource.contains("introListState: LazyListState"))
        assertTrue(contentSource.contains("commentListState: LazyListState"))
        assertTrue(contentSource.contains("pagerState: PagerState"))
        assertFalse(contentSource.contains("val introListState = rememberLazyListState()"))
        assertFalse(contentSource.contains("val commentListState = rememberLazyListState()"))
        assertFalse(contentSource.contains("val pagerState = rememberPagerState"))
    }

    @Test
    fun `phone player collapse state is saveable per video`() {
        val source = loadSource("VideoDetailScreenStateHolder.kt")
        val collapseStateSource = loadSource("VideoDetailInlineCollapseState.kt")

        assertTrue(collapseStateSource.contains("InlinePortraitPlayerCollapseState.Saver"))
        assertTrue(collapseStateSource.contains("rememberSaveable(videoBvid, saver = InlinePortraitPlayerCollapseState.Saver)"))
        assertTrue(source.contains("rememberInlinePortraitPlayerCollapseState(currentBvid)"))
        assertTrue(source.contains("var introScrollPastCollapseThreshold by rememberSaveable(currentBvid)"))
        assertFalse(collapseStateSource.contains("remember { InlinePortraitPlayerCollapseState() }"))
    }

    private fun loadSource(name: String): String {
        val file = File("src/main/java/com/android/purebilibili/feature/video/screen/$name")
        require(file.exists()) { "Cannot locate ${file.absolutePath}" }
        return file.readText()
    }
}
