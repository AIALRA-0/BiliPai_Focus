// 文件路径: feature/story/StoryViewModel.kt
package com.android.purebilibili.feature.story

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.data.model.response.StoryItem
import com.android.purebilibili.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StoryUiState(
    val items: List<StoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentIndex: Int = 0
)

/**
 * 竖屏视频流：数据源与首页推荐一致（[VideoRepository.getHomeVideos]），内容更元化。
 */
class StoryViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StoryUiState())
    val uiState: StateFlow<StoryUiState> = _uiState.asStateFlow()

    /** Home feed page index for load-more (0-based). */
    private var nextPageIndex: Int = 0

    init {
        loadInitialStories()
    }

    private fun loadInitialStories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            nextPageIndex = 0

            val result = VideoRepository.getHomeVideos(idx = 0)
            result.onSuccess { videos ->
                val items = videoItemsToStoryItems(videos)
                _uiState.value = _uiState.value.copy(
                    items = items,
                    isLoading = false,
                    error = if (items.isEmpty()) "暂时没有可播放的推荐视频" else null
                )
                nextPageIndex = 1
                Logger.d("StoryVM", "首页推荐竖屏流: ${items.size} 条")
                items.firstOrNull()?.let { first ->
                    Logger.d(
                        "StoryVM",
                        "首条: title=${first.title.take(20)} bvid=${first.playerArgs?.bvid}"
                    )
                }
            }.onFailure { e ->
                Logger.e("StoryVM", "加载首页推荐失败: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }

    fun loadMoreStories() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val page = nextPageIndex

            val result = VideoRepository.getHomeVideos(idx = page)
            result.onSuccess { videos ->
                val newItems = videoItemsToStoryItems(videos)
                val currentItems = _uiState.value.items
                val mergedItems = mergeStoryFeedItems(
                    existingItems = currentItems,
                    newItems = newItems
                )
                _uiState.value = _uiState.value.copy(
                    items = mergedItems,
                    isLoading = false
                )
                if (newItems.isNotEmpty()) {
                    nextPageIndex = page + 1
                }
                Logger.d(
                    "StoryVM",
                    "加载更多推荐: page=$page +${newItems.size}，合计 ${mergedItems.size}"
                )
            }.onFailure { e ->
                Logger.e("StoryVM", "加载更多失败: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun updateCurrentIndex(index: Int) {
        _uiState.value = _uiState.value.copy(currentIndex = index)

        val items = _uiState.value.items
        if (index >= items.size - 3 && items.isNotEmpty()) {
            loadMoreStories()
        }
    }

    fun refresh() {
        nextPageIndex = 0
        loadInitialStories()
    }
}
