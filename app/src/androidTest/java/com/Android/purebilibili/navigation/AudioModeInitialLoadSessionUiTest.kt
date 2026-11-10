package com.android.purebilibili.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.purebilibili.navigation3.BiliPaiNavKey
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AudioModeInitialLoadSessionUiTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun queueLoadingCannotReplaceSelectionWithTheEntryVideo() {
        verifySession(initiallyReady = true)
    }

    @Test
    fun standaloneEntryLoadsOnceAndDoesNotReloadAfterQueueSelection() {
        verifySession(initiallyReady = false)
    }

    private fun verifySession(initiallyReady: Boolean) {
        val ready = mutableStateOf(initiallyReady)
        val redraw = mutableIntStateOf(0)
        val entry = mutableStateOf(BiliPaiNavKey.AudioMode(sourceBvid = "BVentry", sourceCid = 11L))
        val owner = Any()
        val loads = mutableListOf<AudioModeInitialLoadRequest?>()
        var latest: AudioModeInitialLoadRequest? = null
        rule.setContent {
            // Model recomposition caused by playback UI changes outside the request helper.
            redraw.intValue
            val request = rememberAudioModeInitialLoadRequest(entry.value, owner) { ready.value }
            latest = request
            LaunchedEffect(request?.bvid, request?.cid) { loads.add(request) }
        }
        rule.waitForIdle()
        val initialRequest = if (initiallyReady) null else AudioModeInitialLoadRequest("BVentry", 11L, 0L)
        rule.runOnIdle {
            assertEquals(initialRequest, latest)
            assertEquals(listOf(initialRequest), loads)
        }
        // Queue selection: Success(A) → Loading(B) → Success(B), followed by another selection.
        for (hasSuccess in listOf(true, false, true, false, true)) {
            rule.runOnIdle { ready.value = hasSuccess; redraw.intValue++ }
            rule.runOnIdle {
                assertEquals(initialRequest, latest)
                assertEquals("Queue transitions must not issue an entry reload", listOf(initialRequest), loads)
            }
        }
        // A genuinely new navigation entry must still resolve its own startup request.
        rule.runOnIdle {
            ready.value = false
            entry.value = BiliPaiNavKey.AudioMode(sourceBvid = "BVnewEntry", sourceCid = 22L)
        }
        rule.runOnIdle {
            assertEquals("BVnewEntry", latest?.bvid)
            assertEquals(22L, latest?.cid)
        }
    }
}
