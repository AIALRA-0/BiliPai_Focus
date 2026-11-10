package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.FollowGroupOrderResponse
import com.android.purebilibili.data.model.response.FollowGroupOrderSaveResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class FollowGroupOrderRequestTest {
    @Test
    fun payloadPreservesTheCompleteCustomOrderAndRejectsReservedOrDuplicateIds() {
        assertEquals(mapOf("tagids" to "30,10,20", "csrf" to "sample"), followGroupOrderFields(listOf(30, 10, 20), "sample"))
        listOf(emptyList(), listOf(0L, 1L), listOf(-10L, 1L), listOf(-2L, 1L), listOf(1L, 1L)).forEach { ids ->
            assertFailsWith<IllegalArgumentException> { followGroupOrderFields(ids, "sample") }
        }
        assertFailsWith<IllegalArgumentException> { followGroupOrderFields(listOf(1), "") }
    }

    @Test
    fun missingDataOrStatusCannotBeMistakenForSuccess() {
        assertNull(Json.decodeFromString<FollowGroupOrderResponse>("""{"code":0}""").data)
        assertEquals(emptyList(), Json.decodeFromString<FollowGroupOrderResponse>("""{"code":0,"data":[]}""").data)
        assertEquals(-1, Json.decodeFromString<FollowGroupOrderSaveResponse>("{}").code)
    }
}
