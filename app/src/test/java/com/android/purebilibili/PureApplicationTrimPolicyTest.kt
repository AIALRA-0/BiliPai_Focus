package com.android.purebilibili

import android.content.ComponentCallbacks2
import com.android.purebilibili.app.PureApplicationRuntimeConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PureApplicationTrimPolicyTest {

    @Test
    fun `ui hidden should trim image memory cache for lower background footprint`() {
        assertEquals(
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            PureApplicationRuntimeConfig.resolveImageMemoryCacheTrimLevel(
                ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
            )
        )
    }

    @Test
    fun `low memory levels should forward trim pressure to image cache`() {
        assertEquals(
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            PureApplicationRuntimeConfig.resolveImageMemoryCacheTrimLevel(
                ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
            )
        )
        assertEquals(
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            PureApplicationRuntimeConfig.resolveImageMemoryCacheTrimLevel(
                ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
            )
        )
        assertEquals(
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE,
            PureApplicationRuntimeConfig.resolveImageMemoryCacheTrimLevel(
                ComponentCallbacks2.TRIM_MEMORY_COMPLETE
            )
        )
    }

    @Test
    fun `background pressure should clear image memory cache`() {
        assertEquals(
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            PureApplicationRuntimeConfig.resolveImageMemoryCacheTrimLevel(
                ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
            )
        )
        assertEquals(
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            PureApplicationRuntimeConfig.resolveImageMemoryCacheTrimLevel(
                ComponentCallbacks2.TRIM_MEMORY_MODERATE
            )
        )
        assertTrue(
            PureApplicationRuntimeConfig.shouldClearImageMemoryCacheOnTrimLevel(
                ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
            )
        )
        assertTrue(
            PureApplicationRuntimeConfig.shouldClearImageMemoryCacheOnTrimLevel(
                ComponentCallbacks2.TRIM_MEMORY_MODERATE
            )
        )
        assertFalse(
            PureApplicationRuntimeConfig.shouldClearImageMemoryCacheOnTrimLevel(
                ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
            )
        )
    }

    @Test
    fun `non pressure trim levels should not touch image cache`() {
        assertEquals(
            null,
            PureApplicationRuntimeConfig.resolveImageMemoryCacheTrimLevel(
                ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE
            )
        )
    }

    @Test
    fun `image memory cache percent uses lower background friendly budget`() {
        assertEquals(0.10, PureApplicationRuntimeConfig.resolveImageMemoryCachePercent(), 0.0001)
    }
}
