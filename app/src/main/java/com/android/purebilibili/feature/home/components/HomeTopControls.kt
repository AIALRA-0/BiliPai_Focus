package com.android.purebilibili.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.feature.home.UserState

@Composable
internal fun HomeTopAvatarContent(
    user: UserState,
    shape: Shape,
    fallbackBackgroundColor: Color,
    fallbackTextColor: Color,
    modifier: Modifier = Modifier
) {
    if (user.isLogin && user.face.isNotEmpty()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(FormatUtils.fixImageUrl(user.face))
                .crossfade(true)
                .build(),
            contentDescription = "用户头像",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .fillMaxSize()
                .clip(shape)
        )
    } else {
        androidx.compose.foundation.layout.Box(
            modifier = modifier
                .fillMaxSize()
                .clip(shape)
                .background(fallbackBackgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "未",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = fallbackTextColor
            )
        }
    }
}

@Composable
internal fun HomeTopSearchPillContent(
    searchIcon: ImageVector,
    contentColor: Color,
    textFontSize: TextUnit,
    iconTextGap: Dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = searchIcon,
            contentDescription = "搜索",
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(iconTextGap))
        Text(
            text = "搜索视频、UP主...",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = textFontSize,
            fontWeight = FontWeight.Normal,
            color = contentColor,
            maxLines = 1
        )
    }
}

@Composable
internal fun HomeTopUnreadBadge(
    text: String,
    layout: HomeTopRightUnreadBadgeLayout,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .defaultMinSize(
                minWidth = layout.minWidth,
                minHeight = layout.minHeight
            )
            .background(com.android.purebilibili.core.theme.iOSRed, CircleShape)
            .border(width = 1.dp, color = borderColor, shape = CircleShape)
            .padding(
                horizontal = layout.horizontalPadding,
                vertical = layout.verticalPadding
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
