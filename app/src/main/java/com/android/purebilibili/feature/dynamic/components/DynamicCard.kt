// 文件路径: feature/dynamic/components/DynamicCard.kt
package com.android.purebilibili.feature.dynamic.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.LocalAndroidNativeVariant
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.ui.common.CopySelectionDialog
import com.android.purebilibili.core.ui.rememberAppMoreIcon
import com.android.purebilibili.core.ui.rememberAppVisibilityOffIcon
import com.android.purebilibili.data.model.response.DynamicDesc
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DrawItem
import com.android.purebilibili.feature.dynamic.resolveDynamicActionButtonSlotWeight
import com.android.purebilibili.feature.dynamic.resolveDynamicActionButtonSpacing
import com.android.purebilibili.feature.dynamic.resolveDynamicCardContentPadding
import com.android.purebilibili.feature.dynamic.resolveDynamicCardOuterPadding
import com.android.purebilibili.data.model.response.DynamicStatModule
import com.android.purebilibili.data.model.response.DynamicType
import com.android.purebilibili.data.model.response.OpusContentBlock
import com.android.purebilibili.feature.dynamic.DynamicDeleteAction
import com.android.purebilibili.feature.dynamic.resolveDynamicDeleteAction
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 *  动态卡片V2 - 官方风格
 */
