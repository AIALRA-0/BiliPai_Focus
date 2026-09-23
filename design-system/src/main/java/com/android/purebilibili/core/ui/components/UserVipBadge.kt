package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.theme.ACCESSIBLE_TEXT_MIN_CONTRAST
import com.android.purebilibili.core.theme.resolveAccessibleContainerColors
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
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

/**
 * Theme-colored VIP chip (personal-space style).
 *
 * Soft filled pill from the theme secondary container pair
 * ([AppSurfaceTokens.secondaryContainer] / [AppSurfaceTokens.onSecondaryContainer]),
 * not hard-coded brand pink. Matches the space-header “大会员 / 年度大会员” look.
 *
 * Dark mode: translucent secondaryContainer is flattened over surface, then text is
 * resolved to WCAG 4.5:1 so the label never washes out on dark chips.
 */
@Composable
fun UserVipBadge(
    label: String = "大会员",
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 11.sp,
    compact: Boolean = false,
) {
    val text = resolveUserVipBadgeLabel(label)
    val colorScheme = MaterialTheme.colorScheme
    // Prefer Miuix-bridged tokens so MD3 and Miuix themes stay aligned.
    val themeContainer = AppSurfaceTokens.secondaryContainer()
    val themeContent = AppSurfaceTokens.onSecondaryContainer()
    val accessible = remember(
        themeContainer,
        themeContent,
        colorScheme.surface,
        colorScheme.onSurface,
        colorScheme.onBackground,
        colorScheme.inverseOnSurface,
    ) {
        resolveVipBadgeColors(
            containerColor = themeContainer,
            preferredContentColor = themeContent,
            surfaceColor = colorScheme.surface,
            onSurface = colorScheme.onSurface,
            onBackground = colorScheme.onBackground,
            inverseOnSurface = colorScheme.inverseOnSurface,
        )
    }

    AppSurface(
        modifier = modifier,
        shape = AppShapes.container(ContainerLevel.Pill),
        color = accessible.containerColor,
        contentColor = accessible.contentColor,
    ) {
        AppText(
            text = text,
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 10.dp,
                vertical = if (compact) 3.dp else 5.dp,
            ),
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = accessible.contentColor,
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
