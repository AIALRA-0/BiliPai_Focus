package com.android.purebilibili.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSGreen
import com.android.purebilibili.core.theme.iOSPink
import com.android.purebilibili.core.theme.iOSYellow
import com.android.purebilibili.core.ui.rememberAppBookmarkIcon
import com.android.purebilibili.core.ui.rememberAppChevronForwardIcon
import com.android.purebilibili.core.ui.rememberAppDownloadIcon
import com.android.purebilibili.core.ui.rememberAppHistoryIcon
import com.android.purebilibili.core.ui.rememberAppInboxIcon
import com.android.purebilibili.core.ui.rememberAppLogoutIcon
import com.android.purebilibili.core.ui.rememberAppTvIcon
import com.android.purebilibili.core.ui.components.IOSClickableItem
import com.android.purebilibili.core.ui.components.UserLevelBadge
import com.android.purebilibili.core.ui.blur.unifiedBlur
import com.android.purebilibili.feature.home.UserState
import dev.chrisbanes.haze.HazeState
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Tv
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowDownCircle
import io.github.alexzhirkevich.cupertino.icons.outlined.Bookmark
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronForward
import io.github.alexzhirkevich.cupertino.icons.outlined.Clock
import io.github.alexzhirkevich.cupertino.icons.outlined.RectanglePortraitAndArrowForward
import kotlinx.coroutines.launch

internal data class MineSideDrawerChromeSpec(
    val useMaterialIcons: Boolean,
    val preferOpaqueMd3Container: Boolean,
    val profileChevronSizeDp: Int
)

internal fun resolveMineSideDrawerChromeSpec(
    uiPreset: UiPreset,
    blurEnabled: Boolean
): MineSideDrawerChromeSpec {
    return if (uiPreset == UiPreset.MD3) {
        MineSideDrawerChromeSpec(
            useMaterialIcons = true,
            preferOpaqueMd3Container = !blurEnabled,
            profileChevronSizeDp = 20
        )
    } else {
        MineSideDrawerChromeSpec(
            useMaterialIcons = false,
            preferOpaqueMd3Container = false,
            profileChevronSizeDp = 18
        )
    }
}

