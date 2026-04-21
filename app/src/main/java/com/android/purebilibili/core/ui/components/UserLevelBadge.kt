package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.R

internal enum class UserLevelBadgeAsset {
    LEVEL_0,
    LEVEL_1,
    LEVEL_2,
    LEVEL_3,
    LEVEL_4,
    LEVEL_5,
    LEVEL_6,
    LEVEL_6_SENIOR
}

internal fun resolveUserLevelBadgeAsset(
    level: Int,
    isSeniorMember: Boolean
): UserLevelBadgeAsset? {
    return when {
        isSeniorMember && level == 6 -> UserLevelBadgeAsset.LEVEL_6_SENIOR
        level == 0 -> UserLevelBadgeAsset.LEVEL_0
        level == 1 -> UserLevelBadgeAsset.LEVEL_1
        level == 2 -> UserLevelBadgeAsset.LEVEL_2
        level == 3 -> UserLevelBadgeAsset.LEVEL_3
        level == 4 -> UserLevelBadgeAsset.LEVEL_4
        level == 5 -> UserLevelBadgeAsset.LEVEL_5
        level == 6 -> UserLevelBadgeAsset.LEVEL_6
        else -> null
    }
}

private fun resolveUserLevelBadgeResId(asset: UserLevelBadgeAsset): Int {
    return when (asset) {
        UserLevelBadgeAsset.LEVEL_0 -> R.drawable.lv0
        UserLevelBadgeAsset.LEVEL_1 -> R.drawable.lv1
        UserLevelBadgeAsset.LEVEL_2 -> R.drawable.lv2
        UserLevelBadgeAsset.LEVEL_3 -> R.drawable.lv3
        UserLevelBadgeAsset.LEVEL_4 -> R.drawable.lv4
        UserLevelBadgeAsset.LEVEL_5 -> R.drawable.lv5
        UserLevelBadgeAsset.LEVEL_6 -> R.drawable.lv6
        UserLevelBadgeAsset.LEVEL_6_SENIOR -> R.drawable.lv6_s
    }
}

@Composable
fun UserLevelBadge(
    level: Int,
    isSeniorMember: Boolean = false,
    modifier: Modifier = Modifier,
    height: Dp = 11.dp
) {
    val badgeAsset = resolveUserLevelBadgeAsset(
        level = level,
        isSeniorMember = isSeniorMember
    )
    if (badgeAsset != null) {
        Image(
            bitmap = ImageBitmap.imageResource(id = resolveUserLevelBadgeResId(badgeAsset)),
            contentDescription = "等级$level",
            modifier = modifier.height(height),
            filterQuality = FilterQuality.None
        )
        return
    }

    LegacyUserLevelBadge(
        level = level,
        modifier = modifier
    )
}

@Composable
private fun LegacyUserLevelBadge(
    level: Int,
    modifier: Modifier = Modifier
) {
    val badgeColor = resolveUserLevelFallbackColor(level)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        badgeColor.copy(alpha = 0.92f),
                        badgeColor
                    )
                )
            )
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(
            text = "LV$level",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            lineHeight = 10.sp
        )
    }
}

private fun resolveUserLevelFallbackColor(level: Int): Color {
    return when {
        level >= 6 -> Color(0xFFF04444)
        level >= 5 -> Color(0xFFFF7A45)
        level >= 4 -> Color(0xFFFF8B5A)
        level >= 3 -> Color(0xFFFF9C6E)
        level >= 2 -> Color(0xFFFFAE84)
        else -> Color(0xFFA7ADB8)
    }
}
