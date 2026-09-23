package com.android.purebilibili.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.math.abs

class HomeHeroCarouselPolicyTest {

    @Test
    fun `carousel safely ignores stale pager index after feed shrinks`() {
        val items = listOf("a", "b", "c", "d", "e", "f")

        assertEquals(null, resolveHomeHeroCarouselItemOrNull(items, page = 6))
        assertEquals(
            "hero_6",
            resolveHomeHeroCarouselItemKey(items, page = 6) { it }
        )
    }

    @Test
    fun `carousel key uses item identity when pager index is valid`() {
        assertEquals(
            "BV1stable",
            resolveHomeHeroCarouselItemKey(listOf("BV1stable"), page = 0) { it }
        )
    }

    @Test
    fun `visible hero carousel keeps the reserved top gap without affecting other feeds`() {
        assertEquals(
            169f,
            resolveHomeFeedTopPaddingDp(
                reservedTopPaddingDp = 169f,
                showHeroCarousel = true
            ),
            0.001f
        )
        assertEquals(
            169f,
            resolveHomeFeedTopPaddingDp(
                reservedTopPaddingDp = 169f,
                showHeroCarousel = false
            ),
            0.001f
        )
    }

    @Test
    fun `carousel only shows on recommend page with items when enabled`() {
        assertTrue(
            shouldShowHomeHeroCarousel(
                enabled = true,
                category = HomeCategory.RECOMMEND,
                itemCount = 1
            )
        )
        assertFalse(
            shouldShowHomeHeroCarousel(
                enabled = false,
                category = HomeCategory.RECOMMEND,
                itemCount = 1
            )
        )
        assertFalse(
            shouldShowHomeHeroCarousel(
                enabled = true,
                category = HomeCategory.POPULAR,
                itemCount = 1
            )
        )
        assertFalse(
            shouldShowHomeHeroCarousel(
                enabled = true,
                category = HomeCategory.RECOMMEND,
                itemCount = 0
            )
        )
    }

    @Test
    fun `carousel uses bounded leading feed items`() {
        assertEquals(
            listOf(1, 2, 3),
            selectHomeHeroCarouselItems(listOf(1, 2, 3), maxItems = 8)
        )
        assertEquals(
            (1..8).toList(),
            selectHomeHeroCarouselItems((1..20).toList(), maxItems = 8)
        )
        assertEquals(
            emptyList(),
            selectHomeHeroCarouselItems((1..20).toList(), maxItems = 0)
        )
    }

    @Test
    fun `carousel feed removes visible hero items from regular grid`() {
        val items = listOf("a", "b", "c", "d")
        val carouselItems = listOf("a", "b")

        assertEquals(
            listOf("c", "d"),
            excludeHomeHeroCarouselItems(items, carouselItems) { it }
        )
    }

    @Test
    fun `carousel feed keeps regular grid untouched when carousel is empty`() {
        val items = listOf("a", "b", "c")

        assertEquals(
            items,
            excludeHomeHeroCarouselItems(items, emptyList()) { it }
        )
    }

    @Test
    fun `carousel fills the feed width without side peek gaps`() {
        assertEquals(0f, HOME_HERO_CAROUSEL_SIDE_PEEK_DP)
        assertEquals(0f, HOME_HERO_CAROUSEL_PAGE_SPACING_DP)
    }

    @Test
    fun `home pager yields to the hero carousel while a pointer is active on it`() {
        assertTrue(shouldYieldHomeTopPagerToHeroCarousel(heroCarouselPointerActive = true))
        assertFalse(shouldYieldHomeTopPagerToHeroCarousel(heroCarouselPointerActive = false))
    }

    @Test
    fun `carousel uses adaptive aspect ratio for phone and tablet`() {
        assertEquals(16f / 9f, resolveHomeHeroCarouselAspectRatio(containerWidthDp = 393f), 0.001f)
        assertEquals(2.0f, resolveHomeHeroCarouselAspectRatio(containerWidthDp = 700f), 0.001f)
        assertEquals(21f / 9f, resolveHomeHeroCarouselAspectRatio(containerWidthDp = 900f), 0.001f)
    }

    @Test
    fun `carousel caps its adaptive content width on large screens`() {
        assertEquals(393f, resolveHomeHeroCarouselWidthDp(containerWidthDp = 393f), 0.001f)
        assertEquals(700f, resolveHomeHeroCarouselWidthDp(containerWidthDp = 700f), 0.001f)
        assertEquals(760f, resolveHomeHeroCarouselWidthDp(containerWidthDp = 820f), 0.001f)
        assertEquals(900f, resolveHomeHeroCarouselWidthDp(containerWidthDp = 900f), 0.001f)
        assertEquals(980f, resolveHomeHeroCarouselWidthDp(containerWidthDp = 1200f), 0.001f)
        assertEquals(980f, resolveHomeHeroCarouselWidthDp(containerWidthDp = 1600f), 0.001f)
    }

    @Test
    fun `landscape tablet shrinks the hero instead of filling half the window`() {
        val layout = resolveHomeHeroCarouselLayout(
            containerWidthDp = 1280f,
            windowWidthDp = 1280f,
            windowHeightDp = 800f,
        )
        assertTrue(layout.heightDp <= 248f)
        assertTrue(layout.widthDp < 980f)
        assertEquals(21f / 9f, layout.aspectRatio, 0.001f)
        assertEquals(layout.widthDp / layout.aspectRatio, layout.heightDp, 0.2f)
    }

