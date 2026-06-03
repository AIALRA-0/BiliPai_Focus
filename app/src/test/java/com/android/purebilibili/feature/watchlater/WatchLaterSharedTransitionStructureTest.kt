package com.android.purebilibili.feature.watchlater

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class WatchLaterSharedTransitionStructureTest {

    @Test
    fun watchLaterVideoCard_usesHomeStyleVideoSharedElements() {
        val source = File("src/main/java/com/android/purebilibili/feature/watchlater/WatchLaterScreen.kt")
            .readText()

        assertTrue(source.contains("CardPositionManager.recordVideoCardPosition"))
        assertTrue(source.contains("videoCoverSharedElementKey("))
        assertTrue(source.contains("videoTitleSharedElementKey(item.bvid)"))
        assertTrue(source.contains("videoUpNameSharedElementKey(item.bvid)"))
        assertTrue(source.contains("videoViewsSharedElementKey(item.bvid)"))
    }
}
