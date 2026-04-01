// 文件路径: feature/home/components/BottomBar.kt
package com.android.purebilibili.feature.home.components

// Duplicate import removed
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.luminance
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.combinedClickable  // [新增] 组合点击支持
import androidx.compose.foundation.ExperimentalFoundationApi // [新增]
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuOpen
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer  //  晃动动画
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.android.purebilibili.R
import com.android.purebilibili.feature.home.components.LiquidIndicator
import com.android.purebilibili.navigation.ScreenRoutes
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import com.android.purebilibili.core.ui.blur.shouldAllowDirectHazeLiquidGlassFallback
import com.android.purebilibili.core.ui.blur.shouldAllowHomeChromeLiquidGlass
import com.android.purebilibili.core.ui.blur.unifiedBlur
import com.android.purebilibili.core.ui.blur.currentUnifiedBlurIntensity
import com.android.purebilibili.core.ui.blur.BlurStyles
import com.android.purebilibili.core.ui.blur.BlurSurfaceType
import com.android.purebilibili.core.ui.adaptive.MotionTier
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.core.theme.iOSSystemGray
import com.android.purebilibili.core.theme.BottomBarColors  // 统一底栏颜色配置
import com.android.purebilibili.core.theme.BottomBarColorPalette  // 调色板
import com.android.purebilibili.core.theme.LocalCornerRadiusScale
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.iOSCornerRadius
import kotlinx.coroutines.launch  //  延迟导航
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import com.android.purebilibili.core.ui.animation.rememberDampedDragAnimationState
import com.android.purebilibili.core.ui.animation.horizontalDragGesture
import dev.chrisbanes.haze.hazeEffect // [New]
import dev.chrisbanes.haze.HazeStyle   // [New]
// [LayerBackdrop] AndroidLiquidGlass library for real background refraction
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.foundation.shape.RoundedCornerShape as RoundedCornerShapeAlias
import androidx.compose.ui.Modifier.Companion.then
import dev.chrisbanes.haze.hazeSource
import com.android.purebilibili.core.ui.effect.liquidGlass
import com.android.purebilibili.core.store.LiquidGlassStyle // [New] Top-level enum
import com.android.purebilibili.core.store.LiquidGlassMode
import androidx.compose.foundation.isSystemInDarkTheme // [New] Theme detection for adaptive readability
import kotlin.math.sign

/**
 * 底部导航项枚举 -  使用 iOS SF Symbols 风格图标
 * [HIG] 所有图标包含 contentDescription 用于无障碍访问
 */
enum class BottomNavItem(
    val label: String,
    @StringRes val labelRes: Int,
    @StringRes val contentDescriptionRes: Int,
    val legacyAliases: List<String> = emptyList(),
    val selectedIcon: @Composable () -> Unit,
    val unselectedIcon: @Composable () -> Unit,
    val route: String // [新增] 路由地址
) {
    HOME(
        "首页",
        R.string.bottom_nav_home,
        R.string.bottom_nav_home,
        emptyList(),
        { Icon(CupertinoIcons.Outlined.House, contentDescription = null) },
        { Icon(CupertinoIcons.Outlined.House, contentDescription = null) },
        ScreenRoutes.Home.route
    ),
    DYNAMIC(
        "动态",
        R.string.bottom_nav_dynamic,
        R.string.bottom_nav_dynamic,
        emptyList(),
        { Icon(CupertinoIcons.Outlined.Bell, contentDescription = null) },
        { Icon(CupertinoIcons.Outlined.Bell, contentDescription = null) },
        ScreenRoutes.Dynamic.route
    ),
    STORY(
        "短视频",
        R.string.bottom_nav_story,
        R.string.bottom_nav_story,
        emptyList(),
        { Icon(CupertinoIcons.Filled.PlayCircle, contentDescription = null) },
        { Icon(CupertinoIcons.Outlined.PlayCircle, contentDescription = null) },
        ScreenRoutes.Story.route
    ),
    HISTORY(
        "历史",
        R.string.bottom_nav_history,
        R.string.bottom_nav_history_desc,
        listOf("历史记录"),
        { Icon(CupertinoIcons.Filled.Clock, contentDescription = null) },
        { Icon(CupertinoIcons.Outlined.Clock, contentDescription = null) },
        ScreenRoutes.History.route
    ),
    PROFILE(
        "我的",
        R.string.bottom_nav_profile,
        R.string.bottom_nav_profile_desc,
        listOf("个人中心"),
        { Icon(CupertinoIcons.Outlined.Person, contentDescription = null) },
        { Icon(CupertinoIcons.Outlined.Person, contentDescription = null) },
        ScreenRoutes.Profile.route
    ),
    FAVORITE(
        "收藏",
        R.string.bottom_nav_favorite,
        R.string.bottom_nav_favorite_desc,
        listOf("收藏夹"),
        { Icon(CupertinoIcons.Filled.Star, contentDescription = null) },
        { Icon(CupertinoIcons.Outlined.Star, contentDescription = null) },
        ScreenRoutes.Favorite.route
    ),
    LIVE(
        "直播",
        R.string.bottom_nav_live,
        R.string.bottom_nav_live,
        emptyList(),
        { Icon(CupertinoIcons.Filled.Video, contentDescription = null) },
        { Icon(CupertinoIcons.Outlined.Video, contentDescription = null) },
        ScreenRoutes.LiveList.route
    ),
    WATCHLATER(
        "稍后看",
        R.string.bottom_nav_watch_later,
        R.string.bottom_nav_watch_later_desc,
        listOf("稍后再看"),
        { Icon(CupertinoIcons.Filled.Bookmark, contentDescription = null) },
        { Icon(CupertinoIcons.Outlined.Bookmark, contentDescription = null) },
        ScreenRoutes.WatchLater.route
    ),
    SETTINGS(
        "设置",
        R.string.bottom_nav_settings,
        R.string.bottom_nav_settings,
        emptyList(),
        { Icon(CupertinoIcons.Filled.Gearshape, contentDescription = null) },
        { Icon(CupertinoIcons.Default.Gearshape, contentDescription = null) },
        ScreenRoutes.Settings.route
    )
}

@Composable
internal fun resolveBottomNavItemLabel(item: BottomNavItem): String = stringResource(item.labelRes)

@Composable
internal fun resolveBottomNavItemContentDescription(item: BottomNavItem): String =
    stringResource(item.contentDescriptionRes)

internal fun resolveBottomNavItemLookupKeys(item: BottomNavItem): Set<String> {
    return linkedSetOf(
        item.name,
        item.name.lowercase(),
        item.name.uppercase(),
        item.route,
        item.route.lowercase(),
        item.route.uppercase(),
        item.label,
        item.label.lowercase(),
        *item.legacyAliases.toTypedArray()
    )
}

internal data class BottomBarLayoutPolicy(
    val horizontalPadding: Dp,
    val rowPadding: Dp,
    val maxBarWidth: Dp
)

internal fun resolveBottomBarFloatingHeightDp(
    labelMode: Int,
    isTablet: Boolean
): Float {
    return when (labelMode) {
        0 -> if (isTablet) 72f else 66f
        2 -> if (isTablet) 54f else 52f
        else -> if (isTablet) 64f else 58f
    }
}

internal fun normalizeBottomBarLabelMode(requestedLabelMode: Int): Int = when (requestedLabelMode) {
    0, 1, 2 -> requestedLabelMode
    else -> 0
}

internal fun shouldShowBottomBarIcon(labelMode: Int): Boolean {
    return when (normalizeBottomBarLabelMode(labelMode)) {
        2 -> false
        else -> true
    }
}

internal fun shouldShowBottomBarText(labelMode: Int): Boolean {
    return when (normalizeBottomBarLabelMode(labelMode)) {
        1 -> false
        else -> true
    }
}

