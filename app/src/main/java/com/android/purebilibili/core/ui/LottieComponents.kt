// 文件路径: core/ui/LottieComponents.kt
package com.android.purebilibili.core.ui

import androidx.annotation.RawRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.android.purebilibili.R
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 *  Lottie 动画加载器
 * 使用在线 Lottie 动画 URL
 */
object LottieUrls {
    //  精选免费 Lottie 动画资源
    const val LOADING_DOTS = "https://lottie.host/5d9d2c7c-d7f4-4f3e-9e9f-f8a6e1f8c3a1/loading.json"
    const val LOADING_CIRCLE = "https://assets2.lottiefiles.com/packages/lf20_p8bfn5to.json"
    const val LOADING_BILIBILI = "https://assets10.lottiefiles.com/packages/lf20_jcikwtux.json"
    const val LIKE_HEART = "https://assets4.lottiefiles.com/packages/lf20_hc7rwmvb.json"
    const val STAR = "https://assets9.lottiefiles.com/packages/lf20_c50nklxn.json"
    const val CONFETTI = "https://assets10.lottiefiles.com/packages/lf20_u4yrau.json"
    const val SUCCESS = "https://assets4.lottiefiles.com/packages/lf20_jbrw3hcz.json"
    const val ERROR = "https://assets1.lottiefiles.com/packages/lf20_cr9slsdh.json"
    const val EMPTY = "https://raw.githubusercontent.com/DrKLO/Telegram/master/TMessagesProj/src/main/res/raw/utyan_empty2.json"
    const val REFRESH = "https://assets3.lottiefiles.com/packages/lf20_ykzaax7v.json"
    
    //  新手引导页面动画
    const val WELCOME = "https://assets10.lottiefiles.com/packages/lf20_u4yrau.json"  // 庆祝彩屑
    const val THEME_COLORS = "https://assets5.lottiefiles.com/packages/lf20_jtbfg2nb.json"  // 彩虹渐变
    const val VIDEO_PLAY = "https://assets8.lottiefiles.com/packages/lf20_khzniaya.json"  // 播放按钮
    
    //  Telegram 风格设置页面动画 (使用 Telegram Android 开源项目的官方动画)
    // 来源: https://github.com/DrKLO/Telegram/tree/master/TMessagesProj/src/main/res/raw
    
    private const val TELEGRAM_RAW_BASE = "https://raw.githubusercontent.com/DrKLO/Telegram/master/TMessagesProj/src/main/res/raw"
    
    //  插件中心 - 使用添加图标动画
    const val SETTINGS_PLUGINS = "$TELEGRAM_RAW_BASE/addone_icon.json"
    
    //  外观设置 - 使用相机/图库动画
    const val SETTINGS_APPEARANCE = "$TELEGRAM_RAW_BASE/attach_gallery.json"
    
    //  主题设置 - 使用夜间模式切换动画
    const val SETTINGS_THEME = "$TELEGRAM_RAW_BASE/auto_night_off.json"
    
    // ✨ 动画与效果 - 使用火焰开启动画
    const val SETTINGS_ANIMATION = "$TELEGRAM_RAW_BASE/fire_on.json"
    
    //  播放设置 - 使用相机动画
    const val SETTINGS_PLAYBACK = "$TELEGRAM_RAW_BASE/camera.json"
    
    // 🛡️ 隐私/权限 - 使用消息锁动画
    const val SETTINGS_PRIVACY = "$TELEGRAM_RAW_BASE/large_message_lock.json"
    
    //  设备 - 使用 iPhone 设备动画
    const val SETTINGS_DEVICE = "$TELEGRAM_RAW_BASE/iphone_30.json"
    // 🔔 通知 - 使用静音/取消静音动画
    const val SETTINGS_NOTIFICATION = "$TELEGRAM_RAW_BASE/ic_unmute.json"
    //  数据 - 使用下载动画
    const val SETTINGS_DATA = "$TELEGRAM_RAW_BASE/ic_download.json"
    //  聊天 - 使用气泡动画
    const val SETTINGS_CHAT = "$TELEGRAM_RAW_BASE/bubble.json"
    // 📁 文件夹 - 使用文件夹导入动画
    const val SETTINGS_FOLDER = "$TELEGRAM_RAW_BASE/folder_in.json"
    //  手势滑动 - 使用滑动回复提示动画
    const val SETTINGS_SWIPE = "$TELEGRAM_RAW_BASE/hint_swipe_reply.json"
    
