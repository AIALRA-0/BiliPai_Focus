// 文件路径: feature/dynamic/components/DynamicCard.kt
package com.android.purebilibili.feature.dynamic.components

import coil3.network.NetworkHeaders
import coil3.network.httpHeaders

import coil3.request.crossfade
import com.android.purebilibili.core.ui.components.AppContentCard
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppListItem
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppHorizontalDivider

import com.android.purebilibili.core.ui.UserAvatarCornerMarkBadge
import com.android.purebilibili.core.ui.resolveUserAvatarCornerMark
import com.android.purebilibili.core.ui.AppChromeSizeTokens
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppSurfaceTokens

import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.transition.LocalDynamicImagePreviewTextVisible

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
//  Material Icons
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppWindowAction
import com.android.purebilibili.core.ui.components.AppWindowActionMenu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.TextUnit
import coil3.ImageLoader
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.core.util.BilibiliNavigationTarget
import com.android.purebilibili.core.util.BilibiliNavigationTargetParser
import androidx.compose.foundation.text.selection.SelectionContainer
import com.android.purebilibili.core.ui.common.TextSelectionBottomSheet
import com.android.purebilibili.core.ui.common.TextSelectionPolicy
import com.android.purebilibili.core.ui.common.detectTapWithSelectionFriendly
import com.android.purebilibili.core.ui.rememberAppMoreIcon
import com.android.purebilibili.core.ui.rememberAppVisibilityOffIcon
import com.android.purebilibili.core.ui.rememberAppWarningIcon
import com.android.purebilibili.core.ui.rememberAppChevronDownIcon
import com.android.purebilibili.core.ui.rememberAppChevronUpIcon
import com.android.purebilibili.core.ui.rememberAppCommentIcon
import com.android.purebilibili.core.ui.rememberAppVisibilityOnIcon
import com.android.purebilibili.core.ui.rememberAppShareIcon
import com.android.purebilibili.core.theme.resolveAccessibleContainerColors
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.AppDialogAction
import com.android.purebilibili.core.ui.rememberAppHistoryIcon
import com.android.purebilibili.core.ui.rememberAppDeleteIcon
import com.android.purebilibili.core.ui.rememberAppLinkIcon
import com.android.purebilibili.data.model.response.DynamicDesc
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DrawItem
import com.android.purebilibili.data.model.response.ReplyInteractionData
import com.android.purebilibili.feature.dynamic.resolveDynamicActionButtonSlotWeight
import com.android.purebilibili.feature.dynamic.resolveDynamicActionButtonSpacing
import com.android.purebilibili.feature.dynamic.resolveDynamicCardContentPadding
import com.android.purebilibili.feature.dynamic.resolveDynamicCardOuterPadding
import com.android.purebilibili.feature.dynamic.resolveDynamicLikeState
import com.android.purebilibili.data.model.response.DynamicStatModule
import com.android.purebilibili.data.model.response.DynamicType
import com.android.purebilibili.data.model.response.OpusContentBlock
import com.android.purebilibili.data.repository.DynamicRepository
import com.android.purebilibili.feature.dynamic.DynamicDeleteAction
import com.android.purebilibili.feature.dynamic.resolveDynamicDeleteAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Immutable
data class DynamicCardNavigationActions(
    val onVideoClick: (String) -> Unit,
    val onUserClick: (Long) -> Unit,
    val onBangumiClick: (Long, Long) -> Unit = { _, _ -> },
    val onTopicClick: (Long) -> Unit = {},
    val onTopicKeywordClick: ((String) -> Unit)? = null,
    val onLiveClick: (roomId: Long, title: String, uname: String) -> Unit = { _, _, _ -> },
    val onMusicClick: ((Long) -> Unit)? = null,
    val onCollectionClick: ((Long, Long, String, String) -> Unit)? = null,
    val onCourseClick: ((String, String) -> Unit)? = null,
    val onArticleClick: ((articleId: Long, title: String) -> Unit)? = null,
    val onDynamicDetailClick: ((dynamicId: String) -> Unit)? = null,
    val onUnfoldRelatedClick: ((dynamicId: String) -> Unit)? = null,
    val onPrimaryClickOverride: ((DynamicItem) -> Unit)? = null,
)

@Immutable
data class DynamicCardInteractionActions(
    val onCommentClick: (dynamicId: String) -> Unit = {},
    val onRepostClick: (dynamicId: String) -> Unit = {},
    val onLikeClick: (dynamicId: String) -> Unit = {},
    val onLikeClickWithState: ((dynamicId: String, isLiked: Boolean) -> Unit)? = null,
    val onWatchLaterClick: ((aid: Long) -> Unit)? = null,
    val onSaveDynamicClick: (() -> Unit)? = null,
    val onShareToMessageClick: (() -> Unit)? = null,
    val onCheckDynamicClick: (() -> Unit)? = null,
    val onReserveClick: ((DynamicReserveAction, (Result<DynamicReserveResult>) -> Unit) -> Unit)? = null,
    val onDeleteClick: ((DynamicDeleteAction) -> Unit)? = null,
    val onManageAction: (DynamicManageAction) -> Unit = {},
    val onLoadReplyInteractionStatus: ((oid: Long, type: Int, onLoaded: (ReplyInteractionData?) -> Unit) -> Unit)? = null,
)

@Immutable
data class DynamicCardActions(
    val navigation: DynamicCardNavigationActions,
    val interaction: DynamicCardInteractionActions = DynamicCardInteractionActions(),
)

@Immutable
data class DynamicCardPresentation(
    val isDetail: Boolean = false,
    val currentUserMid: Long? = TokenManager.midCache,
    val isLiked: Boolean = false,
    val likeOverride: Boolean? = null,
    val forwardCountDelta: Int = 0,
)

/**
 * 动态卡片 V2。将导航、交互和展示状态分组，避免 Compose/R8 处理超大参数签名。
 */
