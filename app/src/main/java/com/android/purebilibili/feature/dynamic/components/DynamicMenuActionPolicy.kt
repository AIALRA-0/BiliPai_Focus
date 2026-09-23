package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicCreatePic
import com.android.purebilibili.data.model.response.DynamicPublishDraft
import com.android.purebilibili.data.model.response.DynamicPublishMention
import com.android.purebilibili.data.model.response.DynamicPublishTopic

/**
 * 更多菜单的管理动作与参数解析（对齐 BiliPai 作者区 morePanel 的
 * 置顶 / 可见范围 / 评论互动设置 / 不感兴趣 / 屏蔽作者）。
 */
sealed interface DynamicManageAction {
    data class NotInterested(val dynamicId: String) : DynamicManageAction
    data class ToggleTop(val dynamicId: String, val isCurrentlyTop: Boolean) : DynamicManageAction
    data class SetVisibility(val dynamicId: String, val dynType: Int, val isPrivate: Boolean) : DynamicManageAction
    data class SetReplySubject(val oid: Long, val replyType: Int, val action: Int) : DynamicManageAction
    data class BlockAuthor(
        val authorMid: Long,
        val authorName: String,
        val authorFace: String,
    ) : DynamicManageAction
    data class Report(val dynamicId: String, val authorMid: Long) : DynamicManageAction
    data class Edit(val dynamicId: String, val initialDraft: DynamicPublishDraft) : DynamicManageAction
}

internal fun dispatchDynamicManageAction(
    action: DynamicManageAction,
    onReport: (DynamicManageAction.Report) -> Unit,
    onEdit: (DynamicManageAction.Edit) -> Unit,
    onNotInterested: (DynamicManageAction.NotInterested) -> Unit,
    onOther: (DynamicManageAction) -> Unit,
) {
    when (action) {
        is DynamicManageAction.Report -> onReport(action)
        is DynamicManageAction.Edit -> onEdit(action)
        is DynamicManageAction.NotInterested -> onNotInterested(action)
        else -> onOther(action)
    }
}

internal data class DynamicMenuCapabilities(
    val isOwnDynamic: Boolean,
    val canToggleTop: Boolean,
    val canSetVisibility: Boolean,
    val canManageComments: Boolean,
    val canEdit: Boolean,
    val canBlockAuthor: Boolean,
    val canReport: Boolean,
    val isPrivate: Boolean,
)

internal fun resolveDynamicMenuCapabilities(
    item: DynamicItem,
    currentUserMid: Long?,
): DynamicMenuCapabilities {
    val menuItems = item.modules.module_more?.three_point_items.orEmpty()
    val types = menuItems.mapTo(mutableSetOf()) { it.type }
    val authorMid = item.modules.module_author?.mid ?: 0L
    val hasAuthorOnlyServerAction = types.any {
        it == "THREE_POINT_EDIT" ||
            it == "THREE_POINT_PRIVATE" ||
            it == "THREE_POINT_DELETE" ||
            it == "THREE_POINT_TOP"
    }
    val isOwnDynamic = currentUserMid != null && currentUserMid > 0L && authorMid == currentUserMid ||
        hasAuthorOnlyServerAction
    val privateStatus = menuItems
        .firstOrNull { it.type == "THREE_POINT_PRIVATE" }
        ?.params
        ?.status
        ?: 0

    return DynamicMenuCapabilities(
        isOwnDynamic = isOwnDynamic,
        canToggleTop = isOwnDynamic,
        canSetVisibility = isOwnDynamic && types.contains("THREE_POINT_PRIVATE"),
        canManageComments = isOwnDynamic,
        canEdit = isOwnDynamic && types.contains("THREE_POINT_EDIT"),
        canBlockAuthor = !isOwnDynamic && authorMid > 0L,
        canReport = !isOwnDynamic && (types.isEmpty() || types.contains("THREE_POINT_REPORT")),
        isPrivate = privateStatus == 1,
    )
}

internal data class DynamicReportReason(
    val type: Int,
    val label: String
)

internal fun resolveDynamicReportReasons(): List<DynamicReportReason> = listOf(
    DynamicReportReason(4, "垃圾广告"),
    DynamicReportReason(8, "引战"),
    DynamicReportReason(1, "色情"),
    DynamicReportReason(5, "人身攻击"),
    DynamicReportReason(3, "违法信息"),
    DynamicReportReason(9, "涉政谣言"),
    DynamicReportReason(10, "涉社会事件谣言"),
    DynamicReportReason(12, "虚假不实信息"),
    DynamicReportReason(13, "违法信息外链"),
    DynamicReportReason(0, "其他"),
)

