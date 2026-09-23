package com.android.purebilibili.core.ui.skeleton

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as lazyGridItems
import androidx.compose.foundation.lazy.items as lazyListItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel

/**
 * 内容列表首屏骨架（视频网格 / 媒体行 / 用户行）。
 * 动画对齐首页推荐：柔和 alpha 脉冲，不用左右扫光 shimmer，避免闪烁。
 */

@Composable
fun rememberContentSkeletonPulse(): Float {
    if (com.android.purebilibili.core.ui.skeleton.rememberSkeletonBreathingEnabled()) {
        return com.android.purebilibili.core.ui.skeleton.rememberGentleSkeletonPulse().value
    }
    val transition = rememberInfiniteTransition(label = "contentSkeletonPulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = CONTENT_SKELETON_PULSE_DURATION_MILLIS,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "contentSkeletonPulseAlpha",
    )
    return pulse
}

@Composable
fun rememberContentSkeletonBlockColor(pulse: Float): Color {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val onSurface = MaterialTheme.colorScheme.onSurface
    return remember(pulse, isDark, onSurface) {
        val alpha = if (isDark) {
            CONTENT_SKELETON_DARK_MIN_ALPHA +
                (CONTENT_SKELETON_DARK_MAX_ALPHA - CONTENT_SKELETON_DARK_MIN_ALPHA) * pulse
        } else {
            CONTENT_SKELETON_LIGHT_MIN_ALPHA +
                (CONTENT_SKELETON_LIGHT_MAX_ALPHA - CONTENT_SKELETON_LIGHT_MIN_ALPHA) * pulse
        }
        onSurface.copy(alpha = alpha)
    }
}

@Composable
fun ContentSkeletonBlock(
    color: Color,
    modifier: Modifier = Modifier,
    shape: Shape = AppShapes.container(ContainerLevel.Tag),
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(color),
    )
}

@Composable
fun ContentVideoGridSkeleton(
    modifier: Modifier = Modifier,
    minItemWidth: Dp = 160.dp,
    coverAspectRatio: Float = 16f / 10f,
    itemCount: Int = 8,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    horizontalSpacing: Dp = 8.dp,
    verticalSpacing: Dp = 8.dp,
) {
    val pulse = rememberContentSkeletonPulse()
    val blockColor = rememberContentSkeletonBlockColor(pulse)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minItemWidth),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        userScrollEnabled = false,
        modifier = modifier.fillMaxSize(),
    ) {
        val skeletonKeys = List(itemCount.coerceAtLeast(0)) { it }
        lazyGridItems(
            items = skeletonKeys,
            key = { "content_video_grid_skeleton_$it" },
            contentType = { "content_video_grid_skeleton" },
        ) {
            ContentVideoGridItemSkeleton(
                coverAspectRatio = coverAspectRatio,
                blockColor = blockColor,
            )
        }
    }
}

@Composable
fun ContentVideoGridSkeletonFixedColumns(
    columns: Int,
    modifier: Modifier = Modifier,
    coverAspectRatio: Float = 16f / 10f,
    rows: Int = 4,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    spacing: Dp = 8.dp,
) {
    val safeColumns = columns.coerceAtLeast(1)
    val skeletonKeys = List(safeColumns * rows.coerceAtLeast(1)) { it }
    val pulse = rememberContentSkeletonPulse()
    val blockColor = rememberContentSkeletonBlockColor(pulse)
    LazyVerticalGrid(
        columns = GridCells.Fixed(safeColumns),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing),
        userScrollEnabled = false,
        modifier = modifier.fillMaxSize(),
    ) {
        lazyGridItems(
            items = skeletonKeys,
            key = { "content_video_fixed_skeleton_$it" },
            contentType = { "content_video_fixed_skeleton" },
        ) {
            ContentVideoGridItemSkeleton(
                coverAspectRatio = coverAspectRatio,
                blockColor = blockColor,
            )
        }
    }
}

@Composable
fun ContentVideoGridItemSkeleton(
    coverAspectRatio: Float = 4f / 3f,
    blockColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
    ) {
        ContentSkeletonBlock(
            color = blockColor,
            shape = AppShapes.container(ContainerLevel.Chip),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(coverAspectRatio),
        )
        Spacer(modifier = Modifier.height(8.dp))
        ContentSkeletonBlock(
            color = blockColor,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(16.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))
        ContentSkeletonBlock(
            color = blockColor,
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(12.dp),
        )
    }
}

