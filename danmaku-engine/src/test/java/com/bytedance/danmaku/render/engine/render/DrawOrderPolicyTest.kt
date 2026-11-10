package com.bytedance.danmaku.render.engine.render

import kotlin.test.Test
import kotlin.test.assertEquals

class DrawOrderPolicyTest {
    private data class Item(val order: Int?, val layer: Int, var time: Long, val id: String)
    private val comparator = compareBy<Item>({ it.order }, { it.layer }, { it.time })

    @Test
    fun orderedFrame_preservesEqualKeyOrder() {
        val items = mutableListOf(Item(null, 0, 0, "null"), Item(0, 1, 1, "a"), Item(0, 1, 1, "b"))
        val expected = items.toList()
        sortOnlyIfOutOfOrder(items, comparator)
        assertEquals(expected, items)
    }

    @Test
    fun reversedFrame_matchesExistingStableSortAcrossAllKeys() {
        val items = mutableListOf(
            Item(2, 0, 0, "order"), Item(1, 2, 0, "layer"), Item(1, 1, 2, "late"),
            Item(1, 1, 1, "equal-a"), Item(1, 1, 1, "equal-b"), Item(null, 0, 0, "null")
        )
        val expected = items.sortedWith(comparator)
        sortOnlyIfOutOfOrder(items, comparator)
        assertEquals(expected, items)
    }

    @Test
    fun changedKey_isRecheckedOnNextFrame() {
        val items = mutableListOf(Item(0, 1, 1, "a"), Item(0, 1, 2, "b"))
        sortOnlyIfOutOfOrder(items, comparator)
        items[0].time = 3
        sortOnlyIfOutOfOrder(items, comparator)
        assertEquals(listOf("b", "a"), items.map { it.id })
    }

    @Test
    fun emptyAndSingleItemFrames_areAccepted() {
        val empty = mutableListOf<Item>()
        sortOnlyIfOutOfOrder(empty, comparator)
        assertEquals(emptyList(), empty)
        val single = mutableListOf(Item(0, 0, 0, "only"))
        sortOnlyIfOutOfOrder(single, comparator)
        assertEquals("only", single.single().id)
    }
}
