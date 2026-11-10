package com.android.purebilibili.feature.search

import com.android.purebilibili.data.model.response.SearchType
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchTabLabelPolicyTest {

    @Test
    fun `all result tabs reserve the same badge width before and after loading`() {
        SearchType.entries.forEach { type ->
            val expected = resolveSearchTypeTabMeasurementLabel(type, null)
            listOf(-1, 0, 12, 99, 100, 99520).forEach { count ->
                assertEquals(expected, resolveSearchTypeTabMeasurementLabel(type, count))
            }
        }
        // Measurement does not fabricate a visible result count.
        assertEquals(SearchType.ALL.displayName, resolveSearchTypeTabLabel(SearchType.ALL.displayName, null))
    }

    @Test
    fun `label hides count until a type has loaded`() {
        assertEquals("视频", resolveSearchTypeTabLabel("视频", null))
        assertEquals("视频", resolveSearchTypeTabLabel("视频", -1))
    }

    @Test
    fun `label appends api count and caps at 99+`() {
        assertEquals("视频 12", resolveSearchTypeTabLabel("视频", 12))
        assertEquals("视频 99", resolveSearchTypeTabLabel("视频", 99))
        assertEquals("视频 99+", resolveSearchTypeTabLabel("视频", 100))
        assertEquals("视频 99+", resolveSearchTypeTabLabel("视频", 99520))
    }
}