@Composable
fun DynamicCardV2(
    item: DynamicItem,
    onVideoClick: (String) -> Unit,
    onBangumiClick: (Long, Long) -> Unit = { _, _ -> },
    onUserClick: (Long) -> Unit,
    onLiveClick: (roomId: Long, title: String, uname: String) -> Unit = { _, _, _ -> },
    onArticleClick: ((articleId: Long, title: String) -> Unit)? = null,
    onDynamicDetailClick: ((dynamicId: String) -> Unit)? = null,
    isDetail: Boolean = false,
    onPrimaryClickOverride: ((DynamicItem) -> Unit)? = null,
    gifImageLoader: ImageLoader,
    //  [新增] 评论/转发/点赞回调
    onCommentClick: (dynamicId: String) -> Unit = {},
    onRepostClick: (dynamicId: String) -> Unit = {},
    onLikeClick: (dynamicId: String) -> Unit = {},
    onWatchLaterClick: ((aid: Long) -> Unit)? = null,
    onDeleteClick: ((DynamicDeleteAction) -> Unit)? = null,
    isLiked: Boolean = false
) {
    val author = item.modules.module_author
    val content = item.modules.module_dynamic
    val stat = item.modules.module_stat
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val dynamicPreviewTextVisible by SettingsManager.getDynamicImagePreviewTextVisible(context)
        .collectAsStateWithLifecycle(initialValue = true)
    val authorTimeText = remember(author?.pub_time, author?.pub_ts) {
        author?.let {
            resolveDynamicAuthorTimeText(
                pubTime = it.pub_time,
                pubTs = it.pub_ts
            )
        }.orEmpty()
    }
    val contentHasImages = content?.major?.draw?.items?.isNotEmpty() == true ||
        content?.major?.opus?.pics?.isNotEmpty() == true
    val visibleDynamicDesc = content?.desc?.let { desc ->
        resolveDynamicDescForImages(desc, hasImages = contentHasImages)
    }
    val fullOpusContentBlocks = content?.major?.opus?.let { opus ->
        resolveDynamicOpusPresentationBlocks(opus = opus, isDetail = isDetail)
    }.orEmpty()
    val hasFullOpusDetailContent = fullOpusContentBlocks.isNotEmpty()
    val type = DynamicType.fromApiValue(item.type)
    val cardClickAction = remember(item) { resolveDynamicCardPrimaryAction(item) }
    val watchLaterAid = remember(item) { resolveDynamicWatchLaterAid(item) }
    val deleteAction = remember(item) { resolveDynamicDeleteAction(item) }
    var pendingDeleteAction by remember(item.id_str) { mutableStateOf<DynamicDeleteAction?>(null) }
    val isPrimaryClickEnabled = remember(cardClickAction, onArticleClick, onDynamicDetailClick, onPrimaryClickOverride) {
        shouldEnableDynamicCardPrimaryClick(
            action = cardClickAction,
            hasArticleClick = onArticleClick != null,
            hasDynamicDetailClick = onDynamicDetailClick != null,
            hasPrimaryClickOverride = onPrimaryClickOverride != null
        )
    }

    pendingDeleteAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingDeleteAction = null },
            icon = { Icon(CupertinoIcons.Default.Trash, contentDescription = null) },
            title = { Text(action.title) },
            text = { Text(action.content) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteAction = null
                        onDeleteClick?.invoke(action)
                    }
                ) {
                    Text(action.confirmText, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteAction = null }) {
                    Text(action.cancelText)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = resolveDynamicCardOuterPadding())
            .clickable(enabled = isPrimaryClickEnabled) {
                dispatchDynamicCardPrimaryClick(
                    item = item,
                    action = cardClickAction,
                    onPrimaryClickOverride = onPrimaryClickOverride,
                    onVideoClick = onVideoClick,
                    onBangumiClick = onBangumiClick,
                    onArticleClick = onArticleClick,
                    onDynamicDetailClick = onDynamicDetailClick,
                    onUserClick = onUserClick,
                    onLiveClick = onLiveClick
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = resolveDynamicCardContentPadding())
                .padding(top = 12.dp, bottom = if (isDetail) 0.dp else 10.dp)
        ) {
        //  [新增] 更多菜单状态
        var showMoreMenu by remember { mutableStateOf(false) }
        val context = LocalContext.current
        
        //  用户头部（头像 + 名称 + 时间 + 更多）
        if (author != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                        .data(author.face.let { if (it.startsWith("http://")) it.replace("http://", "https://") else it })
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(enabled = author.mid > 0) {
                            dispatchDynamicCardPrimaryAction(
                                action = DynamicCardPrimaryAction.OpenUser(author.mid),
                                onVideoClick = onVideoClick,
                                onBangumiClick = onBangumiClick,
                                onArticleClick = onArticleClick,
                                onDynamicDetailClick = onDynamicDetailClick,
                                onUserClick = onUserClick,
                                onLiveClick = onLiveClick
                            )
                        },
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        author.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = if (author.vip?.status == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        authorTimeText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                    )
                }
                
                //  [修复] 更多按钮 + 下拉菜单
                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(
                            rememberAppMoreIcon(),
                            contentDescription = "更多",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                        )
                    }
                    
                    // 下拉菜单 - 自适应背景
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer) // 使用 surfaceContainer 获得微略不同的背景
                    ) {
                        // 复制链接
                        DropdownMenuItem(
                            text = { Text("复制链接", color = MaterialTheme.colorScheme.onSurface) },
                            leadingIcon = { 
                                Icon(
                                    CupertinoIcons.Default.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                ) 
                            },
                            onClick = {
                                showMoreMenu = false
                                // 复制动态链接到剪贴板
                                val dynamicUrl = "https://t.bilibili.com/${item.id_str}"
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("动态链接", dynamicUrl))
                                android.widget.Toast.makeText(context, "已复制链接", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                        
                        if (watchLaterAid != null && onWatchLaterClick != null) {
                            DropdownMenuItem(
                                text = { Text("稍后再看", color = MaterialTheme.colorScheme.onSurface) },
                                leadingIcon = {
                                    Icon(
                                        CupertinoIcons.Default.Clock,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onWatchLaterClick(watchLaterAid)
                                }
                            )
                        }

                        if (deleteAction != null && onDeleteClick != null) {
                            DropdownMenuItem(
                                text = { Text(deleteAction.label, color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        CupertinoIcons.Default.Trash,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    pendingDeleteAction = deleteAction
                                }
                            )
                        }

                        // 不感兴趣
                        DropdownMenuItem(
                            text = { Text("不感兴趣", color = MaterialTheme.colorScheme.onSurface) },
                            leadingIcon = { 
                                Icon(
                                    rememberAppVisibilityOffIcon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                ) 
                            },
                            onClick = {
                                showMoreMenu = false
                                android.widget.Toast.makeText(context, "已标记为不感兴趣", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        //  动态内容文字（支持@高亮）
        if (!hasFullOpusDetailContent) visibleDynamicDesc?.let { desc ->
            if (shouldRenderDynamicRichText(desc)) {
                RichTextContent(
                    desc = desc,
                    onUserClick = onUserClick
                )
                Spacer(modifier = Modifier.height(12.dp))
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
                        ?: onDynamicDetailClick?.invoke(item.id_str)
                },
                sharedElementKey = com.android.purebilibili.core.ui.transition.videoPlayerSharedElementKey(archive.bvid)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        content?.major?.pgc?.let { pgc ->
            val bangumiTarget = resolveArchiveBangumiTarget(pgc)
            VideoCardLarge(
                archive = pgc,
                publishTs = author?.pub_ts ?: 0L,
                cornerBadgeText = "番剧",
                onClick = {
                    bangumiTarget?.let { onBangumiClick(it.seasonId, it.epId) }
                        ?: onDynamicDetailClick?.invoke(item.id_str)
                },
                sharedElementKey = com.android.purebilibili.core.ui.transition.videoPlayerSharedElementKey(
                    pgc.bvid.ifBlank { item.id_str }
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        //  图片类型动态（支持GIF + 点击预览）
        content?.major?.draw?.let { draw ->
            var selectedImageIndex by remember { mutableIntStateOf(-1) }
            var sourceRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
            val drawPreviewText = remember(author?.name, visibleDynamicDesc?.text) {
                ImagePreviewTextContent(
                    headline = author?.name.orEmpty(),
                    body = visibleDynamicDesc?.text.orEmpty()
                )
            }
            
            DrawGridV2(
                items = draw.items,
                gifImageLoader = gifImageLoader,
                maxDisplayImages = if (isDetail) null else 9,
                onImageClick = { index, rect ->
                    val action = resolveDynamicCardMediaAction(item, index)
                    if (action is DynamicCardMediaAction.PreviewImages) {
                        selectedImageIndex = action.initialIndex
                        sourceRect = rect
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // 全屏图片预览
            if (selectedImageIndex >= 0) {
                ImagePreviewDialog(
                    images = draw.items.map { it.src },
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
            val visibleOpusSummaryDesc = remember(opus.summary, opus.pics) {
                opus.summary?.let { summary ->
                    resolveDynamicOpusSummaryDescForImages(
                        text = summary.text,
                        richTextNodes = summary.rich_text_nodes,
                        hasImages = opus.pics.isNotEmpty()
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
            
            // 显示标题 (如果有)
            opus.title?.let { title ->
                if (title.isNotEmpty()) {
                    Text(
                        title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            
            // 显示文字摘要 (如果有且 desc 为空)
            if (!shouldRenderDynamicRichText(visibleDynamicDesc)) {
                visibleOpusSummaryDesc?.let { summary ->
                    if (shouldRenderDynamicRichText(summary)) {
                        RichTextContent(
                            desc = summary,
                            onUserClick = onUserClick
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
            
            // 显示图片 (转换为 DrawItem 格式复用现有组件)
            if (fullOpusContentBlocks.isNotEmpty()) {
                val previewImages = remember(opus.pics) { opus.pics.map { it.url } }
                var fullContentSelectedImageIndex by remember { mutableIntStateOf(-1) }
                var fullContentImageIndex = 0
                fullOpusContentBlocks.forEach { block ->
                    when (block) {
                        is OpusContentBlock.Text -> {
                            Text(
                                text = block.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                        is OpusContentBlock.Image -> {
                            val currentImageIndex = fullContentImageIndex
                            fullContentImageIndex += 1
                            val aspectRatio = remember(block.pic.width, block.pic.height) {
                                if (block.pic.width > 0 && block.pic.height > 0) {
                                    block.pic.width.toFloat() / block.pic.height.toFloat()
                                } else {
                                    null
                                }
                            }
                            AsyncImage(
                                model = block.pic.url,
                                contentDescription = opus.title.orEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (aspectRatio != null) {
                                            Modifier.aspectRatio(aspectRatio)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = currentImageIndex in previewImages.indices) {
                                        fullContentSelectedImageIndex = currentImageIndex
                                    },
                                contentScale = ContentScale.FillWidth
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        is OpusContentBlock.LinkCard -> {
                            DynamicOpusLinkCard(
                                card = block.card,
                                modifier = Modifier.padding(bottom = 12.dp),
                                onClick = {
                                    when (val action = resolveDynamicOpusLinkCardAction(block.card)) {
                                        is DynamicOpusLinkCardAction.OpenVideo -> onVideoClick(action.videoId)
                                        is DynamicOpusLinkCardAction.OpenDynamicDetail -> onDynamicDetailClick?.invoke(action.dynamicId)
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
                            )
                        }
                    }
                }

                if (fullContentSelectedImageIndex >= 0) {
                    ImagePreviewDialog(
                        images = previewImages,
                        initialIndex = fullContentSelectedImageIndex,
                        textContent = opusPreviewText,
                        defaultTextVisible = dynamicPreviewTextVisible,
                        onDismiss = { fullContentSelectedImageIndex = -1 }
                    )
                }
            } else if (opus.pics.isNotEmpty()) {
                val drawItems = opus.pics.map { pic ->
                    DrawItem(
                        src = pic.url,
                        width = pic.width,
                        height = pic.height
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
                Spacer(modifier = Modifier.height(12.dp))
                
                // 全屏图片预览
                if (selectedImageIndex >= 0) {
                    ImagePreviewDialog(
                        images = opus.pics.map { it.url },
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
                    maxDisplayImages = if (isDetail) null else 9,
                    onImageClick = { index, rect ->
                        when (val action = resolveDynamicCardMediaAction(item, index, isDetail = isDetail)) {
                            is DynamicCardMediaAction.PreviewImages -> {
                                selectedImageIndex = action.initialIndex
                                sourceRect = rect
                            }
                            is DynamicCardMediaAction.OpenDynamicDetail -> {
                                onDynamicDetailClick?.invoke(action.dynamicId)
                            }
                            DynamicCardMediaAction.None -> Unit
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (selectedImageIndex >= 0) {
                    ImagePreviewDialog(
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
                            ?: onDynamicDetailClick?.invoke(item.id_str)
                    },
                    isCollection = true,
                    collectionTitle = season.title,
                    sharedElementKey = com.android.purebilibili.core.ui.transition.videoPlayerSharedElementKey(
                        seasonArchive.bvid
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Text(
                     "合集：${season.title}", 
                     fontWeight = FontWeight.Bold,
                     color = MaterialTheme.colorScheme.primary
                )
                 Spacer(modifier = Modifier.height(8.dp))
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
                        onDynamicDetailClick = onDynamicDetailClick,
                        onUserClick = onUserClick,
                        onLiveClick = onLiveClick
                    )
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        //  转发动态 - 嵌套显示原始内容
        if (type == DynamicType.FORWARD && item.orig != null) {
            ForwardedContent(
                orig = item.orig,
                onVideoClick = onVideoClick,
                onBangumiClick = onBangumiClick,
                onUserClick = onUserClick,
                gifImageLoader = gifImageLoader,
                defaultPreviewTextVisible = dynamicPreviewTextVisible
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        //  [修复] 底部操作栏：转发、评论、点赞 - 始终显示
        val statModule = stat ?: DynamicStatModule()  // 使用默认值避免按钮消失
        val actionButtonWeight = resolveDynamicActionButtonSlotWeight()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(resolveDynamicActionButtonSpacing())
        ) {
            // 转发按钮
            ActionButton(
                icon = io.github.alexzhirkevich.cupertino.icons.CupertinoIcons.Default.ArrowTurnUpRight,
                count = statModule.forward.count,
                label = "转发",
                enabled = !statModule.forward.forbidden,
                onClick = { onRepostClick(item.id_str) },
                modifier = Modifier.weight(actionButtonWeight)
            )
            
            // 评论按钮
            ActionButton(
                icon = io.github.alexzhirkevich.cupertino.icons.CupertinoIcons.Default.Message,
                count = statModule.comment.count,
                label = "评论",
                enabled = !statModule.comment.forbidden,
                onClick = { onCommentClick(item.id_str) },
                modifier = Modifier.weight(actionButtonWeight)
            )
            
            // 点赞按钮
            ActionButton(
                icon = if (isLiked) io.github.alexzhirkevich.cupertino.icons.CupertinoIcons.Filled.HandThumbsup
                       else io.github.alexzhirkevich.cupertino.icons.CupertinoIcons.Default.HandThumbsup,
                count = statModule.like.count,
                label = "点赞",
                isActive = isLiked,
                onClick = { onLikeClick(item.id_str) },
                modifier = Modifier.weight(actionButtonWeight)
            )
        }
        }

        if (!isDetail) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f),
                thickness = 0.7.dp
            )
        }
    }
}

/**
 *  富文本内容（支持表情、@提及、话题高亮）
 *  解析 API 返回的 rich_text_nodes 来正确渲染表情图片
 */
@Composable
fun RichTextContent(
    desc: DynamicDesc,
    onUserClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    // 收集所有表情节点以创建 InlineContent
    // 支持两种类型格式: RICH_TEXT_NODE_TYPE_EMOJI 和 EMOJI
    val emojiNodes = remember(desc.rich_text_nodes) {
        desc.rich_text_nodes
            .filter { it.type.endsWith("EMOJI") && it.emoji != null }
            .associateBy({ it.text }, { it.emoji!!.icon_url })
    }
    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val annotatedText = remember(desc, primaryColor, textColor) {
        buildDynamicRichTextAnnotatedString(
            desc = desc,
            primaryColor = primaryColor,
            textColor = textColor
        )
    }
    
    // 创建表情的 InlineContent 映射
    val inlineContent = emojiNodes.mapValues { (_, iconUrl) ->
        InlineTextContent(
            Placeholder(
                width = 1.4.em,
                height = 1.4.em,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )
        ) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(iconUrl.let { if (it.startsWith("http://")) it.replace("http://", "https://") else it })
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    val copyText = remember(desc.rich_text_nodes, desc.text) {
        val richNodeText = desc.rich_text_nodes.joinToString(separator = "") { it.text }
        richNodeText.ifBlank { desc.text }.trim()
    }
    var showCopySelectionDialog by remember(copyText) { mutableStateOf(false) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    
    Text(
        text = annotatedText,
        inlineContent = inlineContent,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = textColor,
        onTextLayout = { textLayoutResult = it },
        modifier = Modifier.pointerInput(copyText, annotatedText) {
            detectTapGestures(
                onLongPress = {
                    if (copyText.isNotEmpty()) {
                        showCopySelectionDialog = true
                    }
                },
                onTap = { offset ->
                    val layoutResult = textLayoutResult ?: return@detectTapGestures
                    val position = layoutResult.getOffsetForPosition(offset)
                    val annotation = annotatedText.getStringAnnotations(
                        tag = DYNAMIC_RICH_TEXT_URL_TAG,
                        start = maxOf(0, position - 1),
                        end = minOf(annotatedText.length, position + 1)
                    ).firstOrNull() ?: return@detectTapGestures

                    scope.launch {
                        when (resolveDynamicRichTextOpenMode(annotation.item)) {
                            DynamicRichTextOpenMode.IN_APP -> {
                                val inAppIntent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(annotation.item)
                                ).setPackage(context.packageName)
                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                val launchedInApp = runCatching {
                                    context.startActivity(inAppIntent)
                                }.isSuccess
                                if (!launchedInApp) {
                                    openDynamicRichTextLinkExternally(context, annotation.item, uriHandler)
                                }
                            }

                            DynamicRichTextOpenMode.EXTERNAL -> {
                                openDynamicRichTextLinkExternally(context, annotation.item, uriHandler)
                            }

                            null -> Unit
                        }
                    }
                }
            )
        }
    )
    if (showCopySelectionDialog) {
        CopySelectionDialog(
            text = copyText,
            title = "选择动态内容",
            onDismiss = { showCopySelectionDialog = false }
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
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // 如果有视频则跳转视频
                content?.major?.archive
                    ?.let(::resolveArchivePlayableBvid)
                    ?.let(onVideoClick)
                    ?: author?.let { onUserClick(it.mid) }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),  //  优化间距
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像
        if (author != null) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(author.face.let { if (it.startsWith("http://")) it.replace("http://", "https://") else it })
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(enabled = author.mid > 0) { onUserClick(author.mid) },
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
        }
        
        // 内容区
        Column(modifier = Modifier.weight(1f)) {
            // 用户名 + 时间
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    author?.name ?: "",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    author?.let {
                        resolveDynamicAuthorTimeText(
                            pubTime = it.pub_time,
                            pubTs = it.pub_ts
                        )
                    }.orEmpty(),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 内容预览
            Text(
                previewText,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        
        // 封面缩略图（如果有视频）
        content?.major?.archive?.let { archive ->
            Spacer(modifier = Modifier.width(12.dp))
            AsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(archive.cover.let { if (it.startsWith("http://")) it.replace("http://", "https://") else it })
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(width = 80.dp, height = 50.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}
