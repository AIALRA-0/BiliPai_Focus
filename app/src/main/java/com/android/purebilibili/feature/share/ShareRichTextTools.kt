package com.android.purebilibili.feature.share

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.ui.components.*
import com.android.purebilibili.data.model.response.*
import com.android.purebilibili.feature.dynamic.components.DynamicEmoteCatalog
import com.android.purebilibili.feature.message.ChatEmotePanel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** 发布与转发共用输入工具，提及和话题同时保留展示文字与资源身份。 */
@Composable
internal fun ShareRichTextTools(
    contentKey: String,
    enabled: Boolean,
    onAppend: (String) -> Unit,
    onMention: (MentionSearchUser) -> Unit,
    onTopic: (DynamicTopicSearchItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var panel by remember(contentKey) { mutableStateOf("") }
    var query by remember(contentKey) { mutableStateOf("") }
    var users by remember(contentKey) { mutableStateOf<List<MentionSearchUser>>(emptyList()) }
    var topics by remember(contentKey) { mutableStateOf<List<DynamicTopicSearchItem>>(emptyList()) }
    var packages by remember { mutableStateOf(DynamicEmoteCatalog.snapshotPackages()) }
    var loading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(contentKey, panel, query, retry) {
        if (panel.isBlank()) return@LaunchedEffect
        loading = true
        loadError = null
        try {
            if (panel == "emoji") {
                DynamicEmoteCatalog.ensureLoaded()
                packages = DynamicEmoteCatalog.snapshotPackages()
            } else {
                delay(250)
                if (panel == "mention") {
                    val response = NetworkModule.api.searchMentionUsers(query.ifBlank { null })
                    check(response.code == 0) { response.message.ifBlank { "搜索用户失败" } }
                    users = response.data?.groups.orEmpty().flatMap { it.items }.distinctBy { it.uid }
                } else {
                    val response = NetworkModule.api.searchDynamicPublishTopics(query.ifBlank { null })
                    check(response.code == 0) { response.message.ifBlank { "搜索话题失败" } }
                    topics = response.data?.topic_items.orEmpty()
                }
            }
        } catch (cancelled: CancellationException) { throw cancelled } catch (failure: Exception) {
            loadError = failure.message ?: "加载失败"
        } finally { loading = false }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            AppTextButton(onClick = { panel = if (panel == "emoji") "" else "emoji"; query = "" }, enabled = enabled) { AppText("表情") }
            AppTextButton(onClick = { panel = if (panel == "mention") "" else "mention"; query = "" }, enabled = enabled) { AppText("@ 用户") }
            AppTextButton(onClick = { panel = if (panel == "topic") "" else "topic"; query = "" }, enabled = enabled) { AppText("# 话题") }
        }
        if (panel == "emoji") ChatEmotePanel(packages, panelHeight = 180.dp, loading = loading, error = loadError, onRetry = { retry++ }, onChoose = { if (enabled) onAppend(it) })
        if (panel == "mention" || panel == "topic") {
            AppSearchField(query = query, onQueryChange = { query = it }, placeholder = if (panel == "mention") "搜索用户" else "搜索话题", modifier = Modifier.fillMaxWidth())
            if (loading) AppText("搜索中…")
            if (loadError != null) {
                AppText(loadError.orEmpty())
                AppTextButton(onClick = { retry++ }, enabled = !loading) { AppText("重试") }
            } else if (!loading && (if (panel == "mention") users.isEmpty() else topics.isEmpty())) AppText("没有匹配结果")
            AppLazyColumn(Modifier.fillMaxWidth().heightIn(max = 160.dp)) {
                if (panel == "mention") items(users, key = { it.uid }) { user ->
                    AppTextButton(onClick = { onMention(user); panel = "" }, enabled = enabled) { AppText(user.name) }
                } else items(topics, key = { it.id }) { topic ->
                    AppTextButton(onClick = { onTopic(topic); panel = "" }, enabled = enabled) { AppText(topic.name) }
                }
            }
        }
    }
}
