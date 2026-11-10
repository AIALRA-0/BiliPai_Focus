package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.FanRelationshipResponse
import com.android.purebilibili.data.model.response.FanRemovalResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class FanManagementPolicyTest {
    @Test
    fun removalUsesDedicatedActionAndRejectsSelfOrInvalidAccount() {
        assertEquals(mapOf("fid" to "42", "act" to "7", "re_src" to "11", "csrf" to "sample"), fanRemovalFields(1L, 42L, "sample"))
        assertFailsWith<IllegalArgumentException> { fanRemovalFields(0L, 42L, "sample") }
        assertFailsWith<IllegalArgumentException> { fanRemovalFields(42L, 42L, "sample") }
        assertFailsWith<IllegalArgumentException> { fanRemovalFields(1L, 0L, "sample") }
        assertFailsWith<IllegalArgumentException> { fanRemovalFields(1L, 42L, "") }
    }

    @Test
    fun reverseRelationshipDeterminesWhetherTheOtherUserIsAFan() {
        val json = Json { ignoreUnknownKeys = true }
        val outgoingOnly = json.decodeFromString<FanRelationshipResponse>("""{"code":0,"data":{"relation":{"attribute":2},"be_relation":{"attribute":0}}}""")
        assertEquals(false, outgoingOnly.data?.reverseRelation?.isFollowing)
        val incomingOnly = json.decodeFromString<FanRelationshipResponse>("""{"code":0,"data":{"relation":{"attribute":0},"be_relation":{"attribute":2}}}""")
        assertEquals(true, incomingOnly.data?.reverseRelation?.isFollowing)
        assertNull(json.decodeFromString<FanRelationshipResponse>("""{"code":0,"data":{}}""").data?.reverseRelation)
        assertEquals(-1, json.decodeFromString<FanRemovalResponse>("{}").code)
    }
}