    @Test
    fun `portrait phone hero keeps 16 by 9 full width`() {
        val layout = resolveHomeHeroCarouselLayout(
            containerWidthDp = 393f,
            windowWidthDp = 393f,
            windowHeightDp = 851f,
        )
        assertEquals(393f, layout.widthDp, 0.001f)
        assertEquals(16f / 9f, layout.aspectRatio, 0.001f)
        assertEquals(393f / (16f / 9f), layout.heightDp, 0.2f)
    }

    @Test
    fun `compact height windows cap the hero to a short strip`() {
        val layout = resolveHomeHeroCarouselLayout(
            containerWidthDp = 851f,
            windowWidthDp = 851f,
            windowHeightDp = 393f,
        )
        assertTrue(layout.heightDp <= 188f)
        assertTrue(layout.heightDp >= 132f)
    }

    @Test
    fun `carousel transform avoids 3d fold artifacts while swiping`() {
        val centered = resolveHomeHeroCarouselCardTransform(0f)
        assertTrue(abs(centered.rotationY) < 0.001f)
        assertTrue(abs(centered.scale - 1f) < 0.001f)
        assertTrue(abs(centered.alpha - 1f) < 0.001f)
        assertTrue(abs(centered.translationXFraction) < 0.001f)
        assertTrue(abs(centered.pivotFractionX - 0.5f) < 0.001f)
        assertTrue(abs(centered.contentParallaxFraction) < 0.001f)
        assertTrue(abs(centered.contentScale - 1f) < 0.001f)
        assertTrue(abs(centered.edgeShadeAlpha) < 0.001f)
        assertTrue(abs(centered.shadowElevationFraction - 0.35f) < 0.001f)
        assertTrue(abs(centered.rotationZ) < 0.001f)
        assertTrue(centered.zIndex >= 1f)

        val left = resolveHomeHeroCarouselCardTransform(-1f)
        val right = resolveHomeHeroCarouselCardTransform(1f)
        assertTrue(abs(left.rotationY) < 0.001f)
        assertTrue(abs(right.rotationY) < 0.001f)
        assertTrue(abs(left.translationXFraction) < 0.001f)
        assertTrue(abs(right.translationXFraction) < 0.001f)
        assertTrue(abs(left.rotationZ) < 0.001f)
        assertTrue(abs(right.rotationZ) < 0.001f)
        assertEquals(0.5f, left.pivotFractionX)
        assertEquals(0.5f, right.pivotFractionX)
        assertEquals(left.scale, right.scale)
        assertEquals(left.alpha, right.alpha)
        assertTrue(left.scale < centered.scale)
        assertTrue(left.alpha < centered.alpha)
        assertTrue(left.contentParallaxFraction > 0.05f)
        assertTrue(right.contentParallaxFraction < -0.05f)
        assertEquals(left.contentParallaxFraction, -right.contentParallaxFraction)
        assertTrue(left.contentScale > 1.1f)
        assertEquals(left.contentScale, right.contentScale)
        assertTrue(left.edgeShadeAlpha > 0.2f)
        assertTrue(right.edgeShadeAlpha > 0.2f)
        assertTrue(left.edgeShadeStartFromLeft)
        assertFalse(right.edgeShadeStartFromLeft)
        assertTrue(left.shadowElevationFraction < centered.shadowElevationFraction)

        val draggingLeft = resolveHomeHeroCarouselCardTransform(-0.5f)
        val draggingRight = resolveHomeHeroCarouselCardTransform(0.5f)
        assertTrue(abs(draggingLeft.translationXFraction) < 0.001f)
        assertTrue(abs(draggingRight.translationXFraction) < 0.001f)
        assertTrue(abs(draggingLeft.rotationZ) < 0.001f)
        assertTrue(abs(draggingRight.rotationZ) < 0.001f)
        assertTrue(abs(draggingLeft.rotationY) < 0.001f)
        assertTrue(abs(draggingRight.rotationY) < 0.001f)
        assertTrue(draggingLeft.scale in 0.95f..1f)
        assertTrue(draggingRight.scale in 0.95f..1f)
        assertTrue(draggingLeft.alpha in 0.9f..1f)
        assertTrue(draggingRight.alpha in 0.9f..1f)
        assertTrue(draggingLeft.contentParallaxFraction > 0.02f)
        assertTrue(draggingRight.contentParallaxFraction < -0.02f)
        assertTrue(draggingLeft.contentScale > 1.05f)
        assertTrue(draggingRight.contentScale > 1.05f)
        assertTrue(draggingLeft.edgeShadeAlpha > 0.1f)
        assertTrue(draggingRight.edgeShadeAlpha > 0.1f)

        val pressed = resolveHomeHeroCarouselCardTransform(0f, pressedProgress = 1f)
        assertTrue(pressed.scale < centered.scale)
        assertTrue(pressed.shadowElevationFraction < centered.shadowElevationFraction)
    }

    @Test
    fun `carousel preview stays hidden until first frame is rendered`() {
        assertEquals(0f, resolveHomeHeroCarouselPreviewAlpha(hasRenderedFirstFrame = false))
        assertEquals(1f, resolveHomeHeroCarouselPreviewAlpha(hasRenderedFirstFrame = true))
    }
}