    // ➕ 更多 Telegram 设置动画
    //  同步/联系人
    const val SETTINGS_SYNC = "$TELEGRAM_RAW_BASE/contacts_sync_on.json"
    // 📤 转发
    const val SETTINGS_FORWARD = "$TELEGRAM_RAW_BASE/forward.json"
    //  归档
    const val SETTINGS_ARCHIVE = "$TELEGRAM_RAW_BASE/chats_archive.json"
    //  复制
    const val SETTINGS_COPY = "$TELEGRAM_RAW_BASE/copy.json"
    // 🎁 礼物/打赏
    const val SETTINGS_GIFT = "$TELEGRAM_RAW_BASE/gift.json"
    // ℹ️ 信息/关于
    const val SETTINGS_INFO = "$TELEGRAM_RAW_BASE/info.json"
    //  筛选/过滤
    const val SETTINGS_FILTER = "$TELEGRAM_RAW_BASE/filters.json"
    //  完成/成功
    const val SETTINGS_DONE = "$TELEGRAM_RAW_BASE/done.json"
    //  错误
    const val SETTINGS_ERROR = "$TELEGRAM_RAW_BASE/error.json"
    // 🌐 翻译
    const val SETTINGS_TRANSLATE = "$TELEGRAM_RAW_BASE/msg_translate.json"
    // 📞 来电
    const val SETTINGS_CALLS = "$TELEGRAM_RAW_BASE/incoming_calls.json"
    // 👋 手势
    const val SETTINGS_GESTURE = "$TELEGRAM_RAW_BASE/hand_1.json"

}

/**
 *  设置页本地动画资源
 * 更精美、更贴合功能语义的动画资源
 */
object SettingsHeaderAnimations {
    //  插件中心 - 拼图/插件模块
    val PLUGINS = R.raw.puzzle_plugin
    //  外观设置 - 调色盘/配色
    val APPEARANCE = R.raw.palette_color
    //  主题设置 - 日夜切换
    val THEME = R.raw.auto_night_off
    // ✨ 动画与效果 - 火箭/加速
    val ANIMATION = R.raw.boosts
    //  播放设置 - 静音/取消静音
    val PLAYBACK = R.raw.media_mute_unmute
    // 🛡️ 权限管理 - 安全锁
    val PRIVACY = R.raw.large_message_lock
    //  应用图标 - 星星/高级
    val ICON = R.raw.premium_star
}

/**
 *  从本地 raw 资源加载 Lottie 动画
 * 
 * @param rawResId 本地 raw 资源 ID
 * @param dynamicColor 是否启用动态颜色适配（根据深浅色模式自动调整）
 */
@Composable
fun LottieAnimationFromRaw(
    @RawRes rawResId: Int,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    iterations: Int = LottieConstants.IterateForever,
    autoPlay: Boolean = true,
    dynamicColor: Boolean = true,  // 默认开启颜色适配
    speed: Float = 1f,
    progressOverride: Float? = null
) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(rawResId)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        isPlaying = autoPlay && progressOverride == null,
        speed = speed
    )
    val resolvedProgress = progressOverride?.coerceIn(0f, 1f) ?: progress
    
    //  直接显示动画，使用原生颜色，无背景容器
    // 通过阴影效果确保在深浅色模式下都能清晰可见
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    
    Box(
        modifier = modifier
            .size(size),
        contentAlignment = Alignment.Center
    ) {
        com.airbnb.lottie.compose.LottieAnimation(
            composition = composition,
            progress = { resolvedProgress },
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    // 在深色模式下添加柔和的外发光效果增强可见性
                    if (isDarkTheme) {
                        shadowElevation = 8f
                        ambientShadowColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f)
                        spotShadowColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f)
                    }
                }
        )
    }
}

/**
 *  精美互动设置页面动画头部 (使用本地资源)
 * 用于在设置二级页面顶部显示主题动画
 * 
 * @param rawResId 本地 raw 资源 ID
 * @param title 标题文字（可选）
 * @param subtitle 副标题文字（可选）
 */
