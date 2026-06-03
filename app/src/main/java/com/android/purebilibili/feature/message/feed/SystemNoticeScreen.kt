package com.android.purebilibili.feature.message.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.ui.AdaptiveScaffold
import com.android.purebilibili.core.ui.AdaptiveTopAppBar
import com.android.purebilibili.core.ui.ComfortablePullToRefreshBox
import com.android.purebilibili.core.ui.rememberAppBackIcon
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
    onOpenLink: (String) -> Unit,
    viewModel: SystemNoticeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AdaptiveScaffold(
        topBar = {
            AdaptiveTopAppBar(
                title = "系统通知",
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
                            val annotatedContent = rememberSystemNoticeAnnotatedContent(item.content)
                            MessageFeedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    ClickableText(
                                        text = annotatedContent,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        onClick = { offset ->
                                            annotatedContent
                                                .getStringAnnotations(tag = "link", start = offset, end = offset)
                                                .firstOrNull()
                                                ?.item
                                                ?.let(onOpenLink)
                                        }
                                    )
                                    Text(
                                        text = item.timeAt,
                                        modifier = Modifier
                                            .align(Alignment.End),
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

@Composable
private fun rememberSystemNoticeAnnotatedContent(content: String): AnnotatedString {
    val primaryColor = MaterialTheme.colorScheme.primary
    return remember(content, primaryColor) {
        buildAnnotatedString {
            parseSystemNoticeContentSegments(content).forEach { segment ->
                val link = segment.link
                if (link == null) {
                    append(segment.text)
                } else {
                    pushStringAnnotation(tag = "link", annotation = link)
                    withStyle(SpanStyle(color = primaryColor)) {
                        append(segment.text)
                    }
                    pop()
                }
            }
        }
    }
}
