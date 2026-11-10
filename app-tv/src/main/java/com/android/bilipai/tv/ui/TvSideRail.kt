@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.android.bilipai.tv.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.android.bilipai.tv.TvScreen
import com.android.bilipai.tv.ui.components.TvNavigationItem
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.theme.DarkSurfaceElevated
import com.android.purebilibili.core.ui.motion.AppMotionEasing

private val railMenu = listOf(
    TvScreen.Home to "推荐", TvScreen.Popular to "热门", TvScreen.Region to "分区", TvScreen.Following to "关注",
    TvScreen.Search to "搜索", TvScreen.History to "历史",
    TvScreen.Folders to "收藏", TvScreen.WatchLater to "稍后再看", TvScreen.Settings to "设置",
    TvScreen.Login to "账号",
)

private val railWidth = 200.dp

/**
 * 悬浮侧栏（Apple TV 式）：默认滑出屏幕，焦点进入导航项时滑入，焦点回内容区即滑出。
 * 侧栏悬浮于内容之上，不改变内容区宽度。
 */
@Composable
internal fun TvSideRail(
    visible: Boolean,
    account: String?,
    selectedScreen: TvScreen,
    contentFocus: FocusRequester,
    navigationFocus: FocusRequester,
    onRailFocusChanged: (Boolean) -> Unit,
    onNavigate: (TvScreen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalTvReduceMotion.current
    val railShape = TvUiTokens.shape(ContainerLevel.Floating)
    val offsetX = animateDpAsState(
        targetValue = if (visible) 0.dp else -(railWidth + 64.dp),
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 240, easing = AppMotionEasing.Continuity),
        label = "tv-rail-offset",
    )
    Column(
        modifier = modifier
            .offset { IntOffset(offsetX.value.roundToPx(), 0) }
            .padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
            .width(railWidth)
            .shadow(24.dp, railShape)
            .tvGlass(railShape, DarkSurfaceElevated)
            .onFocusChanged { onRailFocusChanged(it.hasFocus) }
            .focusGroup()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
    ) {
        Text("BiliPai TV", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        railMenu.forEach { (screen, title) ->
            val selected = selectedScreen == screen ||
                screen == TvScreen.Folders && selectedScreen == TvScreen.Favorites
            TvNavigationItem(
                selected = selected,
                onClick = { onNavigate(screen) },
                modifier = Modifier.fillMaxWidth().testTag("tv-nav-${screen.name}")
                    .then(if (selected) Modifier.focusRequester(navigationFocus) else Modifier)
                    .focusProperties { right = contentFocus },
            ) { Text(if (selected) "• $title" else title) }
        }
        Text(account ?: "游客", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
    }
}
