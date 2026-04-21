package com.android.purebilibili.feature.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.data.repository.SearchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchTrendingUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val items: List<SearchKeywordUiModel> = emptyList(),
    val pinnedCount: Int = 0,
    val error: String? = null
)

class SearchTrendingViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(SearchTrendingUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val showInitialLoading = _uiState.value.items.isEmpty()
            _uiState.update {
                it.copy(
                    isLoading = showInitialLoading,
                    isRefreshing = !showInitialLoading,
                    error = null
                )
            }
            SearchRepository.getTrendingKeywords(limit = 30)
                .onSuccess { bundle ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            items = bundle.allItems.map { item -> item.toSearchKeywordUiModel() },
                            pinnedCount = bundle.pinnedItems.size
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = throwable.message ?: "热搜加载失败"
                        )
                    }
                }
        }
    }
}
