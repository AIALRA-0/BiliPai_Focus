package com.android.purebilibili.feature.video.ui.components.glass

import com.android.purebilibili.feature.video.ui.components.*
import com.android.purebilibili.core.ui.AppContentColors
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel

@Composable
fun AspectRatioMenu(
    currentRatio: VideoAspectRatio,
    onRatioSelected: (VideoAspectRatio) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) = com.android.purebilibili.feature.video.ui.components.AspectRatioMenu(
    currentRatio = currentRatio,
    onRatioSelected = onRatioSelected,
    onDismiss = onDismiss,
    modifier = modifier,
)

/**
 * 比例按钮（用于底部控制栏）
 */
@Composable
fun AspectRatioButton(
    currentRatio: VideoAspectRatio,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppSurface(
        onClick = onClick,
        modifier = modifier,
        shape = AppShapes.container(ContainerLevel.Chip),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            AppIcon(
                Icons.Outlined.Star,
                contentDescription = "画面比例",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            AppText(
                text = currentRatio.displayName,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