/** 横向媒体行：左封面 + 右侧标题/副标题，适配直播/专栏/番剧/话题列表。 */
@Composable
fun MediaListRowSkeleton(
    modifier: Modifier = Modifier,
    coverWidth: Dp = 128.dp,
    coverAspectRatio: Float = 16f / 10f,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    contentGap: Dp = 12.dp,
    blockColor: Color? = null,
) {
    val pulse = if (blockColor == null) rememberContentSkeletonPulse() else 0f
    val color = blockColor ?: rememberContentSkeletonBlockColor(pulse)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContentSkeletonBlock(
            color = color,
            shape = AppShapes.container(ContainerLevel.Chip),
            modifier = Modifier
                .width(coverWidth)
                .aspectRatio(coverAspectRatio),
        )
        Spacer(modifier = Modifier.width(contentGap))
        Column(modifier = Modifier.weight(1f)) {
            ContentSkeletonBlock(
                color = color,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(16.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            ContentSkeletonBlock(
                color = color,
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .height(12.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            ContentSkeletonBlock(
                color = color,
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp),
            )
        }
    }
}

/** 用户行：头像 + 两行文案，适配 UP / 主播搜索。 */
@Composable
fun UserListRowSkeleton(
    modifier: Modifier = Modifier,
    avatarSize: Dp = 48.dp,
    blockColor: Color? = null,
) {
    val pulse = if (blockColor == null) rememberContentSkeletonPulse() else 0f
    val color = blockColor ?: rememberContentSkeletonBlockColor(pulse)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContentSkeletonBlock(
            color = color,
            shape = CircleShape,
            modifier = Modifier.size(avatarSize),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            ContentSkeletonBlock(
                color = color,
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(14.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            ContentSkeletonBlock(
                color = color,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(12.dp),
            )
        }
    }
}

/** 评论行：头像、昵称、正文和操作区，供评论区首次加载使用。 */
@Composable
fun CommentListItemSkeleton(
    modifier: Modifier = Modifier,
    blockColor: Color? = null,
) {
    val pulse = if (blockColor == null) rememberContentSkeletonPulse() else 0f
    val color = blockColor ?: rememberContentSkeletonBlockColor(pulse)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        ContentSkeletonBlock(
            color = color,
            shape = CircleShape,
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            ContentSkeletonBlock(
                color = color,
                modifier = Modifier
                    .fillMaxWidth(0.34f)
                    .height(14.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            ContentSkeletonBlock(
                color = color,
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .height(14.dp),
            )
            Spacer(modifier = Modifier.height(7.dp))
            ContentSkeletonBlock(
                color = color,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(14.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            ContentSkeletonBlock(
                color = color,
                modifier = Modifier
                    .fillMaxWidth(0.42f)
                    .height(12.dp),
            )
        }
    }
}

@Composable
fun CommentListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 6,
    contentPadding: PaddingValues = PaddingValues(vertical = 4.dp),
) {
    val pulse = rememberContentSkeletonPulse()
    val blockColor = rememberContentSkeletonBlockColor(pulse)
    val skeletonKeys = List(itemCount.coerceAtLeast(0)) { it }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        userScrollEnabled = false,
    ) {
        lazyListItems(
            items = skeletonKeys,
            key = { "comment_list_skeleton_$it" },
            contentType = { "comment_list_skeleton" },
        ) {
            CommentListItemSkeleton(blockColor = blockColor)
        }
    }
}

/** 可嵌入现有 LazyColumn 的评论骨架组，整组只使用一个脉冲时钟。 */
@Composable
fun CommentListColumnSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 5,
) {
    val pulse = rememberContentSkeletonPulse()
    val blockColor = rememberContentSkeletonBlockColor(pulse)
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(itemCount.coerceAtLeast(0)) {
            CommentListItemSkeleton(blockColor = blockColor)
        }
    }
}

@Composable
fun ContentMediaListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 8,
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp),
    useUserRow: Boolean = false,
    mediaRowCoverWidth: Dp = 128.dp,
    mediaRowCoverAspectRatio: Float = 16f / 10f,
    mediaRowContentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    mediaRowContentGap: Dp = 12.dp,
) {
    val pulse = rememberContentSkeletonPulse()
    val blockColor = rememberContentSkeletonBlockColor(pulse)
    val skeletonKeys = List(itemCount.coerceAtLeast(0)) { it }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        userScrollEnabled = false,
    ) {
        lazyListItems(
            items = skeletonKeys,
            key = { "content_media_list_skeleton_$it" },
            contentType = { if (useUserRow) "user_row_skeleton" else "media_row_skeleton" },
        ) {
            if (useUserRow) {
                UserListRowSkeleton(blockColor = blockColor)
            } else {
                MediaListRowSkeleton(
                    coverWidth = mediaRowCoverWidth,
                    coverAspectRatio = mediaRowCoverAspectRatio,
                    contentPadding = mediaRowContentPadding,
                    contentGap = mediaRowContentGap,
                    blockColor = blockColor,
                )
            }
        }
    }
}

