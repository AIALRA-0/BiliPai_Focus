package com.android.purebilibili.feature.live.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.feature.live.LiveDanmakuItem
import com.android.purebilibili.feature.live.formatLiveSuperChatCountdown
import com.android.purebilibili.feature.live.rememberLiveChromePalette
import com.android.purebilibili.feature.live.resolveLiveSuperChatColor
import com.android.purebilibili.feature.live.resolveLiveSuperChatDurationSec
import com.android.purebilibili.feature.live.shouldExpireLiveSuperChat
import kotlinx.coroutines.delay

@Composable
fun LiveSuperChatSection(
    items: List<LiveDanmakuItem>,
    modifier: Modifier = Modifier,
    onExpired: (LiveDanmakuItem) -> Unit = {},
) {
    val palette = rememberLiveChromePalette()
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(AppSpacingTokens.Large),
        verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Medium)
    ) {
        items(
            items = items,
            key = { "${it.superChatId}_${it.uid}_${it.text}_${it.superChatPrice}" }
        ) { item ->
            LiveSuperChatCard(
                item = item,
                onExpired = { onExpired(item) },
            )
        }
        if (items.isEmpty()) {
            item(key = "empty") {
                AppText(
                    text = "暂无醒目留言",
                    color = palette.secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = AppSpacingTokens.Large),
                )
            }
        }
    }
}

@Composable
private fun LiveSuperChatCard(
    item: LiveDanmakuItem,
    onExpired: () -> Unit,
) {
    val palette = rememberLiveChromePalette()
    val totalSec = resolveLiveSuperChatDurationSec(item.superChatDuration)
    var remainingSec by remember(item.superChatId, item.superChatDuration, item.text) {
        mutableIntStateOf(totalSec)
    }

    LaunchedEffect(item.superChatId, totalSec) {
        remainingSec = totalSec
        var elapsed = 0
        while (!shouldExpireLiveSuperChat(totalSec, elapsed)) {
            delay(1_000)
            elapsed += 1
            remainingSec = (totalSec - elapsed).coerceAtLeast(0)
        }
        onExpired()
    }

    AppSurface(
        shape = AppShapes.borderedContainer(ContainerLevel.Card),
        color = palette.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(
            AppSpacingTokens.Micro / 2,
            palette.border
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    resolveLiveSuperChatColor(item.superChatBackgroundColor).copy(alpha = 0.12f)
                )
                .padding(AppSpacingTokens.Large),
            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AppText(
                    text = item.uname.ifBlank { "醒目留言" },
                    color = palette.primaryText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small)) {
                    AppText(
                        text = formatLiveSuperChatCountdown(remainingSec),
                        color = palette.secondaryText,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    AppText(
                        text = item.superChatPrice.ifBlank { "SC" },
                        color = palette.accentStrong,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            AppText(
                text = item.text,
                color = palette.primaryText,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
