package com.android.purebilibili.feature.video.controller

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QualityManagerDeviceCapabilityTest {

    private val qualityManager = QualityManager()

    @Test
    fun `checkQualityPermission returns unsupported when dolby vision not supported by device`() {
        val result = qualityManager.checkQualityPermission(
            qualityId = 126,
            isLoggedIn = true,
            isVip = true,
            isHdrSupported = true,
            isDolbyVisionSupported = false
        )

        assertEquals(
            QualityPermissionResult.UnsupportedByDevice("杜比视界"),
            result
        )
    }

    @Test
    fun `checkQualityPermission returns unsupported when hdr not supported by device`() {
        val result = qualityManager.checkQualityPermission(
            qualityId = 125,
            isLoggedIn = true,
            isVip = true,
            isHdrSupported = false,
            isDolbyVisionSupported = true
        )

        assertEquals(
            QualityPermissionResult.UnsupportedByDevice("HDR 真彩"),
            result
        )
    }

    @Test
    fun `getQualityLabel hides api qn numbers`() {
        assertEquals("1080P+", qualityManager.getQualityLabel(112))
        assertEquals("1080P60", qualityManager.getQualityLabel(116))
        assertEquals("4K", qualityManager.getQualityLabel(120))
        assertEquals("智能修复", qualityManager.getQualityLabel(100))
        assertEquals("HDR Vivid", qualityManager.getQualityLabel(129))
    }

    @Test
    fun `checkQualityPermission keeps vip requirement priority`() {
        val result = qualityManager.checkQualityPermission(
            qualityId = 126,
            isLoggedIn = true,
            isVip = false,
            isHdrSupported = true,
            isDolbyVisionSupported = false
        )

        assertTrue(result is QualityPermissionResult.RequiresVip)
    }

    @Test
    fun `exact playable server track overrides stale local vip state`() {
        val result = qualityManager.checkQualityPermission(
            qualityId = 120,
            isLoggedIn = true,
            isVip = false,
            serverPlayableQualities = listOf(120, 80)
        )

        assertEquals(QualityPermissionResult.Permitted, result)
    }

    @Test
    fun `checkQualityPermission allows dolby when server advertises target quality`() {
        val result = qualityManager.checkQualityPermission(
            qualityId = 126,
            isLoggedIn = true,
            isVip = true,
            isHdrSupported = true,
            isDolbyVisionSupported = false,
            serverAdvertisedQualities = listOf(126, 120, 80)
        )

        assertEquals(QualityPermissionResult.Permitted, result)
    }

    @Test
    fun `getMaxAvailableQuality skips unsupported hdr and dolby tiers`() {
        val result = qualityManager.getMaxAvailableQuality(
            availableQualities = listOf(126, 125, 120, 80),
            isLoggedIn = true,
            isVip = true,
            isHdrSupported = false,
            isDolbyVisionSupported = false
        )

        assertEquals(120, result)
    }
}
