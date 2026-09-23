package com.android.purebilibili.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle

object AppTypographyTokens {
    val ZeroLetterSpacing = 0.sp
}

/** Typography roles shared by feed cards regardless of their visual composition. */
data class FeedContentTypography(
    val title: TextStyle,
    val author: TextStyle,
    val statistic: TextStyle,
    val coverBadge: TextStyle,
)

enum class FeedTitleHierarchy {
    Compact,
    Standard,
    Prominent,
}

@Composable
fun feedContentTypography(
    titleHierarchy: FeedTitleHierarchy = FeedTitleHierarchy.Compact,
): FeedContentTypography {
    val bodyMedium = MaterialTheme.typography.bodyMedium
    val titleStyle = when (titleHierarchy) {
        // 紧凑卡片：正文默认字号、行高 1.38、最多两行。
        FeedTitleHierarchy.Compact ->
            bodyMedium.copy(lineHeight = bodyMedium.fontSize * 1.38f)
        // 横向卡片：bodyMedium、行高 1.42、字距 0.3。
        FeedTitleHierarchy.Standard ->
            bodyMedium.copy(
                lineHeight = bodyMedium.fontSize * 1.42f,
                letterSpacing = 0.3.sp,
            )
        FeedTitleHierarchy.Prominent ->
            bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                lineHeight = bodyMedium.fontSize * 1.38f,
            )
    }
    val isMiuix = LocalAppUiStyle.current == AppUiStyle.MIUIX
    val author = MaterialTheme.typography.labelMedium
    return FeedContentTypography(
        title = titleStyle,
        author = author.copy(lineHeight = author.fontSize * 1.5f),
        statistic = if (isMiuix) MaterialTheme.typography.labelMedium
            else MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
        coverBadge = (if (isMiuix) MaterialTheme.typography.labelMedium
            else MaterialTheme.typography.labelSmall).copy(fontWeight = FontWeight.Medium),
    )
}
