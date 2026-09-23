package com.android.purebilibili.feature.search

/** The state rendered in the main body of one search result tab. */
enum class SearchResultBodyMode {
    LOADING,
    ERROR,
    EMPTY,
    CONTENT,
}

/** The state rendered after existing search results. */
enum class SearchResultFooterMode {
    NONE,
    LOADING,
    ERROR,
    END,
}

/** The state rendered inside one section of the search landing page. */
enum class SearchLandingSectionMode {
    HIDDEN,
    LOADING,
    ERROR,
    CONTENT,
    EMPTY,
}

data class SearchResultPresentation(
    val body: SearchResultBodyMode,
    val footer: SearchResultFooterMode,
)

internal fun shouldLoadMoreSearchVideos(
    itemCount: Int,
    lastVisibleItemIndex: Int,
    hasMoreResults: Boolean,
    isSearching: Boolean,
    isLoadingMore: Boolean,
    hasError: Boolean,
): Boolean = hasMoreResults && !isSearching && !isLoadingMore && !hasError &&
    (itemCount == 0 || lastVisibleItemIndex >= (itemCount - 3).coerceAtLeast(0))

internal fun SearchUiState.withLoadMoreStarted(): SearchUiState = copy(
    isLoadingMore = true,
    loadMoreError = null,
)

internal fun SearchUiState.withLoadMoreFailure(message: String): SearchUiState = copy(
    isLoadingMore = false,
    loadMoreError = message,
)

internal fun SearchResultPageUiState.withLoadMoreStarted(): SearchResultPageUiState = copy(
    isLoadingMore = true,
    loadMoreError = null,
)

internal fun SearchResultPageUiState.withLoadMoreFailure(message: String): SearchResultPageUiState = copy(
    isLoadingMore = false,
    loadMoreError = message,
)

internal fun resolveSearchResultPresentation(
    itemCount: Int,
    isSearching: Boolean,
    error: String?,
    emptyStateReason: SearchEmptyStateReason,
    isLoadingMore: Boolean,
    loadMoreError: String?,
    hasMoreResults: Boolean,
): SearchResultPresentation {
    val body = when {
        isSearching && itemCount == 0 -> SearchResultBodyMode.LOADING
        error != null && itemCount == 0 -> SearchResultBodyMode.ERROR
        emptyStateReason != SearchEmptyStateReason.NONE && itemCount == 0 -> SearchResultBodyMode.EMPTY
        else -> SearchResultBodyMode.CONTENT
    }

    val footer = if (itemCount == 0 || body != SearchResultBodyMode.CONTENT) {
        SearchResultFooterMode.NONE
    } else {
        when {
            isLoadingMore -> SearchResultFooterMode.LOADING
            loadMoreError != null -> SearchResultFooterMode.ERROR
            !hasMoreResults -> SearchResultFooterMode.END
            else -> SearchResultFooterMode.NONE
        }
    }

    return SearchResultPresentation(body = body, footer = footer)
}

internal fun resolveSearchLandingSectionMode(
    enabled: Boolean,
    itemCount: Int,
    isRefreshing: Boolean,
    error: String?,
): SearchLandingSectionMode = when {
    !enabled -> SearchLandingSectionMode.HIDDEN
    isRefreshing && itemCount == 0 -> SearchLandingSectionMode.LOADING
    error != null && itemCount == 0 -> SearchLandingSectionMode.ERROR
    itemCount == 0 -> SearchLandingSectionMode.EMPTY
    else -> SearchLandingSectionMode.CONTENT
}
