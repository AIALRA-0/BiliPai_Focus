package com.android.purebilibili.core.store

import com.android.purebilibili.feature.video.ui.components.CollectionSortMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CollectionPreferencePolicyTest {

    @Test
    fun `decode collection sort payload falls back to empty map on invalid json`() {
        val result = decodeCollectionSortPreferences("not-json")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `encode and decode collection sort payload round trips`() {
        val payload = encodeCollectionSortPreferences(
            mapOf(
                1001L to CollectionSortMode.DESCENDING,
                1002L to CollectionSortMode.RECENT
            )
        )

        val decoded = decodeCollectionSortPreferences(payload)

        assertEquals(
            mapOf(
                1001L to CollectionSortMode.DESCENDING,
                1002L to CollectionSortMode.RECENT
            ),
            decoded
        )
    }

    @Test
    fun `toggle collection subscription adds missing id`() {
        val result = toggleCollectionSubscription(emptySet(), 42L)

        assertEquals(setOf("42"), result)
    }

    @Test
    fun `toggle collection subscription removes existing id`() {
        val result = toggleCollectionSubscription(setOf("42", "99"), 42L)

        assertEquals(setOf("99"), result)
    }

    @Test
    fun `set collection subscription mirrors remote subscribed state`() {
        assertEquals(setOf("42"), setCollectionSubscription(emptySet(), 42L, true))
        assertEquals(setOf("99"), setCollectionSubscription(setOf("42", "99"), 42L, false))
    }
}
