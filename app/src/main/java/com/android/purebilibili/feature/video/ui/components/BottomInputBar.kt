package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import com.android.purebilibili.core.ui.components.AppIcon
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.store.AppNavigationSettings
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.resolveGlobalLiquidGlassReuseEnabled
import com.android.purebilibili.core.theme.calculateContrastRatio
import com.android.purebilibili.core.ui.rememberAppBookmarkIcon
import com.android.purebilibili.core.ui.rememberAppCoinIcon
import com.android.purebilibili.core.ui.rememberAppLikeFilledIcon
import com.android.purebilibili.core.ui.rememberAppLikeIcon
import com.android.purebilibili.core.ui.rememberAppShareIcon
import com.android.purebilibili.core.ui.blur.BlurSurfaceType
import com.android.purebilibili.core.ui.blur.currentUnifiedBlurIntensity
import com.android.purebilibili.core.ui.blur.unifiedBlur
import com.android.purebilibili.feature.home.components.BottomBarMatchedReusableLiquidDock
import com.android.purebilibili.feature.home.components.resolveBottomBarSurfaceColor
import com.android.purebilibili.feature.home.components.resolveFloatingDockGeometryScale
import com.android.purebilibili.feature.home.components.resolveSharedBottomBarCapsuleShape
import dev.chrisbanes.haze.HazeState
import top.yukonga.miuix.kmp.blur.Backdrop
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import kotlinx.coroutines.flow.distinctUntilChanged

internal const val BOTTOM_INPUT_BAR_PLACEHOLDER_MIN_CONTRAST = 4.5f

internal fun resolveBottomInputBarPlaceholderTextColor(
    inputContainerColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color
): Color {
    return listOf(
        onSurfaceColor,
        onSurfaceVariantColor,
        if (inputContainerColor.luminance() < 0.5f) Color.White else Color.Black
    ).firstOrNull { candidate ->
        calculateContrastRatio(candidate, inputContainerColor) >= BOTTOM_INPUT_BAR_PLACEHOLDER_MIN_CONTRAST
    } ?: onSurfaceColor
}

/**
 * Floating liquid-glass chrome for the detail comment/action bar is gated only by the
 * global "安卓原生液态玻璃" reuse master switch (option 1).
 */
internal fun shouldUseFloatingLiquidBottomInputBar(
    androidNativeLiquidGlassEnabled: Boolean
): Boolean = resolveGlobalLiquidGlassReuseEnabled(androidNativeLiquidGlassEnabled)

/** The comment bar follows the bottom-bar blur preference when liquid glass is not active. */
internal fun shouldUseFrostedBottomInputBar(
    bottomBarBlurEnabled: Boolean,
    floatingLiquidGlass: Boolean,
    hasHazeState: Boolean
): Boolean =
    bottomBarBlurEnabled &&
        !floatingLiquidGlass &&
        hasHazeState

internal fun resolveBottomInputBarContentBottomPadding(
    showBar: Boolean,
    floatingLiquidGlass: Boolean,
    showActionButtonsFallback: Boolean
): Dp {
    if (!showBar) {
        return if (showActionButtonsFallback) 84.dp else 12.dp
    }
    return if (floatingLiquidGlass) 112.dp else 96.dp
}

internal const val BOTTOM_INPUT_BAR_ACTIONS_MIN_WIDTH_DP = 380

internal fun shouldShowBottomInputBarActionButtons(
    showActionButtons: Boolean,
    availableWidthDp: Int
): Boolean = showActionButtons && availableWidthDp >= BOTTOM_INPUT_BAR_ACTIONS_MIN_WIDTH_DP

