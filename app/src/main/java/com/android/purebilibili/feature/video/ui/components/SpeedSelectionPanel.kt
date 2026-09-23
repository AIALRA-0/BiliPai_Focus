// 文件路径: feature/video/ui/components/SpeedSelectionPanel.kt
package com.android.purebilibili.feature.video.ui.components
import com.android.purebilibili.core.ui.components.AppText

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppSlider
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
//  已改用 MaterialTheme.colorScheme.primary

/**
 * 播放速度选项
 */
object PlaybackSpeed {
    val OPTIONS = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)
    
    fun formatSpeed(speed: Float): String {
        return if (speed == 1.0f) "倍速" else "${removeTrailingZeros(speed)}x"
    }
    
    fun formatSpeedFull(speed: Float): String {
        return if (speed == 1.0f) "正常" else "${removeTrailingZeros(speed)}x"
    }

    private fun removeTrailingZeros(value: Float): String {
        return if (value % 1.0f == 0f) {
            value.toInt().toString()
        } else {
            // Keep up to 2 decimal places if needed
            val str = String.format("%.2f", value)
            str.trimEnd('0').trimEnd('.')
        }
    }
}

/**
 * 播放速度选择菜单
 */
@Composable
fun SpeedSelectionMenu(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppSurface(
        modifier = modifier,
        shape = AppShapes.container(ContainerLevel.Card),
        color = Color.Black.copy(alpha = 0.85f),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题
            AppText(
                text = "播放速度",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // 速度选项
            var isCustomMode by remember { mutableStateOf(currentSpeed !in PlaybackSpeed.OPTIONS && currentSpeed != 1.0f) }
            
            if (isCustomMode) {
                // 自定义模式 UI
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    AppText(
                        text = "自定义: ${PlaybackSpeed.formatSpeedFull(currentSpeed)}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // 滑块
                    AppSlider(
                        value = currentSpeed,
                        onValueChange = onSpeedSelected,
                        valueRange = 0.1f..8.0f,
                        steps = 0, // Continuous
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AppText("0.1x", color = Color.White.copy(0.5f), style = MaterialTheme.typography.labelSmall)
                        AppText("8.0x", color = Color.White.copy(0.5f), style = MaterialTheme.typography.labelSmall)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 微调按钮
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AppButton(
                            onClick = { 
                                val newSpeed = (currentSpeed - 0.1f).coerceAtLeast(0.1f)
                                onSpeedSelected((newSpeed * 10).toInt() / 10f) 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            AppText("-0.1")
                        }
                        
                        AppButton(
                            onClick = { 
                                val newSpeed = (currentSpeed + 0.1f).coerceAtMost(8.0f)
                                onSpeedSelected((newSpeed * 10).toInt() / 10f)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            AppText("+0.1")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 返回预设
                    AppTextButton(onClick = { isCustomMode = false }) {
                        AppText("返回预设选项")
                    }
                }
            } else {
                // 预设列表
                 val optionsWithCustom = PlaybackSpeed.OPTIONS + if (currentSpeed !in PlaybackSpeed.OPTIONS) listOf(currentSpeed) else emptyList()
                 // 去重并排序
                 val displayOptions = optionsWithCustom.distinct().sorted()
                 
                 // 如果列表太长，使用 LazyColumn 或者 网格布局？这里保持简单列布局，但是增加自定义按钮
                
                PlaybackSpeed.OPTIONS.forEach { speed ->
                    val isSelected = speed == currentSpeed
                    AppSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        shape = AppShapes.container(ContainerLevel.Chip),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                        onClick = {
                            onSpeedSelected(speed)
                            onDismiss()
                        }
                    ) {
                        AppText(
                            text = PlaybackSpeed.formatSpeedFull(speed),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                }
                
                // 自定义按钮
                AppSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp), // 稍微分开一点
                    shape = AppShapes.container(ContainerLevel.Chip),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.3f)),
                    color = Color.Transparent,
                    onClick = { isCustomMode = true }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                         AppText(
                            text = "自定义倍速...",
                            color = Color.White.copy(0.9f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

/**
 * 倍速按钮（用于底部控制栏）
 */
@Composable
fun SpeedButton(
    currentSpeed: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppSurface(
        onClick = onClick,
        modifier = modifier,
        shape = AppShapes.container(ContainerLevel.Chip),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        AppText(
            text = PlaybackSpeed.formatSpeed(currentSpeed),
            color = if (currentSpeed != 1.0f) MaterialTheme.colorScheme.primary else Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (currentSpeed != 1.0f) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
