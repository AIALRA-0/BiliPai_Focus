package com.android.purebilibili.feature.home

import com.android.purebilibili.core.theme.UiPreset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePullRefreshUiPolicyTest {

    @Test
    fun `md3 preset uses material refresh motion style`() {
        assertEquals(
            HomePullRefreshMotionStyle.MD3,
            resolveHomePullRefreshMotionStyle(UiPreset.MD3)
        )
        assertEquals(
            HomePullRefreshMotionStyle.IOS,
            resolveHomePullRefreshMotionStyle(UiPreset.IOS)
        )
    }

    @Test
    fun `resolvePullRefreshThresholdDp returns comfortable trigger distance`() {
        assertEquals(56f, resolvePullRefreshThresholdDp(), 0.001f)
    }

    @Test
    fun `comfortable pull refresh threshold reduces required finger travel from material default`() {
        val requiredFingerTravelDp = resolveRequiredPullDistanceDp(
            thresholdDp = resolvePullRefreshThresholdDp(),
            dragMultiplier = 0.5f
        )

        assertEquals(112f, requiredFingerTravelDp, 0.001f)
        assertTrue(requiredFingerTravelDp < 160f)
    }

    @Test
    fun `shouldResetToTopOnRefreshStart returns false when already at top`() {
        assertFalse(shouldResetToTopOnRefreshStart(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0))
    }

    @Test
    fun `shouldResetToTopOnRefreshStart returns true when list is scrolled`() {
        assertTrue(shouldResetToTopOnRefreshStart(firstVisibleItemIndex = 1, firstVisibleItemScrollOffset = 0))
        assertTrue(shouldResetToTopOnRefreshStart(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 12))
    }

    @Test
    fun `shouldResetToTopAfterIncrementalRefresh returns false for non-recommend category`() {
        assertFalse(
            shouldResetToTopAfterIncrementalRefresh(
                currentCategory = HomeCategory.POPULAR,
                newItemsCount = 3,
                isRefreshing = false,
                firstVisibleItemIndex = 2,
                firstVisibleItemScrollOffset = 0
            )
        )
    }

    @Test
    fun `shouldResetToTopAfterIncrementalRefresh returns false while refreshing`() {
        assertFalse(
            shouldResetToTopAfterIncrementalRefresh(
                currentCategory = HomeCategory.RECOMMEND,
                newItemsCount = 3,
                isRefreshing = true,
                firstVisibleItemIndex = 2,
                firstVisibleItemScrollOffset = 0
            )
        )
    }

    @Test
    fun `shouldResetToTopAfterIncrementalRefresh returns false when no new items`() {
        assertFalse(
            shouldResetToTopAfterIncrementalRefresh(
                currentCategory = HomeCategory.RECOMMEND,
                newItemsCount = 0,
                isRefreshing = false,
                firstVisibleItemIndex = 2,
                firstVisibleItemScrollOffset = 0
            )
        )
    }

    @Test
    fun `shouldResetToTopAfterIncrementalRefresh returns false when already at top`() {
        assertFalse(
            shouldResetToTopAfterIncrementalRefresh(
                currentCategory = HomeCategory.RECOMMEND,
                newItemsCount = 3,
                isRefreshing = false,
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 0
            )
        )
    }

    @Test
    fun `shouldResetToTopAfterIncrementalRefresh returns true when recommend has new items and list is scrolled`() {
        assertTrue(
            shouldResetToTopAfterIncrementalRefresh(
                currentCategory = HomeCategory.RECOMMEND,
                newItemsCount = 3,
                isRefreshing = false,
                firstVisibleItemIndex = 2,
                firstVisibleItemScrollOffset = 0
            )
        )
    }

    @Test
    fun `shouldResetFollowToTopAfterRefreshCompletion only triggers for unhandled follow event`() {
        assertTrue(
            shouldResetFollowToTopAfterRefreshCompletion(
                currentCategory = HomeCategory.FOLLOW,
                resetKey = 2L,
                handledKey = 1L
            )
        )
        assertFalse(
            shouldResetFollowToTopAfterRefreshCompletion(
                currentCategory = HomeCategory.FOLLOW,
                resetKey = 2L,
                handledKey = 2L
            )
        )
        assertFalse(
            shouldResetFollowToTopAfterRefreshCompletion(
                currentCategory = HomeCategory.POPULAR,
                resetKey = 2L,
                handledKey = 1L
            )
        )
    }

    @Test
    fun `resolvePullRefreshHintText shows pull text while indicator animates back`() {
        assertEquals(
            "下拉刷新...",
            resolvePullRefreshHintText(
                progress = 1.15f,
                isRefreshing = false,
                isStateAnimating = true
            )
        )
    }

    @Test
    fun `resolvePullRefreshHintText shows release text only when actively over threshold`() {
        assertEquals(
            "松手刷新",
            resolvePullRefreshHintText(
                progress = 1.15f,
                isRefreshing = false,
                isStateAnimating = false
            )
        )
    }

    @Test
    fun `resolvePullIndicatorTranslationY keeps minimum gap from cards`() {
        val translationY = resolvePullIndicatorTranslationY(
            dragOffsetPx = 40f,
            indicatorHeightPx = 40f,
            minGapPx = 8f,
            isRefreshing = false
        )
        assertEquals(-8f, translationY, 0.001f)
    }

    @Test
    fun `resolvePullIndicatorTranslationY pins indicator when refreshing`() {
        val translationY = resolvePullIndicatorTranslationY(
            dragOffsetPx = 0f,
            indicatorHeightPx = 40f,
            minGapPx = 8f,
            isRefreshing = true
        )
        assertEquals(0f, translationY, 0.001f)
    }

    @Test
    fun `resolvePullContentOffsetFraction clears extra gap once refreshing is active`() {
        assertEquals(
            0f,
            resolvePullContentOffsetFraction(
                distanceFraction = 0f,
                isRefreshing = true,
                motionStyle = HomePullRefreshMotionStyle.IOS
            ),
            0.001f
        )
    }

    @Test
    fun `resolvePullContentOffsetFraction returns zero when idle and no pull`() {
        assertEquals(
            0f,
            resolvePullContentOffsetFraction(
                distanceFraction = 0f,
                isRefreshing = false,
                motionStyle = HomePullRefreshMotionStyle.IOS
            ),
            0.001f
        )
    }

    @Test
    fun `resolvePullContentOffsetFraction keeps md3 content pinned during pull`() {
        assertEquals(
            0f,
            resolvePullContentOffsetFraction(
                distanceFraction = 1.2f,
                isRefreshing = false,
                motionStyle = HomePullRefreshMotionStyle.MD3
            ),
            0.001f
        )
    }
}
