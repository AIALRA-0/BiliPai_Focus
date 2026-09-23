package com.android.purebilibili.feature.settings
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppHorizontalDivider

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.annotation.DrawableRes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.R
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSGreen
import com.android.purebilibili.core.theme.iOSOrange
import com.android.purebilibili.core.theme.iOSPink
import com.android.purebilibili.core.theme.iOSTeal
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.resolveBottomSafeAreaPadding
import com.android.purebilibili.core.ui.animation.EntranceGroup
import com.android.purebilibili.core.ui.animation.entrance
import com.android.purebilibili.core.ui.components.rememberAdaptivePreferenceIconContentColor
import com.android.purebilibili.core.ui.components.rememberAdaptivePreferenceIconContainerColor
import com.android.purebilibili.core.ui.components.rememberAdaptiveSemanticIconTint
import com.android.purebilibili.core.ui.components.AppPreferenceSectionTitle
import com.android.purebilibili.feature.settings.ui.SettingsPageScaffold

private data class TipEntry(
    @DrawableRes val iconResId: Int,
    val iconTint: Color,
    val title: String,
    val content: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipsSettingsScreen(
    onBack: () -> Unit
) {
    val screenTitle = stringResource(R.string.tips_title)
    val backLabel = stringResource(R.string.common_back)
    val contentBottomPadding = resolveBottomSafeAreaPadding(
        navigationBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        extraBottomPadding = 32.dp
    )

    val basicTips = remember {
        listOf(
            TipEntry(
                iconResId = R.drawable.ms_draw_fill_24,
                iconTint = iOSBlue,
                title = "1. 摸鱼模式：播放器缩小策略",
                content = "在播放设置里把「播放器缩小策略」设为竖屏、横屏、全部或暂停时，再按对应条件缩小播放器。"
            ),
            TipEntry(
                iconResId = R.drawable.ms_keyboard_arrow_up_24,
                iconTint = iOSTeal,
                title = "2. 回顶部不用狂划",
                content = "首页刷太深时，双击底栏「首页」图标，或双击顶部频道标题，可快速回到列表顶部。"
            ),
            TipEntry(
                iconResId = R.drawable.ms_touch_app_fill_24,
                iconTint = iOSOrange,
                title = "3. 封面长按有快捷操作",
                content = "长按视频卡片可快速预览，并直接执行「稍后再看」等常用动作。"
            ),
            TipEntry(
                iconResId = R.drawable.ms_find_in_page_24,
                iconTint = iOSGreen,
                title = "4. 搜索不只搜视频",
                content = "搜索页可切换「视频 / UP主 / 番剧 / 直播」，还能按分区、排序、时长做精确筛选。"
            )
        )
    }

    val hiddenTips = remember {
        listOf(
            TipEntry(
                iconResId = R.drawable.ms_skip_next_fill_24,
                iconTint = iOSPink,
                title = "5. 关闭自动连播，结尾更安静",
                content = "在播放设置关闭「自动播放下一个」后，视频播完会停在结束态，不再自动连播。"
            ),
            TipEntry(
                iconResId = R.drawable.ms_cameraswitch_fill_24,
                iconTint = iOSBlue,
                title = "6. 自动横竖屏更省手",
                content = "开启「自动横竖屏切换」后，跟随手机方向自动进出全屏，单手看片更顺。"
            ),
            TipEntry(
                iconResId = R.drawable.ms_picture_in_picture_fill_24,
                iconTint = iOSTeal,
                title = "7. 小窗和画中画是两种玩法",
                content = "播放设置中的「后台播放模式」可选择应用内小窗、系统画中画，或让两者同时生效；画中画需先授予权限。"
            ),
            TipEntry(
                iconResId = R.drawable.ms_thumb_up_fill_24,
                iconTint = iOSOrange,
                title = "8. 双击点赞可按喜好开关",
                content = "若你容易误触，可在播放设置关闭「双击点赞」；喜欢快操作就保持开启。"
            )
        )
    }

    val advancedTips = remember {
        listOf(
            TipEntry(
                iconResId = R.drawable.ms_flare_24,
                iconTint = iOSBlue,
                title = "9. 空降助手能跳过片头广告",
                content = "在设置开启「空降助手」后，可自动跳过赞助/片头片尾；也可改成仅提示不自动跳过。"
            ),
            TipEntry(
                iconResId = R.drawable.ms_label_fill_24,
                iconTint = iOSPink,
                title = "10. 版本号连点有彩蛋",
                content = "在设置页连续点击版本号会触发隐藏彩蛋提示，适合探索党。"
            ),
            TipEntry(
                iconResId = R.drawable.ms_card_giftcard_fill_24,
                iconTint = iOSOrange,
                title = "11. 趣味彩蛋可随时关闭",
                content = "如果不想看到趣味提示，可在设置里关闭「趣味彩蛋」，界面会更克制。"
            ),
            TipEntry(
                iconResId = R.drawable.ms_link_24,
                iconTint = iOSTeal,
                title = "12. 链接默认打开可一步到位",
                content = "在设置中配置「默认打开链接」后，点到 B 站链接可更稳定地直达应用内页面。"
            )
        )
    }

    EntranceGroup {
        SettingsPageScaffold(
            title = screenTitle,
            onBack = onBack,
            backContentDescription = backLabel,
            bottomContentPadding = contentBottomPadding,
            lazyListContent = {
            item {
                Box(modifier = Modifier.entrance()) {
                    AppPreferenceSectionTitle("基础技巧")
                }
            }
            item {
                Box(modifier = Modifier.entrance()) {
                    TipSection(items = basicTips)
                }
            }

            item {
                Box(modifier = Modifier.entrance()) {
                    AppPreferenceSectionTitle("隐藏技巧")
                }
            }
            item {
                Box(modifier = Modifier.entrance()) {
                    TipSection(items = hiddenTips)
                }
            }

            item {
                Box(modifier = Modifier.entrance()) {
                    AppPreferenceSectionTitle("进阶玩法")
                }
            }
            item {
                Box(modifier = Modifier.entrance()) {
                    TipSection(items = advancedTips)
                }
            }
        },
        )
    }
}

@Composable
private fun TipSection(items: List<TipEntry>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(AppShapes.container(ContainerLevel.Field))
            .background(AppSurfaceTokens.cardContainer())
    ) {
        items.forEachIndexed { index, tip ->
            TipItem(
                iconResId = tip.iconResId,
                iconTint = tip.iconTint,
                title = tip.title,
                content = tip.content
            )
            if (index != items.lastIndex) {
                TipDivider()
            }
        }
    }
}

@Composable
private fun TipItem(
    @DrawableRes iconResId: Int,
    iconTint: Color,
    title: String,
    content: String
) {
    val icon = rememberMaterialSymbol(iconResId)
    val effectiveIconTint = rememberAdaptivePreferenceIconContainerColor(iconTint)
    val iconContentColor = rememberAdaptivePreferenceIconContentColor(effectiveIconTint)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(AppShapes.container(ContainerLevel.Field))
                .background(effectiveIconTint),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                imageVector = icon,
                contentDescription = null,
                tint = iconContentColor,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(AppSpacingTokens.Medium))
        Column {
            AppText(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(AppSpacingTokens.ExtraSmall))
            AppText(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TipDivider() {
    AppHorizontalDivider(
        modifier = Modifier.padding(start = 48.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
        thickness = 0.5.dp
    )
}
