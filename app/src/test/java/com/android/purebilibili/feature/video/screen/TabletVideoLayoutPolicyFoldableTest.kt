package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.core.util.AppFoldPosture
import com.android.purebilibili.core.ui.adaptive.AdaptiveFoldPosture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TabletVideoLayoutPolicyFoldableTest {

    @Test
    fun tabletopPostureReducesPrimaryRatio() {
        val flatPolicy = resolveTabletVideoLayoutPolicy(
            widthDp = 1280,
            foldPosture = AppFoldPosture.Flat
        )
        val tabletopPolicy = resolveTabletVideoLayoutPolicy(
            widthDp = 1280,
            foldPosture = AppFoldPosture.Tabletop
        )

        assertTrue(tabletopPolicy.useTabletopLayout)
        assertTrue(tabletopPolicy.primaryRatio < flatPolicy.primaryRatio)
        assertEquals(0.55f, tabletopPolicy.primaryRatio)
    }

    @Test
    fun bookPostureDoesNotTriggerTabletopLayout() {
        val policy = resolveTabletVideoLayoutPolicy(
            widthDp = 1280,
            foldPosture = AppFoldPosture.Book
        )

        assertFalse(policy.useTabletopLayout)
        assertEquals(0.72f, policy.primaryRatio)
    }

    @Test
    fun cinemaLayoutHidesCurtainOnTabletop() {
        val policy = resolveTabletCinemaLayoutPolicy(
            widthDp = 1280,
            foldPosture = AppFoldPosture.Tabletop
        )

        assertTrue(policy.useTabletopLayout)
        assertEquals(0, policy.curtainPeekWidthDp)
        assertEquals(0, policy.curtainOpenWidthDp)
    }

    @Test
    fun cinemaLayoutKeepsCurtainOnFlat() {
        val policy = resolveTabletCinemaLayoutPolicy(
            widthDp = 1280,
            foldPosture = AppFoldPosture.Flat
        )

        assertFalse(policy.useTabletopLayout)
        assertTrue(policy.curtainOpenWidthDp > 0)
    }

    @Test
    fun halfOpenedPosturesDisableVideoDetailSharedElementMorph() {
        assertTrue(shouldUseVideoDetailSharedElementMorph(AdaptiveFoldPosture.Flat))
        assertFalse(shouldUseVideoDetailSharedElementMorph(AdaptiveFoldPosture.Book))
        assertFalse(shouldUseVideoDetailSharedElementMorph(AdaptiveFoldPosture.Tabletop))
    }
}
