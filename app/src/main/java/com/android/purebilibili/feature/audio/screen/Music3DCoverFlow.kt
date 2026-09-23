package com.android.purebilibili.feature.audio.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.motion.AppMotionTokens
import com.android.purebilibili.feature.audio.player.MusicQueueItemUi
import com.android.purebilibili.feature.home.components.LiquidGlassTuning
import com.android.purebilibili.feature.home.components.biliPaiFloatingDockShell
import com.android.purebilibili.feature.home.components.resolveLiquidGlassTuning
import top.yukonga.miuix.kmp.blur.Backdrop as MiuixBackdrop
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 3D 实体 CD 盒切歌转盘 / 唱片架 (Cover Flow)。
 * 1:1 像素级复刻折叠屏半折悬停态 (Tabletop) 观赏级切歌体验。
 *
 * 核心视觉与交互特色：
 * 1. 原生 Compose 3D 透视：相机距离跟随卡片尺寸、两侧封面朝中心偏转，
 *    层叠推拉仅预绘当前项的直接相邻项。
 * 2. 实体 CD 盒质感：微圆角亚克力透光包边、左侧侧脊厚度高光（Jewel Case Spine）、
 *    顶部/底部铰链卡扣、表面斜向光斑折射、右上角时间戳徽章 (04:24)。
 * 3. 镜面地面倒影与接触阴影：短距离封面镜像以透明度遮罩消隐，并用独立接触阴影建立落地感。
 * 4. 悬浮胶囊控制条 (Pill Bar)：底栏药丸型毛玻璃一体化控制栏 (歌名 - 歌手、点赞、上一曲、播放/暂停、下一曲)。
 */
