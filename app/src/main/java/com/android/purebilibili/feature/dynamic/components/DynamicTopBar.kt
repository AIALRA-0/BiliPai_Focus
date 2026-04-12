// 文件路径: feature/dynamic/components/DynamicTopBar.kt
package com.android.purebilibili.feature.dynamic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import com.android.purebilibili.core.ui.blur.unifiedBlur
import com.android.purebilibili.feature.dynamic.resolveDynamicTopBarHorizontalPadding
import com.android.purebilibili.feature.dynamic.resolveDynamicTopBarTabEndPadding
import com.android.purebilibili.core.ui.blur.BlurStyles
import com.android.purebilibili.core.ui.blur.currentUnifiedBlurIntensity
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
    hazeState: HazeState? = null
) {
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density).let { with(density) { it.toDp() } }
    val tabScrollState = rememberScrollState()
    
    //  读取当前模糊强度以确定背景透明度
    val blurIntensity = currentUnifiedBlurIntensity()
    val backgroundAlpha = BlurStyles.getBackgroundAlpha(blurIntensity)
    
    //  使用 blurIntensity 对应的背景透明度实现毛玻璃质感
    val headerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (hazeState != null) backgroundAlpha else 0f)

    //  [关键修复] 使用透明背景，让主界面的渐变透出来
    Box(
        modifier = modifier
            .fillMaxWidth()
            // 应用模糊效果
            .then(if (hazeState != null) Modifier.unifiedBlur(hazeState) else Modifier)
            .background(headerColor)
    ) {
        Column {
            Spacer(modifier = Modifier.height(statusBarHeight))
            
            //  标题行：标题 - 高度设为 44dp 以与左侧边栏返回按钮对齐
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp) // 固定高度 44dp
                    .padding(horizontal = resolveDynamicTopBarHorizontalPadding()), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 标题
                Text(
                    "动态",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground // 自适应颜色
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
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
            
            // Tab栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(tabScrollState)
                    .padding(
                        start = resolveDynamicTopBarHorizontalPadding(),
                        end = resolveDynamicTopBarHorizontalPadding()
                    ),
                horizontalArrangement = Arrangement.Start
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTab == index
                    val selectedColor = rememberDynamicTabSelectedColor()
                    val defaultColor = rememberDynamicTabUnselectedColor()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onTabSelected(index) }
                            .padding(end = resolveDynamicTopBarTabEndPadding(), bottom = 2.dp)
                    ) {
                        Text(
                            tab,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) selectedColor else defaultColor
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .width(if (isSelected) 24.dp else 18.dp)
                                .height(if (isSelected) 2.5.dp else 2.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isSelected) selectedColor else Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberDynamicTabSelectedColor(): Color {
    return if (isDynamicTopBarDarkSurface(MaterialTheme.colorScheme.surface)) {
        Color(0xFFE7CF97)
    } else {
        MaterialTheme.colorScheme.primary
    }
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
