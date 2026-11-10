package com.android.purebilibili.feature.space

import androidx.paging.PagingSource
import com.android.purebilibili.core.paging.ForwardPage
import com.android.purebilibili.core.paging.ForwardPagingSource
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal data class SpaceContributionPage<T : Any>(
    val number: Int,
    val items: List<T>,
    val total: Int,
    val hasMore: Boolean,
    val offset: String = "",
)

internal fun <T : Any> spaceContributionPagingSource(
    itemKey: (T) -> Long,
    loadPage: suspend (Int) -> SpaceContributionPage<T>,
): PagingSource<Int, SpaceContributionPage<T>> {
    val seen = mutableSetOf<Long>()
    return ForwardPagingSource(1) { number ->
        val page = loadPage(number)
        currentCoroutineContext().ensureActive()
        val fresh = page.items.filter { seen.add(itemKey(it)) }
        val more = page.hasMore && fresh.isNotEmpty()
        ForwardPage(listOf(page.copy(items = fresh, hasMore = more)), (number + 1).takeIf { more })
    }
}
