// 文件路径: feature/dynamic/components/DynamicCommentSheet.kt
package com.android.purebilibili.feature.dynamic.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.feature.dynamic.DynamicViewModel
import com.android.purebilibili.feature.dynamic.resolveDynamicCommentSheetTotalCount
import com.android.purebilibili.feature.video.ui.components.RichCommentText
import com.android.purebilibili.feature.video.ui.components.FanGroupDecorationBadge
import com.android.purebilibili.feature.video.ui.components.resolveFanGroupDecorationCardBgs
import com.android.purebilibili.feature.video.ui.components.resolveFanGroupVisualFromMemberAndSailing
import com.android.purebilibili.feature.video.ui.components.resolveInlineSubReplyToggleLabel
import com.android.purebilibili.feature.video.ui.components.resolveVisibleSubReplies
import com.android.purebilibili.feature.video.ui.components.shouldShowInlineSubReplyToggle
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DynamicCommentOverlayHost(
    viewModel: DynamicViewModel,
    primaryItems: List<DynamicItem>,
    secondaryItems: List<DynamicItem> = emptyList(),
    toastContext: Context
) {
    val selectedDynamicId by viewModel.selectedDynamicId.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val commentsLoading by viewModel.commentsLoading.collectAsStateWithLifecycle()
    val subReplyState by viewModel.subReplyState.collectAsStateWithLifecycle()
    val liveCommentCount by viewModel.commentTotalCount.collectAsStateWithLifecycle()
    val inspectionMode = LocalInspectionMode.current

    if (!selectedDynamicId.isNullOrBlank()) {
        val dynamicId = requireNotNull(selectedDynamicId)
        val dynamicItem = remember(dynamicId, primaryItems, secondaryItems) {
            primaryItems.find { it.id_str == dynamicId }
                ?: secondaryItems.find { it.id_str == dynamicId }
        }
        val fallbackCount = dynamicItem?.modules?.module_stat?.comment?.count ?: 0
        val totalCount = remember(liveCommentCount, fallbackCount) {
            resolveDynamicCommentSheetTotalCount(
                liveCount = liveCommentCount,
                fallbackCount = fallbackCount
            )
        }

        DynamicCommentSheet(
            comments = comments,
            totalCount = totalCount,
            isLoading = commentsLoading,
            onDismiss = { viewModel.closeCommentSheet() },
            onPostComment = { message ->
                viewModel.postComment(dynamicId, message) { _, msg ->
                    if (!inspectionMode) {
                        android.widget.Toast.makeText(toastContext, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onViewReplies = { reply -> viewModel.openSubReply(reply) }
        )
    }

    DynamicSubReplyPreviewHost(
        state = subReplyState,
        onDismiss = { viewModel.closeSubReply() },
        onLoadMore = { viewModel.loadMoreSubReplies() }
    )
}

/**
 *  动态评论底部弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicCommentSheet(
    comments: List<ReplyItem>,
    totalCount: Int,  //  [新增] 总评论数
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onPostComment: (String) -> Unit,
    onViewReplies: (ReplyItem) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var commentText by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    com.android.purebilibili.core.ui.IOSModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
        ) {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "评论 ${if (totalCount > 0) "($totalCount)" else ""}",  //  [修改] 使用 totalCount
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(
                        CupertinoIcons.Default.Xmark,
                        contentDescription = "关闭",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            // 评论列表
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            } else if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            CupertinoIcons.Default.BubbleLeft,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "暂无评论",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(comments, key = { it.rpid }) { reply ->
                        CommentItem(
                            reply = reply,
                            onViewReplies = onViewReplies
                        )
                    }
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            // 评论输入框
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("发一条友善的评论", fontSize = 14.sp) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Button(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            onPostComment(commentText)
                            commentText = ""
                        }
                    },
                    enabled = commentText.isNotBlank(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("发送")
                }
            }
            
            // 底部安全区
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 *  单条评论项
 */
@Composable
private fun CommentItem(
    reply: ReplyItem,
    onViewReplies: (ReplyItem) -> Unit
) {
    val member = reply.member
    var isSubPreviewExpanded by remember(reply.rpid) { mutableStateOf(false) }
    val visibleSubReplies = remember(reply.replies, isSubPreviewExpanded) {
        resolveVisibleSubReplies(
            replies = reply.replies,
            expanded = isSubPreviewExpanded
        )
    }
    val showInlineToggle = remember(reply.replies) {
        shouldShowInlineSubReplyToggle(reply.replies.orEmpty().size)
    }
    val fanGroupVisual = remember(member) {
        resolveFanGroupVisualFromMemberAndSailing(
            member = member,
            cardBgs = resolveFanGroupDecorationCardBgs(member)
        )
    }
    
    Row(modifier = Modifier.fillMaxWidth()) {
        // 头像
        AsyncImage(
            model = member.avatar.let { 
                if (it.startsWith("http://")) it.replace("http://", "https://") else it 
            },
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            // 用户名 + 时间
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = member.uname,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (fanGroupVisual != null) Modifier.weight(1f, fill = false) else Modifier
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatTime(reply.ctime),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                )
                if (fanGroupVisual != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    FanGroupDecorationBadge(visual = fanGroupVisual)
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 评论内容 - 使用 RichCommentText 渲染表情
            val emoteMap = remember(reply.content.emote) {
                reply.content.emote?.mapValues { it.value.url } ?: emptyMap()
            }
            RichCommentText(
                text = reply.content.message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                emoteMap = emoteMap
            )

            Spacer(modifier = Modifier.height(8.dp))
            
            // 点赞数
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    CupertinoIcons.Default.HandThumbsup,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${reply.like}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                )
            }

            if (com.android.purebilibili.feature.dynamic.canOpenDynamicSubReplies(reply)) {
                if (visibleSubReplies.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        visibleSubReplies.forEach { subReply ->
                            val subEmoteMap = remember(subReply.content.emote) {
                                subReply.content.emote?.mapValues { it.value.url } ?: emptyMap()
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${subReply.member.uname}:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Box(modifier = Modifier.weight(1f)) {
                                    RichCommentText(
                                        text = subReply.content.message,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        emoteMap = subEmoteMap,
                                        maxLines = 2
                                    )
                                }
                            }
                        }

                        if (showInlineToggle) {
                            Text(
                                text = resolveInlineSubReplyToggleLabel(expanded = isSubPreviewExpanded),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { isSubPreviewExpanded = !isSubPreviewExpanded }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { onViewReplies(reply) },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "查看回复(${com.android.purebilibili.feature.dynamic.resolveDynamicSubReplyCount(reply)})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * 格式化时间戳
 */
private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - timestamp
    return when {
        diff < 60 -> "刚刚"
        diff < 3600 -> "${diff / 60}分钟前"
        diff < 86400 -> "${diff / 3600}小时前"
        diff < 604800 -> "${diff / 86400}天前"
        else -> {
            val date = java.text.SimpleDateFormat("MM-dd", java.util.Locale.CHINA)
                .format(java.util.Date(timestamp * 1000))
            date
        }
    }
}
