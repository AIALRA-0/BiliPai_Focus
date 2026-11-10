package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.core.ui.components.appActionClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.AppContentColors
import com.android.purebilibili.core.ui.AppDialogAction
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppListItem
import com.android.purebilibili.core.ui.components.AppCheckbox
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.core.ui.components.AppTextField
import com.android.purebilibili.data.model.response.DynamicPublishMention
import com.android.purebilibili.data.model.response.DynamicPublishTopic
import com.android.purebilibili.data.model.response.DynamicTopicSearchItem
import com.android.purebilibili.data.model.response.MentionSearchUser
import com.android.purebilibili.data.repository.CommentRepository
import com.android.purebilibili.data.repository.DynamicCreateRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

@Composable
internal fun DynamicMentionPickerDialog(
    onDismiss: () -> Unit,
    onSelected: (List<DynamicPublishMention>) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<MentionSearchUser>>(emptyList()) }
    var selected by remember { mutableStateOf<List<DynamicPublishMention>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }
    fun dismiss() {
        searchJob?.cancel()
        onDismiss()
    }
    fun search() {
        searchJob?.cancel()
        loading = true
        message = null
        val requestedQuery = query
        searchJob = scope.launch {
            CommentRepository.searchMentionUsers(requestedQuery).fold(
                onSuccess = {
                    if (query == requestedQuery) {
                        users = it
                        message = if (it.isEmpty()) "没有找到用户" else null
                    }
                },
                onFailure = { if (query == requestedQuery) { users = emptyList(); message = it.message ?: "搜索失败" } },
            )
            loading = false
        }
    }
    LaunchedEffect(Unit) { search() }
    AppAlertDialog(
        onDismissRequest = ::dismiss,
        title = { AppText("@ 用户") },
        text = {
            PublishPickerContent(
                query = query,
                onQueryChange = { query = it; searchJob?.cancel(); loading = false; users = emptyList(); message = null },
                placeholder = "搜索昵称",
                actionLabel = if (loading) "搜索中…" else "搜索",
                onSearch = ::search,
                message = message,
            ) {
                users.forEach { user ->
                    AppListItem(
                        headlineContent = { AppText(user.name) },
                        supportingContent = { AppText("${user.fans} 粉丝") },
                        leadingContent = {
                            AsyncImage(
                                model = user.face,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(AppSpacingTokens.TripleExtraLarge)
                                    .clip(AppShapes.container(ContainerLevel.Pill)),
                                contentScale = ContentScale.Crop,
                            )
                        },
                        trailingContent = { AppCheckbox(checked = selected.any { it.uid == user.uid }, onCheckedChange = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .appActionClickable {
                                selected = if (selected.any { it.uid == user.uid }) selected.filterNot { it.uid == user.uid }
                                else selected + DynamicPublishMention(user.uid, user.name)
                            },
                    )
                }
            }
        },
        confirmButton = { AppTextButton(enabled = selected.isNotEmpty(), onClick = { onSelected(selected) }) { AppText("确定（${selected.size}）") } },
        dismissButton = { AppTextButton(onClick = ::dismiss) { AppText("取消") } },
    )
}

@Composable
internal fun DynamicTopicPickerDialog(
    onDismiss: () -> Unit,
    onSelected: (DynamicPublishTopic) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var topics by remember { mutableStateOf<List<DynamicTopicSearchItem>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(false) }
    var activeQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    fun search(loadMore: Boolean = false) {
        if (loading) return
        loading = true
        message = null
        val requestedQuery = if (loadMore) activeQuery else query
        val requestedPage = if (loadMore) page + 1 else 1
        scope.launch {
            DynamicCreateRepository.searchPublishTopics(requestedQuery, requestedPage).fold(
                onSuccess = { result ->
                    if (query == requestedQuery) {
                        topics = (if (loadMore) topics + result.topic_items else result.topic_items).distinctBy { it.id }
                        page = requestedPage
                        activeQuery = requestedQuery
                        hasMore = result.page_info?.has_more ?: (result.topic_items.size >= 20)
                        message = if (topics.isEmpty()) "没有找到话题" else null
                    }
                },
                onFailure = { if (query == requestedQuery) message = it.message ?: "搜索失败" },
            )
            loading = false
        }
    }
    LaunchedEffect(Unit) { search(false) }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("选择话题") },
        text = {
            PublishPickerContent(
                query = query,
                onQueryChange = { query = it; topics = emptyList(); hasMore = false; message = null },
                placeholder = "搜索话题",
                actionLabel = if (loading) "搜索中…" else "搜索",
                onSearch = { search(false) },
                message = message,
            ) {
                topics.forEach { topic ->
                    AppListItem(
                        headlineContent = { AppText("#${topic.name}#") },
                        supportingContent = topic.stat_desc.takeIf(String::isNotBlank)?.let { stat ->
                            { AppText(stat) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .appActionClickable { onSelected(DynamicPublishTopic(topic.id, topic.name)) },
                    )
                }
                if (hasMore) AppTextButton(onClick = { search(true) }, enabled = !loading) { AppText(if (loading) "加载中…" else "加载更多") }
            }
        },
        confirmButton = {},
        dismissButton = { AppDialogAction(onClick = onDismiss) { AppText("取消") } },
    )
}

@Composable
internal fun DynamicEmotePickerDialog(
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit,
) {
    var emotes by remember { mutableStateOf(DynamicEmoteCatalog.snapshot()) }
    var loading by remember { mutableStateOf(emotes.isEmpty()) }
    val emoteCatalogSessionKey = DynamicEmoteCatalog.currentSessionKey()
    LaunchedEffect(emoteCatalogSessionKey) {
        loading = true
        emotes = DynamicEmoteCatalog.ensureLoaded()
        loading = false
    }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("选择表情") },
        text = {
            if (emotes.isEmpty()) {
                AppText(if (loading) "正在加载表情…" else "暂无表情")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 72.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall),
                    verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall),
                ) {
                    items(emotes.entries.toList(), key = { it.key }) { (text, url) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 72.dp)
                                .appActionClickable { onSelected(text) }
                                .padding(AppSpacingTokens.ExtraSmall),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AsyncImage(model = url, contentDescription = null, modifier = Modifier.size(AppSpacingTokens.TripleExtraLarge), contentScale = ContentScale.Fit)
                            AppText(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { AppDialogAction(onClick = onDismiss) { AppText("取消") } },
    )
}

@Composable
private fun PublishPickerContent(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    actionLabel: String,
    onSearch: () -> Unit,
    message: String?,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small)) {
        AppTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = placeholder,
            singleLine = true,
        )
        AppTextButton(onClick = onSearch) { AppText(actionLabel) }
        message?.let { AppText(it, color = AppContentColors.secondaryText()) }
        Column(
            modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
            content = { content() },
        )
    }
}
