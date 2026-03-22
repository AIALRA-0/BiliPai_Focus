package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.feature.dynamic.components.ImagePreviewTextContent
import com.android.purebilibili.core.ui.animation.MaybeDissolvableVideoCard
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.HandThumbsup
import io.github.alexzhirkevich.cupertino.icons.outlined.BubbleLeft
import io.github.alexzhirkevich.cupertino.icons.outlined.HandThumbsup
import io.github.alexzhirkevich.cupertino.icons.outlined.MinusCircle
import io.github.alexzhirkevich.cupertino.icons.outlined.Trash

const val SUB_REPLY_DETAIL_HEADER_TAG = "subreply_detail_header"
const val SUB_REPLY_DETAIL_ROOT_TAG = "subreply_detail_root"
const val SUB_REPLY_DETAIL_LIST_TAG = "subreply_detail_reply_list"
const val SUB_REPLY_DETAIL_IMAGE_TAG_PREFIX = "subreply_detail_image_"

@Composable
internal fun SubReplyDetailContent(
    rootReply: ReplyItem,
    subReplies: List<ReplyItem>,
    isLoading: Boolean,
    isEnd: Boolean,
    emoteMap: Map<String, String>,
    onLoadMore: () -> Unit,
    onTimestampClick: ((Long) -> Unit)? = null,
    upMid: Long = 0,
    showUpFlag: Boolean = false,
    onImagePreview: ((List<String>, Int, Rect?, ImagePreviewTextContent?) -> Unit)? = null,
    onReplyClick: ((ReplyItem) -> Unit)? = null,
    dissolvingIds: Set<Long> = emptySet(),
    currentMid: Long = 0,
    onDissolveStart: ((Long) -> Unit)? = null,
    onDeleteComment: ((Long) -> Unit)? = null,
    onCommentLike: ((Long) -> Unit)? = null,
    likedComments: Set<Long> = emptySet(),
    onUrlClick: ((String) -> Unit)? = null,
    onAvatarClick: ((String) -> Unit)? = null
) {
    val unusedShowUpFlag = showUpFlag
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= layoutInfo.totalItemsCount - 2 && !isLoading && !isEnd
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag(SUB_REPLY_DETAIL_HEADER_TAG),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "评论详情",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag(SUB_REPLY_DETAIL_LIST_TAG),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item(key = "root_reply") {
                Box(modifier = Modifier.testTag(SUB_REPLY_DETAIL_ROOT_TAG)) {
                    SubReplyDetailItem(
                        item = rootReply,
                        upMid = upMid,
                        emoteMap = emoteMap,
                        showUpFlag = unusedShowUpFlag,
                        onTimestampClick = onTimestampClick,
                        onImagePreview = onImagePreview,
                        onReplyClick = { onReplyClick?.invoke(rootReply) },
                        onDeleteClick = if (currentMid > 0 && rootReply.mid == currentMid) {
                            { onDeleteComment?.invoke(rootReply.rpid) }
                        } else null,
                        onLikeClick = { onCommentLike?.invoke(rootReply.rpid) },
                        isLiked = rootReply.action == 1 || rootReply.rpid in likedComments,
                        onUrlClick = onUrlClick,
                        onAvatarClick = { onAvatarClick?.invoke(it) ?: Unit }
                    )
                }
                HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceContainerHigh)
                Text(
                    text = "相关回复",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp),
                    fontWeight = FontWeight.Medium
                )
            }

            items(
                items = subReplies,
                key = { it.rpid }
            ) { item ->
                MaybeDissolvableVideoCard(
                    isDissolving = item.rpid in dissolvingIds,
                    onDissolveComplete = { onDeleteComment?.invoke(item.rpid) },
                    cardId = "subreply_detail_${item.rpid}",
                    modifier = Modifier.padding(bottom = 1.dp)
                ) {
                    SubReplyDetailItem(
                        item = item,
                        upMid = upMid,
                        emoteMap = emoteMap,
                        showUpFlag = unusedShowUpFlag,
                        onTimestampClick = onTimestampClick,
                        onImagePreview = onImagePreview,
                        onReplyClick = { onReplyClick?.invoke(item) },
                        onDeleteClick = if (currentMid > 0 && item.mid == currentMid) {
                            { onDissolveStart?.invoke(item.rpid) }
                        } else null,
                        onLikeClick = { onCommentLike?.invoke(item.rpid) },
                        isLiked = item.action == 1 || item.rpid in likedComments,
                        onUrlClick = onUrlClick,
                        onAvatarClick = { onAvatarClick?.invoke(it) ?: Unit }
                    )
                }
            }

            item(key = "footer") {
                LaunchedEffect(isLoading, isEnd) {
                    if (!isLoading && !isEnd) {
                        onLoadMore()
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> CupertinoActivityIndicator()
                        else -> Text(
                            text = "没有更多回复了",
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubReplyDetailItem(
    item: ReplyItem,
    upMid: Long,
    emoteMap: Map<String, String>,
    showUpFlag: Boolean,
    onTimestampClick: ((Long) -> Unit)?,
    onImagePreview: ((List<String>, Int, Rect?, ImagePreviewTextContent?) -> Unit)?,
    onReplyClick: () -> Unit,
    onDeleteClick: (() -> Unit)?,
    onLikeClick: (() -> Unit)?,
    isLiked: Boolean,
    onUrlClick: ((String) -> Unit)?,
    onAvatarClick: (String) -> Unit
) {
    val displayLocation = remember(item.replyControl?.location) {
        resolveReplyLocationText(item.replyControl?.location)
    }
    val displayLikeCount = remember(item.like, item.action, isLiked) {
        resolveReplyDisplayLikeCount(
            baseLikeCount = item.like,
            initialAction = item.action,
            isLiked = isLiked
        )
    }
    val localEmoteMap = remember(item.content.emote, emoteMap) {
        val inlineEmotes = item.content.emote.orEmpty()
        if (inlineEmotes.isEmpty()) {
            emoteMap
        } else {
            buildMap(emoteMap.size + inlineEmotes.size) {
                putAll(emoteMap)
                inlineEmotes.forEach { (key, value) -> put(key, value.url) }
            }
        }
    }
    val specialLabelText = remember(item.cardLabels, showUpFlag, item.upAction) {
        resolveReplySpecialLabelText(
            cardLabels = item.cardLabels,
            showUpFlag = showUpFlag,
            upAction = item.upAction
        )
    }
    val isUpComment = upMid > 0 && item.mid == upMid

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(FormatUtils.fixImageUrl(item.member.avatar))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onAvatarClick(item.member.mid) }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.member.uname,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.member.vip?.vipStatus == 1) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (isUpComment) {
                        UpTag()
                    }

                    if (item.member.levelInfo.currentLevel > 0) {
                        LevelTag(level = item.member.levelInfo.currentLevel)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                RichCommentText(
                    text = item.content.message,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    emoteMap = localEmoteMap,
                    onTimestampClick = onTimestampClick,
                    onUrlClick = onUrlClick
                )

                if (!item.content.pictures.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CommentPictures(
                        pictures = item.content.pictures,
                        onImageClick = { images, index, rect ->
                            onImagePreview?.invoke(
                                images,
                                index,
                                rect,
                                resolveReplyPreviewTextContent(item)
                            )
                        },
                        testTagPrefix = "$SUB_REPLY_DETAIL_IMAGE_TAG_PREFIX${item.rpid}_"
                    )
                }

                if (!specialLabelText.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ReplySpecialLabelChip(text = specialLabelText)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = buildString {
                            append(formatTime(item.ctime))
                            if (!displayLocation.isNullOrEmpty()) {
                                append(" · $displayLocation")
                            }
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable(enabled = onLikeClick != null) { onLikeClick?.invoke() }
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) CupertinoIcons.Filled.HandThumbsup else CupertinoIcons.Outlined.HandThumbsup,
                            contentDescription = "Like",
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        if (displayLikeCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = FormatUtils.formatStat(displayLikeCount.toLong()),
                                fontSize = 12.sp,
                                color = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Icon(
                        imageVector = CupertinoIcons.Outlined.MinusCircle,
                        contentDescription = "Dislike",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Icon(
                        imageVector = CupertinoIcons.Outlined.BubbleLeft,
                        contentDescription = "Reply",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onReplyClick() }
                    )

                    if (onDeleteClick != null) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(
                            imageVector = CupertinoIcons.Outlined.Trash,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onDeleteClick() }
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(start = 68.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )
    }
}