@Composable
fun SettingsAnimatedHeaderLocal(
    @RawRes rawResId: Int,
    title: String? = null,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    animationSize: Dp = 120.dp,
    interactionLevel: Float = 0f,
    animationSpeed: Float = 1f,
    progressOverride: Float? = null,
    enableTapBoost: Boolean = true
) {
    //  入场动画
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    //  呼吸缩放动画
    val infiniteTransition = rememberInfiniteTransition(label = "headerPulseLocal")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "headerScaleLocal"
    )
    val clampedInteraction = interactionLevel.coerceIn(0f, 1f)
    val scope = rememberCoroutineScope()
    val boost = remember { Animatable(0f) }
    val boostedInteraction = (clampedInteraction + boost.value).coerceIn(0f, 1f)
    val effectiveSpeed = (animationSpeed * (1f + 0.6f * boostedInteraction)).coerceIn(0.6f, 2f)
    
    //  入场动画值
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(500),
        label = "headerAlphaLocal"
    )
    val animatedOffset by animateFloatAsState(
        targetValue = if (isVisible) 0f else 30f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 300f
        ),
        label = "headerOffsetLocal"
    )
    
    //  根据动画资源ID选择渐变颜色（每个页面不同颜色）
    val gradientColors = remember(rawResId) {
        when (rawResId) {
            R.raw.puzzle_plugin -> listOf(
                androidx.compose.ui.graphics.Color(0xFF43CEA2),  // 青绿
                androidx.compose.ui.graphics.Color(0xFF185A9D)   // 深蓝
            )
            R.raw.palette_color -> listOf(
                androidx.compose.ui.graphics.Color(0xFFFF6B6B),  // 珊瑚红
                androidx.compose.ui.graphics.Color(0xFF4ECDC4)   // 薄荷青
            )
            R.raw.auto_night_off -> listOf(
                androidx.compose.ui.graphics.Color(0xFFFFB347),  // 暖黄
                androidx.compose.ui.graphics.Color(0xFF2C3E50)   // 夜空蓝
            )
            R.raw.boosts -> listOf(
                androidx.compose.ui.graphics.Color(0xFFF7971E),  // 橙黄
                androidx.compose.ui.graphics.Color(0xFFFFD200)   // 明黄
            )
            R.raw.media_mute_unmute -> listOf(
                androidx.compose.ui.graphics.Color(0xFF667EEA),  // 蓝紫
                androidx.compose.ui.graphics.Color(0xFFF093FB)   // 粉紫
            )
            R.raw.large_message_lock -> listOf(
                androidx.compose.ui.graphics.Color(0xFFF093FB),  // 粉紫
                androidx.compose.ui.graphics.Color(0xFFF5576C)   // 玫红
            )
            else -> listOf(
                androidx.compose.ui.graphics.Color(0xFF667EEA),  // 默认蓝紫
                androidx.compose.ui.graphics.Color(0xFF764BA2)
            )
        }
    }
    val highlightFactor = 0.12f * boostedInteraction
    val enhancedColors = gradientColors.map { color ->
        androidx.compose.ui.graphics.lerp(
            color,
            androidx.compose.ui.graphics.Color.White,
            highlightFactor
        )
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .graphicsLayer {
                alpha = animatedAlpha
                translationY = animatedOffset
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //  Lottie 动画 (从本地资源加载) 
        //  移除强制圆形背景，让动画更生动
        Box(
            modifier = Modifier
                .size(animationSize)
                .graphicsLayer {
                    val interactionScale = 1f + 0.05f * boostedInteraction
                    scaleX = scale * interactionScale
                    scaleY = scale * interactionScale
                }
                .then(
                    if (enableTapBoost) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            scope.launch {
                                boost.stop()
                                boost.snapTo(0f)
                                boost.animateTo(1f, tween(180))
                                boost.animateTo(0f, tween(520))
                            }
                        }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            LottieAnimationFromRaw(
                rawResId = rawResId,
                size = animationSize,  // 动画占满容器
                speed = effectiveSpeed,
                progressOverride = progressOverride
            )
        }
        
        // 标题（可选）
        if (title != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        
        // 副标题（可选）
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/**
 *  通用 Lottie 动画组件
 */
@Composable
fun LottieAnimation(
    url: String,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    iterations: Int = LottieConstants.IterateForever,
    autoPlay: Boolean = true
) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.Url(url)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        isPlaying = autoPlay
    )
    
    com.airbnb.lottie.compose.LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(size)
    )
}

/**
 *  加载动画组件
 */
