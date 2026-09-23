package com.android.purebilibili.feature.dynamic.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
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
import com.android.purebilibili.core.ui.AppDialogAction
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppListItem
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

@Composable
internal fun DynamicMentionPickerDialog(
    onDismiss: () -> Unit,
    onSelected: (DynamicPublishMention) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<MentionSearchUser>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun search() {
        if (loading) return
        loading = true
        message = null
        scope.launch {
            CommentRepository.searchMentionUsers(query).fold(
                onSuccess = {
                    users = it
                    message = if (it.isEmpty()) "没有找到用户" else null
                },
                onFailure = { message = it.message ?: "搜索失败" },
            )
            loading = false
        }
    }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("@ 用户") },
        text = {
            PublishPickerContent(
                query = query,
                onQueryChange = { query = it },
                placeholder = "搜索昵称",
                actionLabel = if (loading) "搜索中…" else "搜索",
                onSearch = ::search,
                message = message,
            ) {
                users.take(8).forEach { user ->
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(DynamicPublishMention(user.uid, user.name)) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { AppDialogAction(onClick = onDismiss) { AppText("取消") } },
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
    val scope = rememberCoroutineScope()
    fun search() {
        if (loading) return
        loading = true
        message = null
        scope.launch {
            DynamicCreateRepository.searchPublishTopics(query).fold(
                onSuccess = {
                    topics = it
                    message = if (it.isEmpty()) "没有找到话题" else null
                },
                onFailure = { message = it.message ?: "搜索失败" },
            )
            loading = false
        }
    }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("选择话题") },
        text = {
            PublishPickerContent(
                query = query,
                onQueryChange = { query = it },
                placeholder = "搜索话题",
                actionLabel = if (loading) "搜索中…" else "搜索",
                onSearch = ::search,
                message = message,
            ) {
                topics.take(8).forEach { topic ->
                    AppListItem(
                        headlineContent = { AppText("#${topic.name}#") },
                        supportingContent = topic.stat_desc.takeIf(String::isNotBlank)?.let { stat ->
                            { AppText(stat) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(DynamicPublishTopic(topic.id, topic.name)) },
                    )
                }
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
    val emoteCatalogSessionKey = DynamicEmoteCatalog.currentSessionKey()
    LaunchedEffect(emoteCatalogSessionKey) { emotes = DynamicEmoteCatalog.ensureLoaded() }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("选择表情") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall),
            ) {
                emotes.entries.take(40).chunked(4).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { (text, url) ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelected(text) },
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = text,
                                    modifier = Modifier.size(AppSpacingTokens.TripleExtraLarge),
                                    contentScale = ContentScale.Fit,
                                )
                                AppText(text, maxLines = 1)
                            }
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
        message?.let { AppText(it) }
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            content = { content() },
        )
    }
}