internal fun resolveDynamicEditDraft(item: DynamicItem): DynamicPublishDraft {
    val dynamic = item.modules.module_dynamic
    val opus = dynamic?.major?.opus
    val richNodes = dynamic?.desc?.rich_text_nodes.orEmpty()
        .ifEmpty { opus?.summary?.rich_text_nodes.orEmpty() }
    val images = buildList {
        dynamic?.major?.draw?.items.orEmpty().forEach { image ->
            if (image.src.isNotBlank()) {
                add(
                    DynamicCreatePic(
                        img_src = image.src,
                        img_width = image.width,
                        img_height = image.height,
                        img_size = 0f,
                    )
                )
            }
        }
        opus?.pics.orEmpty().forEach { image ->
            if (image.url.isNotBlank()) {
                add(
                    DynamicCreatePic(
                        img_src = image.url,
                        img_width = image.width,
                        img_height = image.height,
                        img_size = image.size.toFloat(),
                    )
                )
            }
        }
    }.distinctBy { it.img_src }
    val additional = dynamic?.additional
    val isPrivate = item.modules.module_more?.three_point_items.orEmpty()
        .firstOrNull { it.type == "THREE_POINT_PRIVATE" }
        ?.params
        ?.status == 1
    return DynamicPublishDraft(
        text = dynamic?.desc?.text.orEmpty().ifBlank { opus?.summary?.text.orEmpty() },
        title = opus?.title.orEmpty(),
        imageUris = images.map(DynamicCreatePic::img_src),
        voteId = additional?.vote?.vote_id ?: 0L,
        voteTitle = additional?.vote?.desc.orEmpty(),
        reserveId = additional?.reserve?.rid ?: 0L,
        private = isPrivate,
        mentions = richNodes.mapNotNull { node ->
            node.takeIf { it.type.contains("AT", ignoreCase = true) }
                ?.rid
                ?.toLongOrNull()
                ?.takeIf { it > 0L }
                ?.let { DynamicPublishMention(uid = it, name = node.text.trim().removePrefix("@")) }
        }.distinctBy(DynamicPublishMention::uid),
        emotes = richNodes.mapNotNull { node ->
            node.takeIf { it.type.contains("EMOJI", ignoreCase = true) }
                ?.let { it.text.ifBlank { it.orig_text }.ifBlank { it.emoji?.text.orEmpty() } }
                ?.takeIf(String::isNotBlank)
        }.distinct(),
        topic = dynamic?.topic?.takeIf { it.id > 0L }?.let {
            DynamicPublishTopic(id = it.id, name = it.name)
        },
        existingImages = images,
    )
}

// x/v2/reply/subject/modify 的 action 取值
internal const val DYNAMIC_REPLY_ACTION_ENABLE_SELECTION = 1 // 开启评论精选
internal const val DYNAMIC_REPLY_ACTION_DISABLE_SELECTION = 2 // 停止评论精选
internal const val DYNAMIC_REPLY_ACTION_CLOSE_REPLY = 3 // 关闭评论
internal const val DYNAMIC_REPLY_ACTION_OPEN_REPLY = 4 // 恢复评论

internal fun resolveDynamicPinnedMenuLabel(isCurrentlyTop: Boolean): String =
    if (isCurrentlyTop) "取消置顶" else "置顶"

internal fun resolveDynamicVisibilityMenuLabel(isPrivate: Boolean): String =
    if (isPrivate) "设为公开" else "设为仅自己可见"

// x/dynamic/feed/dyn/private_pub_setting 的 action 取值
internal fun resolveDynamicVisibilityAction(isPrivate: Boolean): String =
    if (isPrivate) "private_pub" else "public_pub"

internal fun resolveDynamicReplySelectionAction(isCurrentlyEnabled: Boolean): Int =
    if (isCurrentlyEnabled) DYNAMIC_REPLY_ACTION_DISABLE_SELECTION else DYNAMIC_REPLY_ACTION_ENABLE_SELECTION

internal fun resolveDynamicReplyOpenAction(isCurrentlyEnabled: Boolean): Int =
    if (isCurrentlyEnabled) DYNAMIC_REPLY_ACTION_CLOSE_REPLY else DYNAMIC_REPLY_ACTION_OPEN_REPLY

// 评论互动目标：评论区 oid/type，来自 basic.comment_id_str / comment_type。
internal fun resolveDynamicReplySubjectOid(item: DynamicItem): Long? =
    item.basic?.comment_id_str?.toLongOrNull()?.takeIf { it > 0L }

internal fun resolveDynamicReplySubjectType(item: DynamicItem): Int =
    item.basic?.comment_type ?: 0

// 可见范围接口的 object_id 参数：{"dyn_id":"...","dyn_type":N}
internal fun buildDynamicVisibilityObjectId(dynamicId: String, dynType: Int): String =
    """{"dyn_id":"$dynamicId","dyn_type":$dynType}"""

// 动态类型数值（dyn_type）：优先取 three_point_items 下发的参数，无则回退 0。
internal fun resolveDynamicDynType(item: DynamicItem): Int =
    item.modules.module_more?.three_point_items
        ?.firstNotNullOfOrNull { itemParams -> itemParams.params?.dyn_type?.takeIf { it > 0 } }
        ?: 0
