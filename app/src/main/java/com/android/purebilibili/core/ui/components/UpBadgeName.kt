package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.android.purebilibili.core.ui.LocalUpBadgeVisibility
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
    trailingSlotMinHeight: Dp = 0.dp,
    showUpBadge: Boolean? = null,
    /** 内联尾随内容:紧跟名称文本之后渲染(用于「已关注」等紧凑状态标签)。 */
    inlineTrailingContent: (@Composable () -> Unit)? = null,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    metaMaxLines: Int = 1,
    metaOverflow: TextOverflow = TextOverflow.Ellipsis,
    // 名称右侧留白：名称占满可用宽度后，省略号与后续元素之间保留的空隙。
    // 默认 0 不影响未显式传入的调用方。
    nameEndPadding: Dp = 0.dp
) {
    val shouldShowMeta = !metaText.isNullOrBlank()
    // null = 跟随全局「UP 认证徽章」开关(设置 > 外观 > 首页与列表),
    // 相关推荐、搜索、分区等未显式传参的调用点统一生效。
    val effectiveShowUpBadge = showUpBadge ?: LocalUpBadgeVisibility.current.showBadges
    val shouldRenderTrailingSlot = shouldRenderUpBadgeTrailingSlot(
        hasTrailingContent = badgeTrailingContent != null,
        reserveTrailingSlot = reserveTrailingSlot
    )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier,
            verticalAlignment = if (shouldShowMeta) Alignment.Top else Alignment.CenterVertically
        ) {
            if (shouldRenderUserUpBadge(effectiveShowUpBadge)) {
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
                // 名称列需要占用剩余空间；fill=false 会在带有头像、UP 标识和尾部槽位时
                // 将可用宽度压缩到零，最终只留下省略号。
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (inlineTrailingContent != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = name.ifBlank { "未知UP主" },
                            style = nameStyle,
                            color = nameColor,
                            maxLines = maxLines,
                            overflow = overflow,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .padding(end = nameEndPadding)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        inlineTrailingContent()
                    }
                } else {
                    Text(
                        text = name.ifBlank { "未知UP主" },
                        style = nameStyle,
                        color = nameColor,
                        maxLines = maxLines,
                        overflow = overflow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = nameEndPadding)
                    )
                }
                if (shouldShowMeta) {
                    Text(
                        text = metaText.orEmpty(),
                        style = metaStyle,
                        color = metaColor,
                        maxLines = metaMaxLines,
                        overflow = metaOverflow
                    )
                }
            }

            if (shouldRenderTrailingSlot) {
                Spacer(modifier = Modifier.width(spacing))
                Box(
                    modifier = Modifier
                        .widthIn(min = trailingSlotMinWidth)
                        .heightIn(min = trailingSlotMinHeight),
                    contentAlignment = Alignment.CenterStart
                ) {
                    badgeTrailingContent?.invoke()
                }
            }
        }
    }
}
