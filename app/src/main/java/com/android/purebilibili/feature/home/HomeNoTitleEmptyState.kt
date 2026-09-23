package com.android.purebilibili.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.components.AppTextButton

@Composable
internal fun HomeNoTitleEmptyState(
    modifier: Modifier = Modifier,
    topPadding: Dp = AppSpacingTokens.None,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(
                top = topPadding,
                start = AppSpacingTokens.ExtraLarge,
                end = AppSpacingTokens.ExtraLarge,
                bottom = AppSpacingTokens.ExtraLarge,
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Medium)
        ) {
            Text(
                text = "首页栏目已全部关闭",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "可前往 设置 -> 常规 -> Focus\n重新开启任一首页 title",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            AppTextButton(onClick = onSettingsClick) {
                Text(text = "打开 Focus 设置")
            }
        }
    }
}
