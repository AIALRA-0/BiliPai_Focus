package com.android.purebilibili.feature.message.feed

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.coroutines.AccountScopedRequests
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.core.ui.AppTopBar
import com.android.purebilibili.core.ui.AdaptivePullToRefreshBox
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.core.ui.components.*
import com.android.purebilibili.core.ui.components.AppLazyColumn as LazyColumn
import com.android.purebilibili.data.model.response.MessageLikeDetailItem
import com.android.purebilibili.data.model.response.MessageLikeDetailCard
import com.android.purebilibili.data.repository.MessageManagementRepository
import com.android.purebilibili.feature.message.MessageAppScaffold
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class LikeDetailState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val items: List<MessageLikeDetailItem> = emptyList(),
    val card: MessageLikeDetailCard? = null,
    val hasMore: Boolean = false,
    val error: String? = null,
    val errorOnMore: Boolean = false,
)
internal class LikeDetailViewModel : ViewModel() {
    private val requests = AccountScopedRequests(viewModelScope)
    private val state = MutableStateFlow(LikeDetailState())
    val uiState = state.asStateFlow()
    private var id = 0L
    private var page = 0
    private var generation = 0L
    private var job: Job? = null
    init { requests.observe { if (id > 0) load(id) } }
    fun load(cardId: Long) {
        id = cardId
        job?.cancel()
        page = 0
        state.value = LikeDetailState()
        fetch(refresh = true)
    }
    fun refresh() { job?.cancel(); fetch(refresh = true) }
    fun retry() { if (state.value.errorOnMore) more() else refresh() }
    fun more() { if (!state.value.loadingMore && !state.value.loading && state.value.hasMore) fetch(refresh = false) }
    private fun fetch(refresh: Boolean) {
        val version = if (refresh) ++generation else generation
        val nextPage = if (refresh) 1 else page + 1
        val lastMid = if (refresh) 0 else state.value.items.lastOrNull()?.user?.mid ?: 0L
        state.update { it.copy(refreshing = refresh && !it.loading, loadingMore = !refresh, error = null) }
        job = requests.launch {
            MessageManagementRepository.likeDetail(id, nextPage, lastMid).fold(onSuccess = { data ->
                if (version != generation) return@fold
                page = nextPage
                state.update {
                    val merged = (if (refresh) data.items else it.items + data.items).distinctBy { item -> item.user?.mid }
                    it.copy(loading = false, refreshing = false, loadingMore = false, items = merged, card = data.card ?: it.card,
                        hasMore = data.page?.isEnd != true && data.items.isNotEmpty() && (refresh || merged.size > it.items.size))
                }
            }, onFailure = { error -> if (version == generation) state.update { it.copy(loading = false, refreshing = false, loadingMore = false, error = error.message ?: "加载失败", errorOnMore = !refresh) } })
        }
    }
}

@Composable
internal fun LikeDetailScreen(cardId: Long, contentLink: String?, onBack: () -> Unit, onOpenLink: (String) -> Unit, onOpenSpace: (Long) -> Unit) {
    val account by TokenManager.accountIdentity.collectAsStateWithLifecycle()
    val model: LikeDetailViewModel = viewModel(key = "like_detail_${account}_$cardId")
    val state by model.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(cardId, account) { model.load(cardId) }
    BackHandler(onBack = onBack)
    MessageAppScaffold(topBar = { AppTopBar(title = "点赞详情", navigationIcon = { AppIconButton(onClick = onBack) { AppIcon(rememberAppBackIcon(), "返回") } }) }) { padding ->
        AdaptivePullToRefreshBox(isRefreshing = state.refreshing, onRefresh = model::refresh, indicatorTopInset = padding.calculateTopPadding()) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
                state.card?.let { card -> item {
                    AppText("${card.business}：${card.title}", Modifier.fillMaxWidth().clickable(enabled = !contentLink.isNullOrBlank()) { contentLink?.let(onOpenLink) }.padding(16.dp))
                } }
                if (state.loading) items(6) { MessageFeedCardSkeleton(Modifier.fillMaxWidth()) }
                state.error?.let { error -> item { MessageFeedError(error, onRetry = model::retry, modifier = Modifier.padding(16.dp)) } }
                items(state.items, key = { it.user?.mid ?: it.likeTime }) { item ->
                    Row(Modifier.fillMaxWidth().clickable { item.user?.mid?.takeIf { it > 0 }?.let(onOpenSpace) }.padding(16.dp)) {
                        MessageFeedAvatar(item.user?.avatar.orEmpty())
                        Column(Modifier.padding(start = 12.dp)) {
                            AppText("${item.user?.nickname.orEmpty()} 赞了我", style = MaterialTheme.typography.bodyLarge)
                            AppText(formatMessageFeedTime(item.likeTime.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (!state.loading && state.items.isEmpty() && state.error == null) item { MessageFeedEmpty("暂无点赞记录", Modifier.padding(24.dp)) }
                if (state.hasMore) item {
                    LaunchedEffect(state.items.size) { if (state.error == null) model.more() }
                    MessageFeedLoadMore(state.loadingMore, state.hasMore, model::more)
                }
            }
        }
    }
}
