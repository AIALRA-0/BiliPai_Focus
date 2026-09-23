package com.android.purebilibili.feature.audio.screen

import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.util.AppFoldPosture
import com.android.purebilibili.core.util.AppHingeOrientation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MusicPlayerLayoutPolicyTest {

    @Test
    fun `unhinged tabletop reserves room for enlarged shelf`() {
        assertEquals(MusicTabletopPaneSizes(384, 0, 416), resolveMusicTabletopPaneSizes(800))
        val small = resolveMusicTabletopPaneSizes(560)
        assertEquals(560, small.upperHeightDp + small.lowerHeightDp)
        assertTrue(small.lowerHeightDp > small.upperHeightDp)
    }


    @Test
    fun `short landscape windows use compact two pane controls`() {
        listOf(640 to 360, 800 to 360, 920 to 412).forEach { (width, height) ->
            assertEquals(
                MusicPlayerLayout.COMPACT_LANDSCAPE,
                resolveMusicPlayerLayout(widthDp = width, heightDp = height, isInPipMode = false),
            )
        }
        assertEquals(
            MusicPlayerLayout.COMPACT_LANDSCAPE,
            resolveMusicPlayerLayout(widthDp = 800, heightDp = 360, fontScale = 1.5f, isInPipMode = false),
        )
    }

    @Test
    fun `landscape policy preserves portrait tablet and pip layouts`() {
        assertEquals(MusicPlayerLayout.COMPACT_PAGER,
            resolveMusicPlayerLayout(widthDp = 360, heightDp = 800, isInPipMode = false))
        assertEquals(MusicPlayerLayout.EXPANDED_SPLIT,
            resolveMusicPlayerLayout(widthDp = 800, heightDp = 480, isInPipMode = false))
        assertEquals(MusicPlayerLayout.PIP_ARTWORK,
            resolveMusicPlayerLayout(widthDp = 800, heightDp = 360, isInPipMode = true))
    }


    @Test
    fun `compact portrait uses horizontal pager`() {
        assertEquals(
            MusicPlayerLayout.COMPACT_PAGER,
            resolveMusicPlayerLayout(widthDp = 393, isInPipMode = false)
        )
    }

    @Test
    fun `wide screen keeps artwork and lyrics visible together`() {
        assertEquals(
            MusicPlayerLayout.EXPANDED_SPLIT,
            resolveMusicPlayerLayout(widthDp = 900, isInPipMode = false)
        )
    }

    @Test
    fun `pip always renders artwork only`() {
        assertEquals(
            MusicPlayerLayout.PIP_ARTWORK,
            resolveMusicPlayerLayout(widthDp = 900, isInPipMode = true)
        )
    }

    @Test
    fun `compact pager tabs stay on cover and lyrics`() {
        assertEquals(listOf("封面", "歌词"), resolveMusicPlayerPageTabs())
        assertEquals(70, MUSIC_PLAYER_COMPACT_DOCK_BOTTOM_PADDING_DP)
    }

    @Test
    fun `chrome keeps artwork backdrop when liquid refraction is disabled`() {
        val md3 = resolveMusicPlayerChromeSpec(AppUiStyle.MATERIAL3, glassEnabled = false)
        assertEquals(18, md3.horizontalPaddingDp)
        assertEquals(80, md3.playButtonSizeDp)
        assertTrue(md3.usePaletteImmersiveBackdrop)
        assertTrue(md3.coverShapeIsCircle)

        val miuix = resolveMusicPlayerChromeSpec(AppUiStyle.MIUIX, glassEnabled = false)
        assertEquals(16, miuix.horizontalPaddingDp)
        assertEquals(72, miuix.playButtonSizeDp)
        assertTrue(miuix.usePaletteImmersiveBackdrop)

        val glass = resolveMusicPlayerChromeSpec(AppUiStyle.MATERIAL3, glassEnabled = true)
        assertTrue(glass.usePaletteImmersiveBackdrop)
        assertTrue(glass.coverShapeIsCircle)
    }

    @Test
    fun `split layout requires two readable panes`() {
        assertEquals(
            MusicPlayerLayout.COMPACT_PAGER,
            resolveMusicPlayerLayout(widthDp = 600, isInPipMode = false)
        )
        assertEquals(
            MusicPlayerLayout.EXPANDED_SPLIT,
            resolveMusicPlayerLayout(widthDp = 720, isInPipMode = false)
        )
        assertEquals(
            MusicPlayerLayout.EXPANDED_SPLIT,
            resolveMusicPlayerLayout(widthDp = 840, heightDp = 700, isInPipMode = false)
        )
    }

    @Test
    fun `large font on a short window falls back to compact controls`() {
        assertEquals(
            MusicPlayerLayout.COMPACT_PAGER,
            resolveMusicPlayerLayout(
                widthDp = 900,
                heightDp = 520,
                fontScale = 1.4f,
                isInPipMode = false,
            )
        )
    }

    @Test
    fun `real horizontal hinge overrides manual split and preserves clearance`() {
        assertEquals(
            MusicPlayerLayout.TABLETOP,
            resolveMusicPlayerLayout(
                widthDp = 900,
                heightDp = 760,
                isInPipMode = false,
                preference = MusicPlayerLayoutPreference.SPLIT,
                posture = AppFoldPosture.Tabletop,
                hingeOrientation = AppHingeOrientation.Horizontal,
                hingeStartDp = 350,
                hingeEndDp = 370,
                hasObstructingHinge = true,
            )
        )
        assertEquals(
            MusicTabletopPaneSizes(334, 52, 374),
            resolveMusicTabletopPaneSizes(760, hingeStartDp = 350, hingeEndDp = 370)
        )
    }

    @Test
    fun `vertical separating hinge uses book split only when both panes fit`() {
        assertEquals(
            MusicPlayerLayout.EXPANDED_SPLIT,
            resolveMusicPlayerLayout(
                widthDp = 840,
                heightDp = 700,
                isInPipMode = false,
                posture = AppFoldPosture.Book,
                hingeOrientation = AppHingeOrientation.Vertical,
                hingeStartDp = 410,
                hingeEndDp = 430,
                hasObstructingHinge = true,
            )
        )
        assertEquals(
            MusicPlayerLayout.COMPACT_PAGER,
            resolveMusicPlayerLayout(
                widthDp = 600,
                heightDp = 700,
                isInPipMode = false,
                hingeOrientation = AppHingeOrientation.Vertical,
                hingeStartDp = 250,
                hingeEndDp = 270,
                hasObstructingHinge = true,
            )
        )
    }

    @Test
    fun `compact artwork respects available height`() {
        assertEquals(
            288,
            resolveMusicArtworkSizeDp(
                availableWidthDp = 393,
                availableHeightDp = 720,
                layout = MusicPlayerLayout.COMPACT_PAGER
            )
        )
        assertEquals(
            240,
            resolveMusicArtworkSizeDp(
                availableWidthDp = 393,
                availableHeightDp = 320,
                layout = MusicPlayerLayout.COMPACT_PAGER
            )
        )
    }

    @Test
    fun `cover style cycles through card square and turntable`() {
        assertEquals(
            MusicCoverStyle.APPLE_MUSIC_SQUARE,
            resolveNextCoverStyle(MusicCoverStyle.APPLE_MUSIC_CARD)
        )
        assertEquals(
            MusicCoverStyle.TURNTABLE,
            resolveNextCoverStyle(MusicCoverStyle.APPLE_MUSIC_SQUARE)
        )
        assertEquals(
            MusicCoverStyle.APPLE_MUSIC_CARD,
            resolveNextCoverStyle(MusicCoverStyle.TURNTABLE)
        )
        assertEquals("宽屏", resolveCoverStyleShortLabel(MusicCoverStyle.APPLE_MUSIC_CARD))
        assertEquals("方图", resolveCoverStyleShortLabel(MusicCoverStyle.APPLE_MUSIC_SQUARE))
        assertEquals("转盘", resolveCoverStyleShortLabel(MusicCoverStyle.TURNTABLE))
    }

    @Test
    fun `large screen adaptive layout scales padding and gutters`() {
        assertEquals(48, resolveLargeScreenGutterDp(widthDp = 900))
        assertEquals(28, resolveLargeScreenGutterDp(widthDp = 700))
        assertEquals(48, resolveLargeScreenHorizontalPaddingDp(widthDp = 900))
        assertEquals(24, resolveLargeScreenHorizontalPaddingDp(widthDp = 700))
        assertEquals(1200, LARGE_SCREEN_MAX_CONTENT_WIDTH_DP)
    }
}
