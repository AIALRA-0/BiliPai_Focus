package com.android.purebilibili.feature.dynamic

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/** Empty visible identities mean scrolling or no measured users; callers own page cancellation. */
internal suspend fun prefetchDynamicSidebarNeighborAvatars(
    users: List<SidebarUser>,
    visibleUserIds: Flow<List<Long>>,
    load: suspend (String) -> Unit,
) {
    visibleUserIds.collectLatest { ids ->
        if (ids.isEmpty()) return@collectLatest
        delay(120)
        for (url in resolveDynamicSidebarNeighborAvatarUrls(users, ids)) {
            currentCoroutineContext().ensureActive()
            load(url)
        }
    }
}