internal fun resolveBottomBarBottomPaddingDp(
    isFloating: Boolean,
    isTablet: Boolean
): Float {
    if (!isFloating) return 0f
    return if (isTablet) 18f else 12f
}

internal data class BottomBarIndicatorPolicy(
    val widthMultiplier: Float,
    val minWidthDp: Float,
    val maxWidthDp: Float,
    val maxWidthToItemRatio: Float,
    val clampToBounds: Boolean,
    val edgeInsetDp: Float
)

internal data class BottomBarIndicatorVisualPolicy(
    val isInMotion: Boolean,
    val shouldRefract: Boolean,
    val useNeutralTint: Boolean
)

internal data class BottomBarRefractionLayerPolicy(
    val captureTintedContentLayer: Boolean,
    val useCombinedBackdrop: Boolean
)

internal data class BottomBarRefractionMotionProfile(
    val progress: Float,
    val exportPanelOffsetFraction: Float,
    val indicatorPanelOffsetFraction: Float,
    val visiblePanelOffsetFraction: Float,
    val visibleSelectionEmphasis: Float,
    val exportSelectionEmphasis: Float,
    val exportCaptureWidthScale: Float,
    val forceChromaticAberration: Boolean,
    val indicatorLensAmountScale: Float,
    val indicatorLensHeightScale: Float
)

internal fun resolveBottomBarIndicatorVisualPolicy(
    position: Float,
    isDragging: Boolean,
    velocity: Float,
    useNeutralIndicatorTint: Boolean
): BottomBarIndicatorVisualPolicy {
    val isFractional = abs(position - position.roundToInt().toFloat()) > 0.001f
    val isInMotion = isDragging || isFractional || abs(velocity) > 45f
    return BottomBarIndicatorVisualPolicy(
        isInMotion = isInMotion,
        shouldRefract = isInMotion,
        useNeutralTint = isInMotion && useNeutralIndicatorTint
    )
}

internal fun resolveBottomBarRefractionLayerPolicy(
    isFloating: Boolean,
    isLiquidGlassEnabled: Boolean,
    indicatorVisualPolicy: BottomBarIndicatorVisualPolicy
): BottomBarRefractionLayerPolicy {
    val captureTintedContentLayer =
        isFloating && isLiquidGlassEnabled && indicatorVisualPolicy.shouldRefract
    return BottomBarRefractionLayerPolicy(
        captureTintedContentLayer = captureTintedContentLayer,
        useCombinedBackdrop = false
    )
}

internal fun resolveBottomBarRefractionMotionProfile(
    position: Float,
    velocity: Float,
    isDragging: Boolean
): BottomBarRefractionMotionProfile {
    val signedFractionalOffset = position - position.roundToInt().toFloat()
    val fractionalProgress = (abs(signedFractionalOffset) * 2f).coerceIn(0f, 1f)
    val speedProgress = (abs(velocity) / 1400f).coerceIn(0f, 1f)
    val baseProgress = fractionalProgress.coerceAtLeast(speedProgress)
    val rawProgress = when {
        isDragging -> baseProgress.coerceAtLeast(0.18f)
        baseProgress > 0.03f -> baseProgress
        else -> 0f
    }
    if (rawProgress <= 0f) {
        return BottomBarRefractionMotionProfile(
            progress = 0f,
            exportPanelOffsetFraction = 0f,
            indicatorPanelOffsetFraction = 0f,
            visiblePanelOffsetFraction = 0f,
            visibleSelectionEmphasis = 1f,
            exportSelectionEmphasis = 1f,
            exportCaptureWidthScale = 1f,
            forceChromaticAberration = false,
            indicatorLensAmountScale = 1f,
            indicatorLensHeightScale = 1f
        )
    }

    val progress = (rawProgress * rawProgress * (3f - 2f * rawProgress)).coerceIn(0f, 1f)
    val direction = when {
        abs(velocity) > 24f -> sign(velocity)
        abs(signedFractionalOffset) > 0.001f -> sign(signedFractionalOffset)
        else -> 0f
    }

    return BottomBarRefractionMotionProfile(
        progress = progress,
        exportPanelOffsetFraction = 0f,
        indicatorPanelOffsetFraction = 0f,
        visiblePanelOffsetFraction = 0f,
        visibleSelectionEmphasis = lerp(1f, 0.38f, progress),
        exportSelectionEmphasis = 1f,
        exportCaptureWidthScale = 1f,
        forceChromaticAberration = progress > 0.05f,
        indicatorLensAmountScale = lerp(1f, 0.26f, progress),
        indicatorLensHeightScale = lerp(1f, 0.18f, progress)
    )
}

internal fun resolveBottomBarMovingIndicatorSurfaceColor(isDarkTheme: Boolean): Color {
    return if (isDarkTheme) {
        Color(0xFFF6F8FB)
    } else {
        Color(0xFFFDFEFF)
    }
}

internal fun resolveBottomBarChromeMaterialMode(
    showGlassEffect: Boolean,
    hasBlur: Boolean
): TopTabMaterialMode {
    return when {
        showGlassEffect -> TopTabMaterialMode.LIQUID_GLASS
        hasBlur -> TopTabMaterialMode.BLUR
        else -> TopTabMaterialMode.PLAIN
    }
}

internal fun resolveBottomBarIndicatorTintAlpha(
    shouldRefract: Boolean,
    liquidGlassProgress: Float,
    configuredAlpha: Float
): Float {
    if (shouldRefract) return configuredAlpha
    val minAlpha = lerp(
        start = 0.24f,
        stop = 0.32f,
        fraction = liquidGlassProgress.coerceIn(0f, 1f)
    )
    return configuredAlpha.coerceAtLeast(minAlpha)
}

internal fun resolveBottomBarIndicatorTintAlpha(
    shouldRefract: Boolean,
    liquidGlassMode: LiquidGlassMode,
    configuredAlpha: Float
): Float {
    return resolveBottomBarIndicatorTintAlpha(
        shouldRefract = shouldRefract,
        liquidGlassProgress = when (liquidGlassMode) {
            LiquidGlassMode.CLEAR -> 0f
            LiquidGlassMode.BALANCED -> 0.5f
            LiquidGlassMode.FROSTED -> 1f
        },
        configuredAlpha = configuredAlpha
    )
}

internal fun resolveBottomBarIndicatorPolicy(itemCount: Int): BottomBarIndicatorPolicy {
    val topTuning = resolveTopTabVisualTuning()
    return if (itemCount >= 5) {
        BottomBarIndicatorPolicy(
            widthMultiplier = topTuning.floatingIndicatorWidthMultiplier + 0.02f,
            minWidthDp = topTuning.floatingIndicatorMinWidthDp + 2f,
            maxWidthDp = topTuning.floatingIndicatorMaxWidthDp + 2f,
            maxWidthToItemRatio = topTuning.floatingIndicatorMaxWidthToItemRatio + 0.02f,
            clampToBounds = true,
            edgeInsetDp = 2f
        )
    } else {
        BottomBarIndicatorPolicy(
            widthMultiplier = topTuning.floatingIndicatorWidthMultiplier + 0.04f,
            minWidthDp = topTuning.floatingIndicatorMinWidthDp + 4f,
            maxWidthDp = topTuning.floatingIndicatorMaxWidthDp + 4f,
            maxWidthToItemRatio = topTuning.floatingIndicatorMaxWidthToItemRatio + 0.04f,
            clampToBounds = true,
            edgeInsetDp = 2f
        )
    }
}

internal fun resolveBottomIndicatorHeightDp(
    labelMode: Int,
    isTablet: Boolean,
    itemCount: Int
): Float {
    return when {
        labelMode == 0 && isTablet && itemCount >= 5 -> 56f
        labelMode == 0 && isTablet -> 60f
        labelMode == 0 && itemCount >= 5 -> 50f
        labelMode == 0 -> 58f
        else -> 54f
    }
}

