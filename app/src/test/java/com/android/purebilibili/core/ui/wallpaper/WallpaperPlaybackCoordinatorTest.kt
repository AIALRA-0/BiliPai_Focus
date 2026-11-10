package com.android.purebilibili.core.ui.wallpaper

import kotlin.test.Test
import kotlin.test.assertEquals

class WallpaperPlaybackCoordinatorTest {
    @Test
    fun previewStopsBackgroundBeforeStartingAndRestoresItOnClose() {
        val coordinator = WallpaperPlaybackCoordinator()
        val background = Any()
        val preview = Any()
        val events = mutableListOf<String>()
        coordinator.acquire(background, -10) { events += "background:$it" }
        events.clear()
        coordinator.acquire(preview, 10) { events += "preview:$it" }
        assertEquals(listOf("preview:false", "background:false", "preview:true"), events)
        events.clear()
        coordinator.release(preview)
        assertEquals(listOf("preview:false", "background:true"), events)
    }

    @Test
    fun hiddenOrDisposedBackgroundCannotResumeAfterPreviewCloses() {
        val coordinator = WallpaperPlaybackCoordinator()
        val background = Any()
        val preview = Any()
        val events = mutableListOf<String>()
        coordinator.acquire(background, 0) { events += "background:$it" }
        coordinator.acquire(preview, 10) { events += "preview:$it" }
        coordinator.release(background)
        events.clear()
        coordinator.release(preview)
        assertEquals(listOf("preview:false"), events)
    }

    @Test
    fun newVisiblePageWinsAtEqualPriorityAndLowerPriorityDoesNotStealPlayback() {
        val coordinator = WallpaperPlaybackCoordinator()
        val oldPage = Any()
        val newPage = Any()
        val root = Any()
        val events = mutableListOf<String>()
        coordinator.acquire(oldPage, 0) { events += "old:$it" }
        coordinator.acquire(newPage, 0) { events += "new:$it" }
        events.clear()
        coordinator.acquire(root, -10) { events += "root:$it" }
        assertEquals(listOf("root:false"), events)
        events.clear()
        coordinator.release(newPage)
        assertEquals(listOf("new:false", "old:true"), events)
    }
}
