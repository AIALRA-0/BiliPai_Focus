// 文件路径: feature/home/components/iOSRefreshIndicator.kt
package com.android.purebilibili.feature.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.LocalAndroidNativeVariant
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.ui.PresetPrimitiveRenderer
import com.android.purebilibili.core.ui.resolvePresetPrimitiveRenderer
import com.android.purebilibili.feature.home.resolvePullRefreshHintText
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator

/**
 * Renderer kind for [iOSRefreshIndicator]. iOS keeps its Cupertino spinner with
 * the rubber-band overshoot; MD3 falls back to Material's [CircularProgressIndicator];
 * the Miuix bridge currently reuses the Material indicator (BiliPai routes Miuix
 * refresh visuals through the same Material PullToRefresh container).
 */
enum class IOSRefreshIndicatorRenderer {
    CUPERTINO_IOS,
    MATERIAL3_CIRCULAR,
    MIUIX_BRIDGED
}

fun resolveRefreshIndicatorRenderer(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant
): IOSRefreshIndicatorRenderer = when (
    resolvePresetPrimitiveRenderer(uiPreset, androidNativeVariant)
) {
    PresetPrimitiveRenderer.IOS -> IOSRefreshIndicatorRenderer.CUPERTINO_IOS
    PresetPrimitiveRenderer.MATERIAL3 -> IOSRefreshIndicatorRenderer.MATERIAL3_CIRCULAR
    PresetPrimitiveRenderer.MIUIX_BRIDGED -> IOSRefreshIndicatorRenderer.MIUIX_BRIDGED
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun Md3ScreenshotRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    indicatorHeight: Dp,
    modifier: Modifier = Modifier
) {
    val progress = state.distanceFraction
    val hintText = resolvePullRefreshHintText(
        progress = progress,
        isRefreshing = isRefreshing,
        isStateAnimating = state.isAnimating
    )
    val alpha by animateFloatAsState(
        targetValue = if (progress > 0.08f || isRefreshing) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.82f),
        label = "md3_screenshot_pull_alpha"
    )
    val indicatorScale by animateFloatAsState(
        targetValue = when {
            isRefreshing -> 1f
            progress >= 1f && !state.isAnimating -> 1.04f
            else -> (0.86f + progress.coerceIn(0f, 1f) * 0.14f)
        },
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 360f),
        label = "md3_screenshot_pull_scale"
    )
    val strokeColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                scaleX = indicatorScale
                scaleY = indicatorScale
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(42.dp),
                    color = strokeColor,
                    strokeWidth = 3.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 26.dp, height = indicatorHeight)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color.Transparent)
                        .border(
                            width = 3.dp,
                            color = strokeColor,
                            shape = RoundedCornerShape(15.dp)
                        )
                )
            }

            if (hintText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (hintText == "松手刷新") "松开刷新" else hintText,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            }
        }
    }
}

/**
 *  iOS 风格下拉刷新指示器
 * 
 * 特点：
 * - 下拉时显示"下拉刷新..."
 * - 达到阈值时显示"松手刷新"  
 * - 刷新中显示 iOS 风格旋转动画
 * - 刷新完成显示"刷新成功"
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun iOSRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val renderer = resolveRefreshIndicatorRenderer(
        uiPreset = LocalUiPreset.current,
        androidNativeVariant = LocalAndroidNativeVariant.current
    )
    //  进度值（0.0 ~ 1.0+）
    val progress = state.distanceFraction
    
    //  是否达到刷新阈值
    val isOverThreshold = progress >= 1f && !state.isAnimating
    
    //  提示文字
    val hintText = resolvePullRefreshHintText(
        progress = progress,
        isRefreshing = isRefreshing,
        isStateAnimating = state.isAnimating
    )
    
    //  箭头只表达阈值状态，使用高阻尼避免松手前后出现夸张回弹。
    val arrowRotation by animateFloatAsState(
        targetValue = if (isOverThreshold) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.9f,
            stiffness = 540f
        ),
        label = "arrow_rotation"
    )
    
    //  透明度动画
    val alpha by animateFloatAsState(
        targetValue = if (progress > 0.1f || isRefreshing) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.92f, stiffness = 620f),
        label = "alpha"
    )
    
    //  缩放跟随手势强度，阈值态只做轻强调，不制造果冻感。
    val scale by animateFloatAsState(
        targetValue = when {
            isRefreshing -> 1f
            isOverThreshold -> 1.03f
            else -> (progress.coerceIn(0f, 1f) * 0.28f + 0.72f).coerceAtMost(1f)
        },
        animationSpec = spring(
            dampingRatio = 0.9f,
            stiffness = 620f
        ),
        label = "scale"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                this.scaleX = scale
                this.scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            if (isRefreshing) {
                //  iOS uses the Cupertino spinner; MD3/Miuix fall back to Material's
                //  CircularProgressIndicator so the indicator reads as native chrome.
                if (renderer == IOSRefreshIndicatorRenderer.CUPERTINO_IOS) {
                    CupertinoActivityIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                }
            } else if (progress > 0.1f) {
                //  箭头图标（旋转表示状态变化）
                Text(
                    text = "↓",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.rotate(arrowRotation)
                )
            }
            
            if (hintText.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = hintText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
