package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.WbiUtils
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.videoCardTitleMaxLines
import com.android.purebilibili.core.ui.videoCardTitleOverflow
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppCircularProgressIndicator
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.rememberAppBottomSheetMotion
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.SpaceVideoItem
import com.android.purebilibili.feature.common.resolveIndexedVideoLazyKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 竖屏详情点 UP 头像：半屏预览（关注 / 进入空间 / 近期投稿网格）。
 * 深浅色走 [resolveUpPreviewSheetSurfaceColors]。
 *
 * [onVisibilityProgressChange]：1=完全展开（播放器应上缩），0=收起；下拉时跟手回位。
 */
@Composable
fun UpPreviewSheet(
    visible: Boolean,
    owner: Owner,
    isFollowing: Boolean,
    followerCount: Int? = null,
    videoCount: Int? = null,
    likeCount: Int? = null,
    seedVideos: List<RelatedVideo> = emptyList(),
    onDismiss: () -> Unit,
    onFollowClick: () -> Unit,
    onEnterSpace: (Long) -> Unit,
    onVideoClick: (bvid: String, cid: Long) -> Unit,
    onVisibilityProgressChange: (Float) -> Unit = {},
) {
    if (!visible && owner.mid <= 0L) return

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val sheetMotion = rememberAppBottomSheetMotion()
    val colors = resolveUpPreviewSheetSurfaceColors(MaterialTheme.colorScheme)
    val sheetShape = AppShapes.container(ContainerLevel.Sheet)

    var loading by remember(owner.mid) { mutableStateOf(false) }
    var videos by remember(owner.mid) {
        mutableStateOf(
            seedVideos
                .filter { it.owner.mid == owner.mid || it.owner.mid <= 0L }
                .mapNotNull { it.toUpPreviewVideoItem() }
                .take(12)
        )
    }
    var resolvedFollower by remember(owner.mid, followerCount) {
        mutableStateOf(followerCount)
    }
    var resolvedVideoCount by remember(owner.mid, videoCount) {
        mutableStateOf(videoCount)
    }
    var resolvedLikeCount by remember(owner.mid, likeCount) {
        mutableStateOf(likeCount)
    }

    LaunchedEffect(visible, owner.mid) {
        if (!visible || owner.mid <= 0L) return@LaunchedEffect
        loading = videos.isEmpty()
        // Network on IO; Compose state only on Main (writing Snapshot state off-Main can crash).
        val fetched = withContext(Dispatchers.IO) {
            runCatching {
                var nextVideos: List<UpPreviewVideoItem>? = null
                var nextVideoCount: Int? = null
                var nextFollower: Int? = null
                var nextLikes: Int? = null
                val nav = NetworkModule.api.getNavInfo()
                val wbi = nav.data?.wbi_img
                val imgKey = wbi?.img_url?.substringAfterLast("/")?.substringBefore(".")
                val subKey = wbi?.sub_url?.substringAfterLast("/")?.substringBefore(".")
                if (!imgKey.isNullOrBlank() && !subKey.isNullOrBlank()) {
                    val videoParams = WbiUtils.sign(
                        mapOf(
                            "mid" to owner.mid.toString(),
                            "pn" to "1",
                            "ps" to "12",
                            "order" to "pubdate",
                        ),
                        imgKey,
                        subKey,
                    )
                    val videoResp = NetworkModule.spaceApi.getSpaceVideos(videoParams)
                    if (videoResp.code == 0) {
                        val list = videoResp.data?.list?.vlist.orEmpty()
                            .map { it.toUpPreviewVideoItem() }
                        if (list.isNotEmpty()) nextVideos = list
                        val count = videoResp.data?.page?.count
                        if (count != null && count > 0) nextVideoCount = count
                    }
                }
                val relation = NetworkModule.spaceApi.getRelationStat(owner.mid)
                if (relation.code == 0) {
                    relation.data?.follower?.let { nextFollower = it }
                }
                val upStat = NetworkModule.spaceApi.getUpStat(owner.mid)
                if (upStat.code == 0) {
                    upStat.data?.likes?.let {
                        nextLikes = it.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
                    }
                }
                UpPreviewFetchResult(
                    videos = nextVideos,
                    videoCount = nextVideoCount,
                    follower = nextFollower,
                    likes = nextLikes,
                )
            }.getOrNull()
        }
        if (fetched != null) {
            fetched.videos?.let { videos = it }
            fetched.videoCount?.let { resolvedVideoCount = it }
            fetched.follower?.let { resolvedFollower = it }
            fetched.likes?.let { resolvedLikeCount = it }
        }
        loading = false
    }

    com.android.purebilibili.core.ui.LocalNavigationBackHandler(enabled = visible) { onDismiss() }

    // Header + grid share one LazyVerticalGrid so the top band is not a dead zone.
    val gridState = rememberLazyGridState()
    val flingBehavior = ScrollableDefaults.flingBehavior()
    val sheetHeightDp = screenHeight * UP_PREVIEW_SHEET_HEIGHT_FRACTION
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 96.dp.toPx() }
    val maxSheetDragPx = with(density) { sheetHeightDp.toPx() }
    var sheetDragOffsetPx by remember(owner.mid) { mutableFloatStateOf(0f) }
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    val hostVisibilityProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "upPreviewHostVisibility",
    )
    val presentationProgress = resolveUpPreviewSheetVisibilityProgress(
        hostVisible = visible,
        hostVisibilityProgress = hostVisibilityProgress,
        sheetDragOffsetPx = sheetDragOffsetPx,
        sheetHeightPx = maxSheetDragPx,
    )
    val latestOnVisibilityProgressChange by rememberUpdatedState(onVisibilityProgressChange)
    LaunchedEffect(presentationProgress) {
        latestOnVisibilityProgressChange(presentationProgress)
    }
    val sheetNestedScrollConnection = remember(
        dismissThresholdPx,
        maxSheetDragPx,
    ) {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (
                    source != NestedScrollSource.UserInput ||
                    available.y >= 0f ||
                    sheetDragOffsetPx <= 0f
                ) {
                    return Offset.Zero
                }

                val consumedY = available.y.coerceAtLeast(-sheetDragOffsetPx)
                sheetDragOffsetPx = (sheetDragOffsetPx + consumedY).coerceAtLeast(0f)
                return Offset(x = 0f, y = consumedY)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput || available.y == 0f) {
                    return Offset.Zero
                }
                if (available.y > 0f) {
                    sheetDragOffsetPx =
                        (sheetDragOffsetPx + available.y).coerceAtMost(maxSheetDragPx)
                }
                // Keep the modal isolated from the detail page at every grid boundary.
                return Offset(x = 0f, y = available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (sheetDragOffsetPx <= 0f) return Velocity.Zero
                val shouldDismiss = shouldDismissUpPreviewSheet(
                    dragOffsetPx = sheetDragOffsetPx,
                    velocityYPxPerSecond = available.y,
                    dismissThresholdPx = dismissThresholdPx,
                )
                if (shouldDismiss) {
                    latestOnDismiss()
                } else {
                    sheetDragOffsetPx = 0f
                }
                return Velocity(x = 0f, y = available.y)
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity = Velocity(x = 0f, y = available.y)
        }
    }

    LaunchedEffect(visible, owner.mid) {
        if (visible) sheetDragOffsetPx = 0f
    }
    // Ensure progress clears after fully hidden (host animation finished).
    LaunchedEffect(visible, hostVisibilityProgress) {
        if (!visible && hostVisibilityProgress <= 0.001f) {
            latestOnVisibilityProgressChange(0f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(if (visible || hostVisibilityProgress > 0.001f) 8f else 0f),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = sheetMotion.scrimEnter,
            exit = sheetMotion.scrimExit,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.scrimColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    )
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = sheetMotion.contentEnter,
            exit = sheetMotion.contentExit,
        ) {
            // clip before elevation/translation so left/right top corners stay circular
            // (Surface shape alone can still let grid content paint square edges under shadow).
            AppSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sheetHeightDp)
                    .clip(sheetShape)
                    .nestedScroll(sheetNestedScrollConnection)
                    .graphicsLayer { translationY = sheetDragOffsetPx },
                shape = sheetShape,
                color = colors.sheetColor,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
            ) {
                val bottomPad = WindowInsets.navigationBars
                    .asPaddingValues()
                    .calculateBottomPadding()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    flingBehavior = flingBehavior,
                    userScrollEnabled = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = bottomPad),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        bottom = 12.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(
                        key = "up_preview_header",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        UpPreviewSheetHeader(
                            owner = owner,
                            isFollowing = isFollowing,
                            followerCount = resolvedFollower,
                            videoCount = resolvedVideoCount,
                            likeCount = resolvedLikeCount,
                            colors = colors,
                            onFollowClick = onFollowClick,
                            onEnterSpace = { onEnterSpace(owner.mid) },
                        )
                    }

                    when {
                        loading && videos.isEmpty() -> {
                            item(
                                key = "up_preview_loading",
                                span = { GridItemSpan(maxLineSpan) },
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    AppCircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 2.dp,
                                        color = colors.followFillColor,
                                    )
                                }
                            }
                        }
                        videos.isEmpty() -> {
                            item(
                                key = "up_preview_empty",
                                span = { GridItemSpan(maxLineSpan) },
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    AppText(
                                        text = "暂无投稿",
                                        color = colors.supportingColor,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                        else -> {
                            itemsIndexed(
                                items = videos,
                                key = { index, item ->
                                    resolveIndexedVideoLazyKey(
                                        namespace = "up_preview_video",
                                        index = index,
                                        bvid = item.bvid,
                                    )
                                },
                            ) { _, item ->
                                UpPreviewVideoCard(
                                    item = item,
                                    colors = colors,
                                    onClick = {
                                        resolveUpPreviewVideoClickTarget(item.bvid)?.let {
                                            onVideoClick(it.first, it.second)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class UpPreviewFetchResult(
    val videos: List<UpPreviewVideoItem>?,
    val videoCount: Int?,
    val follower: Int?,
    val likes: Int?,
)

@Composable
private fun UpPreviewSheetHeader(
    owner: Owner,
    isFollowing: Boolean,
    followerCount: Int?,
    videoCount: Int?,
    likeCount: Int?,
    colors: UpPreviewSheetSurfaceColors,
    onFollowClick: () -> Unit,
    onEnterSpace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colors.followFillColor.copy(alpha = 0.28f),
                            colors.sheetColor,
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.supportingColor.copy(alpha = 0.35f))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = FormatUtils.fixImageUrl(owner.face),
                contentDescription = "UP主头像",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.coverPlaceholderColor),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    text = owner.name.ifBlank { "UP主" },
                    color = colors.titleColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    overflow = TextOverflow.Visible,
                )
                Spacer(Modifier.height(4.dp))
                AppText(
                    text = resolveUpPreviewStatLine(
                        followerCount = followerCount,
                        videoCount = videoCount,
                        likeCount = likeCount,
                    ).ifBlank { " " },
                    color = colors.supportingColor,
                    style = MaterialTheme.typography.bodySmall,
                    overflow = TextOverflow.Visible,
                )
            }
            Spacer(Modifier.width(8.dp))
            AppSurface(
                onClick = onFollowClick,
                shape = CircleShape,
                color = if (isFollowing) {
                    colors.followingFillColor
                } else {
                    colors.followFillColor
                },
            ) {
                AppText(
                    text = if (isFollowing) "已关注" else "+ 关注",
                    color = if (isFollowing) {
                        colors.followingContentColor
                    } else {
                        colors.followContentColor
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            AppText(
                text = "进入空间  >",
                color = colors.enterSpaceColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(AppShapes.container(ContainerLevel.Field))
                    .clickable(onClick = onEnterSpace)
                    .padding(horizontal = 6.dp, vertical = 8.dp),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.dividerColor)
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun UpPreviewVideoCard(
    item: UpPreviewVideoItem,
    colors: UpPreviewSheetSurfaceColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.container(ContainerLevel.Field))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(AppShapes.container(ContainerLevel.Field))
                .background(colors.coverPlaceholderColor)
        ) {
            AsyncImage(
                model = FormatUtils.fixImageUrl(item.coverUrl),
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            // 左下播放量 / 右下时长
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    text = formatUpPreviewCount(item.playCount),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
                if (item.durationText.isNotBlank()) {
                    AppText(
                        text = item.durationText,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        AppText(
            text = item.title,
            color = colors.titleColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = videoCardTitleMaxLines(),
            overflow = videoCardTitleOverflow(),
        )
        if (item.createdAtSeconds > 0L) {
            Spacer(Modifier.height(2.dp))
            AppText(
                text = formatUpPreviewRelativeDate(item.createdAtSeconds),
                color = colors.supportingColor,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

private fun RelatedVideo.toUpPreviewVideoItem(): UpPreviewVideoItem? {
    if (bvid.isBlank()) return null
    return UpPreviewVideoItem(
        bvid = bvid,
        title = title,
        coverUrl = pic,
        playCount = stat.view,
        durationText = FormatUtils.formatDuration(duration),
        createdAtSeconds = 0L,
    )
}

private fun SpaceVideoItem.toUpPreviewVideoItem(): UpPreviewVideoItem {
    return UpPreviewVideoItem(
        bvid = bvid,
        title = title,
        coverUrl = pic,
        playCount = play,
        durationText = length,
        createdAtSeconds = created,
    )
}

private fun formatUpPreviewRelativeDate(createdAtSeconds: Long): String {
    if (createdAtSeconds <= 0L) return ""
    val nowSec = System.currentTimeMillis() / 1000L
    val delta = (nowSec - createdAtSeconds).coerceAtLeast(0L)
    val days = TimeUnit.SECONDS.toDays(delta)
    return when {
        days <= 0L -> "今天"
        days == 1L -> "昨天"
        days < 30L -> "${days}天前"
        days < 365L -> "${days / 30}个月前"
        else -> "${days / 365}年前"
    }
}
