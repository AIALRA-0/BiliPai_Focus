package com.android.purebilibili.feature.dynamic

internal fun searchDynamicAuthors(users: List<SidebarUser>, query: String): List<SidebarUser> {
    val keyword = query.trim()
    return users.asSequence()
        .filter { it.uid > 0L && it.name.isNotBlank() }
        .distinctBy { it.uid }
        .filter { keyword.isEmpty() || it.name.contains(keyword, ignoreCase = true) }
        .sortedBy {
            when {
                keyword.isEmpty() -> 0
                it.name.equals(keyword, ignoreCase = true) -> 0
                it.name.startsWith(keyword, ignoreCase = true) -> 1
                else -> 2
            }
        }
        .toList()
}
