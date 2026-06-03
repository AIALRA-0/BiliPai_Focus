package com.android.purebilibili.feature.video.ui.section

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoTriplePressPolicyTest {

    @Test
    fun tripleMode_doesNotStartOnInitialPressOnly() {
        assertFalse(
            shouldStartTriplePress(
                longPressConfirmed = false
            )
        )
    }

    @Test
    fun tripleMode_startsOnlyAfterLongPressConfirmation() {
        assertTrue(
            shouldStartTriplePress(
                longPressConfirmed = true
            )
        )
    }

    @Test
    fun release_cancelsIncompleteTriplePress() {
        assertTrue(
            shouldCancelTriplePressOnRelease(
                isTriplePressing = true,
                tripleCompleted = false
            )
        )
    }

    @Test
    fun release_keepsCompletedTriplePressFinished() {
        assertFalse(
            shouldCancelTriplePressOnRelease(
                isTriplePressing = true,
                tripleCompleted = true
            )
        )
    }

    @Test
    fun release_withoutTripleModeDoesNotCancelAnything() {
        assertFalse(
            shouldCancelTriplePressOnRelease(
                isTriplePressing = false,
                tripleCompleted = false
            )
        )
    }

    @Test
    fun portraitLikeButton_exposesLongPressTripleCallback() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/ui/overlay/PortraitInteractionBar.kt")
            .takeIf { it.exists() }
            ?: File("app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/PortraitInteractionBar.kt")
        val text = source.readText()

        assertTrue(text.contains("onLikeLongClick"))
        assertTrue(text.contains("portraitTripleProgress"))
        assertTrue(text.contains("shouldCancelPortraitTriplePressOnRelease"))
        assertFalse(text.contains("onLongClick = onLikeLongClick"))
    }
}
