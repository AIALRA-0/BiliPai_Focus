package com.android.purebilibili.core.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FeedPrependScrollPolicyTest {
    @Test
    fun `refresh at top anchors the old card rather than the chrome spacer`() {
        val target = resolveFeedPrependScrollTarget(
            itemKeys = listOf("new", "old_a", "old_b"),
            dividerIndex = 1,
            leadingItemCount = 1,
            visibleItems = listOf(
                FeedVisibleAnchor("feed_chrome_top_inset", 0, 0, 200),
                FeedVisibleAnchor("old_a", 1, 84, 200),
                FeedVisibleAnchor("old_b", 2, 84, 200),
            ),
        )
        assertEquals(FeedPrependScrollTarget(3, -84), target)
    }

    @Test
    fun `repeat refresh skips the old divider and keeps the visible card offset`() {
        val target = resolveFeedPrependScrollTarget(
            itemKeys = listOf("newer", "new", "old_a", "old_b"),
            dividerIndex = 1,
            leadingItemCount = 1,
            visibleItems = listOf(
                FeedVisibleAnchor("old_content_divider", 2, -10, 200),
                FeedVisibleAnchor("old_a", 3, 38, 200),
            ),
        )
        assertEquals(FeedPrependScrollTarget(4, -38), target)
    }

    @Test
    fun `cards covered by the overlay header are not used as reading anchors`() {
        val target = resolveFeedPrependScrollTarget(
            itemKeys = listOf("newer", "new", "old_a"),
            dividerIndex = 1,
            leadingItemCount = 1,
            visibleItems = listOf(
                FeedVisibleAnchor("new", 1, -86, 110),
                FeedVisibleAnchor("old_content_divider", 2, 36, 36),
                FeedVisibleAnchor("old_a", 3, 84, 200),
            ),
            viewportStartOffset = 84,
        )
        assertEquals(FeedPrependScrollTarget(4, -84), target)
    }

    @Test
    fun `scrolled card preserves its clipped offset without a chrome item`() {
        val target = resolveFeedPrependScrollTarget(
            itemKeys = listOf("new", "old_a", "old_b", "old_c"),
            dividerIndex = 1,
            leadingItemCount = 0,
            visibleItems = listOf(FeedVisibleAnchor("old_b", 1, -35, 200)),
        )
        assertEquals(FeedPrependScrollTarget(3, 35), target)
    }

    @Test
    fun `unchanged data pagination and already restored layouts do not scroll`() {
        for (keys in listOf(listOf("new", "old"), listOf("new", "old", "next_page"))) {
            assertNull(resolveFeedPrependScrollTarget(
                itemKeys = keys,
                dividerIndex = 1,
                leadingItemCount = 1,
                visibleItems = listOf(FeedVisibleAnchor("old", 3, 84, 200)),
            ))
        }
    }

    @Test
    fun `initial loads replacements and feeds without an incremental divider do not scroll`() {
        for (divider in listOf(-1, 0, 2)) {
            assertNull(resolveFeedPrependScrollTarget(
                itemKeys = listOf("new", "old"),
                dividerIndex = divider,
                leadingItemCount = 1,
                visibleItems = listOf(FeedVisibleAnchor("old", 1, 84, 200)),
            ))
        }
        assertNull(resolveFeedPrependScrollTarget(
            itemKeys = listOf("new", "old"),
            dividerIndex = 1,
            leadingItemCount = 1,
            visibleItems = listOf(FeedVisibleAnchor("removed", 1, 84, 200)),
        ))
        assertNull(resolveFeedPrependScrollTarget(
            itemKeys = listOf("new", "old"),
            dividerIndex = 1,
            leadingItemCount = 1,
            visibleItems = emptyList(),
        ))
    }
}
