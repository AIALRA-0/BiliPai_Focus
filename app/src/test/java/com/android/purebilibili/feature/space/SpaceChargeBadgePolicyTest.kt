package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.SpaceVideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpaceChargeBadgePolicyTest {

    @Test
    fun chargingSpaceVideoShowsChargeBadge() {
        assertEquals(
            "充电专属",
            resolveSpaceVideoChargeBadgeLabel(
                SpaceVideoItem(isChargingArc = true)
            )
        )
    }

    @Test
    fun ugcPaySpaceVideoShowsChargeBadge() {
        assertEquals(
            "充电专属",
            resolveSpaceVideoChargeBadgeLabel(
                SpaceVideoItem(ugcPay = 1)
            )
        )
    }

    @Test
    fun regularSpaceVideoDoesNotShowBadge() {
        assertNull(resolveSpaceVideoChargeBadgeLabel(SpaceVideoItem()))
    }
}