/** 文章详情首屏：标题、作者信息与正文段落，避免用居中转圈造成内容跳变。 */
@Composable
fun ArticleDetailSkeleton(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
) {
    val pulse = rememberContentSkeletonPulse()
    val blockColor = rememberContentSkeletonBlockColor(pulse)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        userScrollEnabled = false,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ContentSkeletonBlock(blockColor, Modifier.fillMaxWidth(0.92f).height(28.dp))
            Spacer(Modifier.height(10.dp))
            ContentSkeletonBlock(blockColor, Modifier.fillMaxWidth(0.68f).height(28.dp))
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ContentSkeletonBlock(blockColor, Modifier.size(42.dp), CircleShape)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    ContentSkeletonBlock(blockColor, Modifier.fillMaxWidth(0.36f).height(14.dp))
                    Spacer(Modifier.height(7.dp))
                    ContentSkeletonBlock(blockColor, Modifier.fillMaxWidth(0.52f).height(12.dp))
                }
            }
        }
        lazyListItems(List(7) { it }) { index ->
            ContentSkeletonBlock(
                blockColor,
                Modifier
                    .fillMaxWidth(if (index % 3 == 2) 0.72f else 1f)
                    .height(16.dp),
            )
        }
    }
}

/** 海报型详情首屏：番剧封面、标题资料、操作区与后续章节。 */
@Composable
fun PosterDetailSkeleton(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    val pulse = rememberContentSkeletonPulse()
    val blockColor = rememberContentSkeletonBlockColor(pulse)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        userScrollEnabled = false,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row {
                ContentSkeletonBlock(
                    blockColor,
                    Modifier.width(132.dp).aspectRatio(0.75f),
                    AppShapes.container(ContainerLevel.Card),
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ContentSkeletonBlock(blockColor, Modifier.fillMaxWidth(0.9f).height(24.dp))
                    ContentSkeletonBlock(blockColor, Modifier.fillMaxWidth(0.58f).height(14.dp))
                    ContentSkeletonBlock(blockColor, Modifier.fillMaxWidth(0.72f).height(14.dp))
                    ContentSkeletonBlock(blockColor, Modifier.widthIn(min = 96.dp).fillMaxWidth(0.48f).height(40.dp))
                }
            }
        }
        item { ContentSkeletonBlock(blockColor, Modifier.fillMaxWidth().height(44.dp)) }
        item { ContentSkeletonBlock(blockColor, Modifier.fillMaxWidth(0.34f).height(20.dp)) }
        lazyListItems(List(4) { it }) { index ->
            ContentSkeletonBlock(
                blockColor,
                Modifier.fillMaxWidth(if (index == 3) 0.66f else 1f).height(15.dp),
            )
        }
    }
}

/** 分类入口网格：图标块 + 分类名，对齐直播“全部标签”等目录页。 */
@Composable
fun ContentCategoryGridSkeleton(
    columns: Int,
    modifier: Modifier = Modifier,
    rows: Int = 4,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    spacing: Dp = 16.dp,
) {
    val safeColumns = columns.coerceAtLeast(1)
    val pulse = rememberContentSkeletonPulse()
    val blockColor = rememberContentSkeletonBlockColor(pulse)
    LazyVerticalGrid(
        columns = GridCells.Fixed(safeColumns),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        userScrollEnabled = false,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        lazyGridItems(List(safeColumns * rows.coerceAtLeast(1)) { it }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ContentSkeletonBlock(
                    blockColor,
                    Modifier.fillMaxWidth().aspectRatio(1f),
                    AppShapes.container(ContainerLevel.Card),
                )
                Spacer(Modifier.height(8.dp))
                ContentSkeletonBlock(blockColor, Modifier.fillMaxWidth(0.62f).height(13.dp))
            }
        }
    }
}

/** 热搜页首屏：顶部视觉横幅 + 排名文本行。 */
@Composable
fun TrendingListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 10,
    contentPadding: PaddingValues = PaddingValues(bottom = 24.dp),
) {
    val pulse = rememberContentSkeletonPulse()
    val blockColor = rememberContentSkeletonBlockColor(pulse)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        userScrollEnabled = false,
    ) {
        item {
            ContentSkeletonBlock(
                blockColor,
                Modifier.fillMaxWidth().height(240.dp),
                AppShapes.container(ContainerLevel.Card),
            )
        }
        lazyListItems(List(itemCount.coerceAtLeast(0)) { it }) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ContentSkeletonBlock(blockColor, Modifier.width(24.dp).height(18.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    ContentSkeletonBlock(
                        blockColor,
                        Modifier.fillMaxWidth(if (index % 3 == 0) 0.82f else 0.64f).height(16.dp),
                    )
                    Spacer(Modifier.height(7.dp))
                    ContentSkeletonBlock(blockColor, Modifier.fillMaxWidth(0.38f).height(11.dp))
                }
            }
        }
    }
}

private const val CONTENT_SKELETON_PULSE_DURATION_MILLIS = 2_000
private const val CONTENT_SKELETON_LIGHT_MIN_ALPHA = 0.06f
private const val CONTENT_SKELETON_LIGHT_MAX_ALPHA = 0.11f
private const val CONTENT_SKELETON_DARK_MIN_ALPHA = 0.10f
private const val CONTENT_SKELETON_DARK_MAX_ALPHA = 0.16f