internal fun resolveBottomBarLayoutPolicy(
    containerWidth: Dp,
    itemCount: Int,
    isTablet: Boolean,
    labelMode: Int,
    isFloating: Boolean
): BottomBarLayoutPolicy {
    if (!isFloating) {
        return BottomBarLayoutPolicy(
            horizontalPadding = 0.dp,
            rowPadding = 20.dp,
            maxBarWidth = containerWidth
        )
    }

    val safeItemCount = itemCount.coerceAtLeast(1)
    val rowPadding = when {
        isTablet && safeItemCount >= 6 -> 16.dp
        isTablet -> 18.dp
        safeItemCount >= 5 -> 12.dp
        else -> 16.dp
    }
    val normalizedLabelMode = when (labelMode) {
        0, 1, 2 -> labelMode
        else -> 0
    }
    val minItemWidth = when (normalizedLabelMode) {
        0 -> if (isTablet) 62.dp else 52.dp
        2 -> if (isTablet) 60.dp else 52.dp
        else -> if (isTablet) 58.dp else 50.dp
    }
    val preferredItemWidth = when (normalizedLabelMode) {
        0 -> if (isTablet) 84.dp else 80.dp
        2 -> if (isTablet) 80.dp else 74.dp
        else -> if (isTablet) 76.dp else 72.dp
    }
    val minBarWidth = (rowPadding * 2) + (minItemWidth * safeItemCount)
    val preferredBarWidth = (rowPadding * 2) + (preferredItemWidth * safeItemCount)

    val phoneRatio = when {
        safeItemCount >= 6 -> 0.84f
        safeItemCount == 5 -> 0.88f
        safeItemCount == 4 -> 0.92f
        else -> 0.93f
    }
    val widthRatio = if (isTablet) 0.86f else phoneRatio
    val visualCap = containerWidth * widthRatio
    val hardCap = if (isTablet) 640.dp else 432.dp
    val minEdgePadding = if (isTablet) 16.dp else 10.dp
    val containerCap = (containerWidth - (minEdgePadding * 2)).coerceAtLeast(0.dp)
    val maxAllowed = minOf(hardCap, visualCap, containerCap)

    val resolvedBarWidth = maxOf(
        minBarWidth,
        minOf(preferredBarWidth, maxAllowed)
    ).coerceAtMost(containerWidth)

    val horizontalPadding = ((containerWidth - resolvedBarWidth) / 2).coerceAtLeast(0.dp)
    return BottomBarLayoutPolicy(
        horizontalPadding = horizontalPadding,
        rowPadding = rowPadding,
        maxBarWidth = resolvedBarWidth
    )
}

