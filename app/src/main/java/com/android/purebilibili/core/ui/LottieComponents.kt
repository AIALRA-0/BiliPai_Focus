// 文件路径: core/ui/LottieComponents.kt
package com.android.purebilibili.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.android.purebilibili.core.plugin.skin.LocalUiSkinState
import com.android.purebilibili.core.plugin.skin.UiSkinAnimatedAsset
import com.android.purebilibili.core.plugin.skin.UiSkinSurface
import com.android.purebilibili.core.plugin.skin.assetPath

/**
 *  Lottie 动画加载器
 * 使用在线 Lottie 动画 URL
 */
object LottieUrls {
    //  通用状态动画
    const val SUCCESS = "https://assets4.lottiefiles.com/packages/lf20_jbrw3hcz.json"
    const val ERROR = "https://assets1.lottiefiles.com/packages/lf20_cr9slsdh.json"
    const val EMPTY = "https://raw.githubusercontent.com/DrKLO/Telegram/master/TMessagesProj/src/main/res/raw/utyan_empty2.json"

    //  新手引导页面动画
    const val THEME_COLORS = "https://assets5.lottiefiles.com/packages/lf20_jtbfg2nb.json"  // 彩虹渐变
    const val VIDEO_PLAY = "https://assets8.lottiefiles.com/packages/lf20_khzniaya.json"  // 播放按钮
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
 *  加载动画组件（按 UI 预设分发：iOS 吉祥物 / MD3 LoadingIndicator / Miuix 进度环）
 */
@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    text: String? = null
) {
    val uiSkinState = LocalUiSkinState.current
    val skinLoadingPath = uiSkinState.assetPath(UiSkinSurface.LOADING_INDICATOR) {
        it.loadingAnimation ?: it.loadingFrame
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (skinLoadingPath != null) {
            UiSkinAnimatedAsset(
                path = skinLoadingPath,
                size = size,
                contentDescription = "加载中",
            )
        } else {
            AdaptiveLoadingIndicator(
                size = size,
                strokeWidth = 2.4.dp,
            )
        }
        if (text != null) {
            Spacer(modifier = Modifier.height(AppSpacingTokens.Small))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Theme-aware loading indicator entry used across feature screens.
 *
 * Historically iOS-only cute person; now routes through [AdaptiveLoadingIndicator]
 * so MD3 uses the official morphing [androidx.compose.material3.LoadingIndicator]
 * (dynamic primary) and Miuix uses native progress chrome. iOS keeps the mascot.
 *
 * @param size optional visual size. Prefer this over [Modifier.size] so compact
 *   slots (≤ 32.dp) can select the compact circular recipe on MD3/Miuix.
 */
@Composable
fun CutePersonLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    strokeWidth: Dp = 2.dp,
    size: Dp? = null,
) {
    AdaptiveLoadingIndicator(
        modifier = modifier,
        size = size,
        color = color,
        strokeWidth = strokeWidth,
    )
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
            .padding(AppSpacingTokens.DoubleExtraLarge)
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
        Spacer(modifier = Modifier.height(AppSpacingTokens.Large))
        
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
            Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
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
            .padding(AppSpacingTokens.DoubleExtraLarge)
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
        Spacer(modifier = Modifier.height(AppSpacingTokens.Large))
        Text(
            text = displayMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = if (showEncouragement)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
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
