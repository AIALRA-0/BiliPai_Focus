package com.android.purebilibili.feature.search

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.transition.SharedCardContainerHost
import com.android.purebilibili.core.ui.transition.sharedCardContainerBounds

internal fun searchUpContainerKey(mid: Long) = "search-up-container:$mid"
internal fun searchBangumiContainerKey(seasonId: Long) = "search-bangumi-container:$seasonId"
internal fun searchArticleContainerKey(articleId: Long) = "search-article-container:$articleId"

internal enum class SearchCardKind { UP, BANGUMI, ARTICLE, DYNAMIC }
internal data class SearchCardTarget(
    val kind: SearchCardKind,
    val id: Long,
    val sourceKey: String,
    val title: String = "",
    val dynamicId: String = "",
)

internal fun saveSearchCardTarget(target: SearchCardTarget): List<Any> =
    listOf(target.kind.name, target.id, target.sourceKey, target.title, target.dynamicId)

internal fun restoreSearchCardTarget(values: List<Any>): SearchCardTarget? {
    if (values.size < 3) return null
    // Restore UP/Bangumi containers saved before article targets were introduced.
    val kind = when (val savedKind = values[0]) {
        true -> SearchCardKind.UP
        false -> SearchCardKind.BANGUMI
        is String -> SearchCardKind.entries.firstOrNull { it.name == savedKind } ?: return null
        else -> return null
    }
    return SearchCardTarget(
        kind, values[1] as? Long ?: return null, values[2] as? String ?: return null,
        values.getOrNull(3) as? String ?: "", values.getOrNull(4) as? String ?: "",
    )
}
private val SearchCardTargetSaver = Saver<SearchCardTarget?, List<Any>>(
    save = { it?.let(::saveSearchCardTarget) ?: emptyList() },
    restore = ::restoreSearchCardTarget,
)

@OptIn(ExperimentalSharedTransitionApi::class)
private val LocalSearchResultSharedScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
private val LocalSearchResultVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/** Reuse Bangumi's exact host. Result expansion does not push a second navigation entry. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SearchResultContainerHost(
    content: @Composable ((Long, String) -> Unit, (Long, String) -> Unit, (Long, String, String) -> Unit, Boolean) -> Unit,
    destination: @Composable (SearchCardTarget, () -> Unit, (SearchCardTarget) -> Unit, Boolean, Modifier) -> Unit,
) {
    val parentSharedScope = LocalSharedTransitionScope.current
    val parentVisibilityScope = LocalAnimatedVisibilityScope.current
    SharedCardContainerHost<SearchCardTarget>(
        saver = SearchCardTargetSaver,
        keyOf = { it.sourceKey },
        content = { open, visible ->
            CompositionLocalProvider(
                LocalSearchResultSharedScope provides LocalSharedTransitionScope.current,
                LocalSearchResultVisibilityScope provides LocalAnimatedVisibilityScope.current,
                // Existing video cards keep exactly the scopes they had before this host.
                LocalSharedTransitionScope provides parentSharedScope,
                LocalAnimatedVisibilityScope provides parentVisibilityScope,
            ) {
                content({ id, key ->
                    open(SearchCardTarget(SearchCardKind.UP, id, key))
                },
                    { id, key ->
                        open(SearchCardTarget(SearchCardKind.BANGUMI, id, key))
                    },
                    { id, title, key ->
                        when (val target = resolveArticleNavigationTarget(id)) {
                            is ArticleNavigationTarget.NativeArticle -> open(
                                SearchCardTarget(SearchCardKind.ARTICLE, target.articleId, key, title)
                            )
                            is ArticleNavigationTarget.NativeDynamic -> open(
                                SearchCardTarget(SearchCardKind.DYNAMIC, id, key, title, target.dynamicId)
                            )
                            null -> Unit
                        }
                    }, visible)
            }
        },
        destination = destination,
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.searchResultContainerBounds(key: String): Modifier = sharedCardContainerBounds(
    modifier = this,
    key = key,
    sharedScope = LocalSearchResultSharedScope.current,
    visibilityScope = LocalSearchResultVisibilityScope.current,
)
