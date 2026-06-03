package com.android.purebilibili.feature.profile

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileWallpaperActionLayoutPolicyTest {

    @Test
    fun regularPhoneWidth_usesCompactThreeUpWallpaperStrip() {
        assertEquals(3, resolveProfileWallpaperActionColumnCount(screenWidthDp = 393))
    }

    @Test
    fun narrowPhoneWidth_wrapsCompactWallpaperStripToTwoColumns() {
        assertEquals(2, resolveProfileWallpaperActionColumnCount(screenWidthDp = 320))
    }

    @Test
    fun wallpaperStripBlurFollowsSharedBlurToggle() {
        assertEquals(
            true,
            resolveProfileWallpaperActionBlurEnabled(
                headerBlurEnabled = false,
                bottomBarBlurEnabled = true
            )
        )
    }

    @Test
    fun compactThreeColumnWallpaperStrip_usesTwoLineActionLabels() {
        assertEquals(
            ProfileWallpaperActionLabelMode.TWO_LINE,
            resolveProfileWallpaperActionLabelMode(
                screenWidthDp = 393,
                columnCount = 3
            )
        )
        assertEquals(
            listOf("官方", "壁纸"),
            resolveProfileWallpaperActionTitleLines(
                title = "官方壁纸",
                labelMode = ProfileWallpaperActionLabelMode.TWO_LINE
            )
        )
    }

    @Test
    fun narrowTwoColumnWallpaperStrip_keepsSingleLineActionLabels() {
        assertEquals(
            ProfileWallpaperActionLabelMode.SINGLE_LINE,
            resolveProfileWallpaperActionLabelMode(
                screenWidthDp = 320,
                columnCount = 2
            )
        )
        assertEquals(
            listOf("本地相册"),
            resolveProfileWallpaperActionTitleLines(
                title = "本地相册",
                labelMode = ProfileWallpaperActionLabelMode.SINGLE_LINE
            )
        )
    }

    @Test
    fun profileWallpaperBlendBandExtendsPastBannerCutoff() {
        assertEquals(
            196f,
            resolveProfileWallpaperBlendBandDp(topBannerHeightDp = 420f)
        )
    }
}