@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    text: String? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CutePersonLoadingIndicator(
            modifier = Modifier.size(size),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.4.dp
        )
        if (text != null) {
            Spacer(modifier = Modifier.height(BiliDesign.Spacing.sm))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

internal fun resolveMascotBounceWave(phase: Float): Float {
    val clamped = phase.coerceIn(0f, 1f)
    return sin((clamped * (2f * PI)).toFloat())
}

internal fun resolveMascotDotAlpha(phase: Float, index: Int): Float {
    val safeIndex = index.coerceIn(0, 2)
    val offsetPhase = phase.coerceIn(0f, 1f) - safeIndex * 0.17f
    val wave = (sin((offsetPhase * (2f * PI)).toFloat()) + 1f) / 2f
    return (0.18f + 0.82f * wave).coerceIn(0.18f, 1f)
}

@Composable
fun CutePersonLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 2.dp
) {
    val transition = rememberInfiniteTransition(label = "cute-loading")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1120, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cute-loading-phase"
    )
    val density = LocalDensity.current
    val wave = resolveMascotBounceWave(phase)
    val iconSize = (strokeWidth.value * 9f).coerceIn(14f, 30f).dp
    val dotSize = (iconSize.value * 0.2f).coerceIn(2f, 5f).dp
    val translationY = with(density) { (-wave * 2.8f).dp.toPx() }

    Box(
        modifier = modifier.sizeIn(
            minWidth = iconSize + 8.dp,
            minHeight = iconSize + 8.dp
        ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 1.dp),
            horizontalArrangement = Arrangement.spacedBy(dotSize * 0.8f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .alpha(resolveMascotDotAlpha(phase = phase, index = index))
                        .background(color.copy(alpha = 0.92f), CircleShape)
                )
            }
        }

        Icon(
            imageVector = CupertinoIcons.Default.Person,
            contentDescription = "加载中",
            tint = color,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    rotationZ = wave * 8f
                    this.translationY = translationY
                }
        )
    }
}

/**
 *  点赞动画按钮
 */
