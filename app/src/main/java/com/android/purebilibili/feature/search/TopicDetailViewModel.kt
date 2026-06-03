package com.android.purebilibili.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.TopicTopDetails
import com.android.purebilibili.data.repository.TopicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TopicDetailUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val details: TopicTopDetails? = null,
    val items: List<DynamicItem> = emptyList(),
    val offset: String = "",
    val hasMore: Boolean = false,
    val error: String? = null
)

class TopicDetailViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TopicDetailUiState())
    val uiState = _uiState.asStateFlow()

    private var loadedTopicId: Long = 0L

    fun load(topicId: Long) {
        if (topicId <= 0L) {
            _uiState.update { it.copy(error = "话题不存在", isLoading = false) }
            return
        }
        if (loadedTopicId == topicId && (_uiState.value.details != null || _uiState.value.isLoading)) return
        loadedTopicId = topicId
        _uiState.update {
            TopicDetailUiState(isLoading = true)
        }
        viewModelScope.launch {
            val detailResult = TopicRepository.getTopicDetail(topicId)
            val feedResult = TopicRepository.getTopicFeed(topicId)
            val details = detailResult.getOrNull()
            val page = feedResult.getOrNull()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    details = details,
                    items = page?.items.orEmpty(),
                    offset = page?.offset.orEmpty(),
                    hasMore = page?.hasMore == true,
                    error = detailResult.exceptionOrNull()?.message
                        ?: feedResult.exceptionOrNull()?.message
                )
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        val topicId = loadedTopicId
        if (topicId <= 0L || !state.hasMore || state.isLoading || state.isLoadingMore) return
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            TopicRepository.getTopicFeed(topicId, offset = state.offset)
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            items = mergeDynamicItems(it.items, page.items),
                            offset = page.offset,
                            hasMore = page.hasMore,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            error = error.message ?: "加载更多失败"
                        )
                    }
                }
        }
    }

    private fun mergeDynamicItems(
        existing: List<DynamicItem>,
        incoming: List<DynamicItem>
    ): List<DynamicItem> {
        val seen = LinkedHashSet<String>()
        val merged = ArrayList<DynamicItem>(existing.size + incoming.size)
        (existing + incoming).forEach { item ->
            val key = item.id_str.ifBlank { item.hashCode().toString() }
            if (seen.add(key)) merged += item
        }
        return merged
    }
}
