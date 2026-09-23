package com.android.purebilibili.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals

class WatchLaterRepositoryPolicyTest {
    @Test
    fun wbiPageParams_preserveFilterSearchAndOrder() {
        val params = buildWatchLaterPageParams(
            page = 3,
            viewed = 2,
            keyword = "  Kotlin  ",
            ascending = true,
        )

        assertEquals("3", params["pn"])
        assertEquals("20", params["ps"])
        assertEquals("2", params["viewed"])
        assertEquals("Kotlin", params["key"])
        assertEquals("true", params["asc"])
        assertEquals("true", params["need_split"])
    }
}