@Composable
fun BottomInputBar(
    modifier: Modifier = Modifier,
    isLiked: Boolean,
    isFavorited: Boolean,
    isCoined: Boolean,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCoinClick: () -> Unit,
    onShareClick: () -> Unit,
    onCommentClick: () -> Unit,
    backdrop: Backdrop? = null,
    hazeState: HazeState? = null,
    isScrollInProgressProvider: () -> Boolean = { false },
    scrollPositionProvider: () -> Pair<Int, Int>? = { null },
    showActionButtons: Boolean = true,
) {
    val context = LocalContext.current
    val homeSettings by SettingsManager
        .getHomeSettings(context)
        .collectAsStateWithLifecycle(initialValue = HomeSettings())
    val appNavigationSettings by SettingsManager
        .getAppNavigationSettings(context)
        .collectAsStateWithLifecycle(initialValue = AppNavigationSettings())
    val autoHideOnScroll = appNavigationSettings.bottomBarVisibilityMode ==
        SettingsManager.BottomBarVisibilityMode.SCROLL_HIDE
    var isVisible by remember { mutableStateOf(true) }
    val currentScrollPositionProvider by rememberUpdatedState(scrollPositionProvider)

    LaunchedEffect(autoHideOnScroll) {
        if (!autoHideOnScroll) {
            isVisible = true
            return@LaunchedEffect
        }
        var previousPosition = currentScrollPositionProvider() ?: return@LaunchedEffect
        snapshotFlow { currentScrollPositionProvider() }
            .distinctUntilChanged()
            .collect { currentPosition ->
                currentPosition ?: return@collect
                val (previousItem, previousOffset) = previousPosition
                val (currentItem, currentOffset) = currentPosition
                isVisible = when {
                    currentItem == 0 && currentOffset < 100 -> true
                    currentItem > previousItem -> false
                    currentItem < previousItem -> true
                    currentOffset > previousOffset + 24 -> false
                    currentOffset < previousOffset - 24 -> true
                    else -> isVisible
                }
                previousPosition = currentPosition
            }
    }
    val floatingLiquidGlass = shouldUseFloatingLiquidBottomInputBar(
        androidNativeLiquidGlassEnabled = homeSettings.androidNativeLiquidGlassEnabled
    )
    val frostedBottomBar = shouldUseFrostedBottomInputBar(
        bottomBarBlurEnabled = homeSettings.isBottomBarBlurEnabled,
        floatingLiquidGlass = floatingLiquidGlass,
        hasHazeState = hazeState != null
    )

    BoxWithConstraints(modifier = modifier) {
        val availableWidthDp = maxWidth.value.toInt()
        val effectiveShowActionButtons = shouldShowBottomInputBarActionButtons(
            showActionButtons = showActionButtons,
            availableWidthDp = availableWidthDp,
        )

        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier,
        ) {
            if (floatingLiquidGlass) {
                FloatingLiquidBottomInputBar(
                    modifier = Modifier,
                    backdrop = backdrop,
                    isLiked = isLiked,
                    isFavorited = isFavorited,
                    isCoined = isCoined,
                    onLikeClick = onLikeClick,
                    onFavoriteClick = onFavoriteClick,
                    onCoinClick = onCoinClick,
                    onShareClick = onShareClick,
                    onCommentClick = onCommentClick,
                    isScrollInProgressProvider = isScrollInProgressProvider,
                    showActionButtons = effectiveShowActionButtons,
                )
            } else {
                DockedSolidBottomInputBar(
                    modifier = Modifier,
                    hazeState = hazeState,
                    frostedBottomBar = frostedBottomBar,
                    isLiked = isLiked,
                    isFavorited = isFavorited,
                    isCoined = isCoined,
                    onLikeClick = onLikeClick,
                    onFavoriteClick = onFavoriteClick,
                    onCoinClick = onCoinClick,
                    onShareClick = onShareClick,
                    onCommentClick = onCommentClick,
                    showActionButtons = effectiveShowActionButtons,
                )
            }
        }
    }
}

