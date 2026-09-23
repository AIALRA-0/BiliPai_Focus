package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.feature.bangumi.policy.CourseNavigationTarget
import com.android.purebilibili.feature.bangumi.policy.parseCourseNavigation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CourseNavigationPolicyTest {

    @Test
    fun parseCourseNavigation_parsesSeasonIdFromWebUrl() {
        val target = parseCourseNavigation("https://www.bilibili.com/cheese/play/ss196")
        assertEquals(CourseNavigationTarget(seasonId = 196L, epId = 0L), target)
    }

    @Test
    fun parseCourseNavigation_parsesEpisodeIdFromWebUrl() {
        val target = parseCourseNavigation("https://www.bilibili.com/cheese/play/ep3388")
        assertEquals(CourseNavigationTarget(seasonId = 0L, epId = 3388L), target)
    }

    @Test
    fun parseCourseNavigation_parsesSeasonAndEpisodeIdFromScheme() {
        val target = parseCourseNavigation("bilibili://cheese/play/ss196?ep_id=3388")
        assertEquals(CourseNavigationTarget(seasonId = 196L, epId = 3388L), target)
    }

    @Test
    fun parseCourseNavigation_returnsNullForNonCourseUrl() {
        assertNull(parseCourseNavigation("https://www.bilibili.com/video/BV1xx411c7mD"))
        assertNull(parseCourseNavigation(""))
    }
}
