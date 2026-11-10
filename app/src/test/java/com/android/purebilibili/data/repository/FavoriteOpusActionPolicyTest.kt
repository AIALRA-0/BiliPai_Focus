package com.android.purebilibili.data.repository

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FavoriteOpusActionPolicyTest {
    @Test
    fun `unfavorite preserves opus identity and uses community action`() {
        val body = buildFavoriteOpusRemovalRequest(1201902028962398230)
        val entity = body.getValue("entity").jsonObject
        assertEquals("1201902028962398230", entity.getValue("object_id_str").jsonPrimitive.content)
        assertEquals("2", entity.getValue("type").jsonObject.getValue("biz").jsonPrimitive.content)
        assertEquals("4", body.getValue("action").jsonPrimitive.content)
        assertFailsWith<IllegalArgumentException> { buildFavoriteOpusRemovalRequest(0) }
    }
}
