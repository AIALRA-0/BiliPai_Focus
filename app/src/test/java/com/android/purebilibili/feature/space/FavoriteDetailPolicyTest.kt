package com.android.purebilibili.feature.space

import kotlin.test.Test
import kotlin.test.assertEquals

class FavoriteDetailPolicyTest {
    @Test
    fun `oldest first starts at final server page including partial pages`() {
        val cases = mapOf(0 to 1, 1 to 1, 20 to 1, 21 to 2, 40 to 2, 41 to 3)
        cases.forEach { (count, expectedPage) ->
            assertEquals(expectedPage, favoriteLastServerPage(count), "resource count $count")
        }
    }

    @Test
    fun `final server page arithmetic does not overflow`() {
        assertEquals(107374183, favoriteLastServerPage(Int.MAX_VALUE))
    }
}
