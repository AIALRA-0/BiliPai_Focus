package com.android.purebilibili.feature.message.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.ui.AdaptiveScaffold
import com.android.purebilibili.core.ui.AdaptiveTopAppBar
import com.android.purebilibili.core.ui.ComfortablePullToRefreshBox
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.data.model.response.MessageFeedAtItem
import com.android.purebilibili.data.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AtMeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val items: List<MessageFeedAtItem> = emptyList(),
    val cursor: Long? = null,
    val cursorTime: Long? = null,
    val hasMore: Boolean = false,
    val error: String? = null
)

class AtMeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AtMeUiState())
    val uiState: StateFlow<AtMeUiState> = _uiState.asStateFlow()

    init {
        loadInitial()
    }

    fun loadInitial() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            MessageRepository.getAtFeed().fold(
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        items = data.items.orEmpty(),
                        cursor = data.cursor?.id,
                        cursorTime = data.cursor?.time,
                        hasMore = data.cursor?.isEnd != true && !data.items.isNullOrEmpty()
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
            MessageRepository.getAtFeed().fold(
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        items = data.items.orEmpty(),
                        cursor = data.cursor?.id,
                        cursorTime = data.cursor?.time,
                        hasMore = data.cursor?.isEnd != true && !data.items.isNullOrEmpty()
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
            MessageRepository.getAtFeed(cursor = current.cursor, cursorTime = current.cursorTime).fold(
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        items = current.items + data.items.orEmpty(),
                        cursor = data.cursor?.id,
                        cursorTime = data.cursor?.time,
                        hasMore = data.cursor?.isEnd != true && !data.items.isNullOrEmpty()
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                }
            )
        }
    }

    fun remove(id: Long) {
        viewModelScope.launch {
            MessageRepository.deleteFeedItem(type = 2, id = id).onSuccess {
                _uiState.value = _uiState.value.copy(
                    items = _uiState.value.items.filterNot { it.id == id }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtMeScreen(
    onBack: () -> Unit,
    onOpenLink: (String) -> Unit,
    onOpenSpace: (Long) -> Unit,
    viewModel: AtMeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    AdaptiveScaffold(
        topBar = {
            AdaptiveTopAppBar(
                title = "@我",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(rememberAppBackIcon(), contentDescription = "返回")
                    }
                }
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
                    text = "暂无@我消息",
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
                            AtMeCard(
                                item = item,
                                onClick = {
                                    firstNonBlank(item.item?.nativeUri, item.item?.uri)?.let(onOpenLink)
                                },
                                onUserClick = {
                                    item.user?.mid?.takeIf { it > 0 }?.let(onOpenSpace)
                                },
                                onRemove = { viewModel.remove(item.id) }
                            )
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

@Composable
private fun AtMeCard(
    item: MessageFeedAtItem,
    onClick: () -> Unit,
    onUserClick: () -> Unit,
    onRemove: () -> Unit
) {
    MessageFeedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            MessageFeedAvatar(
                avatarUrl = item.user?.avatar.orEmpty(),
                modifier = Modifier.clickable(onClick = onUserClick)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.user?.nickname.orEmpty().ifBlank { "用户" }} 在${item.item?.business.orEmpty().ifBlank { "内容" }}中@了我",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                item.item?.sourceContent?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatMessageFeedTime(item.atTime.toInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "删除",
                        modifier = Modifier.clickable(onClick = onRemove),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
