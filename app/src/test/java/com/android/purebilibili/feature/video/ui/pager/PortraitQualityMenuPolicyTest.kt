package com.android.purebilibili.feature.video.ui.pager

import kotlin.test.Test
import kotlin.test.assertEquals

class PortraitQualityMenuPolicyTest {

    @Test
    fun qualityMenu_mergesPortraitAndDetailOptions() {
        assertEquals(
            listOf(120, 80, 64, 32),
            resolvePortraitQualityMenuIds(
                portraitQualityIds = listOf(80, 64),
                detailQualityIds = listOf(120, 80, 32),
                selectedQualityId = 80,
            ),
        )
    }

    @Test
    fun qualityMenu_singleWarmupOptionAddsRequestableLowerQualities() {
        assertEquals(
            listOf(80, 64, 32, 16),
            resolvePortraitQualityMenuIds(
                portraitQualityIds = listOf(80),
                detailQualityIds = emptyList(),
                selectedQualityId = 80,
            ),
        )
    }
}
