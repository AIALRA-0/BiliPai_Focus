package com.android.purebilibili.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals

class CollectionSubscriptionPolicyTest {

    @Test
    fun `subscription request matches PiliPlus favorite season endpoint and form fields`() {
        val request = buildCollectionSubscriptionRequest(
            seasonId = 725909L,
            subscribe = true,
            csrf = "csrf-token"
        )

        assertEquals("x/v3/fav/season/fav", request.path)
        assertEquals("web", request.platform)
        assertEquals(725909L, request.seasonId)
        assertEquals("csrf-token", request.csrf)
    }

    @Test
    fun `unsubscription request matches PiliPlus unfavorite season endpoint`() {
        val request = buildCollectionSubscriptionRequest(
            seasonId = 725909L,
            subscribe = false,
            csrf = "csrf-token"
        )

        assertEquals("x/v3/fav/season/unfav", request.path)
    }
}
