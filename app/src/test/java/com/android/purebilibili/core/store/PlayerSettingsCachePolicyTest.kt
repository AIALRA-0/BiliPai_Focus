package com.android.purebilibili.core.store

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSettingsCachePolicyTest {

    @Test
    fun `resolveMigratedHwDecodeValue prefers current cache when present`() {
        val result = resolveMigratedHwDecodeValue(
            currentExists = true,
            currentValue = false,
            legacyExists = true,
            legacyValue = true
        )

        assertFalse(result)
    }

    @Test
    fun `resolveMigratedHwDecodeValue uses legacy cache when current cache is absent`() {
        val result = resolveMigratedHwDecodeValue(
            currentExists = false,
            currentValue = true,
            legacyExists = true,
            legacyValue = false
        )

        assertFalse(result)
    }

    @Test
    fun `resolveMigratedHwDecodeValue defaults enabled when no cache exists`() {
        val result = resolveMigratedHwDecodeValue(
            currentExists = false,
            currentValue = false,
            legacyExists = false,
            legacyValue = false
        )

        assertTrue(result)
    }
}
