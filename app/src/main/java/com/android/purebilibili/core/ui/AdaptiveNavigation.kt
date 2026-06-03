// 文件路径: core/ui/AdaptiveNavigation.kt
package com.android.purebilibili.core.ui

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.blur.shouldAllowRuntimeShaderBackedHazeEffect
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.core.util.WindowWidthSizeClass
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * 📍 导航项数据
 */
data class AdaptiveNavItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val badgeCount: Int = 0
)

/**
 * 🧭 自适应导航容器
 * 
 * 根据屏幕尺寸自动切换导航模式：
 * - Compact: 底部导航栏 (BottomBar)
 * - Medium/Expanded: 侧边导航栏 (NavigationRail)
 * 
 * @param items 导航项列表
 * @param selectedItemId 当前选中项 ID
 * @param onItemSelected 选中项回调
 * @param hazeState 毛玻璃状态（可选）
 * @param modifier Modifier
 * @param content 主内容区域
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun AdaptiveNavigationContainer(
    items: List<AdaptiveNavItem>,
    selectedItemId: String,
    onItemSelected: (String) -> Unit,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val useSideNav = windowSizeClass.shouldUseSideNavigation
    
    if (useSideNav) {
        // 🖥️ 平板模式：侧边导航栏
        Row(modifier = modifier.fillMaxSize()) {
            // 侧边导航栏
            AdaptiveSideNavigationRail(
                items = items,
                selectedItemId = selectedItemId,
                onItemSelected = onItemSelected,
                hazeState = hazeState
            )
            
            // 主内容区域
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            ) {
                content()
            }
        }
    } else {
        // 📱 手机模式：底部导航栏
        Box(modifier = modifier.fillMaxSize()) {
            // 主内容区域
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp)  // 为底栏预留空间
            ) {
                content()
            }
            
            // 底部导航栏
            AdaptiveBottomNavigationBar(
                items = items,
                selectedItemId = selectedItemId,
                onItemSelected = onItemSelected,
                hazeState = hazeState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * 🚀 侧边导航栏（平板模式）
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun AdaptiveSideNavigationRail(
    items: List<AdaptiveNavItem>,
    selectedItemId: String,
    onItemSelected: (String) -> Unit,
    hazeState: HazeState? = null
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val activeHazeState = hazeState
        ?.takeIf { shouldAllowRuntimeShaderBackedHazeEffect(Build.VERSION.SDK_INT) }
    
    // Expanded 模式使用带标签的 NavigationDrawer，Medium 模式使用纯图标的 Rail
    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .width(if (isExpanded) 80.dp else 72.dp)
            .then(
                if (activeHazeState != null) {
                    Modifier.hazeEffect(
                        state = activeHazeState,
                        style = HazeMaterials.ultraThin()
                    )
                } else Modifier
            ),
        containerColor = if (activeHazeState != null) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Spacer(Modifier.height(12.dp))
        
        items.forEach { item ->
            val selected = item.id == selectedItemId
            
            NavigationRailItem(
                selected = selected,
                onClick = { onItemSelected(item.id) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (item.badgeCount > 0) {
                                Badge { Text(item.badgeCount.toString()) }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.icon,
                            contentDescription = item.label
                        )
                    }
                },
                label = if (isExpanded) {{ Text(item.label) }} else null,
                alwaysShowLabel = isExpanded,
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

/**
 * 📱 底部导航栏（手机模式）
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun AdaptiveBottomNavigationBar(
    items: List<AdaptiveNavItem>,
    selectedItemId: String,
    onItemSelected: (String) -> Unit,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    val activeHazeState = hazeState
        ?.takeIf { shouldAllowRuntimeShaderBackedHazeEffect(Build.VERSION.SDK_INT) }
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (activeHazeState != null) {
                    Modifier.hazeEffect(
                        state = activeHazeState,
                        style = HazeMaterials.ultraThin()
                    )
                } else Modifier
            ),
        containerColor = if (activeHazeState != null) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        items.forEach { item ->
            val selected = item.id == selectedItemId
            
            NavigationBarItem(
                selected = selected,
                onClick = { onItemSelected(item.id) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (item.badgeCount > 0) {
                                Badge { Text(item.badgeCount.toString()) }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.icon,
                            contentDescription = item.label
                        )
                    }
                },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

/**
 * 📐 自适应分栏布局容器
 * 
 * 平板模式下自动分为左右两栏，手机模式下单列显示
 * 
 * @param primaryContent 主内容（左侧）
 * @param secondaryContent 次要内容（右侧）
 * @param primaryRatio 主内容占比（0.0-1.0）
 */
@Composable
fun AdaptiveSplitLayout(
    primaryContent: @Composable () -> Unit,
    secondaryContent: @Composable () -> Unit,
    primaryRatio: Float = 0.65f,
    modifier: Modifier = Modifier
) {
    val windowSizeClass = LocalWindowSizeClass.current
    
    if (windowSizeClass.shouldUseSplitLayout) {
        // 🖥️ 平板模式：左右分栏
        Row(modifier = modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(primaryRatio)
            ) {
                primaryContent()
            }
            
            // 分隔线
            Spacer(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
            
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f - primaryRatio)
            ) {
                secondaryContent()
            }
        }
    } else {
        // 📱 手机模式：单列布局
        Box(modifier = modifier.fillMaxSize()) {
            primaryContent()
        }
    }
}
