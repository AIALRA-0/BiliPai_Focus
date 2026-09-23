package com.android.purebilibili.feature.dynamic.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicEmoteCatalogPolicyTest {

    private val signedIn = DynamicEmoteCatalogSessionKey(mid = 42L, authenticated = true)

    @Test
    fun `complete catalog is reused for the same account`() {
        assertFalse(
            shouldReloadDynamicEmoteCatalog(
                requestedSessionKey = signedIn,
                loadedSessionKey = signedIn,
                fullyLoaded = true,
                lastAttemptMs = 1_000L,
                nowMs = 100_000L,
            )
        )
    }

    @Test
    fun `partial catalog retries after cooldown`() {
        assertFalse(
            shouldReloadDynamicEmoteCatalog(
                requestedSessionKey = signedIn,
                loadedSessionKey = signedIn,
                fullyLoaded = false,
                lastAttemptMs = 10_000L,
                nowMs = 10_000L + DYNAMIC_EMOTE_RETRY_INTERVAL_MS - 1L,
            )
        )
        assertTrue(
            shouldReloadDynamicEmoteCatalog(
                requestedSessionKey = signedIn,
                loadedSessionKey = signedIn,
                fullyLoaded = false,
                lastAttemptMs = 10_000L,
                nowMs = 10_000L + DYNAMIC_EMOTE_RETRY_INTERVAL_MS,
            )
        )
    }

    @Test
    fun `account or authentication change reloads immediately`() {
        assertTrue(
            shouldReloadDynamicEmoteCatalog(
                requestedSessionKey = signedIn,
                loadedSessionKey = DynamicEmoteCatalogSessionKey(mid = 0L, authenticated = false),
                fullyLoaded = true,
                lastAttemptMs = 100_000L,
                nowMs = 100_001L,
            )
        )
    }
}
