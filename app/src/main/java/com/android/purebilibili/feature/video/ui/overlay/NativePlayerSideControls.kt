package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.LocalAppButtonSlotColorCorrectionEnabled
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppSurface

/** MD3、Miuix 非玻璃播放器侧边按钮，保留原有 34dp 尺寸与触控策略。 */
@Composable
internal fun NativePlayerSideButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadiusDp: Int = 8,
    iconSizeDp: Int = 20,
) {
    PlayerControlTouchBounds(height = 34.dp) {
        CompositionLocalProvider(LocalAppButtonSlotColorCorrectionEnabled provides false) {
            AppSurface(
                onClick = onClick,
                modifier = modifier.size(34.dp),
                shape = RoundedCornerShape(cornerRadiusDp.dp),
                color = Color(0x45000000),
                contentColor = Color.White,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        tint = Color.White,
                        modifier = Modifier.size(iconSizeDp.dp),
                    )
                }
            }
        }
    }
}

/** 共用横屏侧键的材质入口；关闭玻璃后完整保留原生参数。 */
@Composable
internal fun PlayerSideButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadiusDp: Int = 8,
    iconSizeDp: Int = 20,
) {
    if (!LocalPlayerControlGlassEnabled.current) {
        NativePlayerSideButton(icon, contentDescription, onClick, modifier, cornerRadiusDp, iconSizeDp)
        return
    }
    PlayerControlGlassDock(
        enabled = true,
        modifier = modifier.size(PlayerGlassIconButtonSize),
        shape = androidx.compose.foundation.shape.CircleShape,
    ) {
        CompositionLocalProvider(LocalAppButtonSlotColorCorrectionEnabled provides false) {
            AppSurface(
                onClick = onClick,
                modifier = Modifier.fillMaxSize(),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = Color.Transparent,
                contentColor = Color.White,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppIcon(icon, contentDescription, tint = Color.White, modifier = Modifier.size(iconSizeDp.dp))
                }
            }
        }
    }
}
