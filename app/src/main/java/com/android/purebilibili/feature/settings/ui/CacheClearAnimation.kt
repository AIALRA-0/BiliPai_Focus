// 文件路径: feature/settings/CacheClearAnimation.kt
package com.android.purebilibili.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.purebilibili.core.util.CacheClearTarget
import com.android.purebilibili.core.theme.iOSBlue
import kotlin.math.*
import kotlin.random.Random
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 *  缓存清理进度数据
 */
data class CacheClearProgress(
    val current: Long,
    val total: Long,
    val isComplete: Boolean = false,
    val clearedSize: String = ""
)

// ==================== iOS 风格色彩系统 ====================

private object CacheAnimationColors {
    // 主色调 - 柔和的蓝紫渐变
    val primaryBlue = Color(0xFF007AFF)
    val secondaryPurple = Color(0xFF5856D6)
    val accentCyan = Color(0xFF32ADE6)
    
    // 完成状态 - 清新绿色
    val successGreen = Color(0xFF34C759)
    val successGreenLight = Color(0xFF30D158)
    
    // 粒子色彩 - 轻盈的渐变色系
    val particleColors = listOf(
        Color(0xFF007AFF).copy(alpha = 0.6f),
        Color(0xFF5856D6).copy(alpha = 0.5f),
        Color(0xFF32ADE6).copy(alpha = 0.4f),
        Color(0xFFAF52DE).copy(alpha = 0.3f),
        Color(0xFFFF9500).copy(alpha = 0.2f)
    )
}

// ==================== 粒子系统 ====================

/**
 *  消散粒子数据
 */
private data class DissolveParticle(
    val id: Int,
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: Color,
    val velocityX: Float,
    val velocityY: Float,
    val rotationSpeed: Float,
    val lifetime: Float,
    val maxLifetime: Float,
    val shape: ParticleShape
)

private enum class ParticleShape { CIRCLE, SQUARE, DIAMOND }

/**
 * ✨ Telegram 风格粒子飞出效果
 * 
 * 基于 Telegram ThanosEffect 物理参数：
 * - 重力: 65 dp/s²（向下）
 * - 侧向漂移: 19 dp/s
 * - 速度衰减: 0.99x/帧
 * - 生命周期: 0.7-1.5秒
 */
