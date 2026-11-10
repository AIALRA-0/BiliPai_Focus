package com.android.purebilibili.feature.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchCardTargetRestoreTest {
    @Test
    fun `all result kinds preserve their return card and detail identity`() {
        SearchCardKind.entries.forEach { kind ->
            val target = SearchCardTarget(kind, 123L, "card-instance", "专栏标题", "1234567890123456789")
            assertEquals(target, restoreSearchCardTarget(saveSearchCardTarget(target)))
        }
    }

    @Test
    fun `old UP and bangumi saved containers still restore`() {
        assertEquals(
            SearchCardTarget(SearchCardKind.UP, 123L, "up-card"),
            restoreSearchCardTarget(listOf(true, 123L, "up-card")),
        )
        assertEquals(
            SearchCardTarget(SearchCardKind.BANGUMI, 456L, "bangumi-card"),
            restoreSearchCardTarget(listOf(false, 456L, "bangumi-card")),
        )
    }

    @Test
    fun `empty or unknown saved targets are ignored`() {
        assertNull(restoreSearchCardTarget(emptyList()))
        assertNull(restoreSearchCardTarget(listOf("unknown", 1L, "card")))
        assertNull(restoreSearchCardTarget(listOf("ARTICLE", "invalid-id", "card")))
    }
}
