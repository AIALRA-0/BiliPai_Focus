package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.theme.ACCESSIBLE_TEXT_MIN_CONTRAST
import com.android.purebilibili.core.theme.resolveAccessibleContainerColors
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel

/**
 * Resolve display text for VIP badges.
 * Prefer server label; fall back by vip type when label is empty.
 */
fun resolveUserVipBadgeLabel(
    label: String?,
    vipType: Int = 0,
): String {
    val trimmed = label?.trim().orEmpty()
    if (trimmed.isNotEmpty()) return trimmed
    return when (vipType) {
        2 -> "年度大会员"
        1 -> "大会员"
        else -> "大会员"
    }
}

/** PiliPlus VIP pink, independent of the app accent color. */
fun resolveUserVipPink(isDark: Boolean): Color =
    if (isDark) Color(0xFFD44E7D) else Color(0xFFFF6699)

@Composable
fun userVipPink(): Color =
    resolveUserVipPink(MaterialTheme.colorScheme.surface.luminance() < 0.5f)

/** Pink filled VIP label with white text, matching PiliPlus space headers. */
@Composable
fun UserVipBadge(
    label: String = "大会员",
    modifier: Modifier = Modifier,
    fontSize: TextUnit = MaterialTheme.typography.labelSmall.fontSize,
    compact: Boolean = false,
) {
    val text = resolveUserVipBadgeLabel(label)
    val vipPink = userVipPink()

    AppSurface(
        modifier = modifier,
        shape = AppShapes.container(ContainerLevel.Pill),
        color = vipPink,
        contentColor = Color.White,
    ) {
        AppText(
            text = text,
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 10.dp,
                vertical = if (compact) 3.dp else 5.dp,
            ),
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Flatten translucent theme containers and pick a readable label color.
 * Pure Kotlin so dark-mode contrast can be unit-tested without Compose.
 */
fun resolveVipBadgeColors(
    containerColor: Color,
    preferredContentColor: Color,
    surfaceColor: Color,
    onSurface: Color,
    onBackground: Color,
    inverseOnSurface: Color,
): AccessibleVipBadgeColors {
    val resolved = resolveAccessibleContainerColors(
        containerColor = containerColor,
        contentColor = preferredContentColor,
        backgroundColor = surfaceColor,
        fallbackContentColors = listOf(
            onSurface,
            onBackground,
            inverseOnSurface,
            Color.White,
            Color.Black,
        ),
        minimumContrast = ACCESSIBLE_TEXT_MIN_CONTRAST,
    )
    return AccessibleVipBadgeColors(
        containerColor = resolved.containerColor,
        contentColor = resolved.contentColor,
    )
}

data class AccessibleVipBadgeColors(
    val containerColor: Color,
    val contentColor: Color,
)