@Composable
private fun DockedSolidBottomInputBar(
    modifier: Modifier,
    hazeState: HazeState?,
    frostedBottomBar: Boolean,
    isLiked: Boolean,
    isFavorited: Boolean,
    isCoined: Boolean,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCoinClick: () -> Unit,
    onShareClick: () -> Unit,
    onCommentClick: () -> Unit,
    showActionButtons: Boolean = true,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val bottomBarColor = resolveBottomBarSurfaceColor(
        surfaceColor = surfaceColor,
        blurEnabled = frostedBottomBar,
        blurIntensity = currentUnifiedBlurIntensity()
    )
    val inputContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val inputTextColor = resolveBottomInputBarPlaceholderTextColor(
        inputContainerColor = inputContainerColor,
        onSurfaceColor = MaterialTheme.colorScheme.onSurface,
        onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    AppSurface(
        color = bottomBarColor,
        tonalElevation = if (frostedBottomBar) 0.dp else 8.dp,
        shadowElevation = if (frostedBottomBar) 0.dp else 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (frostedBottomBar && hazeState != null) {
                    Modifier.unifiedBlur(
                        hazeState = hazeState,
                        surfaceType = BlurSurfaceType.BOTTOM_BAR
                    )
                } else {
                    Modifier
                }
            )
    ) {
        BottomInputBarContentRow(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            inputContainerColor = inputContainerColor,
            inputTextColor = inputTextColor,
            isLiked = isLiked,
            isFavorited = isFavorited,
            isCoined = isCoined,
            onLikeClick = onLikeClick,
            onFavoriteClick = onFavoriteClick,
            onCoinClick = onCoinClick,
            onShareClick = onShareClick,
            onCommentClick = onCommentClick,
            showActionButtons = showActionButtons,
        )
    }
}

