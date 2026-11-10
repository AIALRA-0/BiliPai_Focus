package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AudioModeSourcePolicyTest {
    @Test
    fun oldTrackAndDifferentPartDoNotResolveNewEntry() {
        assertFalse(matchesAudioModeInitialSource("BVnew", 22L, "BVold", 11L))
        assertFalse(matchesAudioModeInitialSource("BVnew", 22L, "BVnew", 11L))
        assertTrue(matchesAudioModeInitialSource("BVnew", 22L, "BVnew", 22L))
    }

    @Test
    fun unspecifiedPartAndNowPlayingEntryAcceptTheirExistingContent() {
        assertTrue(matchesAudioModeInitialSource("BVnew", 0L, "BVnew", 22L))
        assertTrue(matchesAudioModeInitialSource("", 0L, "BVplaying", 33L))
    }

    @Test
    fun queueSwitchAfterLoadingResolvesTheEntryGate() {
        // 入口 B 已发起加载并经历过 Loading，切歌后到达的成功态属于本次播放会话。
        assertTrue(
            shouldResolveAudioModeInitialSource(
                requestedBvid = "BVentry",
                requestedCid = 11L,
                loadedBvid = "BVnext",
                loadedCid = 22L,
                entryLoadRequested = true,
                sawLoadingSinceEntry = true,
            )
        )
        // 入口各部分被仓库归一化成别的 cid 时同样要放行，否则会一直停在加载页。
        assertTrue(
            shouldResolveAudioModeInitialSource(
                requestedBvid = "BVentry",
                requestedCid = 11L,
                loadedBvid = "BVentry",
                loadedCid = 22L,
                entryLoadRequested = true,
                sawLoadingSinceEntry = true,
            )
        )
    }

    @Test
    fun entryLoadYieldsToAnInFlightQueueSwitch() {
        // 本页建立时下一首已经在加载：入口请求必须让路，否则会取消切歌并拉回入口曲目。
        assertTrue(
            shouldSkipAudioModeEntryLoadForInFlightSwitch(
                isLoading = true,
                inFlightBvid = "BVnext",
                requestedBvid = "BVentry",
            )
        )
        // 入口自己在加载时无需重复发起，由仓库层的重复请求守卫处理。
        assertFalse(
            shouldSkipAudioModeEntryLoadForInFlightSwitch(
                isLoading = true,
                inFlightBvid = "BVentry",
                requestedBvid = "BVentry",
            )
        )
        // 没有正在进行的加载（新状态或已落地）时，入口照样要发请求。
        assertFalse(
            shouldSkipAudioModeEntryLoadForInFlightSwitch(
                isLoading = false,
                inFlightBvid = "BVnext",
                requestedBvid = "BVentry",
            )
        )
        assertFalse(
            shouldSkipAudioModeEntryLoadForInFlightSwitch(
                isLoading = true,
                inFlightBvid = "",
                requestedBvid = "BVentry",
            )
        )
    }

    @Test
    fun staleSuccessBeforeAnyLoadStillWaitsForTheEntrySource() {
        // 进入时残留的旧成功态、且还没有任何加载，不能直接当成新入口首屏。
        assertFalse(
            shouldResolveAudioModeInitialSource(
                requestedBvid = "BVentry",
                requestedCid = 11L,
                loadedBvid = "BVold",
                loadedCid = 22L,
                entryLoadRequested = false,
                sawLoadingSinceEntry = false,
            )
        )
        // 即使已经发起入口加载，没有观察到 Loading 之前也不能放行别的曲目。
        assertFalse(
            shouldResolveAudioModeInitialSource(
                requestedBvid = "BVentry",
                requestedCid = 11L,
                loadedBvid = "BVold",
                loadedCid = 22L,
                entryLoadRequested = true,
                sawLoadingSinceEntry = false,
            )
        )
    }
}
