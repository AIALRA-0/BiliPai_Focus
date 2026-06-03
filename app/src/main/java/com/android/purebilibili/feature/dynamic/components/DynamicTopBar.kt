// 文件路径: feature/dynamic/components/DynamicTopBar.kt
package com.android.purebilibili.feature.dynamic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import com.android.purebilibili.core.ui.LocalGlobalWallpaperBackdropVisible
import com.android.purebilibili.core.ui.blur.unifiedBlur
import com.android.purebilibili.feature.dynamic.resolveDynamicTopBarHorizontalPadding
import com.android.purebilibili.feature.dynamic.resolveDynamicTopBarLiquidTabSpec
import com.android.purebilibili.core.ui.blur.BlurStyles
import com.android.purebilibili.core.ui.blur.currentUnifiedBlurIntensity
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.feature.home.components.BottomBarLiquidSegmentedControl
import com.android.purebilibili.feature.home.components.SegmentedControlChromeStyle
import com.android.purebilibili.feature.home.components.resolveSegmentedControlChromeStyle
import com.android.purebilibili.feature.home.components.resolveSegmentedControlLiquidGlassEnabled
import com.kyant.backdrop.Backdrop
import dev.chrisbanes.haze.HazeState

//  动态页面布局模式
enum class DynamicDisplayMode {
    SIDEBAR,     // 侧边栏模式（默认，UP主列表在左侧）
    HORIZONTAL   // 横向模式（UP主列表在顶部，类似 Telegram）
}

/**
 *  带Tab的顶栏
 */
