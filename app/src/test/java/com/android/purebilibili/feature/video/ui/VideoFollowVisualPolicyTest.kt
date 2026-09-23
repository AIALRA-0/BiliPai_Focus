package com.android.purebilibili.feature.video.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VideoFollowVisualPolicyTest {

    @Test
    fun `followed state uses themed secondary accent instead of neutral styling`() {
        val darkPolicy = resolveVideoFollowVisualPolicy(isFollowing = true, darkTheme = true)
        val lightPolicy = resolveVideoFollowVisualPolicy(isFollowing = true, darkTheme = false)

        assertEquals(FollowButtonTone.PRIMARY_CONTAINER, darkPolicy.detailButtonTone)
        assertEquals(FollowTextTone.ON_PRIMARY_CONTAINER, darkPolicy.detailTextTone)
        assertEquals(FollowBadgeTone.PRIMARY, darkPolicy.relatedBadgeTone)
        assertEquals(lightPolicy.detailButtonTone, darkPolicy.detailButtonTone)
    }

    @Test
    fun `unfollowed state uses primary on dark theme`() {
        val policy = resolveVideoFollowVisualPolicy(isFollowing = false, darkTheme = true)

        assertEquals(FollowButtonTone.PRIMARY, policy.detailButtonTone)
        assertEquals(FollowTextTone.ON_PRIMARY, policy.detailTextTone)
        assertNull(policy.relatedBadgeTone)
    }

    @Test
    fun `unfollowed state uses container tone on light theme to avoid heavy primary`() {
        val policy = resolveVideoFollowVisualPolicy(isFollowing = false, darkTheme = false)

        assertEquals(FollowButtonTone.PRIMARY_CONTAINER, policy.detailButtonTone)
        assertEquals(FollowTextTone.ON_PRIMARY_CONTAINER, policy.detailTextTone)
        assertEquals(FollowBadgeTone.PRIMARY, policy.relatedBadgeTone)
    }
}
