package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Test

class CommonListAlignedRowsTest {
    @Test
    fun collectionsBreakRowsAndIncompleteRowsKeepTheirPlace() {
        val items = listOf(
            VideoItem(id = 1), VideoItem(id = 2), VideoItem(id = 3),
            VideoItem(id = 4, isCollectionResource = true),
            VideoItem(id = 5), VideoItem(id = 6),
        )
        val rows = resolveCommonListAlignedRows(items, items.map { it.id.toString() }, 2)
        assertEquals(listOf(0..1, 2..2, 3..3, 4..5), rows.map { it.indices })
        assertEquals(listOf(false, false, true, false), rows.map { it.fullWidth })
        assertEquals("4", rows[2].key)
    }

    @Test
    fun rowKeyRemainsStableWhenLastRowReceivesAnotherItem() {
        val one = listOf(VideoItem(id = 1))
        val two = one + VideoItem(id = 2)
        assertEquals(
            resolveCommonListAlignedRows(one, listOf("1"), 2).single().key,
            resolveCommonListAlignedRows(two, listOf("1", "2"), 2).single().key,
        )
    }
}
