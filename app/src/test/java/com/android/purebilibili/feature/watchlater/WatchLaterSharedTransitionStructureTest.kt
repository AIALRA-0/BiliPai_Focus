package com.android.purebilibili.feature.watchlater

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WatchLaterSharedTransitionStructureTest {

    @Test
    fun watchLaterVideoCard_usesWholeCardShellSharedBounds() {
        val source = File("src/main/java/com/android/purebilibili/feature/watchlater/WatchLaterScreen.kt")
            .readText()

        assertTrue(source.contains("CardPositionManager.recordVideoCardPosition"))
        assertTrue(source.contains("rememberNativeVideoCardSnapshotController("))
        assertTrue(source.contains("nativeCardSnapshot.capture()"))
        assertTrue(source.contains("videoCardShellSharedBoundsOrEmpty("))
        assertTrue(source.contains("crossfadeSourceContent = true"))
        assertFalse(source.contains("videoTitleSharedElementKey("))
        assertFalse(source.contains("videoUpNameSharedElementKey("))
        assertFalse(source.contains("videoViewsSharedElementKey("))
        assertTrue(source.contains("sourceRoute = sourceRoute"))
    }
}
