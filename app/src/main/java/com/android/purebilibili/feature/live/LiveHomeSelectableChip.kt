package com.android.purebilibili.feature.live

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText

/**
 * BiliPai 风格的直播首页分区/标签 chip，使用中性 App* 组件与主题 Token 实现。
 */
@Composable
fun LiveHomeSelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val container = if (selected) {
        AppSurfaceTokens.cardContainer()
    } else {
        Color.Transparent
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        AppSurfaceTokens.onSurfaceVariantSummary()
    }
    AppSurface(
        onClick = onClick,
        modifier = modifier,
        color = container,
        contentColor = content,
        shape = AppShapes.container(ContainerLevel.Pill),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        AppText(
            text = label,
            color = content,
            style = if (compact) {
                MaterialTheme.typography.labelMedium
            } else {
                MaterialTheme.typography.labelLarge
            },
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.padding(
                horizontal = AppSpacingTokens.Small,
                vertical = if (compact) 5.dp else AppSpacingTokens.ExtraSmall,
            ),
        )
    }
}