@Composable
fun DataDissolveParticles(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    progress: Float = 0f,
    primaryColor: Color = CacheAnimationColors.primaryBlue
) {
    var particles by remember { mutableStateOf(listOf<DissolveParticle>()) }
    var frameCount by remember { mutableIntStateOf(0) }
    
    // Telegram 物理参数
    val gravityPerFrame = 65f * 0.016f * 0.001f   // 重力（每帧，归一化）
    val lateralDriftPerFrame = 19f * 0.016f * 0.001f  // 侧向漂移（每帧，归一化）
    val velocityDecay = 0.99f  // 速度衰减
    
    LaunchedEffect(isActive) {
        if (isActive) {
            while (true) {
                frameCount++
                
                // 根据进度调整粒子生成密度（进度越高越多粒子）
                val spawnRate = when {
                    progress < 0.2f -> 3
                    progress < 0.5f -> 2
                    progress < 0.8f -> 1
                    else -> 1
                }
                
                val maxParticles = (30 + progress * 40).toInt()  // 30-70 粒子
                
                if (frameCount % spawnRate == 0 && particles.size < maxParticles) {
                    // 从垃圾桶中央向上发射粒子
                    val initialAngle = -PI.toFloat() / 2 + (Random.nextFloat() - 0.5f) * PI.toFloat() * 0.6f
                    val initialSpeed = Random.nextFloat() * 0.015f + 0.01f  // 初始速度
                    
                    val particle = DissolveParticle(
                        id = frameCount,
                        // 从垃圾桶顶部中央发射
                        x = 0.5f + (Random.nextFloat() - 0.5f) * 0.15f,
                        y = 0.45f + (Random.nextFloat() - 0.5f) * 0.1f,
                        radius = Random.nextFloat() * 3f + 2f,
                        color = CacheAnimationColors.particleColors.random(),
                        // 初始速度：主要向上，带随机侧向分量
                        velocityX = cos(initialAngle) * initialSpeed + (Random.nextFloat() - 0.5f) * 0.005f,
                        velocityY = sin(initialAngle) * initialSpeed,
                        rotationSpeed = Random.nextFloat() * 4f - 2f,
                        lifetime = 0f,
                        // Telegram 生命周期：0.7-1.5秒
                        maxLifetime = 0.7f + Random.nextFloat() * 0.8f,
                        shape = ParticleShape.entries.random()
                    )
                    particles = particles + particle
                }
                
                // Telegram 风格物理更新
                particles = particles.mapNotNull { p ->
                    val newLifetime = p.lifetime + 0.016f  // 约 60fps
                    if (newLifetime > p.maxLifetime) null
                    else {
                        // 应用重力、侧向漂移和速度衰减
                        val newVelocityX = (p.velocityX + lateralDriftPerFrame) * velocityDecay
                        val newVelocityY = (p.velocityY + gravityPerFrame) * velocityDecay
                        
                        p.copy(
                            x = p.x + newVelocityX,
                            y = p.y + newVelocityY,
                            velocityX = newVelocityX,
                            velocityY = newVelocityY,
                            lifetime = newLifetime
                        )
                    }
                }
                
                kotlinx.coroutines.delay(16L)
            }
        } else {
            particles = emptyList()
        }
    }
    
    Canvas(modifier = modifier) {
        particles.forEach { p ->
            // Telegram 透明度：time / 0.55 渐变
            val lifeRatio = p.lifetime / p.maxLifetime
            val alphaProgress = (lifeRatio / 0.55f).coerceIn(0f, 1f)
            val alpha = (1f - alphaProgress).coerceIn(0f, 1f)
            
            val scale = (1f - lifeRatio * 0.5f).coerceIn(0.2f, 1f)
            val x = p.x * size.width
            val y = p.y * size.height
            
            when (p.shape) {
                ParticleShape.CIRCLE -> {
                    drawCircle(
                        color = p.color.copy(alpha = alpha * p.color.alpha),
                        radius = p.radius * scale,
                        center = Offset(x, y)
                    )
                }
                ParticleShape.SQUARE -> {
                    rotate(p.rotationSpeed * p.lifetime * 90f, Offset(x, y)) {
                        drawRect(
                            color = p.color.copy(alpha = alpha * p.color.alpha),
                            topLeft = Offset(x - p.radius * scale, y - p.radius * scale),
                            size = Size(p.radius * 2 * scale, p.radius * 2 * scale)
                        )
                    }
                }
                ParticleShape.DIAMOND -> {
                    rotate(45f + p.rotationSpeed * p.lifetime * 60f, Offset(x, y)) {
                        drawRect(
                            color = p.color.copy(alpha = alpha * p.color.alpha),
                            topLeft = Offset(x - p.radius * scale * 0.7f, y - p.radius * scale * 0.7f),
                            size = Size(p.radius * 1.4f * scale, p.radius * 1.4f * scale)
                        )
                    }
                }
            }
        }
    }
}

// ==================== 圆形进度环 ====================

/**
 *  iOS 风格圆形进度环
 * 替代传统进度条，更加优雅现代
 */
@Composable
fun CircularProgressRing(
    modifier: Modifier = Modifier,
    progress: Float,
    isComplete: Boolean,
    size: Dp = 200.dp,
    strokeWidth: Dp = 8.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    
    // 进度环旋转动画（清理中）
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // 渐变起始角度动画
    val gradientAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientAngle"
    )
    
    // 脉冲效果
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // 完成时的动画值
    val completionScale by animateFloatAsState(
        targetValue = if (isComplete) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "completionScale"
    )
    
    val animatedProgress by animateFloatAsState(
        targetValue = if (isComplete) 1f else progress,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "animatedProgress"
    )
    
    Canvas(
        modifier = modifier
            .size(size)
            .rotate(if (!isComplete) rotation else 0f)
    ) {
        val canvasSize = this.size.minDimension
        val radius = (canvasSize / 2) - strokeWidth.toPx()
        val center = Offset(canvasSize / 2, canvasSize / 2)
        val stroke = strokeWidth.toPx()
        
        val scale = if (isComplete) completionScale else pulseScale
        
        // 背景轨道 - 半透明
        drawCircle(
            color = Color(0xFF8E8E93).copy(alpha = 0.15f),
            radius = radius * scale,
            center = center,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        
        // 渐变进度弧
        val sweepAngle = animatedProgress * 360f
        val colors = if (isComplete) {
            listOf(
                CacheAnimationColors.successGreen,
                CacheAnimationColors.successGreenLight
            )
        } else {
            listOf(
                CacheAnimationColors.primaryBlue,
                CacheAnimationColors.accentCyan,
                CacheAnimationColors.secondaryPurple,
                CacheAnimationColors.primaryBlue
            )
        }
        
        val brush = Brush.sweepGradient(
            colors = colors,
            center = center
        )
        
        drawArc(
            brush = brush,
            startAngle = -90f + gradientAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius * scale, center.y - radius * scale),
            size = Size(radius * 2 * scale, radius * 2 * scale),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        
        // 进度头部光晕效果
        if (!isComplete && animatedProgress > 0.01f) {
            val headAngle = Math.toRadians((-90.0 + sweepAngle + gradientAngle).toDouble())
            val headX = center.x + radius * scale * cos(headAngle).toFloat()
            val headY = center.y + radius * scale * sin(headAngle).toFloat()
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.6f),
                        Color.White.copy(alpha = 0f)
                    ),
                    center = Offset(headX, headY),
                    radius = stroke * 2
                ),
                radius = stroke * 2,
                center = Offset(headX, headY)
            )
        }
    }
}

