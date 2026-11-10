package com.android.purebilibili.feature.dynamic

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DynamicSidebarAvatarPrefetchTest {
    private val users = (1L..30L).map { SidebarUser(it, "user$it", "https://avatar/$it.jpg") }

    @Test
    fun `neighbors follow user identities instead of header positions and stay bounded`() {
        assertEquals(
            listOf(13, 14, 15, 9, 8, 7).map { "https://avatar/$it.jpg" },
            resolveDynamicSidebarNeighborAvatarUrls(users, listOf(10L, 11L, 12L)),
        )
        assertTrue(resolveDynamicSidebarNeighborAvatarUrls(users, listOf(-1L)).isEmpty())
        assertTrue(resolveDynamicSidebarNeighborAvatarUrls(users, emptyList()).isEmpty())
        assertEquals(3, resolveDynamicSidebarNeighborAvatarUrls(users, listOf(1L)).size)
        assertEquals(3, resolveDynamicSidebarNeighborAvatarUrls(users, listOf(30L)).size)
    }

    @Test
    fun `avatar decoding stays bounded on phones and high density displays`() {
        assertEquals(48, resolveDynamicUserAvatarDecodeSizePx(1f))
        assertEquals(144, resolveDynamicUserAvatarDecodeSizePx(3f))
        assertEquals(256, resolveDynamicUserAvatarDecodeSizePx(10f))
        assertEquals(48, resolveDynamicUserAvatarDecodeSizePx(Float.NaN))
    }

    @Test
    fun `neighbor prefetch skips blank avatars and deduplicates normalized urls`() {
        val repeated = listOf(
            SidebarUser(1, "A", "http://avatar/shared.jpg"),
            SidebarUser(2, "B", ""),
            SidebarUser(3, "C", "https://avatar/visible.jpg"),
            SidebarUser(4, "D", "//avatar/shared.jpg"),
        )
        assertEquals(
            listOf("https://avatar/shared.jpg"),
            resolveDynamicSidebarNeighborAvatarUrls(repeated, listOf(3L)),
        )
        assertEquals(3, resolveDynamicSidebarAvatarPrefetchUrls(users, 27, Int.MAX_VALUE).size)
    }

    @Test
    fun `fast scrolling cancels in flight work and never accumulates a decode queue`() = runTest {
        val viewport = MutableStateFlow(listOf(10L))
        var active = 0
        var peakActive = 0
        val started = mutableListOf<String>()
        val job = launch {
            prefetchDynamicSidebarNeighborAvatars(users, viewport) { url ->
                started += url
                active++
                peakActive = maxOf(peakActive, active)
                try { awaitCancellation() } finally { active-- }
            }
        }
        runCurrent()
        advanceTimeBy(100)
        viewport.value = emptyList()
        runCurrent()
        advanceTimeBy(500)
        assertTrue(started.isEmpty())

        viewport.value = listOf(10L)
        runCurrent()
        advanceTimeBy(120)
        runCurrent()
        assertEquals(listOf("https://avatar/11.jpg"), started)
        viewport.value = emptyList()
        runCurrent()
        assertEquals(0, active)

        viewport.value = listOf(20L)
        runCurrent()
        advanceTimeBy(120)
        runCurrent()
        assertEquals("https://avatar/21.jpg", started.last())
        assertEquals(1, peakActive)
        job.cancelAndJoin()
        assertEquals(0, active)
        advanceTimeBy(1000)
        assertEquals(2, started.size)
    }
}
