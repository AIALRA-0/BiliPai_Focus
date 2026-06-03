package com.android.purebilibili.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeFollowFeedMappingPolicyTest {

    @Test
    fun resolveDynamicArchiveAid_parsesArchiveAidString() {
        assertEquals(1456400345L, resolveDynamicArchiveAid(archiveAid = "1456400345", fallbackId = 0L))
    }

    @Test
    fun resolveDynamicArchiveAid_fallsBackToExistingIdWhenArchiveAidInvalid() {
        assertEquals(9988L, resolveDynamicArchiveAid(archiveAid = "", fallbackId = 9988L))
    }

    @Test
    fun shouldIncludeHomeFollowDynamicInVideoFeed_onlyWhenArchiveBvidExists() {
        assertTrue(shouldIncludeHomeFollowDynamicInVideoFeed("BV1xx411c7mD"))
        assertFalse(shouldIncludeHomeFollowDynamicInVideoFeed(""))
    }
}
