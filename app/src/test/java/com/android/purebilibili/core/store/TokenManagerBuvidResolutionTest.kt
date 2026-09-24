package com.android.purebilibili.core.store

import kotlin.test.Test
import kotlin.test.assertEquals

class TokenManagerBuvidResolutionTest {

    @Test
    fun requestFallbackIsStableAcrossInitialEmptyDataStoreEmission() {
        val previousBuvid = TokenManager.buvid3Cache
        try {
            TokenManager.buvid3Cache = null
            var generatedCount = 0

            val requestBuvid = TokenManager.getOrCreateBuvid3 {
                generatedCount++
                "request-fallback"
            }
            val dataStoreBuvid = TokenManager.resolveBuvid3AfterDataStoreLoad(
                dataStoreBuvid = null,
                generateFallback = {
                    generatedCount++
                    "data-store-fallback"
                },
            )
            val repeatedRequestBuvid = TokenManager.getOrCreateBuvid3 {
                generatedCount++
                "unexpected-regeneration"
            }

            assertEquals("request-fallback", requestBuvid)
            assertEquals("request-fallback", dataStoreBuvid)
            assertEquals("request-fallback", repeatedRequestBuvid)
            assertEquals("request-fallback", TokenManager.buvid3Cache)
            assertEquals(1, generatedCount)
        } finally {
            TokenManager.buvid3Cache = previousBuvid
        }
    }

    @Test
    fun persistedDataStoreBuvidRemainsAuthoritative() {
        val previousBuvid = TokenManager.buvid3Cache
        try {
            TokenManager.buvid3Cache = "shared-preferences-buvid"

            val resolved = TokenManager.resolveBuvid3AfterDataStoreLoad(
                dataStoreBuvid = "data-store-buvid",
                generateFallback = { error("A stored Buvid should not need a fallback") },
            )

            assertEquals("data-store-buvid", resolved)
            assertEquals("data-store-buvid", TokenManager.buvid3Cache)
        } finally {
            TokenManager.buvid3Cache = previousBuvid
        }
    }
}
