package com.android.purebilibili.feature.home

enum class HomeVideoClickSource {
    GRID,
    TODAY_WATCH,
    PREVIEW
}

data class HomeVideoClickRequest(
    val bvid: String,
    val dynamicId: String = "",
    val cid: Long = 0L,
    val coverUrl: String = "",
    val isVerticalVideo: Boolean = false,
    val source: HomeVideoClickSource = HomeVideoClickSource.GRID,
    val sourceRoute: String? = null
)

fun resolveHomeCategoryVideoSourceRoute(category: HomeCategory): String {
    return "home?category=${category.name}"
}
