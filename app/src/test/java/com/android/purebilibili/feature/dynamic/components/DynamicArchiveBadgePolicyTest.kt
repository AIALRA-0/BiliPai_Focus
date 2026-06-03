package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.data.model.response.ArchiveMajor
import com.android.purebilibili.data.model.response.DynamicMajorBadge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DynamicArchiveBadgePolicyTest {

    @Test
    fun archiveBadgePrefersDesktopBadgeText() {
        assertEquals(
            "充电专属",
            resolveDynamicArchiveBadgeLabel(
                ArchiveMajor(
                    badge = DynamicMajorBadge(text = "充电专属"),
                    isChargingArc = true
                )
            )
        )
    }

    @Test
    fun archiveChargeFieldsFallbackToChargeBadge() {
        assertEquals(
            "充电专属",
            resolveDynamicArchiveBadgeLabel(
                ArchiveMajor(elecArcType = 1)
            )
        )
    }

    @Test
    fun regularArchiveDoesNotShowBadge() {
        assertNull(resolveDynamicArchiveBadgeLabel(ArchiveMajor()))
    }
}
