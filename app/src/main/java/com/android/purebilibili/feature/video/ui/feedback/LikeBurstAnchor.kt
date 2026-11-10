package com.android.purebilibili.feature.video.ui.feedback

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * 按钮锚点槽位：记录最近一次布局的按钮位置，并带代际号防止竞态清除。
 *
 * 为什么延迟清除而不是离开组合立即清除：关注胶囊按钮这类"点击后即消失"的按钮，
 * 卸载（清锚点）与反馈动画读取锚点在同帧竞速；延迟 [CLEAR_DELAY_MS] 清除
 * （略长于反馈动画约 2.6s 的生命周期）既覆盖动画窗口，又能保证离开页面后
 * 不会把过期坐标带到其他页面的反馈上。
 */
internal class AnchorSlot {
    var bounds: Rect? by mutableStateOf(null)
        private set
    private var generation = 0L
    private val handler = Handler(Looper.getMainLooper())

    fun report(newBounds: Rect) {
        generation += 1
        bounds = newBounds
    }

    fun scheduleClearIfStale() {
        val genAtDetach = generation
        handler.postDelayed({
            if (generation == genAtDetach) bounds = null
        }, CLEAR_DELAY_MS)
    }

    private companion object {
        const val CLEAR_DELAY_MS = 3_500L
    }
}

/**
 * 最近一次组合的点赞按钮在根坐标系中的位置，供点赞庆祝动画做图标锚定。
 * 页面上同时只会渲染一个点赞入口（竖屏动作行 / 全屏顶栏 / 横屏侧栏），
 * 因此用单例登记即可；动画触发时读取到的是刚被点击的那个按钮的位置。
 */
internal object LikeBurstAnchorRegistry {
    val likeIcon = AnchorSlot()
}

/**
 * 收藏按钮的位置登记，供收藏成功动画锚定到图标上方。
 */
internal object FavoriteActionAnchorRegistry {
    val favoriteIcon = AnchorSlot()
}

/**
 * 关注按钮的位置登记，供关注/取关动画锚定到按钮上方。
 * 只在单一实例的按钮上挂载（视频页作者栏、竖屏全屏浮层、个人空间页、直播页）；
 * 列表行、弹窗内的关注按钮不要挂：多实例会互相覆盖锚点，弹窗则处于独立窗口坐标系。
 */
internal object FollowActionAnchorRegistry {
    val followButton = AnchorSlot()
}

/** 缓存按钮的位置登记，供缓存完成动画锚定到图标上方。 */
internal object DownloadActionAnchorRegistry {
    val downloadIcon = AnchorSlot()
}

/** 统一的"上报 + 离开组合延迟清除"锚点修饰符工厂。 */
@Composable
private fun Modifier.actionAnchor(report: (Rect) -> Unit, detach: () -> Unit): Modifier {
    DisposableEffect(Unit) {
        onDispose { detach() }
    }
    return onGloballyPositioned { coordinates ->
        report(coordinates.boundsInRoot())
    }
}

/** 挂在点赞按钮（或其最小包裹容器）上，持续上报按钮在窗口中的位置。 */
@Composable
fun Modifier.likeBurstAnchor(): Modifier = actionAnchor(
    report = { LikeBurstAnchorRegistry.likeIcon.report(it) },
    detach = { LikeBurstAnchorRegistry.likeIcon.scheduleClearIfStale() },
)

/** 挂在收藏按钮（或其最小包裹容器）上，持续上报按钮在窗口中的位置。 */
@Composable
fun Modifier.favoriteActionAnchor(): Modifier = actionAnchor(
    report = { FavoriteActionAnchorRegistry.favoriteIcon.report(it) },
    detach = { FavoriteActionAnchorRegistry.favoriteIcon.scheduleClearIfStale() },
)

/** 挂在关注按钮（或其最小包裹容器）上，持续上报按钮在窗口中的位置。 */
@Composable
fun Modifier.followActionAnchor(): Modifier = actionAnchor(
    report = { FollowActionAnchorRegistry.followButton.report(it) },
    detach = { FollowActionAnchorRegistry.followButton.scheduleClearIfStale() },
)

/** 挂在缓存按钮（或其最小包裹容器）上，持续上报按钮在窗口中的位置。 */
@Composable
fun Modifier.downloadActionAnchor(): Modifier = actionAnchor(
    report = { DownloadActionAnchorRegistry.downloadIcon.report(it) },
    detach = { DownloadActionAnchorRegistry.downloadIcon.scheduleClearIfStale() },
)