@Composable
fun DynamicTopBarWithTabs(
    selectedTab: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    displayMode: DynamicDisplayMode = DynamicDisplayMode.SIDEBAR,
    onDisplayModeChange: (DynamicDisplayMode) -> Unit = {},
    hazeState: HazeState? = null,
    backdrop: Backdrop? = null
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val uiPreset = LocalUiPreset.current
    val homeSettings by SettingsManager
        .getHomeSettings(context)
        .collectAsStateWithLifecycle(initialValue = HomeSettings(),
            context = kotlin.coroutines.EmptyCoroutineContext
        )
    val statusBarHeight = WindowInsets.statusBars.getTop(density).let { with(density) { it.toDp() } }
    val liquidTabSpec = resolveDynamicTopBarLiquidTabSpec()
    val reusesLiquidGlassDock = shouldReuseDynamicTopBarLiquidGlassDock(
        hasBackdrop = backdrop != null,
        storedLiquidGlassEnabled = homeSettings.isBottomBarLiquidGlassEnabled,
        uiPreset = uiPreset,
        androidNativeLiquidGlassEnabled = homeSettings.androidNativeLiquidGlassEnabled
    )
    
    //  读取当前模糊强度以确定背景透明度
    val blurIntensity = currentUnifiedBlurIntensity()
    val backgroundAlpha = BlurStyles.getBackgroundAlpha(blurIntensity)
    val globalWallpaperVisible = LocalGlobalWallpaperBackdropVisible.current
    val shouldUseHeaderBlur = shouldUseDynamicTopBarHeaderBlur(
        hasHazeState = hazeState != null,
        globalWallpaperVisible = globalWallpaperVisible,
        reusesLiquidGlassDock = reusesLiquidGlassDock
    )
    
    //  使用 blurIntensity 对应的背景透明度实现毛玻璃质感
    val headerColor = resolveDynamicTopBarHeaderColor(
        surfaceColor = MaterialTheme.colorScheme.surface,
        backgroundAlpha = if (shouldUseHeaderBlur) backgroundAlpha else 0f,
        globalWallpaperVisible = globalWallpaperVisible
    )

    //  [关键修复] 使用透明背景，让主界面的渐变透出来
    Box(
        modifier = modifier
            .fillMaxWidth()
            // 应用模糊效果
            .then(if (shouldUseHeaderBlur && hazeState != null) Modifier.unifiedBlur(hazeState) else Modifier)
            .background(headerColor)
    ) {
        Column {
            Spacer(modifier = Modifier.height(statusBarHeight))
            
            //  紧凑标签行：宽屏动态页优先展示内容密度
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(liquidTabSpec.heightDp.dp)
                    .padding(horizontal = resolveDynamicTopBarHorizontalPadding()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DynamicCompactTabRow(
                    selectedTab = selectedTab,
                    tabs = tabs,
                    onTabSelected = onTabSelected,
                    modifier = Modifier.weight(1f),
                    backdrop = backdrop
                )
                
                //  布局模式切换按钮
                IconButton(
                    onClick = {
                        val newMode = if (displayMode == DynamicDisplayMode.SIDEBAR) 
                            DynamicDisplayMode.HORIZONTAL else DynamicDisplayMode.SIDEBAR
                        onDisplayModeChange(newMode)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (displayMode == DynamicDisplayMode.SIDEBAR)
                            CupertinoIcons.Default.ListBullet else CupertinoIcons.Default.RectangleStack,
                        contentDescription = "切换布局模式",
                        tint = MaterialTheme.colorScheme.onSurface, // 自适应颜色
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DynamicCompactTabRow(
    selectedTab: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null
) {
    BottomBarLiquidSegmentedControl(
        items = tabs,
        selectedIndex = selectedTab,
        onSelected = onTabSelected,
        modifier = modifier,
        height = 44.dp,
        indicatorHeight = 36.dp,
        labelFontSize = 14.sp,
        preferInlineContentStyle = true,
        backdrop = backdrop
    )
}

@Composable
private fun rememberDynamicTabSelectedColor(): Color = resolveDynamicTabSelectedColor(MaterialTheme.colorScheme.primary)

internal fun resolveDynamicTabSelectedColor(primaryColor: Color): Color = primaryColor

internal fun resolveDynamicTopBarHeaderColor(
    surfaceColor: Color,
    backgroundAlpha: Float,
    globalWallpaperVisible: Boolean
): Color {
    return if (globalWallpaperVisible) {
        Color.Transparent
    } else {
        surfaceColor.copy(alpha = backgroundAlpha)
    }
}

internal fun shouldUseDynamicTopBarHeaderBlur(
    hasHazeState: Boolean,
    globalWallpaperVisible: Boolean,
    reusesLiquidGlassDock: Boolean = false
): Boolean = hasHazeState && !globalWallpaperVisible && !reusesLiquidGlassDock

internal fun shouldReuseDynamicTopBarLiquidGlassDock(
    hasBackdrop: Boolean,
    storedLiquidGlassEnabled: Boolean,
    uiPreset: com.android.purebilibili.core.theme.UiPreset,
    androidNativeLiquidGlassEnabled: Boolean
): Boolean {
    if (!hasBackdrop) return false
    val chromeStyle = resolveSegmentedControlChromeStyle(
        uiPreset = uiPreset,
        androidNativeLiquidGlassEnabled = androidNativeLiquidGlassEnabled,
        preferInlineContentStyle = true
    )
    if (chromeStyle != SegmentedControlChromeStyle.LIQUID_PILL) return false
    return resolveSegmentedControlLiquidGlassEnabled(
        storedLiquidGlassEnabled = storedLiquidGlassEnabled,
        liquidGlassEffectsEnabled = true,
        uiPreset = uiPreset,
        androidNativeLiquidGlassEnabled = androidNativeLiquidGlassEnabled
    )
}

@Composable
private fun rememberDynamicTabUnselectedColor(): Color {
    return if (isDynamicTopBarDarkSurface(MaterialTheme.colorScheme.surface)) {
        Color.White.copy(alpha = 0.9f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun isDynamicTopBarDarkSurface(color: Color): Boolean {
    val perceivedBrightness = (color.red * 0.299f) + (color.green * 0.587f) + (color.blue * 0.114f)
    return perceivedBrightness < 0.45f
}
