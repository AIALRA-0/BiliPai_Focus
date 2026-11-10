package com.android.purebilibili.core.ui.components

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class AppTabIndicatorPolicyTest {

    @Test
    fun `equal width tabs use glyph bounds for the settled underline`() {
        val slots = resolveMeasuredTabContentSlots(
            listOf(AppTabSlot(0.dp, 160.dp, 128.dp), AppTabSlot(160.dp, 160.dp, 128.dp)),
            listOf(32.dp, 48.dp),
        )
        assertEquals(
            ElasticTabIndicatorBounds(64f, 32f),
            resolveElasticTabIndicatorBounds(
                position = 0f,
                tabLeftsDp = slots.map { it.left.value },
                tabWidthsDp = slots.map { it.width.value },
                contentWidthsDp = slots.map { it.contentWidth.value },
                matchContentSize = true,
            ),
        )
        assertEquals(160.dp, slots[0].width)
    }

    @Test
    fun `label underline keeps native minimum and stays within its tab`() {
        val slots = listOf(AppTabSlot(0.dp, 80.dp, 48.dp), AppTabSlot(80.dp, 80.dp, 48.dp))
        val measured = resolveMeasuredTabContentSlots(slots, listOf(10.dp, 200.dp))
        assertEquals(24.dp, measured[0].contentWidth)
        assertEquals(80.dp, measured[1].contentWidth)
        assertEquals(slots, resolveMeasuredTabContentSlots(slots, null))
    }

    @Test
    fun `elastic underline sits on the current label when settled`() {
        assertEquals(
            ElasticTabIndicatorBounds(leftDp = 30f, widthDp = 40f),
            resolveElasticTabIndicatorBounds(
                position = 0f,
                tabLeftsDp = listOf(0f, 100f),
                tabWidthsDp = listOf(100f, 100f),
                contentWidthsDp = listOf(40f, 80f),
                matchContentSize = true,
            ),
        )
        assertEquals(
            ElasticTabIndicatorBounds(leftDp = 110f, widthDp = 80f),
            resolveElasticTabIndicatorBounds(
                position = 1f,
                tabLeftsDp = listOf(0f, 100f),
                tabWidthsDp = listOf(100f, 100f),
                contentWidthsDp = listOf(40f, 80f),
                matchContentSize = true,
            ),
        )
    }

    @Test
    fun `elastic underline stretches between labels during page motion`() {
        val midpoint = resolveElasticTabIndicatorBounds(
            position = 0.5f,
            tabLeftsDp = listOf(0f, 100f),
            tabWidthsDp = listOf(100f, 100f),
            contentWidthsDp = listOf(40f, 80f),
            matchContentSize = true,
        )
        assertEquals(53.431f, midpoint.leftDp, 0.001f)
        assertEquals(101.421f, midpoint.widthDp, 0.001f)
    }

    @Test
    fun `full-width underline uses the tab slot instead of the label`() {
        assertEquals(
            ElasticTabIndicatorBounds(leftDp = 0f, widthDp = 100f),
            resolveElasticTabIndicatorBounds(
                position = 0f,
                tabLeftsDp = listOf(0f, 100f),
                tabWidthsDp = listOf(100f, 120f),
                contentWidthsDp = listOf(40f, 80f),
                matchContentSize = false,
            ),
        )
    }
}
