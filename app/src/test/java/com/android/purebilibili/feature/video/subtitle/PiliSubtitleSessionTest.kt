package com.android.purebilibili.feature.video.subtitle

import com.android.purebilibili.feature.video.ui.components.PlayerSubtitleAppearance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class PiliSubtitleSessionTest {
    @Test fun acquiredNativeCuesStaySeparateFromImportedDocuments() {
        val session = PiliSubtitleSession(SubtitleDocumentIdentity("BVnative", 100))
        val native = SubtitleTrackMeta(lan = "zh-Hans", lanDoc = "中文",
            subtitleUrl = "https://aisubtitle.hdslb.com/bfs/subtitle/real.json")
        val nativeCues = listOf(SubtitleCue(0, 2000, "native"))
        session.acquiredTracks = listOf(native)
        session.acquiredCues = mapOf(native.trackKey to nativeCues)
        session.selectedTrackKey = native.trackKey
        session.append("local.srt", listOf(SubtitleCue(0, 2000, "local")))
        assertSame(nativeCues, session.acquiredCues[native.trackKey])
        assertEquals(native.subtitleUrl, session.acquiredTracks.single().subtitleUrl)
        assertNotEquals(native.trackKey, session.selectedTrackKey)
        assertEquals(1, session.importedTracks.size)
    }

    @Test fun importedTrackContainsRealCuesAndIsSelected() {
        val session = PiliSubtitleSession(SubtitleDocumentIdentity("BVtest", 42))
        val cues = listOf(SubtitleCue(1000, 2000, "Actual document text"))
        session.append("captions.srt", cues)
        val track = session.importedTracks.single()
        assertEquals("captions.srt", track.name)
        assertSame(cues, track.cues)
        assertEquals(track.key, session.selectedTrackKey)
        session.append("second.ass", cues)
        assertNotEquals(track.key, session.selectedTrackKey)
        assertEquals(2, session.importedTracks.size)
    }

    @Test fun sessionsSurviveModeChangesButNeverLeakAcrossParts() {
        val first = PiliSubtitleSessions.get("BVidentitytest", 101)
        first.append("local.vtt", listOf(SubtitleCue(0, 1000, "first")))
        first.timeOffsetMs = 250
        assertSame(first, PiliSubtitleSessions.get("BVidentitytest", 101))
        val next = PiliSubtitleSessions.get("BVidentitytest", 102)
        assertNull(next.selectedTrackKey)
        assertEquals(0, next.importedTracks.size)
        assertEquals(0L, next.timeOffsetMs)
    }

    @Test fun appearanceUsesReferenceDefaultsAndBounds() {
        assertEquals(PlayerSubtitleAppearance(), normalizeSubtitleAppearance(PlayerSubtitleAppearance()))
        val clamped = normalizeSubtitleAppearance(PlayerSubtitleAppearance(
            inlineFontScale = 9f, fullscreenFontScale = -1f, fontWeight = 20, strokeWidth = 8f,
            horizontalPadding = -10, bottomPadding = 300, backgroundOpacity = -1f
        ))
        assertEquals(PlayerSubtitleAppearance(2.5f, 0.5f, 9, 5f, 0, 200, 0f), clamped)
    }
}
