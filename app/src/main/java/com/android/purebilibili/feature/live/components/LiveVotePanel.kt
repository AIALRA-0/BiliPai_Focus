package com.android.purebilibili.feature.live.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppLinearProgressIndicator
import com.android.purebilibili.data.repository.LiveVoteInfo
import com.android.purebilibili.data.repository.LiveVoteSnapshot
import kotlin.math.roundToInt

@Composable
fun LiveVotePanel(snapshot: LiveVoteSnapshot, modifier: Modifier = Modifier) {
    val votes = buildList {
        snapshot.current?.let(::add)
        addAll(snapshot.history.filterNot { history ->
            snapshot.current?.interactionId?.takeIf { it > 0L } == history.interactionId
        })
    }
    if (votes.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize().padding(AppSpacingTokens.ExtraLarge),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppText("当前暂无投票", color = MaterialTheme.colorScheme.onSurfaceVariant)
            AppText(
                "投票开始后会自动显示，仅提供查看。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = AppSpacingTokens.Medium),
        verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Medium)
    ) {
        itemsIndexed(
            items = votes,
            key = { index, vote -> vote.interactionId.takeIf { it > 0L } ?: "vote-$index-${vote.question}" }
        ) { _, vote ->
            LiveVoteCard(vote)
        }
    }
}

@Composable
private fun LiveVoteCard(vote: LiveVoteInfo) {
    AppSurface(
        modifier = Modifier.fillMaxWidth(),
        color = AppSurfaceTokens.cardContainer(),
        shape = AppShapes.container(ContainerLevel.Card)
    ) {
        Column(
            modifier = Modifier.padding(AppSpacingTokens.Medium),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AppText(vote.question, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                AppText(
                    if (vote.isActive) "进行中" else vote.resultText.ifBlank { "已结束" },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (vote.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            vote.options.forEach { option ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    AppText(option.description, style = MaterialTheme.typography.bodyMedium)
                    AppText("${(option.percent * 100f).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                }
                AppLinearProgressIndicator(
                    progress = { option.percent.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            val timing = when {
                vote.isActive && vote.remainingMillis > 0L -> "剩余 ${vote.remainingMillis / 1000L} 秒"
                vote.endTimeText.isNotBlank() -> "结束于 ${vote.endTimeText}"
                else -> ""
            }
            if (timing.isNotBlank()) {
                AppText(timing, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