/**
 * 首页侧边栏 - 优化版 (带毛玻璃效果)
 * 采用更紧凑的布局和更现代的视觉风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineSideDrawer(
    drawerState: DrawerState,
    user: UserState,
    onLogout: () -> Unit,
    onHistoryClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onBangumiClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onWatchLaterClick: () -> Unit,
    onInboxClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit,
    hazeState: HazeState? = null, // 毛玻璃效果状态
    isBlurEnabled: Boolean = true // [新增] 模糊开关状态
) {
    val uiPreset = LocalUiPreset.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val layoutPolicy = remember(configuration.screenWidthDp) {
        resolveMineSideDrawerLayoutPolicy(
            widthDp = configuration.screenWidthDp
        )
    }
    // 侧边栏宽度自适应：中屏/大屏不再沿用手机上限 360dp
    val drawerWidth = remember(configuration.screenWidthDp, layoutPolicy) {
        resolveMineSideDrawerWidthDp(
            screenWidthDp = configuration.screenWidthDp,
            policy = layoutPolicy
        ).dp
    }
    
    // 辅助函数：关闭侧边栏并执行回调
    fun closeAndRun(action: () -> Unit) {
        scope.launch {
            drawerState.close()
            action()
        }
    }
    
    // 检测深色模式
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    val blurActive = hazeState != null && isBlurEnabled
    val drawerMotionBudget = resolveDrawerMotionBudget(
        isDrawerTransitionRunning = drawerState.currentValue != drawerState.targetValue
    )
    val effectiveBlurActive = shouldEnableDrawerBlur(
        blurActive = blurActive,
        budget = drawerMotionBudget
    )
    val chromeSpec = remember(uiPreset, effectiveBlurActive) {
        resolveMineSideDrawerChromeSpec(
            uiPreset = uiPreset,
            blurEnabled = effectiveBlurActive
        )
    }
    val palette = resolveDrawerGlassPalette(isDark = isDark, blurEnabled = effectiveBlurActive)
    val downloadIcon = rememberAppDownloadIcon()
    val historyIcon = rememberAppHistoryIcon()
    val tvIcon = rememberAppTvIcon()
    val bookmarkIcon = rememberAppBookmarkIcon()
    val inboxIcon = rememberAppInboxIcon()
    val logoutIcon = rememberAppLogoutIcon()
    val chevronForwardIcon = rememberAppChevronForwardIcon()

    // 动态文字颜色
    val activeContentColor = if (isDark) Color(0xFFF8FAFF) else Color(0xFF101114)
    // 动态次级文字/图标颜色
    val secondaryContentColor = if (isDark) Color(0xFFC4C8D1) else Color(0xFF2E2F33).copy(alpha = 0.86f)
    // 动态分割线颜色
    val dividerColor = if (isDark) Color.White.copy(alpha = palette.dividerAlpha) else Color.Black.copy(alpha = palette.dividerAlpha)
    val drawerBaseColor = if (chromeSpec.preferOpaqueMd3Container) {
        MaterialTheme.colorScheme.surfaceContainer
    } else if (isDark) {
        Color(0xFF0B0D12).copy(alpha = palette.drawerBaseAlpha)
    } else {
        Color.White.copy(alpha = palette.drawerBaseAlpha)
    }
    val itemSurfaceColor = if (chromeSpec.preferOpaqueMd3Container) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else if (isDark) {
        Color.White.copy(alpha = palette.itemSurfaceAlpha)
    } else {
        Color(0xFFFDFEFF).copy(alpha = palette.itemSurfaceAlpha)
    }
    val itemBorderColor = if (isDark) Color.White.copy(alpha = palette.itemBorderAlpha) else Color.Black.copy(alpha = palette.itemBorderAlpha)
    val chevronColor = secondaryContentColor.copy(alpha = if (isDark) 0.92f else 0.84f)

    // 使用 Surface 替代 ModalDrawerSheet 以绕过最小宽度限制 (240dp)
    Surface(
        color = drawerBaseColor,
        contentColor = activeContentColor,
        shape = RoundedCornerShape(
            topEnd = layoutPolicy.drawerEdgeRadiusDp.dp,
            bottomEnd = layoutPolicy.drawerEdgeRadiusDp.dp
        ), // 保持抽屉的右侧圆角
        modifier = Modifier
            .fillMaxHeight()
            .width(drawerWidth)
            .then(
                if (effectiveBlurActive) {
                    Modifier.unifiedBlur(
                        hazeState = requireNotNull(hazeState),
                        enabled = true,
                        shape = RoundedCornerShape(
                            topEnd = layoutPolicy.drawerEdgeRadiusDp.dp,
                            bottomEnd = layoutPolicy.drawerEdgeRadiusDp.dp
                        )
                    )
                } else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                Color.White.copy(alpha = 0.05f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.08f)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.White.copy(alpha = 0.06f),
                                Color.Transparent
                            )
                        }
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(vertical = layoutPolicy.contentVerticalPaddingDp.dp)
            ) {
            // 1. 用户信息区域 - 可点击进入个人主页
            // 移除 Surface 背景，只保留点击区域和内容
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = layoutPolicy.sectionHorizontalPaddingDp.dp)
                    .clip(RoundedCornerShape(layoutPolicy.profileCardCornerRadiusDp.dp))
                    .background(itemSurfaceColor)
                    .border(
                        BorderStroke(0.8.dp, itemBorderColor),
                        RoundedCornerShape(layoutPolicy.profileCardCornerRadiusDp.dp)
                    )
                    .clickable { closeAndRun(onProfileClick) }
                    // 背景完全透明，依靠下方毛玻璃效果
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(layoutPolicy.profileRowPaddingDp.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 头像 (尺寸再次微调，适应更窄的栏宽)
                    AsyncImage(
                        model = user.face,
                        contentDescription = "用户头像",
                        modifier = Modifier
                            .size(layoutPolicy.profileAvatarSizeDp.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // 用户名和等级
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.name.ifEmpty { "未登录" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = activeContentColor,
                            maxLines = 1
                        )
                        
                        if (user.isLogin) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 等级徽章
                                if (user.level > 0) {
                                    UserLevelBadge(level = user.level)
                                }
                                
                                // VIP 徽章
                                if (user.isVip) {
                                    Surface(
                                        color = Color(0xFFFB7299),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "大会员",
                                            color = Color.White,
                                            fontSize = layoutPolicy.badgeFontSp.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // 右箭头
                    Icon(
                        imageVector = chevronForwardIcon,
                        contentDescription = null,
                        tint = secondaryContentColor,
                        modifier = Modifier.size(chromeSpec.profileChevronSizeDp.dp)
                    )
                }
            }
            
            // 分割线样式
            val dividerThickness = 0.5.dp
            
            // 组间分割线 (全宽带padding)
            HorizontalDivider(
                modifier = Modifier.padding(
                    horizontal = layoutPolicy.dividerHorizontalPaddingDp.dp,
                    vertical = layoutPolicy.dividerVerticalPaddingDp.dp
                ),
                thickness = dividerThickness,
                color = dividerColor
            )

            // 2. 常用服务 - iOS 风格列表
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = layoutPolicy.sectionHorizontalPaddingDp.dp),
                shape = RoundedCornerShape(layoutPolicy.sectionCornerRadiusDp.dp),
                color = itemSurfaceColor,
                border = BorderStroke(0.8.dp, itemBorderColor)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IOSClickableItem(
                        icon = downloadIcon,
                        title = "离线缓存",
                        onClick = { closeAndRun(onDownloadClick) },
                        iconTint = MaterialTheme.colorScheme.primary,
                        textColor = activeContentColor,
                        valueColor = secondaryContentColor,
                        chevronTint = chevronColor
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 48.dp), thickness = dividerThickness, color = dividerColor)
                    IOSClickableItem(
                        icon = historyIcon,
                        title = "历史记录",
                        onClick = { closeAndRun(onHistoryClick) },
                        iconTint = iOSBlue,
                        textColor = activeContentColor,
                        valueColor = secondaryContentColor,
                        chevronTint = chevronColor
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 48.dp), thickness = dividerThickness, color = dividerColor)
                    IOSClickableItem(
                        icon = tvIcon,
                        title = "番剧影视",
                        onClick = { closeAndRun(onBangumiClick) },
                        iconTint = iOSPink,
                        textColor = activeContentColor,
                        valueColor = secondaryContentColor,
                        chevronTint = chevronColor
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 48.dp), thickness = dividerThickness, color = dividerColor)
                    IOSClickableItem(
                        icon = bookmarkIcon,
                        title = "我的收藏",
                        onClick = { closeAndRun(onFavoriteClick) },
                        iconTint = iOSYellow,
                        textColor = activeContentColor,
                        valueColor = secondaryContentColor,
                        chevronTint = chevronColor
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 48.dp), thickness = dividerThickness, color = dividerColor)
                    IOSClickableItem(
                        icon = bookmarkIcon,
                        title = "稍后再看",
                        onClick = { closeAndRun(onWatchLaterClick) },
                        iconTint = iOSGreen,
                        textColor = activeContentColor,
                        valueColor = secondaryContentColor,
                        chevronTint = chevronColor
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 48.dp), thickness = dividerThickness, color = dividerColor)
                    IOSClickableItem(
                        icon = inboxIcon,
                        title = "消息中心",
                        onClick = { closeAndRun(onInboxClick) },
                        iconTint = iOSPink,
                        textColor = activeContentColor,
                        valueColor = secondaryContentColor,
                        chevronTint = chevronColor
                    )
                }
            }
            
            // 组间分割线
            HorizontalDivider(
                modifier = Modifier.padding(
                    horizontal = layoutPolicy.dividerHorizontalPaddingDp.dp,
                    vertical = layoutPolicy.dividerVerticalPaddingDp.dp
                ),
                thickness = dividerThickness,
                color = dividerColor
            )
            
            // 3. 退出登录按钮
            if (user.isLogin) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = layoutPolicy.sectionHorizontalPaddingDp.dp),
                    shape = RoundedCornerShape(layoutPolicy.sectionCornerRadiusDp.dp),
                    color = itemSurfaceColor,
                    border = BorderStroke(0.8.dp, itemBorderColor)
                ) {
                    IOSClickableItem(
                        icon = logoutIcon,
                        title = "退出登录",
                        onClick = { closeAndRun(onLogout) },
                        iconTint = Color(0xFFFF453A), // iOS 红色
                        textColor = Color(0xFFFF453A),
                        valueColor = secondaryContentColor,
                        chevronTint = chevronColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(layoutPolicy.footerSpacerHeightDp.dp))
        }
        }
    }
}
