package com.android.purebilibili.feature.video.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeedSelectionMenuOptionsTest {
    @Test
    fun unloadedConfigurationDoesNotBecomeCurrentSpeedOnlyMenu() {
        assertNull(resolveSpeedSelectionMenuOptions(null, 1f))
    }

    @Test
    fun loadedCustomSpeedsRemainSelectable() {
        val speeds = listOf(0.5f, 1f, 1.3f, 2f)
        assertEquals(speeds, resolveSpeedSelectionMenuOptions(speeds, 1f))
    }

    @Test
    fun temporaryCurrentSpeedIsIncludedWithoutRemovingConfiguredChoices() {
        assertEquals(
            listOf(0.5f, 1f, 1.75f, 2f),
            resolveSpeedSelectionMenuOptions(listOf(0.5f, 1f, 2f), 1.75f),
        )
    }

    @Test
    fun intentionalSingleSpeedConfigurationIsPreserved() {
        assertEquals(listOf(1f), resolveSpeedSelectionMenuOptions(listOf(1f), 1f))
    }
}
