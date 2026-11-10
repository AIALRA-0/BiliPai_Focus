package com.android.purebilibili.feature.dynamic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.repository.DynamicRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal sealed interface DynamicDetailUiState {
    data object Loading : DynamicDetailUiState
    data class Success(val item: DynamicItem) : DynamicDetailUiState
    data class Error(val message: String) : DynamicDetailUiState
}

/** Owned by the navigation entry, so a child space does not restart detail/comment loading. */
internal class DynamicDetailViewModel(
    private val loadDetail: suspend (String) -> Result<DynamicItem> = DynamicRepository::getDynamicDetail,
    private val readSeed: (String) -> DynamicItem? = DynamicRepository::peekDynamicDetailSeed,
) : ViewModel() {
    private val _uiState = MutableStateFlow<DynamicDetailUiState>(DynamicDetailUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private var loadedId: String? = null
    private var loadJob: Job? = null
    private var initializedComments: Triple<String, Long, Long>? = null

    fun load(dynamicId: String, fallbackError: String, force: Boolean = false) {
        if (!force && loadedId == dynamicId) return
        val sameDetail = loadedId == dynamicId
        loadedId = dynamicId
        loadJob?.cancel()
        if (!sameDetail) initializedComments = null
        val retained = _uiState.value.takeIf { sameDetail && it is DynamicDetailUiState.Success }
        _uiState.value = retained ?: readSeed(dynamicId)?.let { DynamicDetailUiState.Success(it) }
            ?: DynamicDetailUiState.Loading
        loadJob = viewModelScope.launch {
            val result = loadDetail(dynamicId)
            currentCoroutineContext().ensureActive()
            _uiState.value = result.fold(
                onSuccess = { DynamicDetailUiState.Success(it) },
                onFailure = {
                    if (it is CancellationException) throw it
                    DynamicDetailUiState.Error(it.message ?: fallbackError)
                },
            )
        }
    }

    fun initializeCommentsOnce(
        targetKey: String,
        rootReplyId: Long,
        targetReplyId: Long,
        initialize: () -> Unit,
    ) {
        val target = Triple(targetKey, rootReplyId, targetReplyId)
        if (initializedComments == target) return
        initialize()
        initializedComments = target
    }
}
