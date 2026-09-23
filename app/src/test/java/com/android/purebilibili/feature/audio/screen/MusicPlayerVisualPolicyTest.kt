package com.android.purebilibili.feature.audio.screen

import com.android.purebilibili.feature.audio.lyrics.parseSplLyrics
import com.android.purebilibili.feature.audio.lyrics.LyricDocument
import com.android.purebilibili.feature.audio.lyrics.LyricLine
import com.android.purebilibili.feature.audio.lyrics.resolveActiveLyricIndex
import com.android.purebilibili.feature.audio.lyrics.resolveLyricFocusScrollOffsetPx
import com.android.purebilibili.feature.video.player.PlayMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MusicPlayerVisualPolicyTest {

    @Test
    fun `cover flow fades distant covers completely while preserving focus`() {
        assertEquals(1f, resolveMusicCoverFlowItemAlpha(0f))
        assertTrue(resolveMusicCoverFlowItemAlpha(1f) > resolveMusicCoverFlowItemAlpha(2f))
        assertTrue(resolveMusicCoverFlowItemAlpha(2.5f) < resolveMusicCoverFlowItemAlpha(2f))
        assertEquals(0f, resolveMusicCoverFlowItemAlpha(3f))
        assertEquals(0f, resolveMusicCoverFlowItemAlpha(8f))
    }


    @Test
    fun `pager indicator follows drag and stays inside two segments`() {
        assertEquals(0f, resolveMusicPagerIndicatorPosition(0, -0.4f))
        assertEquals(0.35f, resolveMusicPagerIndicatorPosition(0, 0.35f))
        assertEquals(1f, resolveMusicPagerIndicatorPosition(1, 0.4f))
    }

    @Test
    fun `liquid controls require supported foreground renderer`() {
        assertFalse(resolveMusicLiquidGlassEnabled(32, true, false, false))
        assertTrue(resolveMusicLiquidGlassEnabled(33, true, false, false))
        assertTrue(resolveMusicLiquidGlassEnabled(36, true, false, false))
        assertFalse(resolveMusicLiquidGlassEnabled(35, true, true, false))
        assertFalse(resolveMusicLiquidGlassEnabled(35, true, false, true))
        assertFalse(resolveMusicLiquidGlassEnabled(35, false, false, false))
    }

    @Test
    fun `play mode segmented indices round trip`() {
        PlayMode.entries.forEach { mode ->
            assertEquals(mode, resolveMusicPlayMode(resolveMusicPlayModeIndex(mode)))
        }
        assertEquals(PlayMode.SEQUENTIAL, resolveMusicPlayMode(-1))
    }

    @Test
    fun `secondary transport maps shuffle and repeat glyphs`() {
        assertEquals(
            MusicSecondaryTransportState(shuffleEnabled = false, repeatGlyph = MusicRepeatGlyph.OFF),
            resolveMusicSecondaryTransport(PlayMode.SEQUENTIAL)
        )
        assertEquals(
            MusicSecondaryTransportState(shuffleEnabled = true, repeatGlyph = MusicRepeatGlyph.ALL),
            resolveMusicSecondaryTransport(PlayMode.SHUFFLE)
        )
        assertEquals(
            MusicSecondaryTransportState(shuffleEnabled = false, repeatGlyph = MusicRepeatGlyph.ONE),
            resolveMusicSecondaryTransport(PlayMode.REPEAT_ONE)
        )
        assertEquals(
            MusicSecondaryTransportState(shuffleEnabled = false, repeatGlyph = MusicRepeatGlyph.ALL),
            resolveMusicSecondaryTransport(PlayMode.REPEAT_ALL)
        )
    }

    @Test
    fun `shuffle and repeat toggles follow bbplayer icon actions`() {
        assertEquals(PlayMode.SHUFFLE, resolvePlayModeAfterShuffleToggle(PlayMode.SEQUENTIAL))
        assertEquals(PlayMode.SEQUENTIAL, resolvePlayModeAfterShuffleToggle(PlayMode.SHUFFLE))
        assertEquals(PlayMode.REPEAT_ONE, resolvePlayModeAfterRepeatToggle(PlayMode.SEQUENTIAL))
        assertEquals(PlayMode.REPEAT_ALL, resolvePlayModeAfterRepeatToggle(PlayMode.REPEAT_ONE))
        assertEquals(PlayMode.SEQUENTIAL, resolvePlayModeAfterRepeatToggle(PlayMode.REPEAT_ALL))
        assertEquals(PlayMode.REPEAT_ONE, resolvePlayModeAfterRepeatToggle(PlayMode.SHUFFLE))
        assertEquals(
            MusicSecondaryTransportState(shuffleEnabled = true, repeatGlyph = MusicRepeatGlyph.ONE),
            resolveMusicSecondaryTransport(PlayMode.REPEAT_ONE, shuffleEnabled = true)
        )
        assertEquals(PlayMode.SEQUENTIAL, resolveRepeatModeAfterToggle(PlayMode.REPEAT_ALL))
        assertEquals(PlayMode.REPEAT_ONE, resolveRepeatModeAfterToggle(PlayMode.SHUFFLE))
    }

    @Test
    fun `current lyric line follows offset adjusted playback time`() {
        val document = parseSplLyrics(
            """
            [00:01.00]One
            [00:03.00]Two
            [00:05.00]Three
            """.trimIndent()
        ).withOffset(500L)

        assertEquals(-1, resolveActiveLyricIndex(document, positionMs = 1_000L))
        assertEquals(0, resolveActiveLyricIndex(document, positionMs = 1_600L))
        assertEquals(1, resolveActiveLyricIndex(document, positionMs = 3_500L))
        assertEquals(2, resolveActiveLyricIndex(document, positionMs = 8_000L))
        assertEquals(-1, resolveActiveLyricIndex(document, positionMs = 15_501L))
    }

    @Test
    fun `explicit lyric ending leaves long instrumental gap unfocused`() {
        val document = parseSplLyrics(
            """
            [00:01.00]Short line<00:02.00>
            [00:10.00]After gap
            """.trimIndent()
        )

        assertEquals(0, resolveActiveLyricIndex(document, 1_500L))
        assertEquals(-1, resolveActiveLyricIndex(document, 5_000L))
        assertEquals(1, resolveActiveLyricIndex(document, 10_000L))
    }

    @Test
    fun `overlapping lyrics prefer latest active line`() {
        val document = LyricDocument(
            lines = listOf(
                LyricLine(1_000L, 8_000L, "Long"),
                LyricLine(3_000L, 4_000L, "Short")
            )
        )

        assertEquals(1, resolveActiveLyricIndex(document, 3_500L))
        assertEquals(0, resolveActiveLyricIndex(document, 5_000L))
    }

    @Test
    fun `lyric focus offset scales with viewport instead of density constants`() {
        assertEquals(-300, resolveLyricFocusScrollOffsetPx(viewportHeightPx = 1_000))
        assertEquals(-600, resolveLyricFocusScrollOffsetPx(viewportHeightPx = 2_000))
        assertEquals(0, resolveLyricFocusScrollOffsetPx(viewportHeightPx = 0))
    }

    @Test
    fun `lyric focus keeps current line sharp and progressively blurs distant lines`() {
        assertEquals(
            MusicLyricFocusStyle(blurRadiusDp = 0, alphaPercent = 100),
            resolveMusicLyricFocusStyle(lineIndex = 4, currentIndex = 4, blurEnabled = true)
        )
        assertEquals(
            MusicLyricFocusStyle(blurRadiusDp = 1, alphaPercent = 62),
            resolveMusicLyricFocusStyle(lineIndex = 5, currentIndex = 4, blurEnabled = true)
        )
        assertEquals(
            MusicLyricFocusStyle(blurRadiusDp = 7, alphaPercent = 20),
            resolveMusicLyricFocusStyle(lineIndex = 8, currentIndex = 4, blurEnabled = true)
        )
    }

    @Test
    fun `lyric blur falls back to opacity when renderer or motion policy disables it`() {
        assertFalse(resolveMusicLyricsBlurEnabled(sdkInt = 30, effectsEnabled = true, reduceMotion = false))
        assertTrue(resolveMusicLyricsBlurEnabled(sdkInt = 31, effectsEnabled = true, reduceMotion = false))
        assertFalse(resolveMusicLyricsBlurEnabled(sdkInt = 35, effectsEnabled = false, reduceMotion = false))
        assertFalse(resolveMusicLyricsBlurEnabled(sdkInt = 35, effectsEnabled = true, reduceMotion = true))
        assertEquals(
            MusicLyricFocusStyle(blurRadiusDp = 0, alphaPercent = 62),
            resolveMusicLyricFocusStyle(lineIndex = 5, currentIndex = 4, blurEnabled = false)
        )
    }

    @Test
    fun `apple music cover scale relaxes while paused`() {
        assertEquals(1.0f, resolveAppleMusicCoverScale(isPlaying = true, reduceMotion = false))
        assertEquals(0.88f, resolveAppleMusicCoverScale(isPlaying = false, reduceMotion = false))
        assertEquals(0.88f, resolveAppleMusicCoverScale(isPlaying = false, reduceMotion = true))
        assertEquals(16f, resolveAppleMusicCoverShadowElevation(1f))
        assertEquals(11.52f, resolveAppleMusicCoverShadowElevation(0f), 0.001f)
    }

    @Test
    fun `cover flow entrance stabilizes center before opening neighboring covers`() {
        assertEquals(0.25f, resolveMusicCoverFlowItemEntranceProgress(0.25f, 0f))
        assertEquals(0f, resolveMusicCoverFlowItemEntranceProgress(0.25f, 2f))
        assertTrue(resolveMusicCoverFlowItemEntranceProgress(0.5f, 1f) > 0f)
        assertEquals(1f, resolveMusicCoverFlowItemEntranceProgress(1f, 2f))
    }

    @Test
    fun `cover flow reflection waits until covers are seated`() {
        assertEquals(0f, resolveMusicCoverFlowShadowEntranceProgress(0.58f))
        assertTrue(resolveMusicCoverFlowShadowEntranceProgress(0.8f) in 0f..1f)
        assertEquals(1f, resolveMusicCoverFlowShadowEntranceProgress(1f))
    }
}
