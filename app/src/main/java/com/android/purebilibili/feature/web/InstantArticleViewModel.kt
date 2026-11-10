package com.android.purebilibili.feature.web

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.plugin.feed.FeedBlock
import com.android.purebilibili.core.plugin.feed.ReadableArticle
import com.android.purebilibili.core.plugin.feed.fetchReadableArticle
import com.android.purebilibili.core.plugin.feed.parseFeedHtml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal sealed interface InstantArticleUiState {
    data object Loading : InstantArticleUiState
    data class Content(val article: ReadableArticle, val blocks: List<FeedBlock>) : InstantArticleUiState
    data object Unavailable : InstantArticleUiState
    data class NativeRedirect(val url: String) : InstantArticleUiState
}

internal class InstantArticleViewModel : ViewModel() {
    private val mutableState = MutableStateFlow<InstantArticleUiState>(InstantArticleUiState.Loading)
    val state = mutableState.asStateFlow()
    private var requestedUrl: String? = null
    private var request: Job? = null

    fun load(url: String, force: Boolean = false) {
        if (!force && requestedUrl == url) return
        requestedUrl = url
        request?.cancel()
        mutableState.value = InstantArticleUiState.Loading
        request = viewModelScope.launch {
            val result = fetchReadableArticle(url)
            val next = result.fold(
                onSuccess = { article ->
                    val blocks = withContext(Dispatchers.Default) { parseFeedHtml(article.html, article.url) }
                    if (blocks.isEmpty()) InstantArticleUiState.Unavailable
                    else InstantArticleUiState.Content(article, blocks)
                },
                onFailure = { error ->
                    if (error is com.android.purebilibili.core.plugin.feed.NativeArticleRedirect) {
                        InstantArticleUiState.NativeRedirect(error.targetUrl)
                    } else InstantArticleUiState.Unavailable
                },
            )
            ensureActive()
            mutableState.value = next
        }
    }
}
