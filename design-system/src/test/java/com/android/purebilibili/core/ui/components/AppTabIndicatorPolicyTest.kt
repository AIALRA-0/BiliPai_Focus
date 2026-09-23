package com.android.purebilibili.core.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class AppTabIndicatorPolicyTest {

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
