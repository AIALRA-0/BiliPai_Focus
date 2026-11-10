package com.android.purebilibili.feature.video.ui.section

import android.graphics.drawable.Animatable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil3.asDrawable
import coil3.compose.AsyncImage
import com.android.purebilibili.core.ui.rememberDecorativeAnimationGate
import com.android.purebilibili.data.model.response.SpaceLiveRoom
import com.android.purebilibili.data.repository.VideoCatalogRepository
import kotlinx.coroutines.launch

/** 状态绑定 UP 身份并在页面恢复时刷新；未知、请求失败和轮播均不显示开播标识。 */
@Composable
internal fun VideoOwnerLiveBadge(
    ownerMid: Long,
    modifier: Modifier = Modifier,
    onOpenLiveRoom: ((String) -> Unit)? = null,
) {
    var liveRoom by remember(ownerMid) { mutableStateOf<SpaceLiveRoom?>(null) }
    val scope = rememberCoroutineScope()
    LifecycleResumeEffect(ownerMid) {
        val request = scope.launch {
            if (ownerMid > 0L) {
                // 恢复查询失败保留同一作者已知状态，成功返回未开播时才移除徽章。
                VideoCatalogRepository.getCreatorLiveRoom(ownerMid).onSuccess { liveRoom = it }
            }
        }
        onPauseOrDispose { request.cancel() }
    }
    if (liveRoom?.liveStatus != 1) return
    val room = liveRoom
    val roomId = room?.roomId?.takeIf { it > 0L }
        ?: (com.android.purebilibili.core.util.BilibiliNavigationTargetParser.parse(room?.url.orEmpty())
            as? com.android.purebilibili.core.util.BilibiliNavigationTarget.Live)?.roomId?.takeIf { it > 0L }
    val context = LocalContext.current
    val animationGate = rememberDecorativeAnimationGate()
    var animation by remember { mutableStateOf<Animatable?>(null) }
    DisposableEffect(animation, animationGate.active) {
        val current = animation
        if (animationGate.active) current?.start() else current?.stop()
        onDispose { current?.stop() }
    }
    val badgeBackground = MaterialTheme.colorScheme.primary
    val badgeForeground = MaterialTheme.colorScheme.onPrimary
    val themeFilter = remember(badgeBackground, badgeForeground) {
        // 动图粉色底的绿色通道为 114，白色字形为 255；按两端映射保留抗锯齿和动画透明度。
        fun scale(background: Float, foreground: Float) = (foreground - background) * 255f / (255f - 114f)
        val redScale = scale(badgeBackground.red, badgeForeground.red)
        val greenScale = scale(badgeBackground.green, badgeForeground.green)
        val blueScale = scale(badgeBackground.blue, badgeForeground.blue)
        ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
            0f, redScale, 0f, 0f, badgeBackground.red * 255f - redScale * 114f,
            0f, greenScale, 0f, 0f, badgeBackground.green * 255f - greenScale * 114f,
            0f, blueScale, 0f, 0f, badgeBackground.blue * 255f - blueScale * 114f,
            0f, 0f, 0f, 1f, 0f,
        )))
    }
    // 保留动图原始比例、直播身份判断及点击行为，仅将底色与字形映射为主题配色。
    AsyncImage(
        model = "file:///android_asset/live/live.gif",
        contentDescription = "直播中",
        colorFilter = themeFilter,
        modifier = modifier
            .height(14.dp)
            .aspectRatio(135f / 42f)
            .clip(com.android.purebilibili.feature.video.ui.VideoDetailShapes.action())
            .clickable(
                enabled = roomId != null && onOpenLiveRoom != null,
                role = androidx.compose.ui.semantics.Role.Button,
                onClickLabel = "进入直播间",
                onClick = { roomId?.let { onOpenLiveRoom?.invoke("https://live.bilibili.com/$it") } },
            )
            .then(animationGate.modifier),
        onSuccess = {
            val drawable = it.result.image.asDrawable(context.resources) as? Animatable
            drawable?.stop()
            animation = drawable
        },
    )
}