@Composable
fun DynamicCardV2(
    item: DynamicItem,
    gifImageLoader: ImageLoader,
    actions: DynamicCardActions,
    presentation: DynamicCardPresentation = DynamicCardPresentation(),
) {
    val onVideoClick = actions.navigation.onVideoClick
    val onBangumiClick = actions.navigation.onBangumiClick
    val onUserClick = actions.navigation.onUserClick
    val onTopicClick = actions.navigation.onTopicClick
    val onTopicKeywordClick = actions.navigation.onTopicKeywordClick
    val onLiveClick = actions.navigation.onLiveClick
    val onMusicClick = actions.navigation.onMusicClick
    val onCollectionClick = actions.navigation.onCollectionClick
    val onCourseClick = actions.navigation.onCourseClick
    val onArticleClick = actions.navigation.onArticleClick
    val onDynamicDetailClick = actions.navigation.onDynamicDetailClick
    val onUnfoldRelatedClick = actions.navigation.onUnfoldRelatedClick
    val onPrimaryClickOverride = actions.navigation.onPrimaryClickOverride
    val onCommentClick = actions.interaction.onCommentClick
    val onRepostClick = actions.interaction.onRepostClick
    val onLikeClick = actions.interaction.onLikeClick
    val onLikeClickWithState = actions.interaction.onLikeClickWithState
    val onWatchLaterClick = actions.interaction.onWatchLaterClick
    val onSaveDynamicClick = actions.interaction.onSaveDynamicClick
    val onShareToMessageClick = actions.interaction.onShareToMessageClick
    val onCheckDynamicClick = actions.interaction.onCheckDynamicClick
    val onReserveClick = actions.interaction.onReserveClick
    val onDeleteClick = actions.interaction.onDeleteClick
    val onManageAction = actions.interaction.onManageAction
    val onLoadReplyInteractionStatus = actions.interaction.onLoadReplyInteractionStatus
    val isDetail = presentation.isDetail
    val currentUserMid = presentation.currentUserMid
    val isLiked = presentation.isLiked
    val likeOverride = presentation.likeOverride
    val forwardCountDelta = presentation.forwardCountDelta

    if (!item.visible) return
    val openDynamicDetail = remember(item, onDynamicDetailClick) {
        onDynamicDetailClick?.let { callback ->
            { id: String ->
                DynamicRepository.rememberDynamicDetailSeed(item)
                callback(id)
            }
        }
    }
    val author = item.modules.module_author
    val content = item.modules.module_dynamic
    val stat = item.modules.module_stat
    val effectiveIsLiked = resolveDynamicLikeState(
        localOverride = likeOverride,
        localLiked = isLiked,
        serverLiked = stat?.like?.status,
    )
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val dynamicPreviewTextVisible = LocalDynamicImagePreviewTextVisible.current
    val authorTimeText = remember(author?.pub_time, author?.pub_ts) {
        author?.let {
            resolveDynamicAuthorTimeText(
                pubTime = it.pub_time,
                pubTs = it.pub_ts
            )
        }.orEmpty()
    }
    val opus = content?.major?.opus
    val fullOpusContentBlocks = opus?.let { currentOpus ->
        resolveDynamicOpusPresentationBlocks(opus = currentOpus, isDetail = isDetail)
    }.orEmpty()
    val renderableOpusPics = remember(opus, fullOpusContentBlocks) {
        opus?.let { currentOpus ->
            resolveDynamicOpusPreviewPics(currentOpus, fullOpusContentBlocks)
        }.orEmpty()
    }
    val contentHasImages = content?.major?.draw?.items?.let(::resolveRenderableDrawItems)?.isNotEmpty() == true ||
        renderableOpusPics.isNotEmpty()
    val visibleDynamicDesc = content?.desc?.let { desc ->
        resolveDynamicDescForImages(desc, hasImages = contentHasImages)
    }
    // A detail response can provide rich text blocks without embedding the
    // documented `opus.pics` entries in those blocks. In that case use the
    // image-grid path below so pictures are not hidden after the network
    // response replaces the cached feed item.
    val hasFullOpusDetailContent = opus?.let {
        shouldRenderDynamicOpusBlocksAsFullBody(
            opus = it,
            presentationBlocks = fullOpusContentBlocks,
        )
    } == true
    val type = DynamicType.fromApiValue(item.type)
    val cardClickAction = remember(item) { resolveDynamicCardPrimaryAction(item) }
    val watchLaterAid = remember(item) { resolveDynamicWatchLaterAid(item) }
    val deleteAction = remember(item) { resolveDynamicDeleteAction(item) }
    val menuCapabilities = remember(item, currentUserMid) {
        resolveDynamicMenuCapabilities(item, currentUserMid)
    }
    var pendingDeleteAction by remember(item.id_str) { mutableStateOf<DynamicDeleteAction?>(null) }
    var pendingBlockAuthor by remember(item.id_str) { mutableStateOf<DynamicManageAction.BlockAuthor?>(null) }
    var pendingVoteId by remember(item.id_str) { mutableStateOf<Long?>(null) }
    val resolvedAdditionalCard = remember(content?.additional) {
        resolveDynamicAdditionalCard(content?.additional)
    }
    var additionalCardState by remember(item.id_str, resolvedAdditionalCard) {
        mutableStateOf(resolvedAdditionalCard)
    }
    var reserveSubmitting by remember(item.id_str) { mutableStateOf(false) }
    //  [新增] 评论互动设置弹窗状态
    var showReplyInteractionDialog by remember(item.id_str) { mutableStateOf<ReplyInteractionData?>(null) }
    var replyInteractionOid by remember(item.id_str) { mutableStateOf(0L) }
    var replyInteractionType by remember(item.id_str) { mutableStateOf(0) }
    val isCurrentlyTop = item.modules.module_tag?.text == "置顶"
    val isPrimaryClickEnabled = remember(cardClickAction, onArticleClick, openDynamicDetail, onPrimaryClickOverride) {
        shouldEnableDynamicCardPrimaryClick(
            action = cardClickAction,
            hasArticleClick = onArticleClick != null,
            hasDynamicDetailClick = openDynamicDetail != null,
            hasPrimaryClickOverride = onPrimaryClickOverride != null
        )
    }

    pendingVoteId?.let { voteId ->
        DynamicVoteDialog(
            voteId = voteId,
            dynamicId = item.id_str,
            onDismiss = { pendingVoteId = null }
        )
    }

    pendingDeleteAction?.let { action ->
        AppAlertDialog(
            onDismissRequest = { pendingDeleteAction = null },
            title = { AppText(action.title) },
            text = { AppText(action.content) },
            confirmButton = {
                AppDialogAction(
                    onClick = {
                        pendingDeleteAction = null
                        onDeleteClick?.invoke(action)
                    }
                ) {
                    AppText(action.confirmText, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                AppDialogAction(onClick = { pendingDeleteAction = null }) {
                    AppText(action.cancelText)
                }
            }
        )
    }

    pendingBlockAuthor?.let { action ->
        AppAlertDialog(
            onDismissRequest = { pendingBlockAuthor = null },
            title = { AppText("屏蔽 UP 主") },
            text = { AppText("屏蔽后将不再推荐 ${action.authorName} 的内容，并同步到哔哩哔哩黑名单。") },
            confirmButton = {
                AppDialogAction(
                    onClick = {
                        pendingBlockAuthor = null
                        onManageAction(action)
                    },
                ) { AppText("屏蔽", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                AppDialogAction(onClick = { pendingBlockAuthor = null }) { AppText("取消") }
            },
        )
    }

    //  [新增] 评论互动设置弹窗（评论精选 / 评论开关，对齐 BiliPai）
    showReplyInteractionDialog?.let { interactionData ->
        val selection = interactionData.up_reply_selection
        val reply = interactionData.up_reply
        val selectionEnabled = selection?.status == 1
        val replyEnabled = reply?.status == 1
        AppAlertDialog(
            onDismissRequest = { showReplyInteractionDialog = null },
            title = { AppText("评论互动设置") },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AppShapes.container(ContainerLevel.Chip))
                            .clickable(enabled = selection?.can_modify == true) {
                                showReplyInteractionDialog = null
                                onManageAction(
                                    DynamicManageAction.SetReplySubject(
                                        oid = replyInteractionOid,
                                        replyType = replyInteractionType,
                                        action = resolveDynamicReplySelectionAction(selectionEnabled)
                                    )
                                )
                            }
                            .padding(vertical = AppSpacingTokens.Medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(
                            rememberAppCommentIcon(),
                            contentDescription = null,
                            modifier = Modifier.size(AppSpacingTokens.Large),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(AppSpacingTokens.Small))
                        AppText(
                            if (selectionEnabled) "停止评论精选" else "开启评论精选",
                            color = if (selection?.can_modify == true) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                            }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AppShapes.container(ContainerLevel.Chip))
                            .clickable(enabled = reply?.can_modify == true) {
                                showReplyInteractionDialog = null
                                onManageAction(
                                    DynamicManageAction.SetReplySubject(
                                        oid = replyInteractionOid,
                                        replyType = replyInteractionType,
                                        action = resolveDynamicReplyOpenAction(replyEnabled)
                                    )
                                )
                            }
                            .padding(vertical = AppSpacingTokens.Medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(
                            rememberAppVisibilityOffIcon(),
                            contentDescription = null,
                            modifier = Modifier.size(AppSpacingTokens.Large),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(AppSpacingTokens.Small))
                        AppText(
                            if (replyEnabled) "关闭评论" else "恢复评论",
                            color = if (reply?.can_modify == true) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                AppDialogAction(onClick = { showReplyInteractionDialog = null }) {
                    AppText("取消")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = resolveDynamicCardOuterPadding())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = resolveDynamicCardContentPadding())
                .padding(top = AppSpacingTokens.Medium, bottom = if (isDetail) AppSpacingTokens.None else AppSpacingTokens.Small + AppSpacingTokens.Micro)
                .clickable(enabled = isPrimaryClickEnabled) {
                    dispatchDynamicCardPrimaryClick(
                        item = item,
                        action = cardClickAction,
                        onPrimaryClickOverride = onPrimaryClickOverride,
                        onVideoClick = onVideoClick,
                        onBangumiClick = onBangumiClick,
                        onArticleClick = onArticleClick,
                        onDynamicDetailClick = openDynamicDetail,
                        onUserClick = onUserClick,
                        onLiveClick = onLiveClick
                    )
                }
        ) {
        val context = LocalContext.current
        
        //  用户头部（头像 + 名称 + 时间 + 更多）
        if (author != null) {
            val authorClickMid = remember(item) { resolveDynamicAuthorClickMid(item) }
            val ugcSeason = item.modules.module_dynamic?.major?.ugc_season
            val onAuthorHeaderClick = {
                if (authorClickMid != null) {
                    dispatchDynamicCardPrimaryAction(
                        action = DynamicCardPrimaryAction.OpenUser(authorClickMid),
                        onVideoClick = onVideoClick,
                        onBangumiClick = onBangumiClick,
                        onArticleClick = onArticleClick,
                        onDynamicDetailClick = openDynamicDetail,
                        onUserClick = onUserClick,
                        onLiveClick = onLiveClick
                    )
                } else if (ugcSeason != null && ugcSeason.id > 0L && onCollectionClick != null) {
                    onCollectionClick(ugcSeason.id, ugcSeason.mid, ugcSeason.title, ugcSeason.jump_url)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像
                DynamicAuthorFace(
                    faceUrl = author.face,
                    officialType = author.official_verify?.type,
                    vipStatus = author.vip?.status,
                    faceSize = AppSpacingTokens.DoubleExtraLarge + AppSpacingTokens.Small,
                    modifier = Modifier
                        .size(AppChromeSizeTokens.MinimumTouchTarget)
                        .semantics { contentDescription = "查看${author.name}的个人主页" }
                        .clickable(enabled = authorClickMid != null || (ugcSeason != null && ugcSeason.id > 0L && onCollectionClick != null)) {
                            onAuthorHeaderClick()
                        },
                )
                
                Spacer(modifier = Modifier.width(AppSpacingTokens.Medium))
                
                Column(modifier = Modifier.weight(1f)) {
                    AppText(
                        author.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        color = if (author.vip?.status == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable(enabled = authorClickMid != null || (ugcSeason != null && ugcSeason.id > 0L && onCollectionClick != null)) {
                            onAuthorHeaderClick()
                        }
                    )
                    AppText(
                        authorTimeText,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                    )
                }
                
                //  置顶标（module_tag：B 站固定返回 "置顶"，对齐 BiliPai 作者区头部样式）
                if (shouldShowDynamicPinnedTag(item.modules.module_tag)) {
                    Box(
                        modifier = Modifier
                            .padding(start = AppSpacingTokens.ExtraSmall)
                            .border(
                                width = AppSurfaceTokens.OutlineWidth,
                                color = MaterialTheme.colorScheme.primary,
                                shape = AppShapes.container(ContainerLevel.Tag)
                            )
                            .padding(
                                horizontal = AppSpacingTokens.ExtraSmall,
                                vertical = AppSpacingTokens.Micro
                            )
                    ) {
                        AppText(
                            item.modules.module_tag?.text.orEmpty(),
                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                val moreIcon = rememberAppMoreIcon()
                val linkIcon = rememberAppLinkIcon()
                val shareIcon = rememberAppShareIcon()
                val historyIcon = rememberAppHistoryIcon()
                val deleteIcon = rememberAppDeleteIcon()
                val visibilityOffIcon = rememberAppVisibilityOffIcon()
                val chevronUpIcon = rememberAppChevronUpIcon()
                val visibilityOnIcon = rememberAppVisibilityOnIcon()
                val commentIcon = rememberAppCommentIcon()
                val warningIcon = rememberAppWarningIcon()
                val errorTint = MaterialTheme.colorScheme.error
                AppWindowActionMenu(
                    groups = listOf(
                        buildList {
                            add(
                                AppWindowAction(
                                    label = "复制链接",
                                    icon = linkIcon,
                                    onClick = {
                                        val dynamicUrl = "https://t.bilibili.com/${item.id_str}"
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                            as android.content.ClipboardManager
                                        clipboard.setPrimaryClip(
                                            android.content.ClipData.newPlainText("动态链接", dynamicUrl)
                                        )
                                        android.widget.Toast.makeText(
                                            context,
                                            "已复制链接",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                )
                            )
                            add(
                                AppWindowAction(
                                    label = "分享动态",
                                    icon = shareIcon,
                                    onClick = {
                                        val dynamicUrl = "https://t.bilibili.com/${item.id_str}"
                                        val descText = content?.desc?.text?.take(100).orEmpty()
                                        val shareText = if (descText.isNotBlank()) {
                                            "$descText\n$dynamicUrl"
                                        } else {
                                            "分享动态\n$dynamicUrl"
                                        }
                                        val shareIntent = android.content.Intent(
                                            android.content.Intent.ACTION_SEND
                                        ).apply {
                                            this.type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(
                                            android.content.Intent.createChooser(shareIntent, "分享动态")
                                        )
                                    },
                                )
                            )
                            if (watchLaterAid != null && onWatchLaterClick != null) {
                                add(
                                    AppWindowAction(
                                        label = "稍后再看",
                                        icon = historyIcon,
                                        onClick = { onWatchLaterClick(watchLaterAid) },
                                    )
                                )
                            }
                            if (onSaveDynamicClick != null) {
                                add(
                                    AppWindowAction(
                                        label = "保存动态",
                                        icon = historyIcon,
                                        onClick = onSaveDynamicClick,
                                    )
                                )
                            }
                            val canShareToMessage = item.basic?.comment_type in setOf(11, 17) &&
                                item.modules.module_author?.mid != null
                            if (onShareToMessageClick != null && canShareToMessage) {
                                add(
                                    AppWindowAction(
                                        label = "分享至消息",
                                        icon = shareIcon,
                                        onClick = onShareToMessageClick,
                                    )
                                )
                            }
                            if (onCheckDynamicClick != null && menuCapabilities.isOwnDynamic) {
                                add(
                                    AppWindowAction(
                                        label = "检查动态",
                                        icon = warningIcon,
                                        onClick = onCheckDynamicClick,
                                    )
                                )
                            }
                        },
                        buildList {
                            if (!menuCapabilities.isOwnDynamic) {
                                add(
                                    AppWindowAction(
                                        label = "不感兴趣",
                                        icon = visibilityOffIcon,
                                        onClick = {
                                            onManageAction(DynamicManageAction.NotInterested(item.id_str))
                                        },
                                    )
                                )
                            }
                            if (menuCapabilities.canToggleTop) {
                                add(
                                    AppWindowAction(
                                        label = resolveDynamicPinnedMenuLabel(isCurrentlyTop),
                                        icon = chevronUpIcon,
                                        onClick = {
                                            onManageAction(
                                                DynamicManageAction.ToggleTop(item.id_str, isCurrentlyTop)
                                            )
                                        },
                                    )
                                )
                            }
                            if (menuCapabilities.canSetVisibility) {
                                add(
                                    AppWindowAction(
                                        label = resolveDynamicVisibilityMenuLabel(
                                            isPrivate = menuCapabilities.isPrivate
                                        ),
                                        icon = visibilityOnIcon,
                                        onClick = {
                                            onManageAction(
                                                DynamicManageAction.SetVisibility(
                                                    dynamicId = item.id_str,
                                                    dynType = resolveDynamicDynType(item),
                                                    isPrivate = !menuCapabilities.isPrivate
                                                )
                                            )
                                        },
                                    )
                                )
                            }
                            if (menuCapabilities.canManageComments) {
                                add(
                                    AppWindowAction(
                                        label = "评论互动设置",
                                        icon = commentIcon,
                                        onClick = {
                                            val oid = resolveDynamicReplySubjectOid(item)
                                            if (oid == null || onLoadReplyInteractionStatus == null) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "该动态暂不支持互动设置",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                onLoadReplyInteractionStatus(
                                                    oid,
                                                    resolveDynamicReplySubjectType(item)
                                                ) { data ->
                                                    if (data == null) {
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "获取互动设置失败",
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                    } else {
                                                        replyInteractionOid = oid
                                                        replyInteractionType = resolveDynamicReplySubjectType(item)
                                                        showReplyInteractionDialog = data
                                                    }
                                                }
                                            }
                                        },
                                    )
                                )
                            }
                            if (menuCapabilities.canBlockAuthor) {
                                add(
                                    AppWindowAction(
                                        label = "屏蔽该 UP 主",
                                        icon = visibilityOffIcon,
                                        onClick = {
                                            pendingBlockAuthor = DynamicManageAction.BlockAuthor(
                                                authorMid = author?.mid ?: 0L,
                                                authorName = author?.name.orEmpty().ifBlank { "该用户" },
                                                authorFace = author?.face.orEmpty(),
                                            )
                                        },
                                    )
                                )
                            }
                        },
                        buildList {
                            if (menuCapabilities.canEdit) {
                                add(
                                    AppWindowAction(
                                        label = "编辑动态",
                                        onClick = {
                                            onManageAction(
                                                DynamicManageAction.Edit(
                                                    dynamicId = item.id_str,
                                                    initialDraft = resolveDynamicEditDraft(item)
                                                )
                                            )
                                        },
                                    )
                                )
                            }
                            if (deleteAction != null && onDeleteClick != null) {
                                add(
                                    AppWindowAction(
                                        label = deleteAction.label,
                                        icon = deleteIcon,
                                        iconTint = errorTint,
                                        onClick = { pendingDeleteAction = deleteAction },
                                    )
                                )
                            }
                            if (menuCapabilities.canReport) {
                                add(
                                    AppWindowAction(
                                        label = "举报",
                                        icon = warningIcon,
                                        iconTint = errorTint,
                                        onClick = {
                                            onManageAction(
                                                DynamicManageAction.Report(
                                                    dynamicId = item.id_str,
                                                    authorMid = author?.mid ?: 0L
                                                )
                                            )
                                        },
                                    )
                                )
                            }
                        },
                    ),
                ) {
                    AppIcon(
                        moreIcon,
                        contentDescription = "更多",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
        }
        
        //  风险提示条（module_dispute：如“视频内含有危险行为，请勿模仿”，点击打开 jump_url）
        item.modules.module_dispute?.let { dispute ->
            if (shouldShowDynamicDispute(dispute)) {
                val disputeColors = resolveAccessibleContainerColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    fallbackContentColors = listOf(
                        MaterialTheme.colorScheme.onSurface,
                        MaterialTheme.colorScheme.onBackground,
                    ),
                )
                val disputeClickModifier = if (dispute.jump_url.isNotBlank()) {
                    Modifier.clickable {
                        val target = if (dispute.jump_url.startsWith("//")) {
                            "https:${dispute.jump_url}"
                        } else {
                            dispute.jump_url
                        }
                        uriHandler.openUri(target)
                    }
                } else {
                    Modifier
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = AppSpacingTokens.Medium)
                        .clip(AppShapes.container(ContainerLevel.Chip))
                        .background(disputeColors.containerColor)
                        .then(disputeClickModifier)
                        .padding(
                            horizontal = AppSpacingTokens.Small,
                            vertical = AppSpacingTokens.Small
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(
                        rememberAppWarningIcon(),
                        contentDescription = null,
                        modifier = Modifier.size(AppSpacingTokens.Small + AppSpacingTokens.Micro),
                        tint = disputeColors.contentColor
                    )
                    Spacer(modifier = Modifier.width(AppSpacingTokens.ExtraSmall))
                    AppText(
                        dispute.title.ifBlank { dispute.desc },
                        fontSize = MaterialTheme.typography.labelMedium.fontSize,
                        color = disputeColors.contentColor
                    )
                }
            }
        }

        content?.topic?.takeIf { it.name.isNotBlank() }?.let { topic ->
            DynamicTopicLabel(
                topicName = topic.name,
                onClick = {
                    val kw = topic.name.trim().removePrefix("#").removeSuffix("#").trim()
                    if (topic.id > 0L) {
                        onTopicClick(topic.id)
                    } else if (onTopicKeywordClick != null && kw.isNotEmpty()) {
                        onTopicKeywordClick(kw)
                    } else if (kw.isNotEmpty()) {
                        val searchUrl = "bilibili://search?keyword=" + java.net.URLEncoder.encode(kw, java.nio.charset.StandardCharsets.UTF_8.name())
                        val inAppIntent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(searchUrl)
                        ).setPackage(context.packageName)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(inAppIntent) }
                    }
                },
                modifier = Modifier.padding(bottom = AppSpacingTokens.ExtraSmall),
            )
        }

        // 动态标题 (Opus / 专栏 / 带标题图文，统一定位在正文与媒体卡片最上方，对齐 PiliPlus)
        val dynamicCardTitle = remember(opus?.title, content?.major?.article?.title) {
            resolveDynamicHeadlineTitle(
                opus = opus,
                article = content?.major?.article
            )
        }
        if (dynamicCardTitle != null) {
            AppText(
                text = dynamicCardTitle,
                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (isDetail) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = AppSpacingTokens.Small)
            )
        }

        // 失效/删除动态占位提示（对齐 PiliPlus 的 noneWidget 处理）
        val isNoneMajor = content?.major?.type == "MAJOR_TYPE_NONE" || item.type == "DYNAMIC_TYPE_NONE"
        if (isNoneMajor) {
            val tips = content?.major?.none?.tips?.trim()?.takeIf { it.isNotEmpty() } ?: "该动态已失效或被删除"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppSpacingTokens.Small)
                    .clip(AppShapes.container(ContainerLevel.Chip))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = AppSpacingTokens.Medium, vertical = AppSpacingTokens.Small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(
                    rememberAppWarningIcon(),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(AppSpacingTokens.Small))
                AppText(
                    text = tips,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        //  动态内容文字（支持@高亮 / 表情）；优先可渲染表情的 desc 或 opus summary
        val visibleOpusSummaryDescForBody = remember(opus?.summary, renderableOpusPics) {
            val currentOpus = opus ?: return@remember null
            currentOpus.summary?.let { summary ->
                resolveDynamicOpusSummaryDescForImages(
                    text = summary.text,
                    richTextNodes = summary.rich_text_nodes,
                    hasImages = renderableOpusPics.isNotEmpty()
                )
            }
        }
        val preferredBodyDesc = resolvePreferredDynamicDesc(
            primary = visibleDynamicDesc,
            fallback = visibleOpusSummaryDescForBody
        )
        val dynamicCardEmoteMap = remember(content?.desc, opus?.summary, preferredBodyDesc, fullOpusContentBlocks) {
            buildMap {
                putAll(collectDynamicEmojiUrlMap(content?.desc?.rich_text_nodes.orEmpty()))
                putAll(collectDynamicEmojiUrlMap(opus?.summary?.rich_text_nodes.orEmpty()))
                putAll(collectDynamicEmojiUrlMap(preferredBodyDesc?.rich_text_nodes.orEmpty()))
                fullOpusContentBlocks.forEach { block ->
                    if (block is OpusContentBlock.Text) {
                        putAll(collectDynamicEmojiUrlMap(block.richTextNodes))
                    }
                }
            }
        }
        if (!hasFullOpusDetailContent) preferredBodyDesc?.let { desc ->
            if (shouldRenderDynamicRichText(desc)) {
                RichTextContent(
                    desc = desc,
                    onUserClick = onUserClick,
                    onTopicClick = onTopicClick,
                    onTopicKeywordClick = onTopicKeywordClick,
                    onVoteClick = { voteId -> pendingVoteId = voteId },
                    onVideoClick = onVideoClick,
                    onDynamicDetailClick = openDynamicDetail,
                    onBangumiClick = onBangumiClick,
                    onArticleClick = onArticleClick,
                    onLiveClick = onLiveClick,
                    onMusicClick = onMusicClick,
                    extraEmoteUrlMap = dynamicCardEmoteMap,
                )
                Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
            }
        }
        
        //  视频类型动态 - 大图预览
        content?.major?.archive?.let { archive ->
            val playableBvid = resolveArchivePlayableBvid(archive)
            VideoCardLarge(
                archive = archive,
                publishTs = author?.pub_ts ?: 0L,
                cornerBadgeText = resolveDynamicArchiveBadgeLabel(archive),
                onClick = {
                    playableBvid?.let(onVideoClick)
                        ?: openDynamicDetail?.invoke(item.id_str)
                },
                sharedElementKey = com.android.purebilibili.core.ui.transition.videoPlayerSharedElementKey(archive.bvid)
            )
            Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
        }

        content?.major?.pgc?.let { pgc ->
            val bangumiTarget = resolveArchiveBangumiTarget(pgc)
            VideoCardLarge(
                archive = pgc,
                publishTs = author?.pub_ts ?: 0L,
                cornerBadgeText = "番剧",
                onClick = {
                    bangumiTarget?.let { onBangumiClick(it.seasonId, it.epId) }
                        ?: openDynamicDetail?.invoke(item.id_str)
                },
                sharedElementKey = com.android.purebilibili.core.ui.transition.videoPlayerSharedElementKey(
                    pgc.bvid.ifBlank { item.id_str }
                )
            )
            Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
        }
        
        //  图片类型动态（支持GIF + 点击预览）。详情若已拉到完整 opus 正文，不再叠一层九宫格预览。
        content?.major?.draw?.takeIf {
            shouldRenderDynamicDrawGrid(
                hasFullOpusImageContent = hasFullOpusDetailContent &&
                    fullOpusContentBlocks.any {
                        it is OpusContentBlock.Image ||
                            (it is OpusContentBlock.Divider && it.pic != null)
                    },
                opusPics = renderableOpusPics,
            )
        }?.let { draw ->
            var selectedImageIndex by remember { mutableIntStateOf(-1) }
            var sourceRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
            val renderableDrawItems = remember(draw.items) {
                resolveRenderableDrawItems(draw.items)
            }
            val drawPreviewText = remember(author?.name, visibleDynamicDesc?.text) {
                ImagePreviewTextContent(
                    headline = author?.name.orEmpty(),
                    body = visibleDynamicDesc?.text.orEmpty()
                )
            }
            
            DrawGridV2(
                items = renderableDrawItems,
                gifImageLoader = gifImageLoader,
                maxDisplayImages = resolveDynamicOpusPreviewImageLimit(isDetail),
                onImageClick = { index, rect ->
                    val action = resolveDynamicCardMediaAction(item, index)
                    if (action is DynamicCardMediaAction.PreviewImages) {
                        selectedImageIndex = action.initialIndex
                        sourceRect = rect
                    }
                }
            )
            Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
            
            // 全屏图片预览
            if (selectedImageIndex >= 0) {
                ImagePreviewDialog(
                    livePhotoVideos = buildMap {
                        content?.major?.opus?.pics.orEmpty().forEach { pic ->
                            normalizeLivePhotoVideoUrl(pic.live_url)?.let { liveUrl ->
                                put(pic.url, liveUrl)
                                put(normalizeImageUrl(pic.url), liveUrl)
                            }
                        }
                        content?.major?.draw?.items.orEmpty().forEach { pic ->
                            normalizeLivePhotoVideoUrl(pic.live_url)?.let { liveUrl ->
                                put(pic.src, liveUrl)
                                put(normalizeImageUrl(pic.src), liveUrl)
                            }
                        }
                    },
                    images = renderableDrawItems.map { it.src },
                    initialIndex = selectedImageIndex,
                    sourceRect = sourceRect,  //  [新增] 传递源位置用于展开动画
                    textContent = drawPreviewText,
                    defaultTextVisible = dynamicPreviewTextVisible,
                    onDismiss = { selectedImageIndex = -1 }
                )
            }
        }
        
        //  [新增] Opus 图文动态 (新版格式)
        content?.major?.opus?.let { opus ->
            var selectedImageIndex by remember { mutableIntStateOf(-1) }
            var sourceRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
            val visibleOpusSummaryDesc = remember(opus.summary, renderableOpusPics) {
                opus.summary?.let { summary ->
                    resolveDynamicOpusSummaryDescForImages(
                        text = summary.text,
                        richTextNodes = summary.rich_text_nodes,
                        hasImages = renderableOpusPics.isNotEmpty()
                    )
                }
            }
            val opusPreviewText = remember(author?.name, visibleDynamicDesc?.text, visibleOpusSummaryDesc?.text) {
                val body = visibleDynamicDesc?.text.takeUnless { it.isNullOrBlank() }
                    ?: visibleOpusSummaryDesc?.text.orEmpty()
                ImagePreviewTextContent(
                    headline = author?.name.orEmpty(),
                    body = body
                )
            }
            
            // 正文与标题已在上方统一渲染；此处按需渲染正文内容块与图片列表
            
            // 显示图片 (转换为 DrawItem 格式复用现有组件)
            if (hasFullOpusDetailContent) {
                val previewImages = remember(renderableOpusPics) {
                    renderableOpusPics.map { it.url }
                }
                // The desktop opus API documents width/height as nullable. The
                // paragraph image can therefore have dimensions while the same
                // URL in opus.pics does not (or vice versa). Resolve dimensions
                // once from both payload locations so a recomposition never
                // leaves an AsyncImage without a measurable height.
                val opusPicDimensionsByUrl = remember(renderableOpusPics) {
                    renderableOpusPics
                        .filter { it.url.isNotBlank() && it.width > 0 && it.height > 0 }
                        .associateBy { it.url }
                }
                var fullContentSelectedImageIndex by remember { mutableIntStateOf(-1) }
                fullOpusContentBlocks.forEach { block ->
                    when (block) {
                        is OpusContentBlock.Text -> {
                            val richBlockDesc = resolveDynamicOpusTextBlockRichDesc(
                                blockText = block.text,
                                preferredDesc = preferredBodyDesc,
                                blockRichTextNodes = block.richTextNodes,
                            )
                            if (richBlockDesc != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = AppSpacingTokens.Medium),
                                ) {
                                    RichTextContent(
                                        desc = richBlockDesc,
                                        onUserClick = onUserClick,
                                        onTopicClick = onTopicClick,
                                        onTopicKeywordClick = onTopicKeywordClick,
                                        onVoteClick = { voteId -> pendingVoteId = voteId },
                                        onVideoClick = onVideoClick,
                                        onDynamicDetailClick = openDynamicDetail,
                                        onBangumiClick = onBangumiClick,
                                        onArticleClick = onArticleClick,
                                        onLiveClick = onLiveClick,
                                        onMusicClick = onMusicClick,
                                        extraEmoteUrlMap = dynamicCardEmoteMap,
                                    )
                                }
                            } else {
                                AppText(
                                    text = block.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = resolveOpusTextAlign(block.alignment),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = AppSpacingTokens.Medium)
                                )
                            }
                        }
                        is OpusContentBlock.Heading -> {
                            AppText(
                                text = block.text,
                                style = when (block.level) {
                                    1 -> MaterialTheme.typography.headlineSmall
                                    3 -> MaterialTheme.typography.titleMedium
                                    else -> MaterialTheme.typography.titleLarge
                                },
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = resolveOpusTextAlign(block.alignment),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = AppSpacingTokens.Small,
                                        bottom = AppSpacingTokens.Medium,
                                    ),
                            )
                        }
                        is OpusContentBlock.Quote -> {
                            AppContentCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = AppSpacingTokens.Medium),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                contentPadding = PaddingValues(AppSpacingTokens.Medium),
                            ) {
                                AppText(
                                    text = block.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = resolveOpusTextAlign(block.alignment),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        is OpusContentBlock.ListBlock -> {
                            AppText(
                                text = block.items.mapIndexed { index, listItem ->
                                    if (block.ordered) "${index + 1}. $listItem" else "• $listItem"
                                }.joinToString("\n"),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = resolveOpusTextAlign(block.alignment),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = AppSpacingTokens.Medium,
                                        bottom = AppSpacingTokens.Medium,
                                    ),
                            )
                        }
                        is OpusContentBlock.Code -> {
                            AppContentCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = AppSpacingTokens.Medium),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentPadding = PaddingValues(AppSpacingTokens.Medium),
                            ) {
                                AppText(
                                    text = block.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        is OpusContentBlock.Divider -> {
                            val dividerPic = block.pic
                            if (dividerPic != null) {
                                val resolvedDividerPic = remember(dividerPic, opusPicDimensionsByUrl) {
                                    opusPicDimensionsByUrl[dividerPic.url]?.let { known ->
                                        if (dividerPic.width > 0 && dividerPic.height > 0) dividerPic
                                        else dividerPic.copy(width = known.width, height = known.height)
                                    } ?: dividerPic
                                }
                                val dividerAspectRatio = if (resolvedDividerPic.width > 0 && resolvedDividerPic.height > 0) {
                                    resolvedDividerPic.width.toFloat() / resolvedDividerPic.height.toFloat()
                                } else {
                                    16f / 9f
                                }
                                val dividerRequest = remember(resolvedDividerPic.url) {
                                    coil3.request.ImageRequest.Builder(context)
                                        .data(resolvedDividerPic.url)
                                        .httpHeaders(NetworkHeaders.Builder().set("Referer", "https://www.bilibili.com/").build())
                                        .build()
                                }
                                AsyncImage(
                                    model = dividerRequest,
                                    contentDescription = "分割线",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            Modifier.aspectRatio(dividerAspectRatio)
                                        )
                                        .padding(vertical = AppSpacingTokens.Small),
                                    contentScale = ContentScale.FillWidth,
                                )
                            } else {
                                AppHorizontalDivider(
                                    modifier = Modifier.padding(vertical = AppSpacingTokens.Medium),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                        }
                        is OpusContentBlock.Image -> {
                            val resolvedPic = remember(block.pic, opusPicDimensionsByUrl) {
                                opusPicDimensionsByUrl[block.pic.url]?.let { known ->
                                    if (block.pic.width > 0 && block.pic.height > 0) block.pic
                                    else block.pic.copy(width = known.width, height = known.height)
                                } ?: block.pic
                            }
                            val currentImageIndex = previewImages.indexOf(resolvedPic.url)
                            val aspectRatio = remember(resolvedPic.width, resolvedPic.height) {
                                if (resolvedPic.width > 0 && resolvedPic.height > 0) {
                                    resolvedPic.width.toFloat() / resolvedPic.height.toFloat()
                                } else {
                                    // Keep the node measurable while the API omits
                                    // dimensions; the image can then load without
                                    // collapsing and disappearing on recomposition.
                                    4f / 3f
                                }
                            }
                            val imageRequest = remember(resolvedPic.url) {
                                coil3.request.ImageRequest.Builder(context)
                                    .data(resolvedPic.url)
                                    .httpHeaders(NetworkHeaders.Builder().set("Referer", "https://www.bilibili.com/").build())
                                    .build()
                            }
                            AsyncImage(
                                model = imageRequest,
                                contentDescription = opus.title.orEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (aspectRatio > 0f) {
                                            Modifier.aspectRatio(aspectRatio)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clip(AppShapes.container(ContainerLevel.Card))
                                    .clickable(enabled = currentImageIndex in previewImages.indices) {
                                        fullContentSelectedImageIndex = currentImageIndex
                                    },
                                contentScale = ContentScale.FillWidth
                            )
                            Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
                        }
                        is OpusContentBlock.LinkCard -> {
                            DynamicOpusLinkCard(
                                card = block.card,
                                modifier = Modifier.padding(bottom = AppSpacingTokens.Medium),
                                enabled = block.card.jumpUrl.isNotBlank() ||
                                    (block.card.type == "LINK_CARD_TYPE_VOTE" && block.card.oid.toLongOrNull() != null),
                                onClick = {
                                    val opusVoteId = block.card.takeIf {
                                        it.type == "LINK_CARD_TYPE_VOTE"
                                    }?.oid?.toLongOrNull()?.takeIf { it > 0L }
                                    if (opusVoteId != null) {
                                        pendingVoteId = opusVoteId
                                    } else {
                                        when (val action = resolveDynamicOpusLinkCardAction(block.card)) {
                                            is DynamicOpusLinkCardAction.OpenVideo -> onVideoClick(action.videoId)
                                            is DynamicOpusLinkCardAction.OpenDynamicDetail -> openDynamicDetail?.invoke(action.dynamicId)
                                            is DynamicOpusLinkCardAction.OpenArticle -> onArticleClick?.invoke(action.articleId, action.title)
                                            is DynamicOpusLinkCardAction.OpenLive -> onLiveClick(
                                                action.roomId,
                                                block.card.title.ifBlank { "直播间" },
                                                author?.name.orEmpty()
                                            )
                                            is DynamicOpusLinkCardAction.OpenUser -> onUserClick(action.mid)
                                            is DynamicOpusLinkCardAction.OpenBangumi -> onBangumiClick(action.seasonId, action.epId)
                                            is DynamicOpusLinkCardAction.OpenExternalUrl -> runCatching {
                                                uriHandler.openUri(action.url)
                                            }
                                            DynamicOpusLinkCardAction.None -> Unit
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                if (fullContentSelectedImageIndex >= 0) {
                    ImagePreviewDialog(
                        livePhotoVideos = buildMap {
                            content?.major?.opus?.pics.orEmpty().forEach { pic ->
                                normalizeLivePhotoVideoUrl(pic.live_url)?.let { liveUrl ->
                                    put(pic.url, liveUrl)
                                    put(normalizeImageUrl(pic.url), liveUrl)
                                }
                            }
                            content?.major?.draw?.items.orEmpty().forEach { pic ->
                                normalizeLivePhotoVideoUrl(pic.live_url)?.let { liveUrl ->
                                    put(pic.src, liveUrl)
                                    put(normalizeImageUrl(pic.src), liveUrl)
                                }
                            }
                        },
                        images = previewImages,
                        initialIndex = fullContentSelectedImageIndex,
                        textContent = opusPreviewText,
                        defaultTextVisible = dynamicPreviewTextVisible,
                        onDismiss = { fullContentSelectedImageIndex = -1 }
                    )
                }
            } else if (renderableOpusPics.isNotEmpty()) {
                val drawItems = renderableOpusPics.map { pic ->
                    DrawItem(
                        src = pic.url,
                        width = pic.width,
                        height = pic.height,
                        live_url = pic.live_url
                    )
                }
                DrawGridV2(
                    items = drawItems,
                    gifImageLoader = gifImageLoader,
                    maxDisplayImages = resolveDynamicOpusPreviewImageLimit(isDetail),
                    onImageClick = { index, rect ->
                        val action = resolveDynamicCardMediaAction(item, index)
                        if (action is DynamicCardMediaAction.PreviewImages) {
                            selectedImageIndex = action.initialIndex
                            sourceRect = rect
                        }
                    }
                )
                Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
                
                // 全屏图片预览
                if (selectedImageIndex >= 0) {
                    ImagePreviewDialog(
                        livePhotoVideos = buildMap {
                            content?.major?.opus?.pics.orEmpty().forEach { pic ->
                                normalizeLivePhotoVideoUrl(pic.live_url)?.let { liveUrl ->
                                    put(pic.url, liveUrl)
                                    put(normalizeImageUrl(pic.url), liveUrl)
                                }
                            }
                            content?.major?.draw?.items.orEmpty().forEach { pic ->
                                normalizeLivePhotoVideoUrl(pic.live_url)?.let { liveUrl ->
                                    put(pic.src, liveUrl)
                                    put(normalizeImageUrl(pic.src), liveUrl)
                                }
                            }
                        },
                        images = renderableOpusPics.map { it.url },
                        initialIndex = selectedImageIndex,
                        sourceRect = sourceRect,  //  [新增] 传递源位置用于展开动画
                        textContent = opusPreviewText,
                        defaultTextVisible = dynamicPreviewTextVisible,
                        onDismiss = { selectedImageIndex = -1 }
                    )
                }
            }
        }

        content?.major?.article?.let { article ->
            val articleCovers = remember(article.covers) { resolveArticleCoverUrls(article) }
            if (articleCovers.isNotEmpty()) {
                var selectedImageIndex by remember { mutableIntStateOf(-1) }
                var sourceRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
                val articlePreviewText = remember(author?.name, visibleDynamicDesc?.text, article.title, article.desc) {
                    val body = visibleDynamicDesc?.text
                        .takeUnless { it.isNullOrBlank() }
                        ?: article.desc.ifBlank { article.title }
                    ImagePreviewTextContent(
                        headline = author?.name.orEmpty(),
                        body = body
                    )
                }
                val drawItems = remember(article.covers) { resolveArticleCoverDrawItems(article) }
                DrawGridV2(
                    items = drawItems,
                    gifImageLoader = gifImageLoader,
                    maxDisplayImages = resolveDynamicOpusPreviewImageLimit(isDetail),
                    onImageClick = { index, rect ->
                        when (val action = resolveDynamicCardMediaAction(item, index, isDetail = isDetail)) {
                            is DynamicCardMediaAction.PreviewImages -> {
                                selectedImageIndex = action.initialIndex
                                sourceRect = rect
                            }
                            is DynamicCardMediaAction.OpenDynamicDetail -> {
                                openDynamicDetail?.invoke(action.dynamicId)
                            }
                            DynamicCardMediaAction.None -> Unit
                        }
                    }
                )
                Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))

                if (selectedImageIndex >= 0) {
                    ImagePreviewDialog(
                    livePhotoVideos = buildMap {
                        content?.major?.opus?.pics.orEmpty().forEach { pic ->
                            normalizeLivePhotoVideoUrl(pic.live_url)?.let { liveUrl ->
                                put(pic.url, liveUrl)
                                put(normalizeImageUrl(pic.url), liveUrl)
                            }
                        }
                        content?.major?.draw?.items.orEmpty().forEach { pic ->
                            normalizeLivePhotoVideoUrl(pic.live_url)?.let { liveUrl ->
                                put(pic.src, liveUrl)
                                put(normalizeImageUrl(pic.src), liveUrl)
                            }
                        }
                    },
                        images = articleCovers,
                        initialIndex = selectedImageIndex,
                        sourceRect = sourceRect,
                        textContent = articlePreviewText,
                        defaultTextVisible = dynamicPreviewTextVisible,
                        onDismiss = { selectedImageIndex = -1 }
                    )
                }
            }
        }
        
        //  [新增] 合集/剧集动态
        content?.major?.ugc_season?.let { season ->
            val seasonArchive = resolveUgcSeasonArchiveFallback(season)
            val playableBvid = resolveUgcSeasonPlayableBvid(season)
            if (seasonArchive != null) {
                VideoCardLarge(
                    archive = seasonArchive,
                    publishTs = author?.pub_ts ?: 0L,
                    onClick = {
                        playableBvid?.let(onVideoClick)
                            ?: openDynamicDetail?.invoke(item.id_str)
                    },
                    isCollection = true,
                    collectionTitle = season.title,
                    sharedElementKey = com.android.purebilibili.core.ui.transition.videoPlayerSharedElementKey(
                        seasonArchive.bvid
                    )
                )
                Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
            } else {
                AppText(
                     "合集：${season.title}", 
                     fontWeight = FontWeight.Bold,
                     color = MaterialTheme.colorScheme.primary
                )
                 Spacer(modifier = Modifier.height(AppSpacingTokens.Small))
            }
        }
        
        //  直播推荐动态
        content?.major?.live_rcmd?.let { liveRcmd ->
            LiveCard(
                liveRcmd = liveRcmd,
                onLiveClick = { roomId, title, uname ->
                    dispatchDynamicCardPrimaryAction(
                        action = DynamicCardPrimaryAction.OpenLive(roomId, title, uname),
                        onVideoClick = onVideoClick,
                        onBangumiClick = onBangumiClick,
                        onArticleClick = onArticleClick,
                        onDynamicDetailClick = openDynamicDetail,
                        onUserClick = onUserClick,
                        onLiveClick = onLiveClick
                    )
                }
            )
            Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
        }

        content?.major?.live?.let { live ->
            LiveMajorCard(
                live = live,
                onLiveClick = { roomId, title, uname ->
                    dispatchDynamicCardPrimaryAction(
                        action = DynamicCardPrimaryAction.OpenLive(roomId, title, uname),
                        onVideoClick = onVideoClick,
                        onBangumiClick = onBangumiClick,
                        onArticleClick = onArticleClick,
                        onDynamicDetailClick = openDynamicDetail,
                        onUserClick = onUserClick,
                        onLiveClick = onLiveClick
                    )
                }
            )
            Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
        }

        content?.major?.subscription_new?.live_rcmd?.let { liveRcmd ->
            LiveCard(
                liveRcmd = liveRcmd,
                onLiveClick = { roomId, title, uname ->
                    dispatchDynamicCardPrimaryAction(
                        action = DynamicCardPrimaryAction.OpenLive(roomId, title, uname),
                        onVideoClick = onVideoClick,
                        onBangumiClick = onBangumiClick,
                        onArticleClick = onArticleClick,
                        onDynamicDetailClick = openDynamicDetail,
                        onUserClick = onUserClick,
                        onLiveClick = onLiveClick
                    )
                }
            )
            Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
        }

        content?.major?.music?.takeIf { it.title.isNotBlank() }?.let { music ->
            val musicId = music.id.removePrefix("au").removePrefix("AU").toLongOrNull()
            DynamicNativeLinkCard(
                title = music.title,
                subtitle = music.label,
                cover = music.cover,
                kindLabel = "音乐",
                actionLabel = "播放",
                enabled = musicId != null || music.jump_url.isNotBlank(),
                onClick = {
                    if (musicId != null && onMusicClick != null) {
                        onMusicClick(musicId)
                    } else if (music.jump_url.isNotBlank()) {
                        openDynamicUrl(uriHandler, music.jump_url)
                    }
                },
            )
            Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
        }

        content?.major?.medialist?.takeIf { it.title.isNotBlank() }?.let { mediaList ->
            DynamicNativeLinkCard(
                title = mediaList.title,
                subtitle = mediaList.sub_title.ifBlank { "收藏夹" },
                cover = mediaList.cover,
                kindLabel = mediaList.badge?.text.orEmpty().ifBlank { "收藏夹" },
                actionLabel = "打开",
                enabled = mediaList.id.toLongOrNull() != null || mediaList.jump_url.isNotBlank(),
                onClick = {
                    onCollectionClick?.invoke(
                        mediaList.id.toLongOrNull() ?: 0L,
                        item.modules.module_author?.mid ?: 0L,
                        mediaList.title,
                        mediaList.jump_url,
                    )
                        ?: openDynamicUrl(uriHandler, mediaList.jump_url)
                },
            )
            Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
        }

        content?.major?.courses?.takeIf { it.title.isNotBlank() }?.let { course ->
            DynamicNativeLinkCard(
                title = course.title,
                subtitle = listOf(course.sub_title, course.desc)
                    .filter(String::isNotBlank)
                    .distinct()
                    .joinToString(" · "),
                cover = course.cover,
                kindLabel = course.badge?.text.orEmpty().ifBlank { "课程" },
                actionLabel = "打开",
                enabled = course.jump_url.isNotBlank(),
                onClick = {
                    onCourseClick?.invoke(course.jump_url, course.title)
                        ?: openDynamicUrl(uriHandler, course.jump_url)
                },
            )
            Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
        }

        resolveDynamicMajorCard(
            // Music and subscription cards have dedicated native renderers above.
            major = content?.major?.takeUnless {
                it.music != null || it.subscription_new != null || it.medialist != null || it.courses != null
            },
            darkTheme = isSystemInDarkTheme(),
        )?.let { majorCard ->
            DynamicNativeLinkCard(
                title = majorCard.title,
                subtitle = majorCard.subtitle,
                cover = majorCard.cover,
                kindLabel = majorCard.kindLabel,
                actionLabel = majorCard.actionLabel,
                enabled = majorCard.enabled && majorCard.jumpUrl.isNotBlank(),
                onClick = { openDynamicUrl(uriHandler, majorCard.jumpUrl) },
            )
            Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
        }
        
        //  转发动态 - 嵌套显示原始内容
        if (type == DynamicType.FORWARD && item.orig != null) {
            ForwardedContent(
                orig = item.orig,
                onVideoClick = onVideoClick,
                onBangumiClick = onBangumiClick,
                onUserClick = onUserClick,
                onTopicClick = onTopicClick,
                onTopicKeywordClick = onTopicKeywordClick,
                onDynamicDetailClick = openDynamicDetail,
                onArticleClick = onArticleClick,
                onLiveClick = onLiveClick,
                onMusicClick = onMusicClick,
                gifImageLoader = gifImageLoader,
                defaultPreviewTextVisible = dynamicPreviewTextVisible
            )
            Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
        }
        
        additionalCardState?.let { additionalCard ->
            DynamicAdditionalCard(
                model = additionalCard,
                actionLoading = reserveSubmitting,
                onActionClick = if (additionalCard.reserveActionJumpUrl.isNotBlank()) {
                    { openDynamicUrl(uriHandler, additionalCard.reserveActionJumpUrl) }
                } else if (additionalCard.reserveId > 0L && onReserveClick != null) {
                    {
                        if (!reserveSubmitting) {
                            reserveSubmitting = true
                            onReserveClick(
                                DynamicReserveAction(
                                    dynamicId = item.id_str,
                                    reserveId = additionalCard.reserveId,
                                    currentButtonStatus = additionalCard.reserveButtonStatus,
                                    reserveTotal = additionalCard.reserveTotal,
                                )
                            ) { result ->
                                reserveSubmitting = false
                                result.onSuccess { updated ->
                                    additionalCardState = additionalCard.copy(
                                        subtitle = listOf(
                                            additionalCard.reserveDescriptionPrefix,
                                            updated.description,
                                        ).filter(String::isNotBlank).joinToString("  ")
                                            .ifBlank { additionalCard.subtitle },
                                        reserveTotal = updated.reserveTotal,
                                        reserveButtonStatus = updated.buttonStatus,
                                        actionLabel = if (updated.buttonStatus == additionalCard.reserveButtonType) {
                                            additionalCard.reserveCheckedLabel
                                        } else {
                                            additionalCard.reserveUncheckedLabel
                                        },
                                    )
                                }.onFailure { error ->
                                    android.widget.Toast.makeText(
                                        context,
                                        error.message ?: "预约操作失败",
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                        }
                    }
                } else null,
                onClick = {
                    if (additionalCard.voteId > 0L) {
                        pendingVoteId = additionalCard.voteId
                    } else if (additionalCard.jumpUrl.isNotBlank()) {
                        openDynamicUrl(uriHandler, additionalCard.jumpUrl)
                    } else if (additionalCard.reserveDescriptionJumpUrl.isNotBlank()) {
                        openDynamicUrl(uriHandler, additionalCard.reserveDescriptionJumpUrl)
                    }
                }
            )
            Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
        }

        //  [修复] 底部操作栏：转发、评论、点赞 - 始终显示
        val statModule = stat ?: DynamicStatModule()  // 使用默认值避免按钮消失
        val actionButtonWeight = resolveDynamicActionButtonSlotWeight()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppSpacingTokens.ExtraSmall),
            horizontalArrangement = Arrangement.spacedBy(resolveDynamicActionButtonSpacing())
        ) {
            // 转发按钮
            ActionButton(
                count = (statModule.forward.count + forwardCountDelta).coerceAtLeast(0),
                label = "转发",
                enabled = !statModule.forward.forbidden,
                onClick = { onRepostClick(item.id_str) },
                modifier = Modifier.weight(actionButtonWeight)
            )
            
            // 评论按钮
            ActionButton(
                count = statModule.comment.count,
                label = "评论",
                // forbidden 代表评论操作受限；仍允许进入详情查看已有评论。
                enabled = item.id_str.isNotBlank(),
                onClick = {
                    DynamicRepository.rememberDynamicDetailSeed(item)
                    onCommentClick(item.id_str)
                },
                modifier = Modifier.weight(actionButtonWeight)
            )
            
            // 点赞按钮
            ActionButton(
                count = statModule.like.count,
                label = "点赞",
                isActive = effectiveIsLiked,
                onClick = {
                    onLikeClickWithState?.invoke(item.id_str, effectiveIsLiked)
                        ?: onLikeClick(item.id_str)
                },
                modifier = Modifier.weight(actionButtonWeight)
            )
        }
        }

        // PiliPlus: fold InkWell is a sibling of the card body, not the card's
        // primary tap target. Nested clickable here still opened dynamic detail.
        if (!isDetail) {
            val foldStatement = resolveDynamicFoldStatement(item.modules.module_fold)
            if (foldStatement != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = resolveDynamicCardContentPadding())
                        .clip(AppShapes.container(ContainerLevel.Chip))
                        .clickable(enabled = onUnfoldRelatedClick != null) {
                            onUnfoldRelatedClick?.invoke(item.id_str)
                        }
                        .padding(vertical = AppSpacingTokens.Small),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val foldUsers = item.modules.module_fold?.users.orEmpty().take(3)
                    if (foldUsers.isNotEmpty()) {
                        Box(modifier = Modifier.height(22.dp)) {
                            foldUsers.forEachIndexed { index, user ->
                                AsyncImage(
                                    model = coil3.request.ImageRequest.Builder(LocalContext.current)
                                        .data(user.face.let { if (it.startsWith("http://")) it.replace("http://", "https://") else it })
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .offset(x = (index * 14).dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .border(AppSurfaceTokens.OutlineWidth, AppSurfaceTokens.surface(), CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(AppSpacingTokens.Small))
                    }
                    AppText(
                        foldStatement,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(AppSpacingTokens.ExtraSmall))
                    AppIcon(
                        rememberAppChevronDownIcon(),
                        contentDescription = null,
                        modifier = Modifier.size(AppSpacingTokens.Small + AppSpacingTokens.ExtraSmall),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (!isDetail) {
            AppHorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f),
                thickness = AppSpacingTokens.Micro * 0.35f
            )
        }
    }
}

private fun resolveOpusTextAlign(alignment: Int): TextAlign = when (alignment) {
    1 -> TextAlign.Center
    2 -> TextAlign.End
    else -> TextAlign.Start
}

@Composable
private fun DynamicAdditionalCard(
    model: DynamicAdditionalCardModel,
    actionLoading: Boolean,
    onActionClick: (() -> Unit)?,
    onClick: () -> Unit
) {
    DynamicNativeLinkCard(
        title = model.title,
        subtitle = model.subtitle,
        cover = model.cover,
        kindLabel = model.kindLabel,
        actionLabel = model.actionLabel,
        enabled = model.enabled,
        actionEnabled = !model.reserveButtonDisabled && !actionLoading,
        onActionClick = onActionClick,
        onClick = onClick,
    )
}

@Composable
internal fun DynamicNativeLinkCard(
    title: String,
    subtitle: String,
    cover: String,
    kindLabel: String,
    actionLabel: String,
    enabled: Boolean,
    actionEnabled: Boolean = true,
    onActionClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    AppContentCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        contentPadding = PaddingValues(horizontal = AppSpacingTokens.ExtraSmall)
    ) {
        AppListItem(
            overlineContent = {
                AppText(
                    text = kindLabel,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            headlineContent = {
                AppText(
                    text = title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
            },
            supportingContent = subtitle.takeIf { it.isNotBlank() }?.let { supportingText ->
                {
                    AppText(
                        text = supportingText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            leadingContent = if (cover.isNotBlank()) {
                {
                    AsyncImage(
                        model = cover,
                        contentDescription = null,
                        modifier = Modifier
                            .size(width = 88.dp, height = 56.dp)
                            .clip(AppShapes.container(ContainerLevel.Chip)),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                null
            },
            trailingContent = actionLabel.takeIf(String::isNotBlank)?.let { label ->
                {
                    AppText(
                        text = label,
                        color = if (actionEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .heightIn(min = AppChromeSizeTokens.MinimumTouchTarget)
                            .semantics { contentDescription = "操作：$label" }
                            .then(
                                if (onActionClick != null) Modifier.clickable(
                                    enabled = actionEnabled,
                                    onClick = onActionClick,
                                ) else Modifier
                            )
                            .wrapContentHeight(Alignment.CenterVertically),
                    )
                }
            },
        )
    }
}

@Composable
internal fun DynamicTopicLabel(
    topicName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .heightIn(min = AppChromeSizeTokens.MinimumTouchTarget)
            .clip(AppShapes.container(ContainerLevel.Chip))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(AppSpacingTokens.Large + AppSpacingTokens.ExtraSmall)
                .clip(RoundedCornerShape(AppSpacingTokens.ExtraSmall))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            AppText(
                text = "#",
                color = MaterialTheme.colorScheme.primary,
                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.width(AppSpacingTokens.Small))
        AppText(
            text = topicName,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun openDynamicUrl(
    uriHandler: androidx.compose.ui.platform.UriHandler,
    rawUrl: String,
) {
    if (rawUrl.isBlank()) return
    val target = when {
        rawUrl.startsWith("//") -> "https:$rawUrl"
        else -> rawUrl
    }
    runCatching { uriHandler.openUri(target) }
}

/**
 *  富文本内容（支持表情、@提及、话题高亮）
 *  解析 API 返回的 rich_text_nodes 来正确渲染表情图片；
 *  若节点仅为纯文本短码，则用表情面板缓存补全图片。
 *
 *  @param onBlankTap 点击非 @/链接区域时回调（转发原文点击跳原动态）
 */
@Composable
fun RichTextContent(
    desc: DynamicDesc,
    onUserClick: (Long) -> Unit,
    onTopicClick: (Long) -> Unit = {},
    onTopicKeywordClick: ((String) -> Unit)? = null,
    onVoteClick: (Long) -> Unit = {},
    onBlankTap: (() -> Unit)? = null,
    onVideoClick: ((String) -> Unit)? = null,
    onDynamicDetailClick: ((String) -> Unit)? = null,
    onBangumiClick: ((Long, Long) -> Unit)? = null,
    onArticleClick: ((Long, String) -> Unit)? = null,
    onLiveClick: ((Long, String, String) -> Unit)? = null,
    onMusicClick: ((Long) -> Unit)? = null,
    onLinkClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = MaterialTheme.typography.bodyMedium.fontSize,
    fontWeight: FontWeight? = null,
    lineHeight: TextUnit = MaterialTheme.typography.bodyLarge.lineHeight,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    extraEmoteUrlMap: Map<String, String> = emptyMap(),
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var catalogEmoteMap by remember { mutableStateOf(DynamicEmoteCatalog.snapshot()) }
    val emoteCatalogSessionKey = DynamicEmoteCatalog.currentSessionKey()
    LaunchedEffect(emoteCatalogSessionKey) {
        catalogEmoteMap = DynamicEmoteCatalog.ensureLoaded()
    }
    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val richText = remember(desc, primaryColor, textColor, catalogEmoteMap, extraEmoteUrlMap) {
        buildDynamicRichText(
            desc = desc,
            primaryColor = primaryColor,
            textColor = textColor,
            extraEmoteUrlMap = buildMap {
                putAll(catalogEmoteMap)
                putAll(extraEmoteUrlMap)
            }
        )
    }
    val annotatedText = richText.annotatedString

    // 仅对实际用到的表情 id 建 InlineContent，避免整包表情占内存
    val inlineContent = remember(richText.emojiUrlById) {
        richText.emojiUrlById.mapValues { (_, iconUrl) ->
            InlineTextContent(
                Placeholder(
                    width = 1.4.em,
                    height = 1.4.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                )
            ) {
                AsyncImage(
                    model = coil3.request.ImageRequest.Builder(LocalContext.current)
                        .data(iconUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
    val copyText = remember(desc.rich_text_nodes, desc.text) {
        val richNodeText = resolveDynamicRichTextNodeDisplayText(desc.rich_text_nodes)
        richNodeText.ifBlank { desc.text }.trim()
    }
    var showTextSelectionSheet by remember(copyText) { mutableStateOf(false) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    SelectionContainer {
        AppText(
            text = annotatedText,
            inlineContent = inlineContent,
            fontSize = fontSize,
            fontWeight = fontWeight,
            lineHeight = lineHeight,
            maxLines = maxLines,
            overflow = overflow,
            color = textColor,
            onTextLayout = { textLayoutResult = it },
            modifier = modifier.pointerInput(
                copyText,
                annotatedText,
                onUserClick,
                onVoteClick,
                onTopicClick,
                onBlankTap,
                onVideoClick,
                onDynamicDetailClick,
                onBangumiClick,
                onArticleClick,
                onLiveClick,
                onMusicClick,
                onLinkClick,
            ) {
                detectTapWithSelectionFriendly { offset ->
                    val layoutResult = textLayoutResult ?: return@detectTapWithSelectionFriendly
                    val position = layoutResult.getOffsetForPosition(offset)
                    val searchStart = maxOf(0, position - 1)
                    val searchEnd = minOf(annotatedText.length, position + 1)

                    annotatedText.getStringAnnotations(
                        tag = DYNAMIC_RICH_TEXT_USER_TAG,
                        start = searchStart,
                        end = searchEnd
                    ).firstOrNull()?.let { annotation ->
                        annotation.item.toLongOrNull()
                            ?.takeIf { it > 0L }
                            ?.let(onUserClick)
                        return@detectTapWithSelectionFriendly
                    }

                    annotatedText.getStringAnnotations(
                        tag = DYNAMIC_RICH_TEXT_VOTE_TAG,
                        start = searchStart,
                        end = searchEnd
                    ).firstOrNull()?.item
                        ?.toLongOrNull()
                        ?.takeIf { it > 0L }
                        ?.let { voteId ->
                            onVoteClick(voteId)
                            return@detectTapWithSelectionFriendly
                        }

                    // 带 topicId 的话题标签优先跳转话题详情页，而不是关键词搜索。
                    annotatedText.getStringAnnotations(
                        tag = DYNAMIC_RICH_TEXT_TOPIC_TAG,
                        start = searchStart,
                        end = searchEnd
                    ).firstOrNull()?.item
                        ?.toLongOrNull()
                        ?.takeIf { it > 0L }
                        ?.let { topicId ->
                            onTopicClick(topicId)
                            return@detectTapWithSelectionFriendly
                        }

                    // 无 topicId 的话题（纯 #关键词# 或链接搜索）才回落到关键词搜索。
                    annotatedText.getStringAnnotations(
                        tag = DYNAMIC_RICH_TEXT_TOPIC_KEYWORD_TAG,
                        start = searchStart,
                        end = searchEnd
                    ).firstOrNull()?.item?.takeIf { it.isNotBlank() }?.let { keyword ->
                        if (onTopicKeywordClick != null) {
                            onTopicKeywordClick(keyword)
                            return@detectTapWithSelectionFriendly
                        }
                        val searchUrl = "bilibili://search?keyword=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8.name())
                        if (onLinkClick != null) {
                            onLinkClick(searchUrl)
                        } else {
                            val inAppIntent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(searchUrl)
                            ).setPackage(context.packageName)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            val launched = runCatching { context.startActivity(inAppIntent) }.isSuccess
                            if (!launched) {
                                openDynamicRichTextLinkExternally(context, searchUrl, uriHandler)
                            }
                        }
                        return@detectTapWithSelectionFriendly
                    }

                    val urlAnnotation = annotatedText.getStringAnnotations(
                        tag = DYNAMIC_RICH_TEXT_URL_TAG,
                        start = searchStart,
                        end = searchEnd
                    ).firstOrNull()

                    if (urlAnnotation != null) {
                        val rawUrl = urlAnnotation.item
                        scope.launch {
                            val target = BilibiliNavigationTargetParser.parse(rawUrl)
                                ?: if (rawUrl.contains("b23.tv", ignoreCase = true)) {
                                    BilibiliNavigationTargetParser.resolve(rawUrl)
                                } else null
                            if (target != null) {
                                val handled = when (target) {
                                    is BilibiliNavigationTarget.Dynamic -> {
                                        if (onDynamicDetailClick != null) {
                                            onDynamicDetailClick(target.dynamicId)
                                            true
                                        } else false
                                    }
                                    is BilibiliNavigationTarget.Video -> {
                                        if (onVideoClick != null) {
                                            onVideoClick(target.videoId)
                                            true
                                        } else false
                                    }
                                    is BilibiliNavigationTarget.Space -> {
                                        if (target.mid > 0L) {
                                            onUserClick(target.mid)
                                            true
                                        } else false
                                    }
                                    is BilibiliNavigationTarget.BangumiSeason -> {
                                        if (onBangumiClick != null) {
                                            onBangumiClick(target.seasonId, target.mediaId)
                                            true
                                        } else false
                                    }
                                    is BilibiliNavigationTarget.BangumiEpisode -> {
                                        if (onBangumiClick != null) {
                                            onBangumiClick(0L, target.epId)
                                            true
                                        } else false
                                    }
                                    is BilibiliNavigationTarget.Article -> {
                                        if (onArticleClick != null) {
                                            onArticleClick(target.articleId, "")
                                            true
                                        } else false
                                    }
                                    is BilibiliNavigationTarget.Live -> {
                                        if (onLiveClick != null) {
                                            onLiveClick(target.roomId, "", "")
                                            true
                                        } else false
                                    }
                                    is BilibiliNavigationTarget.Music -> {
                                        val auSid = target.musicId.removePrefix("au").removePrefix("AU").toLongOrNull()
                                        if (auSid != null && onMusicClick != null) {
                                            onMusicClick(auSid)
                                            true
                                        } else false
                                    }
                                    is BilibiliNavigationTarget.Search -> {
                                        if (onTopicKeywordClick != null) {
                                            onTopicKeywordClick(target.keyword)
                                            true
                                        } else {
                                            val searchUrl = "bilibili://search?keyword=" + java.net.URLEncoder.encode(target.keyword, java.nio.charset.StandardCharsets.UTF_8.name())
                                            val inAppIntent = android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse(searchUrl)
                                            ).setPackage(context.packageName)
                                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                            runCatching { context.startActivity(inAppIntent) }.isSuccess
                                        }
                                    }
                                }
                                if (handled) return@launch
                            }

                            if (onLinkClick != null) {
                                onLinkClick(rawUrl)
                            } else {
                                when (resolveDynamicRichTextOpenMode(rawUrl)) {
                                    DynamicRichTextOpenMode.IN_APP -> {
                                        val inAppIntent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(rawUrl)
                                        ).setPackage(context.packageName)
                                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        val launchedInApp = runCatching {
                                            context.startActivity(inAppIntent)
                                        }.isSuccess
                                        if (!launchedInApp) {
                                            openDynamicRichTextLinkExternally(
                                                context,
                                                rawUrl,
                                                uriHandler
                                            )
                                        }
                                    }
                                    DynamicRichTextOpenMode.EXTERNAL -> {
                                        openDynamicRichTextLinkExternally(
                                            context,
                                            rawUrl,
                                            uriHandler
                                        )
                                    }
                                    null -> Unit
                                }
                            }
                        }
                        return@detectTapWithSelectionFriendly
                    }

                    // 非 @ / 链接：交给外层（例如转发卡片打开原动态）
                    onBlankTap?.invoke()
                }
            }
        )
    }

    if (showTextSelectionSheet) {
        TextSelectionBottomSheet(
            text = copyText,
            title = "选择动态内容",
            onDismiss = { showTextSelectionSheet = false }
        )
    }
}

private fun openDynamicRichTextLinkExternally(
    context: android.content.Context,
    url: String,
    uriHandler: androidx.compose.ui.platform.UriHandler
) {
    val externalIntent = android.content.Intent(
        android.content.Intent.ACTION_VIEW,
        android.net.Uri.parse(url)
    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)

    val packageManager = context.packageManager
    val externalPackage = packageManager.queryIntentActivities(
        externalIntent,
        android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
    ).firstOrNull { it.activityInfo?.packageName != context.packageName }
        ?.activityInfo
        ?.packageName

    val launchedExternally = if (!externalPackage.isNullOrBlank()) {
        runCatching {
            context.startActivity(externalIntent.setPackage(externalPackage))
        }.isSuccess
    } else {
        false
    }

    if (!launchedExternally) {
        runCatching { uriHandler.openUri(url) }
    }
}

@Composable
private fun DynamicAuthorFace(
    faceUrl: String,
    officialType: Int?,
    vipStatus: Int?,
    faceSize: Dp,
    modifier: Modifier = Modifier,
) {
    val normalizedFace = faceUrl.let { if (it.startsWith("http://")) it.replace("http://", "https://") else it }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(faceSize)) {
            AsyncImage(
                model = coil3.request.ImageRequest.Builder(LocalContext.current)
                    .data(normalizedFace)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            UserAvatarCornerMarkBadge(
                mark = resolveUserAvatarCornerMark(
                    officialType = officialType,
                    vipStatus = vipStatus,
                ),
                modifier = Modifier.align(Alignment.BottomEnd),
                badgeSize = 14.dp,
            )
        }
    }
}

/**
 *  紧凑列表卡片 - 单行显示
 */
@Composable
fun DynamicCardCompact(
    item: DynamicItem,
    onVideoClick: (String) -> Unit,
    onUserClick: (Long) -> Unit
) {
    val author = item.modules.module_author
    val content = item.modules.module_dynamic
    val stat = item.modules.module_stat
    
    // 获取内容预览文本
    val previewText = content?.desc?.text?.take(50) 
        ?: content?.major?.archive?.title 
        ?: "动态"
    
    val authorClickMid = remember(item) { resolveDynamicAuthorClickMid(item) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // 如果有视频则跳转视频
                content?.major?.archive
                    ?.let(::resolveArchivePlayableBvid)
                    ?.let(onVideoClick)
                    ?: authorClickMid?.let(onUserClick)
            }
            .padding(horizontal = AppSpacingTokens.Large, vertical = AppSpacingTokens.Medium),  //  优化间距
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像
        if (author != null) {
            DynamicAuthorFace(
                faceUrl = author.face,
                officialType = author.official_verify?.type,
                vipStatus = author.vip?.status,
                faceSize = AppSpacingTokens.DoubleExtraLarge + AppSpacingTokens.Medium,
                modifier = Modifier
                    .size(AppChromeSizeTokens.MinimumTouchTarget)
                    .semantics { contentDescription = "查看${author.name}的个人主页" }
                    .clickable(enabled = authorClickMid != null) {
                        authorClickMid?.let(onUserClick)
                    },
            )
            
            Spacer(modifier = Modifier.width(AppSpacingTokens.Medium))
        }
        
        // 内容区
        Column(modifier = Modifier.weight(1f)) {
            // 用户名 + 时间
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppText(
                    author?.name ?: "",
                    fontWeight = FontWeight.Medium,
                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(AppSpacingTokens.Small))
                AppText(
                    author?.let {
                        resolveDynamicAuthorTimeText(
                            pubTime = it.pub_time,
                            pubTs = it.pub_ts
                        )
                    }.orEmpty(),
                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                )
            }
            
            Spacer(modifier = Modifier.height(AppSpacingTokens.ExtraSmall))
            
            // 内容预览
            AppText(
                previewText,
                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        
        // 封面缩略图（如果有视频）
        content?.major?.archive?.let { archive ->
            Spacer(modifier = Modifier.width(AppSpacingTokens.Medium))
            AsyncImage(
                model = coil3.request.ImageRequest.Builder(LocalContext.current)
                    .data(archive.cover.let { if (it.startsWith("http://")) it.replace("http://", "https://") else it })
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(width = AppSpacingTokens.TripleExtraLarge + AppSpacingTokens.DoubleExtraLarge, height = AppSpacingTokens.TripleExtraLarge + AppSpacingTokens.Micro)
                    .clip(AppShapes.container(ContainerLevel.Chip)),
                contentScale = ContentScale.Crop
            )
        }
    }
}
