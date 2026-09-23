package com.android.purebilibili.feature.dynamic.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.purebilibili.core.ui.AppModalBottomSheet
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppListItem
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.SessionItem
import com.android.purebilibili.data.model.response.SessionAccountInfo
import com.android.purebilibili.data.repository.MessageRepository
import com.android.purebilibili.data.repository.MessageShareGrpcRepository
import com.android.purebilibili.feature.message.InboxUserInfoResolver
import com.android.purebilibili.feature.message.MessageUserInfoLoader
import com.android.purebilibili.feature.message.UserBasicInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal fun buildDynamicShareCardContent(item: DynamicItem): String {
    val title = item.modules.module_dynamic?.major?.opus?.title.orEmpty()
    val body = item.modules.module_dynamic?.desc?.text
        .orEmpty()
        .ifBlank { item.modules.module_dynamic?.major?.opus?.summary?.text.orEmpty() }
        .take(120)
    val author = item.modules.module_author
    val isDynamic = item.basic?.comment_type == 17
    val sourceId = if (isDynamic) item.id_str else item.basic?.rid_str.orEmpty().ifBlank { item.id_str }
    val thumb = if (isDynamic) {
        author?.face.orEmpty()
    } else {
        item.modules.module_dynamic?.major?.opus?.pics?.firstOrNull()?.url.orEmpty()
    }
    return buildJsonObject {
        put("id", sourceId)
        put("title", title.ifBlank { body })
        put("headline", "")
        put("source", if (isDynamic) 11 else 2)
        if (thumb.isNotBlank()) put("thumb", thumb)
        put("author", author?.name.orEmpty())
        put("author_id", author?.mid?.toString().orEmpty())
    }.toString()
}

internal data class DynamicShareSessionPresentation(
    val name: String,
    val avatarUrl: String,
    val resolvingUserInfo: Boolean,
)

internal fun resolveDynamicShareSessionPresentation(
    session: SessionItem,
    userInfo: UserBasicInfo?,
    resolvingUserInfo: Boolean,
): DynamicShareSessionPresentation {
    if (session.session_type != 1) {
        return DynamicShareSessionPresentation(
            name = session.group_name.trim().ifBlank { "群聊 ${session.talker_id}" },
            avatarUrl = session.group_cover,
            resolvingUserInfo = false,
        )
    }
    return DynamicShareSessionPresentation(
        name = if (resolvingUserInfo) {
            session.account_info?.name.orEmpty().trim().ifBlank { "正在获取用户资料…" }
        } else {
            InboxUserInfoResolver.resolveDisplayName(userInfo, session)
        },
        avatarUrl = InboxUserInfoResolver.resolveDisplayAvatar(userInfo, session),
        resolvingUserInfo = resolvingUserInfo,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DynamicShareToMessageDialog(
    item: DynamicItem,
    onDismiss: () -> Unit,
    onResult: (Boolean, String) -> Unit,
) {
    var sessions by remember(item.id_str) { mutableStateOf<List<SessionItem>>(emptyList()) }
    var loading by remember(item.id_str) { mutableStateOf(true) }
    var loadError by remember(item.id_str) { mutableStateOf<String?>(null) }
    var sendingTo by remember(item.id_str) { mutableStateOf<Long?>(null) }
    var userInfoMap by remember(item.id_str) { mutableStateOf<Map<Long, UserBasicInfo>>(emptyMap()) }
    var resolvingUserMids by remember(item.id_str) { mutableStateOf<Set<Long>>(emptySet()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(item.id_str) {
        val shareTargets = MessageShareGrpcRepository.getShareTargets(size = 5).getOrNull().orEmpty()
        if (shareTargets.isNotEmpty()) {
            sessions = shareTargets.map { target ->
                SessionItem(
                    talker_id = target.mid,
                    session_type = 1,
                    account_info = SessionAccountInfo(
                        name = target.name,
                        pic_url = target.avatarUrl,
                    ),
                )
            }
            loading = false
        } else {
            MessageRepository.getSessions(size = 30).fold(
                onSuccess = {
                    sessions = it.session_list.orEmpty().filter { session ->
                        session.session_type == 1 && session.talker_id > 0L
                    }
                    loading = false
                },
                onFailure = {
                    loadError = it.message ?: "加载会话失败"
                    loading = false
                },
            )
        }
        if (sessions.isNotEmpty()) {
            userInfoMap = sessions.mapNotNull { session ->
                val account = session.account_info ?: return@mapNotNull null
                session.talker_id to UserBasicInfo(
                    mid = session.talker_id,
                    name = account.name,
                    face = account.avatarUrl,
                )
            }.toMap()
        }
        val missingMids = sessions
            .filter { session ->
                session.session_type == 1 &&
                    session.talker_id > 0L &&
                    InboxUserInfoResolver.shouldFetchSessionUserInfo(session, userInfoMap)
            }
            .map(SessionItem::talker_id)
            .distinct()
        resolvingUserMids = missingMids.toSet()
        missingMids.chunked(12).forEach { batch ->
            val fetched = coroutineScope {
                batch.map { mid -> async { mid to MessageUserInfoLoader.fetch(mid) } }.awaitAll()
            }
            userInfoMap = userInfoMap + fetched.mapNotNull { (mid, info) ->
                info?.let { mid to it }
            }
            resolvingUserMids = resolvingUserMids - batch.toSet()
        }
    }
    AppModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    horizontal = AppSpacingTokens.Large,
                    vertical = AppSpacingTokens.Small,
                ),
            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
        ) {
            AppText("分享至消息", style = MaterialTheme.typography.headlineSmall)
            AppText("选择最近联系人，点击后立即发送动态卡片", style = MaterialTheme.typography.bodySmall)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall),
            ) {
                when {
                    loading -> AppText("正在加载最近联系人…")
                    loadError != null -> AppText(loadError.orEmpty())
                    sessions.isEmpty() -> AppText("暂无可分享的最近联系人")
                    else -> sessions.forEach { session ->
                        val presentation = resolveDynamicShareSessionPresentation(
                            session = session,
                            userInfo = userInfoMap[session.talker_id],
                            resolvingUserInfo = session.talker_id in resolvingUserMids,
                        )
                        DynamicShareSessionRow(
                            presentation = presentation,
                            sending = sendingTo == session.talker_id,
                            enabled = sendingTo == null,
                            onClick = {
                                sendingTo = session.talker_id
                                scope.launch {
                                    MessageShareGrpcRepository.sendDynamicShare(
                                        receiverId = session.talker_id,
                                        content = buildDynamicShareCardContent(item),
                                    ).fold(
                                        onSuccess = {
                                            onResult(true, "已分享给${presentation.name}")
                                            onDismiss()
                                        },
                                        onFailure = {
                                            sendingTo = null
                                            onResult(false, it.message ?: "分享失败")
                                        },
                                    )
                                }
                            },
                        )
                    }
                }
            }
            AppButton(
                onClick = onDismiss,
                enabled = sendingTo == null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AppText("取消")
            }
        }
    }
}

@Composable
internal fun DynamicShareSessionRow(
    presentation: DynamicShareSessionPresentation,
    sending: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    AppListItem(
        headlineContent = { AppText(presentation.name) },
        supportingContent = when {
            sending -> ({ AppText("发送中…") })
            presentation.resolvingUserInfo -> ({ AppText("昵称和头像加载中") })
            else -> null
        },
        leadingContent = presentation.avatarUrl.takeIf(String::isNotBlank)?.let { avatar ->
            {
                AsyncImage(
                    model = avatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }
        },
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
    )
}