/**
 *  iOS 风格磨砂玻璃底部导航栏
 * 
 * 特性：
 * - 实时磨砂玻璃效果 (使用 Haze 库)
 * - 悬浮圆角设计
 * - 自动适配深色/浅色模式
 * -  点击触觉反馈
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun FrostedBottomBar(
    currentItem: BottomNavItem = BottomNavItem.HOME,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    isFloating: Boolean = true,
    labelMode: Int = 1,
    homeSettings: com.android.purebilibili.core.store.HomeSettings = com.android.purebilibili.core.store.HomeSettings(),
    onHomeDoubleTap: () -> Unit = {},
    onDynamicDoubleTap: () -> Unit = {},
    visibleItems: List<BottomNavItem> = listOf(BottomNavItem.HOME, BottomNavItem.DYNAMIC, BottomNavItem.HISTORY, BottomNavItem.PROFILE),
    itemColorIndices: Map<String, Int> = emptyMap(),
    onToggleSidebar: (() -> Unit)? = null,
    // [NEW] Scroll offset for liquid glass refraction effect
    scrollOffset: Float = 0f,
    // [NEW] LayerBackdrop for real background refraction (captures content behind the bar)
    backdrop: LayerBackdrop? = null,
    motionTier: MotionTier = MotionTier.Normal,
    isTransitionRunning: Boolean = false,
    forceLowBlurBudget: Boolean = false
) {
    if (LocalUiPreset.current == UiPreset.MD3) {
        MaterialBottomBar(
            currentItem = currentItem,
            onItemClick = onItemClick,
            modifier = modifier,
            visibleItems = visibleItems,
            onToggleSidebar = onToggleSidebar,
            isTablet = com.android.purebilibili.core.util.LocalWindowSizeClass.current.isTablet,
            labelMode = labelMode,
            blurEnabled = hazeState != null,
            hazeState = hazeState,
            motionTier = motionTier,
            isTransitionRunning = isTransitionRunning,
            forceLowBlurBudget = forceLowBlurBudget
        )
        return
    }

    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f // Simple darkness check
    val haptic = rememberHapticFeedback()
    
    // 🔒 [防抖]
    var lastClickTime by remember { mutableStateOf(0L) }
    var lastClickedItem by remember { mutableStateOf<BottomNavItem?>(null) }
    val debounceClick: (BottomNavItem, () -> Unit) -> Unit = remember {
        { item, action ->
            val currentTime = System.currentTimeMillis()
            if (
                shouldAcceptBottomBarTap(
                    tappedItem = item,
                    lastTappedItem = lastClickedItem,
                    currentTimeMillis = currentTime,
                    lastTapTimeMillis = lastClickTime,
                    debounceWindowMillis = 200L
                )
            ) {
                lastClickTime = currentTime
                lastClickedItem = item
                action()
            }
        }
    }
    
    // 📐 [平板适配]
    val windowSizeClass = com.android.purebilibili.core.util.LocalWindowSizeClass.current
    val isTablet = windowSizeClass.isTablet
    
    // 背景颜色
    val blurIntensity = com.android.purebilibili.core.ui.blur.currentUnifiedBlurIntensity()
    val isActivelyScrolling = kotlin.math.abs(scrollOffset) >= 6f
    
    // 📐 高度计算
    val floatingHeight = resolveBottomBarFloatingHeightDp(
        labelMode = labelMode,
        isTablet = isTablet
    ).dp
    val dockedHeight = when (labelMode) {
        0 -> if (isTablet) 72.dp else 72.dp
        2 -> if (isTablet) 52.dp else 56.dp
        else -> if (isTablet) 64.dp else 64.dp
    }
    
    // 📐 这里把 BoxWithConstraints 提到顶层，以便计算 itemWidth 和 indicator 参数
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
    ) {
        val totalWidth = maxWidth
        // 📐 下边距
        val barBottomPadding = resolveBottomBarBottomPaddingDp(
            isFloating = isFloating,
            isTablet = isTablet
        ).dp
        
        // [平板适配] 侧边栏按钮也算作一个 Item，确保指示器宽度与内容一致。
        val sidebarCount = if (isTablet && onToggleSidebar != null) 1 else 0
        val itemCount = visibleItems.size + sidebarCount
        val layoutPolicy = resolveBottomBarLayoutPolicy(
            containerWidth = totalWidth,
            itemCount = itemCount,
            isTablet = isTablet,
            labelMode = labelMode,
            isFloating = isFloating
        )
        val barHorizontalPadding = layoutPolicy.horizontalPadding
        val rowPadding = layoutPolicy.rowPadding
        val targetMaxWidth = layoutPolicy.maxBarWidth
        
        // 内容宽度需减去 padding
        // 注意：isFloating 时 padding 在 Box 上，docked 时无 padding
        // 但这里我们是在 BoxWithConstraints 内部计算，TotalWidth 是包含 padding 的吗？
        // Modifier 传给了 BottomBar，BoxWithConstraints 用了 modifier。
        // 如果 modifier 有 padding，maxWidth 会减小。
        // 原逻辑是在 internal Box 计算 padding。
        
        // 重新计算可用宽度
        val availableWidth = if (isFloating) {
             totalWidth - (barHorizontalPadding * 2)
        } else {
             totalWidth
        }
        val renderedBarWidth = if (isFloating) minOf(availableWidth, targetMaxWidth) else availableWidth
        val contentWidth = (renderedBarWidth - (rowPadding * 2)).coerceAtLeast(0.dp)
        val itemWidth = if (itemCount > 0) contentWidth / itemCount else 0.dp
        
        // 📐 状态提升：DampedDragAnimationState
        val selectedIndex = visibleItems.indexOf(currentItem)
        val dampedDragState = rememberDampedDragAnimationState(
            initialIndex = if (selectedIndex >= 0) selectedIndex else 0,
            itemCount = itemCount,
            onIndexChanged = { index -> 
                if (index in visibleItems.indices) {
                    onItemClick(visibleItems[index])
                } else if (isTablet && onToggleSidebar != null && index == visibleItems.size) {
                    // [Feature] Slide to trigger sidebar
                    onToggleSidebar()
                }
            }
        )
        
        val isValidSelection = selectedIndex >= 0
        val indicatorAlpha by animateFloatAsState(
            targetValue = if (isValidSelection) 1f else 0f,
            label = "indicatorAlpha"
        )
        
        LaunchedEffect(selectedIndex) {
            if (isValidSelection) {
                dampedDragState.updateIndex(selectedIndex)
            }
        }
        
        // 📐 计算指示器位置和变形参数 (用于 Shader)
        val density = LocalDensity.current
        val indicatorWidthPx = with(density) { 90.dp.toPx() }  // Synced with LiquidIndicator
        val indicatorHeightPx = with(density) { 52.dp.toPx() } // Synced with LiquidIndicator
        val itemWidthPx = with(density) { itemWidth.toPx() }
        val startPaddingPx = with(density) { rowPadding.toPx() }
        
        // CenterX: padding + (currentPos * width) + half_width
        // 但这里还需要考虑 Row 的 offset。Row 是居中的。
        // 如果 widthIn(max=640) 生效，content 居中，indicator 坐标也需要偏移?
        // 简化起见，我们假设 LiquidGlass 应用于 "Container Box"，该 Box 与 Content 是一一对应的尺寸。
        // 下面的 UI 结构中，Haze Box 是 widthIn(max=640)，居中。
        // 因此 Shader 坐标系应该是以 Haze Box 为准。
        
        val indicatorCenterX = startPaddingPx + dampedDragState.value * itemWidthPx + (itemWidthPx / 2f)
        val indicatorCenterY = with(density) { (if(isFloating) floatingHeight else dockedHeight).toPx() / 2f }
        
        // 变形逻辑
        val velocity = dampedDragState.velocity
        val velocityFraction = (velocity / 3000f).coerceIn(-1f, 1f)
        val deformation = abs(velocityFraction) * 0.4f
        val targetScaleX = 1f + deformation
        val targetScaleY = 1f - (deformation * 0.6f)
        
        // Animate scales with High Viscosity (Slower response, less bounce)
        val scaleX by animateFloatAsState(targetValue = targetScaleX, animationSpec = spring(dampingRatio = 0.85f, stiffness = 350f), label = "scaleX")
        val scaleY by animateFloatAsState(targetValue = targetScaleY, animationSpec = spring(dampingRatio = 0.85f, stiffness = 350f), label = "scaleY")
        val dragScale by animateFloatAsState(targetValue = if (dampedDragState.isDragging) 1.0f else 1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f), label = "dragScale")

        val finalScaleX = scaleX * dragScale
        val finalScaleY = scaleY * dragScale
        
        // [Fix] Dynamic Refraction & Aberration Intensity
        // Only refract when moving. Static = 0 intensity.
        val isMoving = dampedDragState.isDragging || abs(dampedDragState.velocity) > 50f
        val isDarkTheme = isSystemInDarkTheme()
        // [Restored] Full intensity for both themes - readability handled via text color
        val targetIntensity = if (isMoving) 0.85f else 0f
        val animatedIntensity by animateFloatAsState(
            targetValue = targetIntensity, 
            animationSpec = spring(dampingRatio = 1f, stiffness = 400f), 
            label = "intensity"
        )
        
        // [New] Dynamic Chromatic Aberration (RGB Split)
        // Intensity increases with speed, simulating stress on glass
        // [Adaptive] Reduced in light mode for cleaner look
        val aberrationStrength = if (isDarkTheme) {
            (abs(velocityFraction) * 0.025f).coerceIn(0f, 0.05f)
        } else {
            (abs(velocityFraction) * 0.012f).coerceIn(0f, 0.02f) // Light: subtle aberration
        }
        val animatedAberration by animateFloatAsState(
            targetValue = if (isMoving) aberrationStrength else 0f,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
            label = "aberration"
        )
        
        // 圆角
        val cornerRadiusScale = com.android.purebilibili.core.theme.LocalCornerRadiusScale.current
        val floatingCornerRadius = com.android.purebilibili.core.theme.iOSCornerRadius.Floating * cornerRadiusScale
        val barShape = if (isFloating) RoundedCornerShape(floatingCornerRadius + 8.dp) else RoundedCornerShape(0.dp)
        
        // 垂直偏移
        // 统一对齐策略：所有模式使用同一基线，避免图标与文字上下错位
        val contentVerticalOffset = 0.dp

    // [Fix] 确保指示器互斥显示的最终逻辑
    // 当底栏停靠时，强制禁用液态玻璃（Liquid Glass），仅使用标准磨砂（Frosted Glass）
    val showGlassEffect = homeSettings.isLiquidGlassEnabled && isFloating
    val liquidGlassTuning = remember(
        homeSettings.liquidGlassProgress,
        homeSettings.liquidGlassStyle
    ) {
        resolveLiquidGlassTuning(progress = homeSettings.liquidGlassProgress)
    }
    val contentLuminance = remember(showGlassEffect, liquidGlassTuning.progress, isDarkTheme) {
        if (showGlassEffect && liquidGlassTuning.progress > 0.72f) {
            if (isDarkTheme) 0.18f else 0.82f
        } else 0f
    }
    val isGlassSupported = remember { shouldAllowHomeChromeLiquidGlass(android.os.Build.VERSION.SDK_INT) }
    val allowHazeLiquidGlassFallback = remember {
        shouldAllowDirectHazeLiquidGlassFallback(android.os.Build.VERSION.SDK_INT)
    }
    val bottomChromeMaterialMode = resolveBottomBarChromeMaterialMode(
        showGlassEffect = showGlassEffect,
        hasBlur = hazeState != null
    )
    val barColor = resolveBottomBarContainerColor(
        surfaceColor = MaterialTheme.colorScheme.surface,
        blurEnabled = hazeState != null,
        blurIntensity = blurIntensity,
        liquidGlassProgress = liquidGlassTuning.progress,
        isGlassEffectEnabled = showGlassEffect
    )
    val bottomChromeRenderMode = remember(
        bottomChromeMaterialMode,
        isGlassSupported,
        backdrop,
        hazeState,
        allowHazeLiquidGlassFallback
    ) {
        resolveHomeTopChromeRenderMode(
            materialMode = bottomChromeMaterialMode,
            isGlassSupported = isGlassSupported,
            hasBackdrop = backdrop != null,
            hasHazeState = hazeState != null,
            allowHazeLiquidGlassFallback = allowHazeLiquidGlassFallback
        )
    }
    // [Refraction] 图标+文字模式下，提高镜片高度并轻微下移，让标签文字稳定进入折射区域
    val bottomIndicatorHeight = resolveBottomIndicatorHeightDp(
        labelMode = labelMode,
        isTablet = isTablet,
        itemCount = itemCount
    ).dp
    // Keep indicator vertically centered; avoid extra offset that breaks top/bottom spacing.
    val bottomIndicatorYOffset = 0.dp
    
    // 🟢 最外层容器
    Box(
        modifier = Modifier
            .fillMaxWidth() // [Fix] Ensure container fills width so Alignment.BottomCenter works
            .padding(horizontal = barHorizontalPadding)
            .padding(bottom = barBottomPadding)
            .then(if (isFloating) Modifier.navigationBarsPadding() else Modifier),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 🟢 Haze 背景容器 (也是 Liquid Glass 的应用目标)
        // 这里的 Modifier 顺序很重要
        Box(
            modifier = Modifier
                .then(
                    if (isFloating) {
                         Modifier
                            .widthIn(max = targetMaxWidth)
                            .shadow(8.dp, barShape, ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            .height(floatingHeight)
                    } else {
                        Modifier
                    }
                )
                .fillMaxWidth()
                .clip(barShape)
                // [Refactor] Removed background modifiers from here to separate layers
        ) {
            // [Layer 1] Glass Background Layer
            // Uses LayerBackdrop to capture and refract background content
            // This creates real refraction of video covers/text when scrolling
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .appChromeLiquidSurface(
                        renderMode = bottomChromeRenderMode,
                        shape = barShape,
                        surfaceColor = barColor,
                        hazeState = hazeState,
                        backdrop = backdrop,
                        liquidStyle = homeSettings.liquidGlassStyle,
                        liquidGlassTuning = liquidGlassTuning,
                        motionTier = motionTier,
                        isScrolling = isActivelyScrolling,
                        isTransitionRunning = isTransitionRunning,
                        forceLowBlurBudget = forceLowBlurBudget,
                        style = AppChromeLiquidSurfaceStyle(
                            blurSurfaceType = BlurSurfaceType.BOTTOM_BAR,
                            depthEffect = isFloating,
                            refractionAmountScrollMultiplier = 0.02f,
                            refractionAmountScrollCap = 14f,
                            surfaceAlphaScrollMultiplier = 0.00015f,
                            surfaceAlphaScrollCap = 0.04f,
                            darkThemeWhiteOverlayMultiplier = 0.86f,
                            useTuningSurfaceAlpha = true,
                            hazeBackgroundAlphaMultiplier = 0.4f
                        )
                    )
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent,
                shape = barShape,
                shadowElevation = 0.dp,
                border = if (!isFloating) {
                    null
                } else {
                    androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
                    )
                }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 内容容器 (用于占位高度) - 应用 liquidGlass 效果在这里
                    val isSupported = shouldAllowHomeChromeLiquidGlass(android.os.Build.VERSION.SDK_INT)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isFloating) Modifier.fillMaxHeight() else Modifier.height(dockedHeight))
                            // liquidGlass refracts the icons/text around indicator during horizontal swipe
                            // liquidGlass removed: Refraction now handled by LiquidIndicator using LayerBackdrop
                    ) {
                        // 关键修复：
                        // 1) 移动态时导出一层隐藏的 tint 内容给 capsule 折射
                        // 2) capsule 使用页面 backdrop + tint 内容的 combined backdrop
                        val tintedContentBackdrop = rememberLayerBackdrop()
                        val isDark = isSystemInDarkTheme()
                        val indicatorPosition = dampedDragState.value
                        val indicatorVisualPolicy = resolveBottomBarIndicatorVisualPolicy(
                            position = indicatorPosition,
                            isDragging = dampedDragState.isDragging,
                            velocity = dampedDragState.velocity,
                            useNeutralIndicatorTint = liquidGlassTuning.useNeutralIndicatorTint
                        )
                        val refractionMotionProfile = resolveBottomBarRefractionMotionProfile(
                            position = indicatorPosition,
                            velocity = dampedDragState.velocity,
                            isDragging = dampedDragState.isDragging
                        )
                        val indicatorPanelOffsetPx = with(density) { 4.dp.toPx() } *
                            refractionMotionProfile.indicatorPanelOffsetFraction
                        val indicatorPolicy = remember(itemCount) {
                            resolveBottomBarIndicatorPolicy(itemCount = itemCount)
                        }
                        val indicatorColor = when {
                            indicatorVisualPolicy.shouldRefract ->
                                resolveBottomBarMovingIndicatorSurfaceColor(isDarkTheme = isDark)
                            indicatorVisualPolicy.useNeutralTint ->
                                resolveIos26BottomIndicatorGrayColor(isDarkTheme = isDark)
                            else -> MaterialTheme.colorScheme.primary
                        }
                        val indicatorTintAlpha = resolveBottomBarIndicatorTintAlpha(
                            shouldRefract = indicatorVisualPolicy.shouldRefract,
                            liquidGlassProgress = liquidGlassTuning.progress,
                            configuredAlpha = liquidGlassTuning.indicatorTintAlpha
                        )
                        val refractionLayerPolicy = resolveBottomBarRefractionLayerPolicy(
                            isFloating = isFloating,
                            isLiquidGlassEnabled = showGlassEffect,
                            indicatorVisualPolicy = indicatorVisualPolicy
                        )
                        val indicatorBackdrop: Backdrop? = when {
                            !indicatorVisualPolicy.shouldRefract -> null
                            refractionLayerPolicy.captureTintedContentLayer -> tintedContentBackdrop
                            else -> backdrop
                        }
                        LiquidIndicator(
                            position = indicatorPosition,
                            itemWidth = itemWidth,
                            itemCount = itemCount,
                            // Keep refraction active during in-flight horizontal motion (drag + settle).
                            isDragging = indicatorVisualPolicy.isInMotion,
                            velocity = dampedDragState.velocity,
                            startPadding = rowPadding,
                            viewportShiftPx = indicatorPanelOffsetPx,
                            modifier = Modifier
                                .fillMaxSize()
                                .offset(y = bottomIndicatorYOffset)
                                .alpha(indicatorAlpha),
                            clampToBounds = indicatorPolicy.clampToBounds,
                            edgeInset = indicatorPolicy.edgeInsetDp.dp,
                            indicatorWidthMultiplier = indicatorPolicy.widthMultiplier,
                            indicatorMinWidth = indicatorPolicy.minWidthDp.dp,
                            indicatorMaxWidth = indicatorPolicy.maxWidthDp.dp,
                            maxWidthToItemRatio = indicatorPolicy.maxWidthToItemRatio,
                            indicatorHeight = bottomIndicatorHeight,
                            isLiquidGlassEnabled = showGlassEffect,
                            lensIntensityBoost = liquidGlassTuning.indicatorLensBoost,
                            edgeWarpBoost = liquidGlassTuning.indicatorEdgeWarpBoost,
                            chromaticBoost = liquidGlassTuning.indicatorChromaticBoost,
                            liquidGlassStyle = homeSettings.liquidGlassStyle, // [New] Pass style
                            liquidGlassTuning = liquidGlassTuning,
                            // Dynamic refraction: moving -> refract icons/text/cover, static -> keep pure color.
                            backdrop = indicatorBackdrop,
                            lensAmountScale = refractionMotionProfile.indicatorLensAmountScale,
                            lensHeightScale = refractionMotionProfile.indicatorLensHeightScale,
                            forceChromaticAberration = refractionLayerPolicy.useCombinedBackdrop &&
                                refractionMotionProfile.forceChromaticAberration,
                            color = indicatorColor.copy(alpha = indicatorTintAlpha)
                        )

                        if (refractionLayerPolicy.captureTintedContentLayer) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clearAndSetSemantics {}
                                    .alpha(0f)
                                    .layerBackdrop(tintedContentBackdrop)
                            ) {
                                BottomBarContent(
                                    visibleItems = visibleItems,
                                    selectedIndex = selectedIndex,
                                    itemColorIndices = itemColorIndices,
                                    onItemClick = onItemClick,
                                    onToggleSidebar = onToggleSidebar,
                                    isTablet = isTablet,
                                    labelMode = labelMode,
                                    hazeState = hazeState,
                                    haptic = haptic,
                                    debounceClick = debounceClick,
                                    onHomeDoubleTap = onHomeDoubleTap,
                                    onDynamicDoubleTap = onDynamicDoubleTap,
                                    itemWidth = itemWidth,
                                    rowPadding = rowPadding,
                                    contentVerticalOffset = contentVerticalOffset,
                                    isInteractive = false,
                                    currentPosition = indicatorPosition,
                                    contentLuminance = contentLuminance,
                                    liquidGlassStyle = homeSettings.liquidGlassStyle,
                                    liquidGlassTuning = liquidGlassTuning,
                                    selectionEmphasis = refractionMotionProfile.exportSelectionEmphasis
                                )
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            BottomBarContent(
                                visibleItems = visibleItems,
                                selectedIndex = selectedIndex,
                                itemColorIndices = itemColorIndices,
                                onItemClick = onItemClick,
                                onToggleSidebar = onToggleSidebar,
                                isTablet = isTablet,
                                labelMode = labelMode,
                                hazeState = hazeState,
                                haptic = haptic,
                                debounceClick = debounceClick,
                                onHomeDoubleTap = onHomeDoubleTap,
                                onDynamicDoubleTap = onDynamicDoubleTap,
                                itemWidth = itemWidth,
                                rowPadding = rowPadding,
                                contentVerticalOffset = contentVerticalOffset,
                                isInteractive = true,
                                currentPosition = dampedDragState.value,
                                dragModifier = Modifier.horizontalDragGesture(
                                    dragState = dampedDragState,
                                    itemWidthPx = with(LocalDensity.current) { itemWidth.toPx() }
                                ),
                                // [New] Param for adaptive text color
                                contentLuminance = contentLuminance,
                                liquidGlassStyle = homeSettings.liquidGlassStyle,
                                liquidGlassTuning = liquidGlassTuning,
                                selectionEmphasis = refractionMotionProfile.visibleSelectionEmphasis
                            )
                        }
                    }
                        
                        if (!isFloating) {
                             Spacer(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialBottomBar(
    currentItem: BottomNavItem,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
    visibleItems: List<BottomNavItem>,
    onToggleSidebar: (() -> Unit)?,
    isTablet: Boolean,
    labelMode: Int,
    blurEnabled: Boolean,
    hazeState: HazeState?,
    motionTier: MotionTier,
    isTransitionRunning: Boolean,
    forceLowBlurBudget: Boolean
) {
    val haptic = rememberHapticFeedback()
    val normalizedLabelMode = normalizeBottomBarLabelMode(labelMode)
    val showIcon = shouldShowBottomBarIcon(normalizedLabelMode)
    val showText = shouldShowBottomBarText(normalizedLabelMode)
    val blurIntensity = currentUnifiedBlurIntensity()
    val containerColor = resolveBottomBarSurfaceColor(
        surfaceColor = MaterialTheme.colorScheme.surface,
        blurEnabled = blurEnabled,
        blurIntensity = blurIntensity
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (blurEnabled && hazeState != null) {
                    Modifier.unifiedBlur(
                        hazeState = hazeState,
                        surfaceType = BlurSurfaceType.BOTTOM_BAR,
                        motionTier = motionTier,
                        isScrolling = false,
                        isTransitionRunning = isTransitionRunning,
                        forceLowBudget = forceLowBlurBudget
                    )
                } else {
                    Modifier
                }
            ),
        tonalElevation = if (blurEnabled) 0.dp else 3.dp,
        shadowElevation = 0.dp,
        color = containerColor
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            visibleItems.forEach { item ->
                val itemLabel = resolveBottomNavItemLabel(item)
                val itemContentDescription = resolveBottomNavItemContentDescription(item)
                NavigationBarItem(
                    selected = currentItem == item,
                    onClick = {
                        performMaterialBottomBarTap(
                            haptic = haptic,
                            onClick = { onItemClick(item) }
                        )
                    },
                    icon = {
                        if (showIcon) {
                            Icon(
                                imageVector = resolveMaterialBottomBarIcon(item = item, selected = currentItem == item),
                                contentDescription = itemContentDescription
                            )
                        } else {
                            Spacer(modifier = Modifier.size(0.dp))
                        }
                    },
                    label = if (showText) {
                        { Text(itemLabel) }
                    } else {
                        null
                    },
                    alwaysShowLabel = showText,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            if (isTablet && onToggleSidebar != null) {
                val sidebarLabel = stringResource(R.string.sidebar_toggle)
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        performMaterialBottomBarTap(
                            haptic = haptic,
                            onClick = onToggleSidebar
                        )
                    },
                    icon = {
                        if (showIcon) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.MenuOpen,
                                contentDescription = sidebarLabel
                            )
                        } else {
                            Spacer(modifier = Modifier.size(0.dp))
                        }
                    },
                    label = if (showText) {
                        { Text(sidebarLabel) }
                    } else {
                        null
                    },
                    alwaysShowLabel = showText,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

private fun resolveMaterialBottomBarIcon(
    item: BottomNavItem,
    selected: Boolean
) = when (item) {
    BottomNavItem.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
    BottomNavItem.DYNAMIC -> if (selected) Icons.Filled.Notifications else Icons.Outlined.NotificationsNone
    BottomNavItem.STORY -> if (selected) Icons.Filled.PlayCircle else Icons.Outlined.PlayCircleOutline
    BottomNavItem.HISTORY -> if (selected) Icons.Filled.History else Icons.Outlined.History
    BottomNavItem.PROFILE -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
    BottomNavItem.FAVORITE -> if (selected) Icons.Filled.CollectionsBookmark else Icons.Outlined.CollectionsBookmark
    BottomNavItem.LIVE -> if (selected) Icons.Filled.LiveTv else Icons.Outlined.LiveTv
    BottomNavItem.WATCHLATER -> if (selected) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder
    BottomNavItem.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
}

internal fun resolveBottomBarSurfaceColor(
    surfaceColor: Color,
    blurEnabled: Boolean,
    blurIntensity: com.android.purebilibili.core.ui.blur.BlurIntensity
): Color {
    val alpha = if (blurEnabled) {
        BlurStyles.getBackgroundAlpha(blurIntensity)
    } else {
        return surfaceColor
    }
    return surfaceColor.copy(alpha = alpha)
}

internal fun resolveBottomBarContainerColor(
    surfaceColor: Color,
    blurEnabled: Boolean,
    blurIntensity: com.android.purebilibili.core.ui.blur.BlurIntensity,
    liquidGlassProgress: Float,
    isGlassEffectEnabled: Boolean
): Color {
    val base = resolveBottomBarSurfaceColor(
        surfaceColor = surfaceColor,
        blurEnabled = blurEnabled,
        blurIntensity = blurIntensity
    )
    if (!isGlassEffectEnabled) return base

    val minAlpha = lerp(
        start = 0.18f,
        stop = 0.36f,
        fraction = liquidGlassProgress.coerceIn(0f, 1f)
    )
    val liftedAlpha = base.alpha.coerceAtLeast(minAlpha) + (0.06f * liquidGlassProgress.coerceIn(0f, 1f))
    return base.copy(alpha = liftedAlpha.coerceAtMost(1f))
}

internal fun resolveBottomBarContainerColor(
    surfaceColor: Color,
    blurEnabled: Boolean,
    blurIntensity: com.android.purebilibili.core.ui.blur.BlurIntensity,
    liquidGlassMode: LiquidGlassMode,
    isGlassEffectEnabled: Boolean
): Color {
    return resolveBottomBarContainerColor(
        surfaceColor = surfaceColor,
        blurEnabled = blurEnabled,
        blurIntensity = blurIntensity,
        liquidGlassProgress = when (liquidGlassMode) {
            LiquidGlassMode.CLEAR -> 0f
            LiquidGlassMode.BALANCED -> 0.5f
            LiquidGlassMode.FROSTED -> 1f
        },
        isGlassEffectEnabled = isGlassEffectEnabled
    )
}

internal fun shouldUseHomeCombinedClickable(
    item: BottomNavItem,
    isSelected: Boolean
): Boolean {
    return item == BottomNavItem.HOME && isSelected
}

internal enum class BottomBarPrimaryTapAction {
    Navigate,
    HomeReselect
}

internal fun resolveBottomBarPrimaryTapAction(
    item: BottomNavItem,
    isSelected: Boolean
): BottomBarPrimaryTapAction {
    return if (item == BottomNavItem.HOME && isSelected) {
        BottomBarPrimaryTapAction.HomeReselect
    } else {
        BottomBarPrimaryTapAction.Navigate
    }
}

internal fun performBottomBarPrimaryTap(
    item: BottomNavItem,
    isSelected: Boolean,
    haptic: (HapticType) -> Unit,
    onNavigate: () -> Unit,
    onHomeReselect: () -> Unit
) {
    haptic(HapticType.LIGHT)
    when (resolveBottomBarPrimaryTapAction(item, isSelected)) {
        BottomBarPrimaryTapAction.Navigate -> onNavigate()
        BottomBarPrimaryTapAction.HomeReselect -> onHomeReselect()
    }
}

internal fun performMaterialBottomBarTap(
    haptic: (HapticType) -> Unit,
    onClick: () -> Unit
) {
    haptic(HapticType.LIGHT)
    onClick()
}

internal fun shouldAcceptBottomBarTap(
    tappedItem: BottomNavItem,
    lastTappedItem: BottomNavItem?,
    currentTimeMillis: Long,
    lastTapTimeMillis: Long,
    debounceWindowMillis: Long
): Boolean {
    if (lastTappedItem == null) return true
    if (tappedItem != lastTappedItem) return true
    return currentTimeMillis - lastTapTimeMillis > debounceWindowMillis
}

internal fun shouldUseBottomReselectCombinedClickable(
    item: BottomNavItem,
    isSelected: Boolean
): Boolean {
    return isSelected && item == BottomNavItem.DYNAMIC
}

internal data class BottomBarItemColorBinding(
    val colorIndex: Int,
    val hasCustomAccent: Boolean
)

internal fun resolveBottomBarItemColorBinding(
    item: BottomNavItem,
    itemColorIndices: Map<String, Int>
): BottomBarItemColorBinding {
    if (itemColorIndices.isEmpty()) {
        return BottomBarItemColorBinding(colorIndex = 0, hasCustomAccent = false)
    }

    val match = resolveBottomNavItemLookupKeys(item).firstNotNullOfOrNull { key ->
        itemColorIndices[key]
    }
    return if (match != null) {
        BottomBarItemColorBinding(colorIndex = match, hasCustomAccent = true)
    } else {
        BottomBarItemColorBinding(colorIndex = 0, hasCustomAccent = false)
    }
}

internal fun resolveBottomBarReadableContentColor(
    isLightMode: Boolean,
    liquidGlassProgress: Float,
    contentLuminance: Float
): Color {
    if (isLightMode) {
        return Color.Black
    }
    val shouldUseDarkForeground = liquidGlassProgress >= 0.62f && contentLuminance > 0.6f
    return if (shouldUseDarkForeground) {
        Color.Black.copy(alpha = 0.82f)
    } else {
        Color.White.copy(
            alpha = if (liquidGlassProgress < 0.35f) 0.97f else 0.95f
        )
    }
}

@Composable
private fun BottomBarContent(
    visibleItems: List<BottomNavItem>,
    selectedIndex: Int,
    itemColorIndices: Map<String, Int>,
    onItemClick: (BottomNavItem) -> Unit,
    onToggleSidebar: (() -> Unit)?,
    isTablet: Boolean,
    labelMode: Int,
    hazeState: HazeState?,
    haptic: (HapticType) -> Unit,
    debounceClick: (BottomNavItem, () -> Unit) -> Unit,
    onHomeDoubleTap: () -> Unit,
    onDynamicDoubleTap: () -> Unit,
    itemWidth: Dp,
    rowPadding: Dp,
    contentVerticalOffset: Dp,
    isInteractive: Boolean,
    currentPosition: Float, // [新增] 当前指示器位置，用于动态插值
    dragModifier: Modifier = Modifier,
    contentLuminance: Float = 0f, // [New]
    liquidGlassStyle: LiquidGlassStyle = LiquidGlassStyle.CLASSIC, // [New]
    liquidGlassTuning: LiquidGlassTuning = resolveLiquidGlassTuning(liquidGlassStyle),
    selectionEmphasis: Float = 1f
) {
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxSize()
            .then(dragModifier)
            .padding(horizontal = rowPadding),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // [平板适配] ... (保持不变，省略以简化 diff，实际需完整保留)
        // 为保持 diff 简洁且正确，这里只修改 visibleItems 循环部分
        // 平板侧边栏按钮逻辑可以保持现状，因为它不参与 currentPosition 计算（它是额外的）
        // 但为了完整性，我们需要确保 BottomBarContent 的完整代码。
        
        // 由于 multi_replace 限制，我必须提供完整的 BottomBarContent。
        // ... (平板按钮代码) 
        visibleItems.forEachIndexed { index, item ->
            val isSelected = selectedIndex == index
            val colorBinding = resolveBottomBarItemColorBinding(
                item = item,
                itemColorIndices = itemColorIndices
            )
            
            // [核心逻辑] 计算每个 Item 的选中分数 (0f..1f)
            // 根据当前位置 currentPosition 和 item index 的距离计算
            // 距离 < 1 时开始变色，距离 0 时完全变色
            val distance = abs(currentPosition - index)
            val selectionFraction = (1f - distance).coerceIn(0f, 1f)
            
            BottomBarItem(
                item = item,
                isSelected = isSelected, // 仅用于点击逻辑判断
                selectionFraction = selectionFraction, // [新增] 用于驱动样式
                onClick = { if (isInteractive) onItemClick(item) },
                labelMode = labelMode,
                colorIndex = colorBinding.colorIndex,
                hasCustomAccent = colorBinding.hasCustomAccent,
                iconSize = if (labelMode == 0) 20.dp else 24.dp,
                contentVerticalOffset = contentVerticalOffset,
                modifier = Modifier.weight(1f),
                hazeState = hazeState,
                haptic = haptic,
                debounceClick = debounceClick,
                onHomeDoubleTap = onHomeDoubleTap,
                onDynamicDoubleTap = onDynamicDoubleTap,
                isTablet = isTablet,
                contentLuminance = contentLuminance, // [New]
                liquidGlassStyle = liquidGlassStyle, // [New]
                liquidGlassTuning = liquidGlassTuning,
                selectionEmphasis = selectionEmphasis
            )
        }

        if (isTablet && onToggleSidebar != null) {
            // ... (复制原有逻辑)
            // 简单复制：
            val sidebarLabel = stringResource(R.string.sidebar_toggle)
            var isPending by remember { mutableStateOf(false) }
            val primaryColor = MaterialTheme.colorScheme.primary
            val unselectedColor = if (hazeState != null) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            } else {
                BottomBarColors.UNSELECTED
            }
            val iconColor by animateColorAsState(targetValue = if (isPending) primaryColor else unselectedColor, label = "iconColor")

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().offset(y = contentVerticalOffset)
                    .then(
                        if (isInteractive) {
                            Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    isPending = true
                                    haptic(HapticType.LIGHT)
                                    scope.launch {
                                        kotlinx.coroutines.delay(100)
                                        onToggleSidebar()
                                        isPending = false
                                    }
                                }
                        } else {
                            Modifier
                        }
                    ),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                Box(modifier = Modifier.size(26.dp)) {
                    Icon(imageVector = CupertinoIcons.Outlined.SidebarLeft, contentDescription = sidebarLabel, tint = iconColor, modifier = Modifier.fillMaxSize())
                }
                if (labelMode == 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sidebarLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = iconColor,
                        fontWeight = FontWeight.Medium,
                        fontSize = if (isTablet) 12.sp else 10.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BottomBarItem(
    item: BottomNavItem,
    isSelected: Boolean,
    selectionFraction: Float, // [新增] 0f..1f
    onClick: () -> Unit,
    labelMode: Int,
    colorIndex: Int,
    hasCustomAccent: Boolean,
    iconSize: androidx.compose.ui.unit.Dp,
    contentVerticalOffset: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    hazeState: HazeState?,
    haptic: (HapticType) -> Unit,
    debounceClick: (BottomNavItem, () -> Unit) -> Unit,
    onHomeDoubleTap: () -> Unit,
    onDynamicDoubleTap: () -> Unit,
    isTablet: Boolean,
    contentLuminance: Float = 0f, // [New]
    liquidGlassStyle: LiquidGlassStyle = LiquidGlassStyle.CLASSIC, // [New]
    liquidGlassTuning: LiquidGlassTuning = resolveLiquidGlassTuning(liquidGlassStyle),
    selectionEmphasis: Float = 1f
) {
    val scope = rememberCoroutineScope()
    var isPending by remember { mutableStateOf(false) }
    val itemLabel = resolveBottomNavItemLabel(item)
    val itemContentDescription = resolveBottomNavItemContentDescription(item)
    
    val primaryColor = MaterialTheme.colorScheme.primary
    
    // [Adaptive] High Contrast Scheme for Glass Readability
    // Light Mode: Black text/icons (to stand out against white-ish glass)
    // Dark Mode: White text/icons (to stand out against dark glass)
    // [SimpMusic Style]: Adaptive based on luminance
    // [Fix] Reliably detect Light Mode using surface luminance
    // This handles cases where app theme overrides system theme
    val isLightMode = MaterialTheme.colorScheme.surface.luminance() > 0.5f

    val unselectedColor = resolveBottomBarReadableContentColor(
        isLightMode = isLightMode,
        liquidGlassProgress = liquidGlassTuning.progress,
        contentLuminance = contentLuminance
    )
    
    val selectedAccent = if (hasCustomAccent) {
        BottomBarColors.getColorByIndex(colorIndex)
    } else {
        primaryColor
    }
    val emphasizedSelectionFraction = (selectionFraction * selectionEmphasis).coerceIn(0f, 1f)

    // [修改] 颜色插值：根据 selectionFraction 在 unselected 和 selected 之间混合
    // 还要考虑 isPending (点击态)
    val targetIconColor = androidx.compose.ui.graphics.lerp(
        unselectedColor, 
        selectedAccent, 
        if (isPending) 1f else emphasizedSelectionFraction
    )
    
    // 仍然使用 animateColorAsState 但目标值现在是动态插值的
    // 使用较快的动画以跟手，或者直接使用 lerp 结果如果非常平滑
    // 为了平滑过渡，这里使用 FastOutSlowIn 且时间短
    val iconColor by animateColorAsState(
        targetValue = targetIconColor,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 100), // 快速响应
        label = "iconColor"
    )
    
    // [修改] 缩放插值 - 跃动效果
    // selectionFraction: 0f (未选中) -> 1f (完全选中)
    // 这里的逻辑是：当指示器经过时 (0.5f) 图标最大，两端 (0f/1f) 恢复正常
    // 使用 sin(x * PI) 曲线：sin(0)=0, sin(0.5PI)=1, sin(PI)=0
    // 基础大小 1.0f，最大放大 1.4f (增强版)
    val scaleMultiplier = 0.4f
    val bumpScale = 1.0f + (
        scaleMultiplier * kotlin.math.sin(emphasizedSelectionFraction * Math.PI)
    ).toFloat()
    
    // 直接使用计算出的 bumpScale 作为 scale，因为 selectionFraction 本身已经是平滑动画的值 (由 dampedDragState 驱动)
    // 这样可以保证图标缩放绝对跟随手指/指示器位置，没有任何滞后
    val scale = bumpScale
    
    // [修改] Y轴位移插值
    val targetBounceY = androidx.compose.ui.util.lerp(0f, 0f, emphasizedSelectionFraction)
    val bounceY by animateFloatAsState(
        targetValue = targetBounceY,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "bounceY"
    )
    
    //  晃动角度 (保持不变)
    var wobbleAngle by remember { mutableFloatStateOf(0f) }
    val animatedWobble by animateFloatAsState(
        targetValue = wobbleAngle,
        animationSpec = spring(dampingRatio = 0.2f, stiffness = 600f),
        label = "wobble"
    )
    
    LaunchedEffect(wobbleAngle) {
        if (wobbleAngle != 0f) {
            kotlinx.coroutines.delay(50)
            wobbleAngle = 0f
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxHeight()
            .offset(y = contentVerticalOffset)
            .clearAndSetSemantics {
                contentDescription = itemContentDescription
            }
            .then(
                // 仅当“当前已在首页”时保留双击手势，避免从其他页切首页产生点击延迟
                if (shouldUseBottomReselectCombinedClickable(item, isSelected)) {
                    Modifier.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            debounceClick(item) {
                                performBottomBarPrimaryTap(
                                    item = item,
                                    isSelected = isSelected,
                                    haptic = haptic,
                                    onNavigate = onClick,
                                    onHomeReselect = onHomeDoubleTap
                                )
                                
                                // 2. 视觉反馈 (Visual Feedback)
                                isPending = true
                                scope.launch {
                                    // 晃动动画与导航并行执行
                                    wobbleAngle = 15f
                                    kotlinx.coroutines.delay(200) // 等待动画完成
                                    isPending = false
                                }
                            }
                        },
                        onDoubleClick = {
                            haptic(HapticType.MEDIUM)
                            when (item) {
                                BottomNavItem.HOME -> onHomeDoubleTap()
                                BottomNavItem.DYNAMIC -> onDynamicDoubleTap()
                                else -> Unit
                            }
                        }
                    )
                } else {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { 
                        debounceClick(item) {
                            performBottomBarPrimaryTap(
                                item = item,
                                isSelected = isSelected,
                                haptic = haptic,
                                onNavigate = onClick,
                                onHomeReselect = onHomeDoubleTap
                            )
                            
                            // 2. 视觉反馈 (Visual Feedback)
                            isPending = true
                            scope.launch {
                                // 晃动动画与导航并行执行
                                wobbleAngle = 15f
                                kotlinx.coroutines.delay(200) // 等待动画完成
                                isPending = false
                            }
                        }
                    }
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) { // ... (Icon/Text rendering 保持不变，使用 iconColor/scale 等变量)
        when (labelMode) {
            0 -> { // Icon + Text
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .offset(y = (-0.5).dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            rotationZ = animatedWobble
                            translationY = bounceY
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(LocalContentColor provides iconColor) {
                        if (isSelected) item.selectedIcon() else item.unselectedIcon()
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = itemLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = iconColor,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = if (isTablet) 12.sp else 11.sp,
                    lineHeight = if (isTablet) 12.sp else 11.sp,
                    maxLines = 1
                )
            }
            2 -> { // Text Only
                Text(
                    text = itemLabel,
                    fontSize = if (isTablet) 16.sp else 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = iconColor,
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = animatedWobble
                        translationY = bounceY
                    }
                )
            }
            else -> { // Icon Only
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            rotationZ = animatedWobble
                            translationY = bounceY
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(LocalContentColor provides iconColor) {
                        if (isSelected) item.selectedIcon() else item.unselectedIcon()
                    }
                }
            }
        }
    }
}

internal fun resolveIos26BottomIndicatorGrayColor(isDarkTheme: Boolean): Color {
    return if (isDarkTheme) {
        // Dark mode: brighter neutral gray to float above dark glass.
        Color(0xFFC8CDD6)
    } else {
        // Light mode: deeper neutral gray to stay visible on bright background.
        Color(0xFF9BA5B4)
    }
}
