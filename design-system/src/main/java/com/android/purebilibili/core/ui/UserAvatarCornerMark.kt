package com.android.purebilibili.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Corner mark drawn on a user avatar, matching PiliPlus [PendantAvatar].
 * Live replaces the mark. Otherwise an official role wins over the VIP「大」.
 */
enum class UserAvatarCornerMark {
    None,
    Vip,
    Personal,
    Organization,
}

fun resolveUserAvatarCornerMark(
    officialType: Int?,
    vipStatus: Int?,
    isLive: Boolean = false,
): UserAvatarCornerMark {
    if (isLive) return UserAvatarCornerMark.None
    val official = officialType ?: -1
    if (official < 0) {
        return if ((vipStatus ?: 0) > 0) UserAvatarCornerMark.Vip else UserAvatarCornerMark.None
    }
    return when (official) {
        0 -> UserAvatarCornerMark.Personal
        1 -> UserAvatarCornerMark.Organization
        else -> UserAvatarCornerMark.None
    }
}

private val VipAvatarPink = Color(0xFFFB7299)

@Composable
fun UserAvatarCornerMarkBadge(
    mark: UserAvatarCornerMark,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 14.dp,
) {
    if (mark == UserAvatarCornerMark.None) return
    val description = when (mark) {
        UserAvatarCornerMark.Vip -> "大会员"
        UserAvatarCornerMark.Personal -> "个人认证"
        UserAvatarCornerMark.Organization -> "机构认证"
        UserAvatarCornerMark.None -> return
    }
    Box(
        modifier = modifier
            .size(badgeSize)
            .semantics { contentDescription = description }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        when (mark) {
            UserAvatarCornerMark.Vip -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(1.dp)
                        .clip(CircleShape)
                        .background(VipAvatarPink),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "大",
                        color = Color.White,
                        fontSize = (badgeSize.value * 0.58f).sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = (badgeSize.value * 0.58f).sp,
                    )
                }
            }
            UserAvatarCornerMark.Personal,
            UserAvatarCornerMark.Organization -> {
                Icon(
                    imageVector = Icons.Outlined.Bolt,
                    contentDescription = null,
                    tint = if (mark == UserAvatarCornerMark.Personal) {
                        Color(0xFFFFCC00)
                    } else {
                        Color(0xFF40C4FF)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            UserAvatarCornerMark.None -> Unit
        }
    }
}