@Composable
fun LikeButton(
    isLiked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    var isPlaying by remember { mutableStateOf(false) }
    
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.Url(LottieUrls.LIKE_HEART)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        restartOnPlay = true,
        iterations = 1
    )
    
    // 动画播放完毕后重置状态
    LaunchedEffect(progress) {
        if (progress == 1f) {
            isPlaying = false
        }
    }
    
    Box(
        modifier = modifier
            .size(size)
            .clickable {
                if (!isLiked) {
                    isPlaying = true
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        if (isLiked || isPlaying) {
            com.airbnb.lottie.compose.LottieAnimation(
                composition = composition,
                progress = { if (isLiked && !isPlaying) 1f else progress },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // 未点赞状态显示静态图标
            com.airbnb.lottie.compose.LottieAnimation(
                composition = composition,
                progress = { 0f },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 *  收藏动画按钮
 */
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    var isPlaying by remember { mutableStateOf(false) }
    
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.Url(LottieUrls.STAR)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        restartOnPlay = true,
        iterations = 1
    )
    
    LaunchedEffect(progress) {
        if (progress == 1f) {
            isPlaying = false
        }
    }
    
    Box(
        modifier = modifier
            .size(size)
            .clickable {
                if (!isFavorite) {
                    isPlaying = true
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        com.airbnb.lottie.compose.LottieAnimation(
            composition = composition,
            progress = { if (isFavorite && !isPlaying) 1f else progress },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 *  空状态组件
 *  支持点击动画彩蛋：连续点击会触发有趣的提示
 */
@Composable
fun EmptyState(
    message: String = "暂无内容",
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    //  [彩蛋] 是否启用点击彩蛋
    enableEasterEgg: Boolean = true
) {
    //  点击计数器触发彩蛋
    var clickCount by remember { mutableIntStateOf(0) }
    var easterEggMessage by remember { mutableStateOf<String?>(null) }
    
    //  点击彩蛋消息列表
    val easterEggMessages = remember {
        listOf(
            "别戳我啦～ 😆",
            "我只是个空状态... 🥺",
            "再点也不会有内容的！",
            "你在找什么？🔍",
            "好无聊啊～ 去看点视频吧！",
            "点点点，你可真会点！",
            "咚咚咚！有人在家吗？🚪"
        )
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(BiliDesign.Spacing.xxl)
            .then(
                if (enableEasterEgg) {
                    Modifier.clickable {
                        clickCount++
                        if (clickCount >= 3) {
                            easterEggMessage = easterEggMessages.random()
                        }
                        if (clickCount >= 7) {
                            clickCount = 0  // 重置
                        }
                    }
                } else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LottieAnimation(
            url = LottieUrls.EMPTY,
            size = 150.dp
        )
        Spacer(modifier = Modifier.height(BiliDesign.Spacing.lg))
        
        //  显示彩蛋消息或默认消息（使用柔和的主题色）
        Text(
            text = easterEggMessage ?: message,
            style = MaterialTheme.typography.bodyLarge,
            color = if (easterEggMessage != null) 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            else 
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(BiliDesign.Spacing.md))
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelLarge,
                color = BiliDesign.Colors.BiliPink,
                modifier = Modifier.clickable { onAction() }
            )
        }
    }
}

/**
 *  错误状态组件
 *  支持点击彩蛋：连续点击会显示鼓励消息
 */
@Composable
fun ErrorState(
    message: String = "加载失败",
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    //  [彩蛋] 是否启用点击彩蛋
    enableEasterEgg: Boolean = true
) {
    //  点击计数器触发彩蛋
    var clickCount by remember { mutableIntStateOf(0) }
    var showEncouragement by remember { mutableStateOf(false) }
    
    //  鼓励消息列表
    val encouragements = remember {
        listOf(
            "别灰心！再试一次～ 💪",
            "网络可能在打盹... 😴",
            "加载失败也要保持微笑！😊",
            "休息一下再试试？☕",
            "服务器正在努力中... 🏃",
            "别担心，问题不大！👌"
        )
    }
    
    val displayMessage = if (showEncouragement) {
        encouragements.random()
    } else message
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(BiliDesign.Spacing.xxl)
            .then(
                if (enableEasterEgg) {
                    Modifier.clickable {
                        clickCount++
                        if (clickCount >= 3) {
                            showEncouragement = true
                        }
                        if (clickCount >= 5) {
                            clickCount = 0
                            showEncouragement = false
                        }
                    }
                } else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LottieAnimation(
            url = LottieUrls.ERROR,
            size = 120.dp,
            iterations = 1
        )
        Spacer(modifier = Modifier.height(BiliDesign.Spacing.lg))
        Text(
            text = displayMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = if (showEncouragement)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(BiliDesign.Spacing.md))
            Text(
                text = if (showEncouragement) "冲鸭！" else "点击重试",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onRetry() }
            )
        }
    }
}

/**
 *  成功动画
 */
@Composable
fun SuccessAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    onFinished: () -> Unit = {}
) {
    var finished by remember { mutableStateOf(false) }
    
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.Url(LottieUrls.SUCCESS)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1
    )
    
    LaunchedEffect(progress) {
        if (progress == 1f && !finished) {
            finished = true
            onFinished()
        }
    }
    
    com.airbnb.lottie.compose.LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(size)
    )
}

/**
 *  Telegram 风格设置页面动画头部
 * 用于在设置二级页面顶部显示主题动画
 * 
 * @param lottieUrl Lottie 动画 URL
 * @param title 标题文字（可选）
 * @param subtitle 副标题文字（可选）
 */
@Composable
fun SettingsAnimatedHeader(
    lottieUrl: String,
    title: String? = null,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    animationSize: Dp = 120.dp
) {
    //  入场动画
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    //  呼吸缩放动画
    val infiniteTransition = rememberInfiniteTransition(label = "headerPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "headerScale"
    )
    
    //  入场动画值
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(500),
        label = "headerAlpha"
    )
    val animatedOffset by animateFloatAsState(
        targetValue = if (isVisible) 0f else 30f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 300f
        ),
        label = "headerOffset"
    )
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .graphicsLayer {
                alpha = animatedAlpha
                translationY = animatedOffset
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //  Lottie 动画
        Box(
            modifier = Modifier
                .size(animationSize)
                .graphicsLayer { 
                    scaleX = scale
                    scaleY = scale
                },
            contentAlignment = Alignment.Center
        ) {
            LottieAnimation(
                url = lottieUrl,
                size = animationSize
            )
        }
        
        // 标题（可选）
        if (title != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        
        // 副标题（可选）
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
