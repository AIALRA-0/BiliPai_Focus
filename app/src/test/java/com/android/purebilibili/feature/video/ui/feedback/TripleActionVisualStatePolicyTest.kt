package com.android.purebilibili.feature.video.ui.feedback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripleActionVisualStatePolicyTest {

    @Test
    fun `full triple success activates all three action states`() {
        val state = resolveTripleActionVisualState(
            currentLiked = false,
            currentCoinCount = 0,
            currentFavorited = false,
            likeSuccess = true,
            coinSuccess = true,
            coinFailureMessage = null,
            favoriteSuccess = true
        )

        assertTrue(state.isLiked)
        assertEquals(2, state.coinCount)
        assertTrue(state.isFavorited)
    }

    @Test
    fun `partial triple success preserves unchanged actions`() {
        val state = resolveTripleActionVisualState(
            currentLiked = false,
            currentCoinCount = 0,
            currentFavorited = true,
            likeSuccess = true,
            coinSuccess = false,
            coinFailureMessage = null,
            favoriteSuccess = false
        )

        assertTrue(state.isLiked)
        assertEquals(0, state.coinCount)
        assertTrue(state.isFavorited)
    }

    @Test
    fun `already maxed coin message still highlights the coin state after triple action`() {
        val state = resolveTripleActionVisualState(
            currentLiked = true,
            currentCoinCount = 0,
            currentFavorited = false,
            likeSuccess = false,
            coinSuccess = false,
            coinFailureMessage = "已投满2个硬币",
            favoriteSuccess = false
        )

        assertTrue(state.isLiked)
        assertEquals(2, state.coinCount)
    }

    @Test
    fun `repost triple success records one coin instead of two`() {
        val state = resolveTripleActionVisualState(
            currentLiked = false,
            currentCoinCount = 0,
            currentFavorited = false,
            likeSuccess = true,
            coinSuccess = true,
            coinFailureMessage = null,
            favoriteSuccess = true,
            attemptedCoinCount = 1
        )

        assertEquals(1, state.coinCount)
    }

    @Test
    fun `repost coin limit message settles at one coin`() {
        val state = resolveTripleActionVisualState(
            currentLiked = true,
            currentCoinCount = 0,
            currentFavorited = false,
            likeSuccess = false,
            coinSuccess = false,
            coinFailureMessage = "转载视频最多投1个硬币",
            favoriteSuccess = false,
            attemptedCoinCount = 1
        )

        assertEquals(1, state.coinCount)
    }

    @Test
    fun `attempted coin count never lowers an already higher coin count`() {
        val state = resolveTripleActionVisualState(
            currentLiked = false,
            currentCoinCount = 2,
            currentFavorited = false,
            likeSuccess = true,
            coinSuccess = false,
            coinFailureMessage = "已投满2个硬币",
            favoriteSuccess = true,
            attemptedCoinCount = 1
        )

        assertEquals(2, state.coinCount)
    }
}
