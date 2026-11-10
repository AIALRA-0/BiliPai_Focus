package com.android.purebilibili.data.model.response

import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class DashAudioSelectionTest {
    private val low = DashAudio(id = 30216, baseUrl = "https://example.com/64.m4s", bandwidth = 64_000, codecs = "mp4a.40.2")
    private val high = DashAudio(id = 30280, baseUrl = "https://example.com/192.m4s", bandwidth = 192_000, codecs = "mp4a.40.2")
    private val dolby = DashAudio(id = 30255, baseUrl = "https://example.com/dolby.m4s", bandwidth = 448_000, codecs = "ec-3")
    private val flac = DashAudio(id = 30251, baseUrl = "https://example.com/flac.m4s", bandwidth = 1_800_000, codecs = "fLaC")

    @Test
    fun exactReturnedIdSelectsTheSameSourceAcrossAllContainers() {
        val dash = Dash(audio = listOf(low, high), dolby = Dolby(type = 1, audio = listOf(dolby)), flac = Flac(audio = flac))
        for (track in listOf(low, high, dolby, flac)) {
            assertSame(track, dash.getBestAudio(track.id))
        }
    }

    @Test
    fun autoAndMissingRawIdUseBestOrdinarySourceRatherThanClosestNumericId() {
        val dash = Dash(audio = listOf(low, high, dolby, flac))
        assertSame(high, dash.getBestAudio())
        assertSame(high, dash.getBestAudio(30232))
        assertSame(high, dash.getBestAudio(100010))
    }

    @Test
    fun premiumOnlyCatalogCanResolveAnExplicitRawIdWithoutBaseAudio() {
        val dash = Dash(audio = null, dolby = Dolby(type = 1, audio = listOf(dolby)), flac = Flac(audio = flac))
        assertSame(dolby, dash.getBestAudio(dolby.id))
        assertSame(flac, dash.getBestAudio(flac.id))
        assertNull(dash.getBestAudio())
        assertNull(dash.getBestAudio(30216))
    }

    @Test
    fun invalidRowsAreSkippedAndBackupUrlsRemainPlayable() {
        val backup = low.copy(baseUrl = "", backupUrl = listOf("https://example.com/backup.m4s"))
        val dash = Dash(audio = listOf(high.copy(baseUrl = ""), backup, high.copy(id = 0)))
        assertSame(backup, dash.getBestAudio(30216))
        assertSame(backup, dash.getBestAudio(30280))
        assertNull(Dash(audio = listOf(high.copy(baseUrl = ""))).getBestAudio())
    }

    @Test
    fun duplicateRawIdsUseHighestBandwidthReturnedRepresentation() {
        val improved = high.copy(baseUrl = "https://example.com/improved.m4s", bandwidth = 200_000)
        assertSame(improved, Dash(audio = listOf(high, improved)).getBestAudio(30280))
    }
}
