package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection

@Composable
fun UpBadgeName(
    name: String,
    modifier: Modifier = Modifier,
    badgeTrailingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    metaText: String? = null,
    nameStyle: TextStyle = MaterialTheme.typography.labelMedium,
    nameColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    metaStyle: TextStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
    metaColor: Color = MaterialTheme.colorScheme.primary,
    badgeTextColor: Color = nameColor.copy(alpha = 0.85f),
    badgeBorderColor: Color = nameColor.copy(alpha = 0.35f),
    badgeBackgroundColor: Color = Color.Transparent,
    badgeCornerRadius: Dp = 8.dp,
    badgeHorizontalPadding: Dp = 6.dp,
    badgeVerticalPadding: Dp = 1.dp,
    spacing: Dp = 6.dp,
    reserveTrailingSlot: Boolean = false,
    trailingSlotMinWidth: Dp = 40.dp,
    showUpBadge: Boolean = true,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    val shouldShowMeta = !metaText.isNullOrBlank()
    val shouldRenderTrailingSlot = shouldRenderUpBadgeTrailingSlot(
        hasTrailingContent = badgeTrailingContent != null,
        reserveTrailingSlot = reserveTrailingSlot
    )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier,
            verticalAlignment = if (shouldShowMeta) Alignment.Top else Alignment.CenterVertically
        ) {
            if (shouldRenderUserUpBadge(showUpBadge)) {
                UserUpBadge(
                    containerColor = badgeBackgroundColor,
                    contentColor = badgeTextColor
                )
                Spacer(modifier = Modifier.width(spacing))
            }

            leadingContent?.let {
                it()
                Spacer(modifier = Modifier.width(spacing))
            }

            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = name.ifBlank { "未知UP主" },
                    style = nameStyle,
                    color = nameColor,
                    maxLines = maxLines,
                    overflow = overflow
                )
                if (shouldShowMeta) {
                    Text(
                        text = metaText.orEmpty(),
                        style = metaStyle,
                        color = metaColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (shouldRenderTrailingSlot) {
                Spacer(modifier = Modifier.width(spacing))
                Box(
                    modifier = Modifier.widthIn(min = trailingSlotMinWidth),
                    contentAlignment = Alignment.CenterStart
                ) {
                    badgeTrailingContent?.invoke()
                }
            }
        }
    }
}