@Composable
private fun FloatingLiquidBottomInputBar(
    modifier: Modifier,
    backdrop: Backdrop?,
    isLiked: Boolean,
    isFavorited: Boolean,
    isCoined: Boolean,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCoinClick: () -> Unit,
    onShareClick: () -> Unit,
    onCommentClick: () -> Unit,
    isScrollInProgressProvider: () -> Boolean,
    showActionButtons: Boolean = true,
) {
    val shellShape = resolveSharedBottomBarCapsuleShape()
    val inputTextColor = resolveBottomInputBarPlaceholderTextColor(
        inputContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        onSurfaceColor = MaterialTheme.colorScheme.onSurface,
        onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val navigationBarBottomPadding = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    val bottomInset = 12.dp + navigationBarBottomPadding

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Keep the gesture area transparent so the detail list can draw beneath it.
        // Only the interactive capsules are inset; the page already owns the background.
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = bottomInset)
                .widthIn(max = if (showActionButtons) 360.dp else 420.dp)
                .fillMaxWidth(),
            horizontalArrangement = if (showActionButtons) Arrangement.spacedBy(8.dp) else Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 黑虾线防回归：左右是两个视觉上独立的胶囊，必须分别渲染，不能合成长壳；
            // 两边都保留 lens，并按 44dp 实际高度缩放折射。直接关 lens 会损失液态玻璃，
            // 使用满强度 64dp 几何则会让上下 refraction 在短胶囊中线相撞。
            BottomBarMatchedReusableLiquidDock(
                shape = shellShape,
                modifier = if (showActionButtons) {
                    Modifier
                        .weight(0.9f)
                        .height(44.dp)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                },
                backdrop = backdrop,
                reuseEnabled = true,
                drawShellLens = true,
                shellLensIntensity = resolveFloatingDockGeometryScale(44f),
                isScrollInProgressProvider = isScrollInProgressProvider,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(role = Role.Button) { onCommentClick() }
                        .padding(horizontal = if (showActionButtons) 12.dp else 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AppIcon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = inputTextColor,
                        modifier = Modifier.size(20.dp),
                    )
                    AppText(
                        text = if (showActionButtons) "写评论" else "发一条友善的评论…",
                        color = inputTextColor,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (showActionButtons) {
                BottomBarMatchedReusableLiquidDock(
                    shape = shellShape,
                    modifier = Modifier
                        .weight(1.1f)
                        .height(44.dp),
                    backdrop = backdrop,
                    reuseEnabled = true,
                    drawShellLens = true,
                    shellLensIntensity = resolveFloatingDockGeometryScale(44f),
                    isScrollInProgressProvider = isScrollInProgressProvider,
                ) {
                    BottomInputBarActionButtons(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 6.dp),
                        itemSize = 32.dp,
                        iconSize = 19.dp,
                        spreadItems = true,
                        favoriteIcon = rememberAppBookmarkIcon(),
                        coinIcon = rememberAppCoinIcon(),
                        likeIcon = rememberAppLikeIcon(),
                        likeFilledIcon = rememberAppLikeFilledIcon(),
                        shareIcon = rememberAppShareIcon(),
                        isLiked = isLiked,
                        isFavorited = isFavorited,
                        isCoined = isCoined,
                        onLikeClick = onLikeClick,
                        onFavoriteClick = onFavoriteClick,
                        onCoinClick = onCoinClick,
                        onShareClick = onShareClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomInputBarContentRow(
    modifier: Modifier,
    inputContainerColor: Color,
    inputTextColor: Color,
    isLiked: Boolean,
    isFavorited: Boolean,
    isCoined: Boolean,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCoinClick: () -> Unit,
    onShareClick: () -> Unit,
    onCommentClick: () -> Unit,
    showActionButtons: Boolean = true,
) {
    val favoriteIcon = rememberAppBookmarkIcon()
    val coinIcon = rememberAppCoinIcon()
    val likeIcon = rememberAppLikeIcon()
    val likeFilledIcon = rememberAppLikeFilledIcon()
    val shareIcon = rememberAppShareIcon()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(AppShapes.container(ContainerLevel.Card))
                .background(inputContainerColor)
                .clickable(role = Role.Button) { onCommentClick() }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppIcon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = inputTextColor,
                    modifier = Modifier.size(18.dp)
                )
                AppText(
                    text = "发一条友善的评论…",
                    color = inputTextColor,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (showActionButtons) {
            Spacer(modifier = Modifier.width(8.dp))

            BottomInputBarActionButtons(
                favoriteIcon = favoriteIcon,
                coinIcon = coinIcon,
                likeIcon = likeIcon,
                likeFilledIcon = likeFilledIcon,
                shareIcon = shareIcon,
                isLiked = isLiked,
                isFavorited = isFavorited,
                isCoined = isCoined,
                onLikeClick = onLikeClick,
                onFavoriteClick = onFavoriteClick,
                onCoinClick = onCoinClick,
                onShareClick = onShareClick
            )
        }
    }
}

@Composable
private fun BottomInputBarActionButtons(
    modifier: Modifier = Modifier,
    itemSize: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    itemSpacing: Dp = 4.dp,
    spreadItems: Boolean = false,
    favoriteIcon: ImageVector,
    coinIcon: ImageVector,
    likeIcon: ImageVector,
    likeFilledIcon: ImageVector,
    shareIcon: ImageVector,
    isLiked: Boolean,
    isFavorited: Boolean,
    isCoined: Boolean,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCoinClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = if (spreadItems) {
            Arrangement.SpaceEvenly
        } else {
            Arrangement.spacedBy(itemSpacing)
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconActionButton(
            icon = if (isLiked) likeFilledIcon else likeIcon,
            label = "点赞",
            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            onClick = onLikeClick,
            itemSize = itemSize,
            iconSize = iconSize,
            showLabel = false
        )
        IconActionButton(
            icon = coinIcon,
            label = "投币",
            tint = if (isCoined) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            onClick = onCoinClick,
            itemSize = itemSize,
            iconSize = iconSize,
            showLabel = false
        )
        IconActionButton(
            icon = favoriteIcon,
            label = "收藏",
            tint = if (isFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            onClick = onFavoriteClick,
            itemSize = itemSize,
            iconSize = iconSize,
            showLabel = false
        )
        IconActionButton(
            icon = shareIcon,
            label = "分享",
            tint = MaterialTheme.colorScheme.onSurface,
            onClick = onShareClick,
            itemSize = itemSize,
            iconSize = iconSize,
            showLabel = false
        )
    }
}

@Composable
private fun IconActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    itemSize: Dp,
    iconSize: Dp,
    showLabel: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .sizeIn(minWidth = itemSize, minHeight = itemSize)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(4.dp)
    ) {
        AppIcon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
        if (showLabel) {
            Spacer(modifier = Modifier.height(2.dp))
            AppText(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint
            )
        }
    }
}
