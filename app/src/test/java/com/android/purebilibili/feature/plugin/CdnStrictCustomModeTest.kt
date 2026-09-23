package com.android.purebilibili.feature.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CdnStrictCustomModeTest {
    private val originalCandidates = buildPlaybackCdnCandidates(
        videoUrls = listOf("https://original.bilivideo.com/video.m4s?token=1"),
        audioUrls = listOf("https://original.bilivideo.com/audio.m4s?token=1")
    )

    private val customCandidates = rewritePlaybackCdnCandidatesForCustomRules(
        candidates = originalCandidates,
        rules = listOf(
            CdnCustomRule(
                pattern = "original\\.bilivideo\\.com",
                replacement = "upos-sz-mirroralib.bilivideo.com"
            )
        )
    )

    private val regionCandidates = rewritePlaybackCdnCandidatesForRegion(
        candidates = originalCandidates,
        preferredHosts = listOf("upos-sz-mirrorcosov.bilivideo.com")
    )

    @Test
    fun `strict mode returns only rewritten custom candidates`() {
        val selected = selectPlaybackCdnCandidatesForMode(
            customCandidates = customCandidates,
            regionCandidates = regionCandidates,
            originalCandidates = originalCandidates,
            strictCustomCdn = true,
            healthByHost = emptyMap()
        )

        assertEquals(1, selected.size)
        assertTrue(selected.all { it.source == PlaybackCdnCandidateSource.CUSTOM })
        assertEquals("upos-sz-mirroralib.bilivideo.com", hostFromCdnUrl(selected.single().videoUrl))
    }

    @Test
    fun `strict mode falls back when no custom rule produced a candidate`() {
        val selected = selectPlaybackCdnCandidatesForMode(
            customCandidates = emptyList(),
            regionCandidates = regionCandidates,
            originalCandidates = originalCandidates,
            strictCustomCdn = true,
            healthByHost = emptyMap()
        )

        assertTrue(selected.any { it.source == PlaybackCdnCandidateSource.REGION })
        assertTrue(selected.any { it.source == PlaybackCdnCandidateSource.ORIGINAL })
    }
}
