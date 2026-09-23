package com.android.purebilibili.feature.bangumi.policy

data class CourseNavigationTarget(
    val seasonId: Long,
    val epId: Long = 0L
)

fun parseCourseNavigation(url: String): CourseNavigationTarget? {
    if (url.isBlank()) return null
    // 匹配 ss196, season/196, season_id=196 等
    val ssMatch = Regex("""(?:ss|season/|season_id=)(\d+)""").find(url)?.groupValues?.get(1)?.toLongOrNull()
    // 匹配 ep3388, ep/3388, ep_id=3388 等
    val epMatch = Regex("""(?:ep|ep/|ep_id=)(\d+)""").find(url)?.groupValues?.get(1)?.toLongOrNull()
    if (ssMatch != null && ssMatch > 0L) {
        return CourseNavigationTarget(seasonId = ssMatch, epId = epMatch ?: 0L)
    }
    if (epMatch != null && epMatch > 0L) {
        return CourseNavigationTarget(seasonId = 0L, epId = epMatch)
    }
    return null
}