@Composable
internal fun Music3DCoverFlow(
    queue: List<MusicQueueItemUi>,
    currentIndex: Int,
    isPlaying: Boolean,
    onItemClick: (Int) -> Unit,
    onPlayPause: () -> Unit = {},
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isLiked: Boolean = false,
    onLikeClick: (() -> Unit)? = null,
    cardSizeDp: Int = 155,
    controlsWidthDp: Int = 440,
    showTransportControls: Boolean = true,
    shelfBelowControls: Boolean = false,
    progressContent: (@Composable () -> Unit)? = null,
    glassEnabled: Boolean = false,
    miuixBackdrop: MiuixBackdrop? = null,
    liquidGlassTuning: LiquidGlassTuning = resolveLiquidGlassTuning(progress = 0.5f),
    glassTintColor: Color = Color.Unspecified,
    isDarkEnvironment: Boolean = true,
    reduceMotion: Boolean = false,
) {
    if (queue.isEmpty()) return

    val validCurrentIndex = currentIndex.coerceIn(0, queue.size - 1)
    val pagerState = rememberPagerState(
        initialPage = validCurrentIndex,
        pageCount = { queue.size }
    )
    val coroutineScope = rememberCoroutineScope()
    val reflectionMaskColor = MusicContentColor
    val shelfShadowColor = MusicShadowColor
    var entrancePlayed by rememberSaveable { mutableStateOf(false) }
    val entranceProgress = androidx.compose.runtime.remember {
        Animatable(if (entrancePlayed) 1f else 0f)
    }
    val entranceSpec = AppMotionTokens.emphasizedSpec<Float>()
    val reducedMotionEntranceSpec = AppMotionTokens.standardSpec<Float>()

    LaunchedEffect(reduceMotion) {
        if (!entrancePlayed) {
            entranceProgress.animateTo(
                targetValue = 1f,
                animationSpec = if (reduceMotion) reducedMotionEntranceSpec else entranceSpec,
            )
            entrancePlayed = true
        }
    }

    LaunchedEffect(currentIndex) {
        if (currentIndex in queue.indices && pagerState.currentPage != currentIndex) {
            if (reduceMotion) pagerState.scrollToPage(currentIndex)
            else pagerState.animateScrollToPage(currentIndex)
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val shelf: @Composable () -> Unit = {
                // Measure controls first; only the shelf yields height on short panes.
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .heightIn(max = (cardSizeDp + 36).dp),
                ) {
                    val fittedCardSizeDp = minOf(cardSizeDp, (maxHeight.value - 36).toInt().coerceAtLeast(0))
                    val horizontalPadding = ((maxWidth - fittedCardSizeDp.dp) / 2).coerceAtLeast(0.dp)
                    if (fittedCardSizeDp > 0) {
                        HorizontalPager(
                            state = pagerState,
                            contentPadding = PaddingValues(horizontal = horizontalPadding),
                            beyondViewportPageCount = if (queue.size > 2) 2 else if (queue.size > 1) 1 else 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((fittedCardSizeDp + 36).dp)
                        ) { page ->
                            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                            if (abs(pageOffset) >= 3f) return@HorizontalPager
                            val item = queue[page]
                            val isCenter = abs(pageOffset) < 0.45f
                            val entranceDistance = abs(page - validCurrentIndex).toFloat()

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .zIndex(10f - abs(pageOffset))
                                    .graphicsLayer {
                                        val itemEntrance = resolveMusicCoverFlowItemEntranceProgress(
                                            overallProgress = entranceProgress.value,
                                            distanceFromCenter = entranceDistance,
                                        )
                                        // 1. 设置透视摄像机距离（深邃景深）
                                        cameraDistance = (fittedCardSizeDp * 0.075f).coerceIn(8f, 14f) * density

                                        // 2. Y 轴 3D 旋转角度（双向内旋折角，向中心聚拢呈弧形展台）
                                        rotationY = (pageOffset * -32f).coerceIn(-52f, 52f) *
                                            (if (reduceMotion) 1f else itemEntrance)

                                        // 3. 缩放景深层次：中心卡片 100%，近邻卡片 ~84%，次邻卡片 ~68%
                                        val depthScale = (1f - (abs(pageOffset) * 0.16f)).coerceIn(0.52f, 1f)
                                        val scale = depthScale *
                                            (if (reduceMotion) 1f else (0.96f + 0.04f * itemEntrance))
                                        scaleX = scale
                                        scaleY = scale

                                        // 4. 按距离均匀收拢，避免远端卡片挤成一排
                                        val offsetMag = abs(pageOffset)
                                        // Place near neighbors beside the focus and tuck outer neighbors behind them.
                                        val spacingShift = -(offsetMag - minOf(offsetMag, 1f) * 0.86f -
                                            (offsetMag - 1f).coerceAtLeast(0f) * 0.48f) * fittedCardSizeDp.dp.toPx()
                                        val finalTranslationX = if (pageOffset > 0f) -spacingShift else spacingShift
                                        translationX = finalTranslationX *
                                            (if (reduceMotion) 1f else itemEntrance)
                                        translationY = if (reduceMotion) 0f else (1f - itemEntrance) * 18.dp.toPx()

                                        // 5. 层次高保真可见度
                                        alpha = resolveMusicCoverFlowItemAlpha(abs(pageOffset)) * itemEntrance
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        coroutineScope.launch {
                                            if (reduceMotion) pagerState.scrollToPage(page)
                                            else pagerState.animateScrollToPage(page)
                                        }
                                        onItemClick(page)
                                    }
                                ) {
                                    // 实体 CD 盒主体
                                    Box(
                                        modifier = Modifier
                                            .size(fittedCardSizeDp.dp)
                                            .shadow(
                                                elevation = if (isCenter) 20.dp else 8.dp,
                                                shape = AppShapes.container(ContainerLevel.Chip),
                                                ambientColor = shelfShadowColor.copy(alpha = if (isCenter) 0.42f else 0.22f),
                                                spotColor = shelfShadowColor.copy(alpha = if (isCenter) 0.58f else 0.30f)
                                            )
                                            .clip(AppShapes.container(ContainerLevel.Chip))
                                            .border(
                                                width = 1.dp,
                                                brush = Brush.linearGradient(
                                                    listOf(
                                                        MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.52f),
                                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f),
                                                        MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.38f),
                                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                                                    )
                                                ),
                                                shape = AppShapes.container(ContainerLevel.Chip)
                                            )
                                    ) {
                                        // 唱片封面大图
                                        AsyncImage(
                                            model = item.coverUrl,
                                            contentDescription = item.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )

                                        // 实体 CD 盒左侧侧脊厚度与铰链卡扣（Jewel Case Spine & Hinges）
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .width(7.dp)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.50f),
                                                            shelfShadowColor.copy(alpha = 0.40f),
                                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f),
                                                            Color.Transparent
                                                        )
                                                    )
                                                )
                                        ) {
                                            // 顶部透明铰链卡扣
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(top = 4.dp, start = 1.dp)
                                                    .size(width = 3.dp, height = 7.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.45f),
                                                        AppShapes.container(ContainerLevel.Micro),
                                                    )
                                            )
                                            // 底部透明铰链卡扣
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(bottom = 4.dp, start = 1.dp)
                                                    .size(width = 3.dp, height = 7.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.45f),
                                                        AppShapes.container(ContainerLevel.Micro),
                                                    )
                                            )
                                        }

                                        // 亚克力塑料表面斜向光斑漫反射（Acrylic Sheen）
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.linearGradient(
                                                        0.0f to MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.10f),
                                                        0.22f to MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.05f),
                                                         0.50f to Color.Transparent
                                                    )
                                                )
                                        )
                                    }

                                    // 地面微阴影接触线（Ground Contact Shadow）
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .width((fittedCardSizeDp * 0.82f).dp)
                                            .height(6.dp)
                                            .graphicsLayer {
                                                alpha = resolveMusicCoverFlowShadowEntranceProgress(entranceProgress.value)
                                            }
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(
                                                        shelfShadowColor.copy(alpha = 0.58f),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                    )

                                    // 地面镜面微弱倒影消隐（Floor Mirror Reflection）
                                    Box(
                                        modifier = Modifier
                                            .size(width = fittedCardSizeDp.dp, height = 22.dp)
                                            .graphicsLayer {
                                                scaleY = -1f // 倒影垂直反转
                                                alpha = resolveMusicCoverFlowShadowEntranceProgress(entranceProgress.value)
                                                compositingStrategy = CompositingStrategy.Offscreen
                                            }
                                            .clip(
                                                AppShapes.topRounded(
                                                    AppShapes.containerCornerDp(ContainerLevel.Chip)
                                                )
                                            )
                                            .drawWithContent {
                                                drawContent()
                                                drawRect(
                                                    brush = Brush.verticalGradient(
                                                        0f to Color.Transparent,
                                                        1f to reflectionMaskColor.copy(alpha = 0.42f),
                                                    ),
                                                    blendMode = BlendMode.DstIn,
                                                )
                                            }
                                    ) {
                                        AsyncImage(
                                            model = item.coverUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(fittedCardSizeDp.dp),
                                            contentScale = ContentScale.Crop,
                                            alpha = 0.10f
                                        )
                                    }
                                }
                            }
                        }
                    }

                }
            }
            if (!shelfBelowControls) shelf()

            progressContent?.let { progress ->
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .widthIn(max = controlsWidthDp.dp)
                        .fillMaxWidth()
                        .graphicsLayer {
                            val chromeEntrance = resolveMusicCoverFlowShadowEntranceProgress(
                                entranceProgress.value
                            )
                            alpha = chromeEntrance
                            translationY = if (reduceMotion) 0f else (1f - chromeEntrance) * 12.dp.toPx()
                        }
                ) {
                    progress()
                }
            }

            Spacer(Modifier.height(12.dp))

            // 底部悬浮胶囊控制条（药丸毛玻璃容器 + 歌名 - 歌手 + 心形/上一首/播放/下一首）
            val playingItem = queue[validCurrentIndex]
            val pillContainerColor = resolveMusicGlassContainerColor(glassTintColor, isDarkEnvironment)
            val pillBorderColor = resolveMusicGlassBorderColor(glassTintColor, isDarkEnvironment)
            val pillContentColor = MusicContentColor

            AppSurface(
                shape = CircleShape,
                color = if (miuixBackdrop != null) Color.Transparent else pillContainerColor,
                border = BorderStroke(
                    width = 0.8.dp,
                    color = pillBorderColor
                ),
                shadowElevation = if (miuixBackdrop != null) 0.dp else 6.dp,
                modifier = Modifier
                    .widthIn(max = controlsWidthDp.dp)
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .height(56.dp)
                    .graphicsLayer {
                        val chromeEntrance = resolveMusicCoverFlowShadowEntranceProgress(
                            entranceProgress.value
                        )
                        alpha = chromeEntrance
                        translationY = if (reduceMotion) 0f else (1f - chromeEntrance) * 12.dp.toPx()
                    }
                    .biliPaiFloatingDockShell(
                        backdrop = miuixBackdrop,
                        containerColor = pillContainerColor,
                        pressProgress = 0f,
                        shape = CircleShape,
                        enabled = glassEnabled && miuixBackdrop != null,
                        blurEnabled = !glassEnabled,
                        liquidGlassTuning = liquidGlassTuning
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 18.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 歌名 - 歌手（以 " - " 分隔）
                    AppText(
                        text = "${playingItem.title} - ${playingItem.artist.ifBlank { "未知艺术家" }}",
                        color = pillContentColor.copy(alpha = 0.95f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (showTransportControls) {
                        Spacer(Modifier.width(10.dp))

                        // 点赞按钮（未点赞薄线心，已点赞红粉心）
                        onLikeClick?.let { like ->
                            AppIconButton(
                                onClick = like,
                                modifier = Modifier.size(48.dp)
                            ) {
                                AppIcon(
                                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = if (isLiked) "取消喜欢" else "喜欢",
                                    tint = if (isLiked) MusicLikeColor else pillContentColor.copy(alpha = 0.82f),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        // 上一首
                        AppIconButton(
                            onClick = onPrevious ?: {},
                            enabled = onPrevious != null,
                            modifier = Modifier.size(48.dp)
                        ) {
                            AppIcon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = "上一首",
                                tint = pillContentColor.copy(alpha = if (onPrevious != null) 0.88f else 0.28f),
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // 播放 / 暂停
                        AppIconButton(
                            onClick = onPlayPause,
                            modifier = Modifier.size(48.dp)
                        ) {
                            AppIcon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "暂停" else "播放",
                                tint = pillContentColor.copy(alpha = 0.95f),
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        // 下一首
                        AppIconButton(
                            onClick = onNext ?: {},
                            enabled = onNext != null,
                            modifier = Modifier.size(48.dp)
                        ) {
                            AppIcon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "下一首",
                                tint = pillContentColor.copy(alpha = if (onNext != null) 0.88f else 0.28f),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    } else if (onLikeClick != null) {
                        // 在大屏分屏下已由左侧主控切歌，此处仅保留单手快速点赞
                        Spacer(Modifier.width(8.dp))
                        AppIconButton(
                            onClick = onLikeClick,
                            modifier = Modifier.size(48.dp)
                        ) {
                            AppIcon(
                                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isLiked) "取消喜欢" else "喜欢",
                                tint = if (isLiked) MusicLikeColor else pillContentColor.copy(alpha = 0.82f),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
            if (shelfBelowControls) {
                Spacer(Modifier.height(20.dp))
                shelf()
            }
        }
    }
}
