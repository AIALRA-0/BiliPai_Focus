package com.android.purebilibili.feature.message.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.ui.ComfortablePullToRefreshBox
import com.android.purebilibili.data.model.response.SystemNoticeItem
import com.android.purebilibili.data.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SystemNoticeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val items: List<SystemNoticeItem> = emptyList(),
    val cursor: Long? = null,
    val hasMore: Boolean = false,
    val error: String? = null
)

class SystemNoticeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SystemNoticeUiState())
    val uiState: StateFlow<SystemNoticeUiState> = _uiState.asStateFlow()

    init {
        loadInitial()
    }

    fun loadInitial() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            MessageRepository.getSystemNotices().fold(
                onSuccess = { items ->
                    items.firstOrNull()?.cursor?.takeIf { it > 0 }?.let {
                        MessageRepository.updateSystemNoticeCursor(it)
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        items = items,
                        cursor = items.lastOrNull()?.cursor,
                        hasMore = items.isNotEmpty()
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error.message ?: "加载失败")
                }
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            MessageRepository.getSystemNotices().fold(
                onSuccess = { items ->
                    items.firstOrNull()?.cursor?.takeIf { it > 0 }?.let {
                        MessageRepository.updateSystemNoticeCursor(it)
                    }
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        items = items,
                        cursor = items.lastOrNull()?.cursor,
                        hasMore = items.isNotEmpty()
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(isRefreshing = false, error = error.message ?: "刷新失败")
                }
            )
        }
    }

    fun loadMore() {
        val current = _uiState.value
        if (current.isLoadingMore || !current.hasMore) return
        viewModelScope.launch {
            _uiState.value = current.copy(isLoadingMore = true)
            MessageRepository.getSystemNotices(cursor = current.cursor).fold(
                onSuccess = { items ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        items = current.items + items,
                        cursor = items.lastOrNull()?.cursor ?: current.cursor,
                        hasMore = items.isNotEmpty()
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemNoticeScreen(
    onBack: () -> Unit,
    viewModel: SystemNoticeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                title = { Text("系统通知") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> com.android.purebilibili.core.ui.CutePersonLoadingIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
                uiState.error != null -> MessageFeedError(
                    text = uiState.error ?: "加载失败",
                    onRetry = viewModel::loadInitial,
                    modifier = Modifier.fillMaxSize()
                )
                uiState.items.isEmpty() -> MessageFeedEmpty(
                    text = "暂无系统通知",
                    modifier = Modifier.fillMaxSize()
                )
                else -> ComfortablePullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.items, key = { it.id }) { item ->
                            MessageFeedCard(modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = item.content,
                                        modifier = Modifier.padding(top = 30.dp, end = 48.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = item.timeAt,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(top = 12.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        item {
                            MessageFeedLoadMore(
                                isLoadingMore = uiState.isLoadingMore,
                                hasMore = uiState.hasMore,
                                onLoadMore = viewModel::loadMore
                            )
                        }
                    }
                }
            }
        }
    }
}