/**
 *  优雅的垃圾桶/闪光图标动画
 * 
 * 新增：动态填充层显示缓存减少
 * @param fillLevel 填充等级（1.0=满，0.0=空）
 */
@Composable
fun CenterCleaningIcon(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    isAnimating: Boolean = true,
    isComplete: Boolean = false,
    fillLevel: Float = 1f,  // 新增：填充等级
    primaryColor: Color = CacheAnimationColors.primaryBlue
) {
    val infiniteTransition = rememberInfiniteTransition(label = "icon")
    
    // 垃圾桶盖子摆动
    val lidAngle by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lidAngle"
    )
    
    // 闪光旋转
    val sparkleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle"
    )
    
    // 闪光缩放脉冲
    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleScale"
    )
    
    // 完成动画 - 对勾出现
    val checkScale by animateFloatAsState(
        targetValue = if (isComplete) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "checkScale"
    )
    
    val iconColor = if (isComplete) CacheAnimationColors.successGreen else primaryColor
    
    Canvas(modifier = modifier.size(size)) {
        val canvasSize = size.toPx()
        val centerX = canvasSize / 2
        val centerY = canvasSize / 2
        
        if (isComplete) {
            //  完成状态 - 绘制对勾
            val checkPaint = iconColor
            val checkStroke = canvasSize * 0.08f
            
            val checkPath = Path().apply {
                moveTo(centerX - canvasSize * 0.25f, centerY)
                lineTo(centerX - canvasSize * 0.05f, centerY + canvasSize * 0.2f)
                lineTo(centerX + canvasSize * 0.3f, centerY - canvasSize * 0.15f)
            }
            
            drawPath(
                path = checkPath,
                color = checkPaint,
                style = Stroke(
                    width = checkStroke * checkScale,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            
            // 成功光晕
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CacheAnimationColors.successGreen.copy(alpha = 0.2f * checkScale),
                        CacheAnimationColors.successGreen.copy(alpha = 0f)
                    ),
                    center = Offset(centerX, centerY),
                    radius = canvasSize * 0.6f
                ),
                radius = canvasSize * 0.6f,
                center = Offset(centerX, centerY)
            )
        } else {
            //  清理中 - 绘制优雅的垃圾桶
            val trashWidth = canvasSize * 0.5f
            val trashHeight = canvasSize * 0.55f
            val trashLeft = centerX - trashWidth / 2
            val trashTop = centerY - trashHeight / 2 + canvasSize * 0.05f
            
            // 垃圾桶身体
            val bodyPath = Path().apply {
                moveTo(trashLeft + trashWidth * 0.1f, trashTop + trashHeight * 0.25f)
                lineTo(trashLeft + trashWidth * 0.2f, trashTop + trashHeight)
                lineTo(trashLeft + trashWidth * 0.8f, trashTop + trashHeight)
                lineTo(trashLeft + trashWidth * 0.9f, trashTop + trashHeight * 0.25f)
                close()
            }
            
            drawPath(
                path = bodyPath,
                brush = Brush.linearGradient(
                    colors = listOf(iconColor, iconColor.copy(alpha = 0.7f)),
                    start = Offset(trashLeft, trashTop),
                    end = Offset(trashLeft + trashWidth, trashTop + trashHeight)
                )
            )
            
            // 🎯 新增：动态填充层 - 显示缓存减少
            val animatedFillLevel = fillLevel.coerceIn(0f, 1f)
            if (animatedFillLevel > 0.01f) {
                // 计算填充区域（在垃圾桶内部）
                val fillTop = trashTop + trashHeight * (1f - animatedFillLevel * 0.65f)
                val fillBottom = trashTop + trashHeight * 0.95f
                val fillHeight = fillBottom - fillTop
                
                // 根据梯形形状计算左右边界
                val leftSlope = (trashWidth * 0.1f) / (trashHeight * 0.75f)
                val rightSlope = (trashWidth * 0.1f) / (trashHeight * 0.75f)
                val topOffset = (fillTop - (trashTop + trashHeight * 0.25f))
                
                val fillLeftX = trashLeft + trashWidth * 0.15f + leftSlope * topOffset
                val fillRightX = trashLeft + trashWidth * 0.85f - rightSlope * topOffset
                
                // 渐变色数据块填充
                val fillPath = Path().apply {
                    moveTo(fillLeftX, fillTop)
                    lineTo(trashLeft + trashWidth * 0.22f, fillBottom)
                    lineTo(trashLeft + trashWidth * 0.78f, fillBottom)
                    lineTo(fillRightX, fillTop)
                    close()
                }
                
                // 多层渐变色表示数据
                val dataColors = listOf(
                    Color(0xFF5AC8FA).copy(alpha = 0.7f * animatedFillLevel),  // iOS 浅蓝
                    Color(0xFF007AFF).copy(alpha = 0.6f * animatedFillLevel),  // iOS 蓝
                    Color(0xFF5856D6).copy(alpha = 0.5f * animatedFillLevel),  // iOS 紫
                    Color(0xFFAF52DE).copy(alpha = 0.4f * animatedFillLevel)   // iOS 粉紫
                )
                
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = dataColors,
                        startY = fillTop,
                        endY = fillBottom
                    )
                )
                
                // 添加顶部高光条纹（表示数据）
                if (animatedFillLevel > 0.2f) {
                    val stripeCount = (animatedFillLevel * 4).toInt().coerceIn(1, 4)
                    for (i in 0 until stripeCount) {
                        val stripeY = fillTop + fillHeight * (0.15f + i * 0.22f)
                        if (stripeY < fillBottom - 4f) {
                            drawLine(
                                color = Color.White.copy(alpha = 0.3f * animatedFillLevel),
                                start = Offset(fillLeftX + 4f, stripeY),
                                end = Offset(fillRightX - 4f, stripeY),
                                strokeWidth = 2f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
            
            // 垃圾桶把手/盖子
            val angle = if (isAnimating) lidAngle else 0f
            val lidPivotX = centerX
            val lidPivotY = trashTop + trashHeight * 0.2f
            
            rotate(angle, Offset(lidPivotX, lidPivotY)) {
                // 盖子
                drawRoundRect(
                    color = iconColor,
                    topLeft = Offset(trashLeft - trashWidth * 0.05f, trashTop + trashHeight * 0.1f),
                    size = Size(trashWidth * 1.1f, trashHeight * 0.12f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
                )
                
                // 把手
                drawRoundRect(
                    color = iconColor,
                    topLeft = Offset(centerX - trashWidth * 0.15f, trashTop - trashHeight * 0.05f),
                    size = Size(trashWidth * 0.3f, trashHeight * 0.18f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f)
                )
            }
            
            // 垃圾桶条纹装饰
            val stripeColor = Color.White.copy(alpha = 0.3f)
            for (i in 0..2) {
                val stripeX = trashLeft + trashWidth * (0.3f + i * 0.2f)
                drawLine(
                    color = stripeColor,
                    start = Offset(stripeX, trashTop + trashHeight * 0.35f),
                    end = Offset(stripeX + trashWidth * 0.02f, trashTop + trashHeight * 0.9f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
            
            // ✨ 闪光效果
            if (isAnimating) {
                val sparkles = listOf(
                    Offset(centerX + canvasSize * 0.35f, centerY - canvasSize * 0.25f),
                    Offset(centerX - canvasSize * 0.3f, centerY - canvasSize * 0.35f),
                    Offset(centerX + canvasSize * 0.25f, centerY + canvasSize * 0.3f)
                )
                
                sparkles.forEachIndexed { index, pos ->
                    val scale = sparkleScale * (0.8f + index * 0.1f)
                    val rotation = sparkleRotation + index * 30f
                    
                    rotate(rotation, pos) {
                        // 四角星闪光
                        val starSize = canvasSize * 0.08f * scale
                        val starPath = Path().apply {
                            moveTo(pos.x, pos.y - starSize)
                            lineTo(pos.x + starSize * 0.3f, pos.y - starSize * 0.3f)
                            lineTo(pos.x + starSize, pos.y)
                            lineTo(pos.x + starSize * 0.3f, pos.y + starSize * 0.3f)
                            lineTo(pos.x, pos.y + starSize)
                            lineTo(pos.x - starSize * 0.3f, pos.y + starSize * 0.3f)
                            lineTo(pos.x - starSize, pos.y)
                            lineTo(pos.x - starSize * 0.3f, pos.y - starSize * 0.3f)
                            close()
                        }
                        
                        drawPath(
                            path = starPath,
                            color = Color.White.copy(alpha = 0.8f - index * 0.15f)
                        )
                    }
                }
            }
        }
    }
}

// ==================== 对话框组件 ====================

/**
 *  缓存清理确认对话框
 */
@Composable
internal fun CacheClearConfirmDialog(
    selectedCacheSizeSummary: String,
    options: List<CacheClearOptionUiModel>,
    selectedTargets: Set<CacheClearTarget>,
    onTargetToggle: (CacheClearTarget, Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    com.android.purebilibili.core.ui.IOSAlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "清除缓存", 
                color = MaterialTheme.colorScheme.onSurface, 
                fontWeight = FontWeight.Bold
            ) 
        },
        text = { 
            Column {
                Text(
                    resolveCacheClearConfirmationMessage(selectedTargets),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    selectedCacheSizeSummary,
                    color = MaterialTheme.colorScheme.primary, 
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(16.dp))
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onTargetToggle(option.target, option.target !in selectedTargets)
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Checkbox(
                            checked = option.target in selectedTargets,
                            onCheckedChange = { checked ->
                                onTargetToggle(option.target, checked)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                option.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                option.description,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            com.android.purebilibili.core.ui.IOSDialogAction(
                onClick = onConfirm
            ) { 
                Text(
                    "确认清除",
                    color = if (selectedTargets.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        com.android.purebilibili.core.theme.iOSRed
                    }
                ) 
            }
        },
        dismissButton = { 
            com.android.purebilibili.core.ui.IOSDialogAction(onClick = onDismiss) { 
                Text("取消", color = MaterialTheme.colorScheme.primary) 
            } 
        }
    )
}

/**
 *  iOS 风格缓存清理动画全屏对话框
 * 
 * 设计理念：
 * - 圆形进度环替代传统进度条
 * - 优雅的垃圾桶/闪光图标动画
 * - 细腻的粒子消散效果
 * - 柔和的渐变色彩
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun CacheClearAnimationDialog(
    progress: CacheClearProgress,
    onDismiss: () -> Unit
) {
    val hazeState = com.android.purebilibili.core.ui.blur.rememberRecoverableHazeState()
    
    val progressValue = if (progress.total > 0) {
        (progress.current.toFloat() / progress.total.toFloat()).coerceIn(0f, 1f)
    } else 0f
    
    // 完成后自动关闭
    LaunchedEffect(progress.isComplete) {
        if (progress.isComplete) {
            kotlinx.coroutines.delay(2000L)
            onDismiss()
        }
    }
    
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = progress.isComplete,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // 主内容卡片
            Box(
                modifier = Modifier
                    .padding(48.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            )
                        )
                    )
                    .hazeChild(
                        state = hazeState, 
                        style = HazeMaterials.thin(MaterialTheme.colorScheme.surface)
                    )
                    .padding(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 主动画区域
                    Box(
                        modifier = Modifier.size(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // 粒子效果层
                        DataDissolveParticles(
                            modifier = Modifier.fillMaxSize(),
                            isActive = !progress.isComplete,
                            progress = progressValue
                        )
                        
                        // 圆形进度环
                        CircularProgressRing(
                            modifier = Modifier,
                            progress = progressValue,
                            isComplete = progress.isComplete,
                            size = 180.dp,
                            strokeWidth = 6.dp
                        )
                        
                        // 中心图标
                        CenterCleaningIcon(
                            modifier = Modifier,
                            size = 70.dp,
                            isAnimating = !progress.isComplete,
                            isComplete = progress.isComplete,
                            fillLevel = 1f - progressValue  // 填充随进度减少
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    // 状态文字
                    Text(
                        text = if (progress.isComplete) "清理完成" else "正在清理",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (progress.isComplete) {
                            CacheAnimationColors.successGreen
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 进度详情
                    Text(
                        text = if (progress.clearedSize.isNotEmpty()) {
                            if (progress.isComplete) "共释放 ${progress.clearedSize}" 
                            else "已清理 ${progress.clearedSize}"
                        } else {
                            "准备中..."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // 百分比显示
                    if (!progress.isComplete && progressValue > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(progressValue * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = CacheAnimationColors.primaryBlue.copy(alpha = 0.8f)
                        )
                    }
                    
                    if (progress.isComplete) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "即将自动关闭...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
