package com.android.purebilibili.feature.dynamic.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.transition.LocalVideoCardSharedElementSourceRoute
import com.android.purebilibili.core.ui.transition.resolveVideoCardSharedTransitionMotionSpec
import com.android.purebilibili.core.ui.transition.resolveVideoSharedTransitionVisualSpec
import com.android.purebilibili.core.ui.transition.videoCoverSharedElementKey
import com.android.purebilibili.core.ui.transition.videoTitleSharedElementKey
import com.android.purebilibili.core.util.CardPositionManager
import com.android.purebilibili.data.model.response.ArchiveMajor
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.PlayCircle

/**
 * 对齐 PiliPlus 的动态视频呈现：
 * 1. 手机和平板都保持纵向视频卡
 * 2. 封面使用 16:10 比例
 * 3. 统计信息压到封面渐变层，正文只保留标题信息
 */
@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun VideoCardLarge(
    archive: ArchiveMajor,
    onClick: () -> Unit,
    publishTs: Long = 0L,
    isCollection: Boolean = false,
    collectionTitle: String = "",
    cornerBadgeText: String? = null,
    sharedElementKey: Any? = null
) {
    val context = LocalContext.current
    val coverUrl = remember(archive.cover) { normalizeDynamicCoverUrl(archive.cover) }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = remember(configuration.screenWidthDp, density) {
        with(density) { configuration.screenWidthDp.dp.toPx() }
    }
    val screenHeightPx = remember(configuration.screenHeightDp, density) {
        with(density) { configuration.screenHeightDp.dp.toPx() }
    }
    val sourceRoute = LocalVideoCardSharedElementSourceRoute.current
    val cardBoundsRef = remember { object { var value: androidx.compose.ui.geometry.Rect? = null } }
    val triggerClick = {
        cardBoundsRef.value?.let { bounds ->
            CardPositionManager.recordVideoCardPosition(
                bvid = archive.bvid,
                sourceRoute = sourceRoute,
                bounds = bounds,
                screenWidth = screenWidthPx,
                screenHeight = screenHeightPx,
                density = density.density,
                sourceCornerDp = 10
            )
        }
        onClick()
    }

    var modifier = Modifier
        .fillMaxWidth()
        .onGloballyPositioned { coordinates ->
            cardBoundsRef.value = coordinates.boundsInRoot()
        }
        .clickable(onClick = triggerClick)

    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val sharedElementReady = archive.bvid.isNotBlank() &&
        sourceRoute != null &&
        sharedTransitionScope != null &&
        animatedVisibilityScope != null
    val sharedTransitionMotionSpec = remember(sourceRoute) {
        resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = sourceRoute,
            transitionEnabled = true
        )
    }
    val effectiveSharedElementKey = if (sharedElementReady) {
        videoCoverSharedElementKey(archive.bvid, sourceRoute = sourceRoute)
    } else {
        sharedElementKey
    }
    val sharedTransitionVisualSpec = remember(sourceRoute) {
        resolveVideoSharedTransitionVisualSpec(
            sourceRoute = sourceRoute,
            sourceCornerDp = 10
        )
    }
    val coverShape = RoundedCornerShape(sharedTransitionVisualSpec.sourceCornerDp.dp)
    val coverModifier = if (effectiveSharedElementKey != null && sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = effectiveSharedElementKey),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ ->
                    if (sharedTransitionMotionSpec.enabled) {
                        tween(
                            durationMillis = sharedTransitionMotionSpec.durationMillis,
                            easing = sharedTransitionMotionSpec.easing
                        )
                    } else {
                        com.android.purebilibili.core.ui.motion.AppMotionTokens.spatialSpec()
                    }
                },
                clipInOverlayDuringTransition = OverlayClip(coverShape)
            )
        }
    } else {
        Modifier
    }
    val titleModifier = if (sharedElementReady) {
        with(requireNotNull(sharedTransitionScope)) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = videoTitleSharedElementKey(archive.bvid)),
                animatedVisibilityScope = requireNotNull(animatedVisibilityScope),
                boundsTransform = { _, _ -> com.android.purebilibili.core.ui.motion.AppMotionTokens.spatialSpec() }
            )
        }
    } else {
        Modifier
    }

    Column(modifier = modifier) {
        VideoCardLargeCover(
            archive = archive,
            coverUrl = coverUrl,
            context = context,
            isCollection = isCollection,
            cornerBadgeText = cornerBadgeText,
            coverShape = coverShape,
            modifier = coverModifier
        )
        Spacer(modifier = Modifier.height(6.dp))
        VideoCardLargeInfo(
            archive = archive,
            isCollection = isCollection,
            collectionTitle = collectionTitle,
            publishTs = publishTs,
            titleModifier = titleModifier
        )
    }
}

@Composable
private fun VideoCardLargeCover(
    archive: ArchiveMajor,
    coverUrl: String,
    context: android.content.Context,
    isCollection: Boolean,
    cornerBadgeText: String?,
    coverShape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .clip(coverShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (coverUrl.isNotEmpty()) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(context)
                    .data(coverUrl)
                    .addHeader("Referer", "https://www.bilibili.com/")
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        val badgeText = cornerBadgeText ?: if (isCollection) "合集" else null
        if (!badgeText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(72.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (archive.duration_text.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = archive.duration_text,
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.size(6.dp))
                }

                VideoCardLargeMetaText(text = "${archive.stat.play}播放")
                Spacer(modifier = Modifier.size(6.dp))
                VideoCardLargeMetaText(text = "${archive.stat.danmaku}弹幕")
                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.Black.copy(alpha = 0.28f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CupertinoIcons.Filled.PlayCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoCardLargeInfo(
    archive: ArchiveMajor,
    isCollection: Boolean,
    collectionTitle: String,
    publishTs: Long,
    titleModifier: Modifier = Modifier
) {
    if (isCollection && collectionTitle.isNotBlank()) {
        Text(
            text = collectionTitle,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = archive.title,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = titleModifier
        )
    } else {
        Text(
            text = archive.title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 21.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = titleModifier
        )
    }
}

@Composable
private fun VideoCardLargeMetaText(
    text: String
) {
    Text(
        text = text,
        fontSize = 11.sp,
        color = Color.White,
        maxLines = 1
    )
}

@Composable
fun VideoCardSmall(
    archive: ArchiveMajor,
    publishTs: Long = 0L,
    onClick: () -> Unit
) {
    VideoCardLarge(
        archive = archive,
        onClick = onClick,
        publishTs = publishTs
    )
}

private fun normalizeDynamicCoverUrl(rawCover: String): String {
    val raw = rawCover.trim()
    return when {
        raw.startsWith("https://") -> raw
        raw.startsWith("http://") -> raw.replace("http://", "https://")
        raw.startsWith("//") -> "https:$raw"
        raw.isNotEmpty() -> "https://$raw"
        else -> ""
    }
}
