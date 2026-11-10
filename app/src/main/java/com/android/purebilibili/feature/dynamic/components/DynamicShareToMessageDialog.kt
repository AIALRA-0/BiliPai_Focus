package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.core.ui.components.appActionClickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.purebilibili.core.ui.components.AppListItem
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.SessionItem
import com.android.purebilibili.feature.message.InboxUserInfoResolver
import com.android.purebilibili.feature.message.UserBasicInfo
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
    com.android.purebilibili.feature.share.ContentMessageShareSheet(
        payload = com.android.purebilibili.feature.share.buildDynamicContentSharePayload(item),
        onDismiss = onDismiss,
        onSuccess = { onResult(true, "已分享给 $it 人") },
    )
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
        modifier = Modifier.appActionClickable(enabled = enabled, onClick = onClick),
    )
}
