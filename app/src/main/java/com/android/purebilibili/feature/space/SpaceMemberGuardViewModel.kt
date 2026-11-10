package com.android.purebilibili.feature.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.SpaceGuardMemberItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UP主大航海（舰队）列表页（对齐 PiliPlus member_guard）。 */
class SpaceMemberGuardViewModel(
    private val ruid: Long,
    upName: String,
    initialCount: Long = 0L,
) : ViewModel() {

    data class UiState(
        val upName: String = "",
        val totalCount: Long = 0L,
        val isLoading: Boolean = true,
        val isLoadingMore: Boolean = false,
        val error: String? = null,
        val items: List<SpaceGuardMemberItem> = emptyList(),
        // 顶部领奖台（总督/提督/舰长前三名），刷新时从列表中摘出
        val tops: List<SpaceGuardMemberItem> = emptyList(),
        val hasMore: Boolean = false,
    )

    private val spaceApi = NetworkModule.spaceApi
    private val _uiState = MutableStateFlow(UiState(upName = upName, totalCount = initialCount))
    val uiState = _uiState.asStateFlow()

    private var page = 1

    init {
        refresh()
    }

    fun retry() {
        refresh()
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore || state.error != null) return
        _uiState.value = state.copy(isLoadingMore = true)
        fetch(page = page + 1, isLoadMore = true)
    }

    private fun refresh() {
        page = 1
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        fetch(page = 1, isLoadMore = false)
    }

    private fun fetch(page: Int, isLoadMore: Boolean) {
        viewModelScope.launch {
            try {
                val response = spaceApi.getMemberGuard(
                    mapOf(
                        "ruid" to ruid.toString(),
                        "page" to page.toString(),
                        "page_size" to PAGE_SIZE.toString(),
                    )
                )
                val data = response.data
                if (response.code != 0 || data == null) {
                    _uiState.value = _uiState.value.let {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = if (isLoadMore) it.error else response.message.ifBlank { "获取舰队信息失败" },
                        )
                    }
                    return@launch
                }
                var list = data.guardTopList
                var tops = _uiState.value.tops
                if (!isLoadMore) {
                    tops = list.take(3)
                    list = list.drop(3)
                }
                this@SpaceMemberGuardViewModel.page = page
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = null,
                    items = if (isLoadMore) _uiState.value.items + list else list,
                    tops = tops,
                    hasMore = data.hasMore == 1,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("SpaceMemberGuard", "load failed: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = if (isLoadMore) _uiState.value.error else (e.message ?: "网络请求失败"),
                )
            }
        }
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
