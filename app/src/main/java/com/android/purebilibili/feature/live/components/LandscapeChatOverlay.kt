package com.android.purebilibili.feature.live.components
import com.android.purebilibili.core.ui.components.AppText

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.feature.live.LiveDanmakuItem
import com.android.purebilibili.feature.live.LiveStatusPalette
import com.android.purebilibili.feature.live.resolveLandscapeLiveChatVisualSpec
import com.android.purebilibili.feature.live.resolveLiveMedalColor
import com.android.purebilibili.feature.live.shouldRenderLiveDanmakuImageEmoticon
import kotlinx.coroutines.flow.SharedFlow
import coil3.compose.AsyncImage

/**
 * 横屏模式专用 - 透明弹幕覆盖层
 * 左下角浮动显示，透明渐变背景
 */
@Composable
fun LandscapeChatOverlay(
    danmakuFlow: SharedFlow<LiveDanmakuItem>,
    modifier: Modifier = Modifier
) {
    val visualSpec = remember { resolveLandscapeLiveChatVisualSpec() }
    val messages = remember { mutableStateListOf<LiveDanmakuItem>() }
    val listState = rememberLazyListState()
    
    LaunchedEffect(danmakuFlow) {
        danmakuFlow.collect { item ->
            // 确保列表操作在主线程执行 (Compose 状态修改必须在主线程)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
                try {
                    messages.add(item)
                    if (messages.size > 50) messages.removeAt(0) // 横屏模式只保留最近50条
                    if (!listState.isScrollInProgress && messages.isNotEmpty()) {
                        listState.animateScrollToItem((messages.size - 1).coerceAtLeast(0))
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LandscapeChatOverlay", "❌ Message add error: ${e.message}")
                }
            }
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = visualSpec.horizontalPaddingDp.dp,
                vertical = visualSpec.verticalPaddingDp.dp,
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppText(
                text = "实时弹幕",
                color = LiveStatusPalette.MediaContent,
                fontSize = visualSpec.headerFontSizeSp.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            AppText(
                text = "横屏互动",
                color = LiveStatusPalette.MediaContent.copy(alpha = 0.66f),
                fontSize = visualSpec.subtitleFontSizeSp.sp
            )
        }
        Spacer(Modifier.height(AppSpacingTokens.Small))
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = AppSpacingTokens.Small),
            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall),
            reverseLayout = false // 正常方向，新消息在底部
        ) {
            items(messages) { item ->
                LandscapeChatItem(item, visualSpec)
            }
        }
    }
}



@Composable
private fun LandscapeChatItem(
    item: LiveDanmakuItem,
    visualSpec: com.android.purebilibili.feature.live.LandscapeLiveChatVisualSpec,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = AppSpacingTokens.Micro)
    ) {
        // [新增] 粉丝牌 (横屏版，稍微小一点)
        if (item.medalLevel > 0) {
            val color = resolveLiveMedalColor(item.medalColor)
            AppSurface(
                color = color.copy(alpha = 0.8f), // 稍微透明一点
                shape = AppShapes.container(ContainerLevel.Tag),
                modifier = Modifier.padding(end = AppSpacingTokens.ExtraSmall)
            ) {
                 Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(
                        horizontal = visualSpec.medalHorizontalPaddingDp.dp,
                        vertical = visualSpec.medalVerticalPaddingDp.dp,
                    )
                ) {
                    AppText(
                        text = item.medalName,
                        fontSize = visualSpec.medalFontSizeSp.sp,
                        color = LiveStatusPalette.MediaContent,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(AppSpacingTokens.Micro))
                    AppText(
                        text = "${item.medalLevel}",
                        fontSize = visualSpec.medalFontSizeSp.sp,
                        color = LiveStatusPalette.MediaContent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // 用户名 + 消息
        val textStyle = TextStyle(
            fontSize = visualSpec.messageFontSizeSp.sp,
            color = LiveStatusPalette.MediaContent,
            fontWeight = FontWeight.Medium
        )

        if (shouldRenderLiveDanmakuImageEmoticon(item.emoticonUrl)) {
            AppText(
                text = "${item.uname}: ",
                style = textStyle
            )
            AsyncImage(
                model = item.emoticonUrl,
                contentDescription = item.text,
                modifier = Modifier.size(AppSpacingTokens.DoubleExtraLarge)
            )
        } else {
             // 区分用户名颜色
             val nameColor = if (item.isAdmin) {
                 LiveStatusPalette.MedalFallback
             } else {
                 LiveStatusPalette.OverlayNeutral
             }
             
             AppText(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = nameColor, fontWeight = FontWeight.Bold)) {
                        append(item.uname)
                    }
                    append(": ")
                    append(item.text)
                },
                style = textStyle,
                maxLines = 3 // 横屏允许稍微多一点行数
            )
        }
    }
}
