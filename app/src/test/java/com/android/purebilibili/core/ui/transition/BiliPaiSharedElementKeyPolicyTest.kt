package com.android.purebilibili.core.ui.transition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BiliPaiSharedElementKeyPolicyTest {

    @Test
    fun videoKeys_useElementTypeInsteadOfStringPrefixes() {
        val cover = videoCoverSharedElementKey("BV1")
        val title = videoTitleSharedElementKey("BV1")

        assertEquals(
            BiliPaiSharedElementKey.Video("BV1", VideoSharedElement.COVER),
            cover
        )
        assertNotEquals(cover, title)
    }

    @Test
    fun matchingVideoKeysRemainEqualAcrossSourceAndDestinationWhenSourceRouteIsNotSpecified() {
        assertEquals(
            videoCoverSharedElementKey("BV1"),
            videoCoverSharedElementKey("BV1")
        )
    }

    @Test
    fun sourceRouteCanDisambiguateFutureMultiOriginTransitions() {
        assertNotEquals(
            videoCoverSharedElementKey("BV1", sourceRoute = "home"),
            videoCoverSharedElementKey("BV1", sourceRoute = "search")
        )
    }

    @Test
    fun metadataKeysCanUseSourceRouteToAvoidCrossOriginMatches() {
        assertNotEquals(
            videoTitleSharedElementKey("BV1", sourceRoute = "history"),
            videoTitleSharedElementKey("BV1", sourceRoute = "home")
        )
        assertEquals(
            videoUpNameSharedElementKey("BV1", sourceRoute = "history"),
            videoUpNameSharedElementKey("BV1", sourceRoute = "history")
        )
    }

    @Test
    fun liveAvatarAndArticleKeysAreStructured() {
        assertEquals(BiliPaiSharedElementKey.Live(6L), liveCoverSharedElementKey(6L))
        assertEquals(BiliPaiSharedElementKey.Avatar(42L), avatarSharedElementKey(42L))
        assertEquals(BiliPaiSharedElementKey.ArticleCover(99L), articleCoverSharedElementKey(99L))
    }
}
