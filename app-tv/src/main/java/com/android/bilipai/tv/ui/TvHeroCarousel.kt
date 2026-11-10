package com.android.bilipai.tv.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.android.bilipai.tv.tvId
import com.android.bilipai.tv.ui.components.TvAppButton
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.motion.AppMotionEasing
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val HERO_AUTO_ADVANCE_MS = 7_000L

/** 轮播取推荐流前 N 个；首页网格从其后开始，两端共用同一数值避免重复展示。 */
internal const val TV_HERO_ITEM_COUNT = 6

/** Hero actions share one focus group so content stays fixed throughout a user's decision. */
@Composable
internal fun TvHeroCarousel(
    items: List<VideoItem>,
    navigationFocus: FocusRequester,
    onPlay: (VideoItem) -> Unit,
    onOpen: (VideoItem) -> Unit,
    onAmbientChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    autoAdvanceEnabled: Boolean = true,
) {
    val reduceMotion = LocalTvReduceMotion.current
    val heroItems = items.take(TV_HERO_ITEM_COUNT)
    if (heroItems.isEmpty()) return
    val identities = heroItems.map { it.tvId() }
    var index by rememberSaveable(identities) { mutableIntStateOf(0) }
    var heroFocused by remember { mutableStateOf(false) }
    val current = heroItems[index.coerceIn(0, heroItems.lastIndex)]
    val textShadow = Shadow(color = TvMediaColors.OverlaidTextBackdrop, blurRadius = 8f)

    val interactive = LocalTvInteractive.current
    LaunchedEffect(current.pic, interactive) { if (interactive) onAmbientChange(current.pic) }
    LaunchedEffect(identities, reduceMotion, heroFocused, autoAdvanceEnabled) {
        if (reduceMotion || heroFocused || !autoAdvanceEnabled || heroItems.size < 2) return@LaunchedEffect
        while (isActive) {
            delay(HERO_AUTO_ADVANCE_MS)
            index = (index + 1) % heroItems.size
        }
    }

    Box(
        modifier
            .onFocusChanged { heroFocused = it.hasFocus }
            .focusGroup()
            .testTag("tv-hero"),
    ) {
        HeroImage(current.pic, reduceMotion, Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                // 封面完整展示后仅底部 1/5 渐隐：衔接氛围层不产生横向断层，同时不遮盖封面内容。
                drawRect(
                    brush = Brush.verticalGradient(0.78f to Color.Black, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn,
                )
            })
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(TvUiTokens.pagePadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
        ) {
            // 海报模式：标题/meta 默认隐藏，焦点进入轮播区时淡入（按钮保持可见作为焦点锚点）。
            AnimatedVisibility(
                visible = heroFocused,
                enter = fadeIn(tween(if (reduceMotion) 0 else TvMotion.enterMs, easing = AppMotionEasing.Continuity)) +
                    slideInVertically(tween(if (reduceMotion) 0 else TvMotion.enterMs, easing = AppMotionEasing.Continuity)) { if (reduceMotion) 0 else it / 3 },
                exit = fadeOut(tween(if (reduceMotion) 0 else TvMotion.exitMs, easing = AppMotionEasing.Continuity)),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small)) {
                    Text(
                        text = "为你推荐 · ${index + 1}/${heroItems.size}",
                        style = MaterialTheme.typography.labelSmall.copy(shadow = textShadow),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = current.title,
                        style = MaterialTheme.typography.headlineMedium.copy(shadow = textShadow),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(0.8f),
                    )
                    Text(
                        text = buildList {
                            current.owner.name.takeIf { it.isNotBlank() }?.let(::add)
                            if (current.duration > 0) add(FormatUtils.formatDuration(current.duration))
                            if (current.stat.view > 0) add("${FormatUtils.formatStat(current.stat.view.toLong())}播放")
                        }.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall.copy(shadow = textShadow),
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Medium),
                verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
            ) {
                TvAppButton(
                    onClick = { onPlay(current) },
                    modifier = Modifier
                        .focusProperties { left = navigationFocus }
                        .testTag("tv-hero-play"),
                ) { Text("播放") }
                TvAppButton(onClick = { onOpen(current) }, modifier = Modifier.testTag("tv-hero-details")) { Text("查看详情") }
                if (heroItems.size > 1) {
                    TvAppButton(
                        onClick = { index = (index + heroItems.size - 1) % heroItems.size },
                        modifier = Modifier.testTag("tv-hero-previous"),
                    ) { Text("上一条") }
                    TvAppButton(
                        onClick = { index = (index + 1) % heroItems.size },
                        modifier = Modifier.testTag("tv-hero-next"),
                    ) { Text("下一条") }
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall),
            modifier = Modifier.align(Alignment.TopEnd).padding(TvUiTokens.pagePadding),
        ) {
            heroItems.forEachIndexed { i, _ ->
                Box(Modifier
                    .size(width = if (i == index) AppSpacingTokens.ExtraLarge else AppSpacingTokens.Small,
                        height = AppSpacingTokens.Small)
                    .clip(CircleShape)
                    .background(if (i == index) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f)))
            }
        }
    }
}

@Composable
private fun HeroImage(url: String?, reduceMotion: Boolean, modifier: Modifier = Modifier) {
    val targetUrl = LocalTvBackdropUrl.current ?: url
    if (targetUrl.isNullOrBlank()) return
    val context = LocalContext.current
    if (reduceMotion) {
        HeroAsyncImage(targetUrl, context, modifier)
    } else {
        Crossfade(
            targetState = targetUrl,
            modifier = modifier,
            animationSpec = tween(TvMotion.backdropMs, easing = AppMotionEasing.Continuity),
            label = "hero",
        ) { target -> HeroAsyncImage(target, context, Modifier.fillMaxSize()) }
    }
}

@Composable
private fun HeroAsyncImage(url: String, context: android.content.Context, modifier: Modifier = Modifier) {
    val model = remember(url, context) {
        ImageRequest.Builder(context)
            .data(FormatUtils.fixImageUrl(url))
            .build()
    }
    AsyncImage(
        model = model,
        contentDescription = null,
        error = androidx.compose.ui.graphics.painter.ColorPainter(TvMediaColors.Base),
        // 完整展示封面（Fit）：封面按比例完整呈现，留白处透出页面氛围模糊层（同一封面高斯模糊，视觉连贯）。
        contentScale = ContentScale.Fit,
        alignment = Alignment.TopStart,
        modifier = modifier,
    )
}
