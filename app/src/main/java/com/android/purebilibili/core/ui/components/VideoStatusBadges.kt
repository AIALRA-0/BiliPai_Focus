package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.ui.AppSpacingTokens

/** All media statuses use the global semantic badge; the cover owns placement. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VideoStatusBadges(labels: List<String>, modifier: Modifier = Modifier) {
    if (labels.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall),
        verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall),
    ) {
        labels.forEach { label ->
            AppStatusBadge(
                label = label,
                emphasized = label == "充电专属" || label == "付费" || label == "合作" || label == "联合投稿",
            )
        }
    }
}
