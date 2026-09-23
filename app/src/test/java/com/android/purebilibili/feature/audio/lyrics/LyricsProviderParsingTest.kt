package com.android.purebilibili.feature.audio.lyrics

import kotlin.test.Test
import kotlin.test.assertEquals

class LyricsProviderParsingTest {

    @Test
    fun `netease parser reads candidates and word timed lyrics`() {
        val candidates = parseNeteaseCandidates(
            """{"result":{"songs":[{"id":7,"name":"Song","duration":180000,"artists":[{"name":"Artist"}]}]}}"""
        )
        val lyrics = parseNeteaseLyrics(
            """{"yrc":{"lyric":"[1000,1000](1000,400,0)Your (1400,600,0)voice"},"ytlrc":{"lyric":"[00:01.00]你的声音"}}"""
        )

        assertEquals(
            LyricCandidate(LyricSource.NETEASE, "7", "Song", "Artist", 180_000L),
            candidates.single()
        )
        val document = parseSplLyrics(lyrics.primary, lyrics.translation, source = LyricSource.NETEASE)
        assertEquals(listOf("Your ", "voice"), document.lines.single().spans.map { it.text })
        assertEquals(listOf("你的声音"), document.lines.single().translations)
    }

    @Test
    fun `qq parser reads search and translated lyrics`() {
        val candidates = parseQqCandidates(
            """{"data":{"song":{"list":[{"songmid":"mid1","songname":"Song","interval":180,"singer":[{"name":"Artist"}]}]}}}"""
        )
        val lyrics = parseQqLyrics(
            """{"lyric":"[00:01.00]Song","trans":"[00:01.00]歌曲"}"""
        )

        assertEquals(LyricSource.QQ_MUSIC, candidates.single().source)
        assertEquals(180_000L, candidates.single().durationMs)
        assertEquals("[00:01.00]歌曲", lyrics.translation)
    }

    @Test
    fun `qq parser reads current smartbox search response`() {
        val candidates = parseQqCandidates(
            """{"code":0,"data":{"song":{"itemlist":[{"mid":"0039MnYb0qxYhV","name":"晴天","singer":"周杰伦"}]}}}"""
        )

        assertEquals(
            LyricCandidate(LyricSource.QQ_MUSIC, "0039MnYb0qxYhV", "晴天", "周杰伦", 0L),
            candidates.single()
        )
    }

    @Test
    fun `kugou parser reads candidates and base64 lyrics`() {
        val candidates = parseKugouCandidates(
            """{"status":1,"data":{"info":[{"hash":"hash1","songname":"Song","singername":"Artist","duration":180}]}}"""
        )
        val lyrics = parseKugouDownloadedLyrics("""{"content":"WzAwOjAxLjAwXVNvbmc="}""")

        assertEquals(LyricSource.KUGOU, candidates.single().source)
        assertEquals("[00:01.00]Song", lyrics.primary)
    }

    @Test
    fun `kugou parser reads current song search response`() {
        val candidates = parseKugouCandidates(
            """{"status":1,"data":{"lists":[{"FileHash":"B3A5","SongName":"晴天","SingerName":"周杰伦","Duration":269}]}}"""
        )

        assertEquals(
            LyricCandidate(LyricSource.KUGOU, "B3A5", "晴天", "周杰伦", 269_000L),
            candidates.single()
        )
    }
}
