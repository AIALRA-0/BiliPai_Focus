package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

class AppDialogExitState(
    val progress: State<Float>,
    private val closing: () -> Boolean = { false },
) {
    val isClosing: Boolean get() = closing()
}

val LocalAppDialogExitRunning = compositionLocalOf { false }

/** 窗口退出与业务关闭解耦：只有声明方卸载后才开始视觉退出，不拦截保存和校验。 */
interface AppRetainedDialogPresenter {
    fun show(owner: Any, parent: Any?, content: @Composable (AppDialogExitState) -> Unit)
    fun dismiss(owner: Any)
}

val LocalAppRetainedDialogPresenter = compositionLocalOf<AppRetainedDialogPresenter?> { null }
val LocalAppRetainedDialogOwner = compositionLocalOf<Any?> { null }

/** 返回是否由长期存活的宿主保留窗口；进度 0 为可见，1 为退出完成。 */
@Composable
internal fun RetainAppGlassDialog(content: @Composable (AppDialogExitState) -> Unit): Boolean {
    val presenter = LocalAppRetainedDialogPresenter.current ?: return false
    val owner = remember { Any() }
    val parent = LocalAppRetainedDialogOwner.current
    val latestContent by rememberUpdatedState(content)
    val locals by rememberUpdatedState(currentCompositionLocalContext)
    SideEffect {
        presenter.show(owner, parent) { progress ->
            CompositionLocalProvider(locals) {
                CompositionLocalProvider(LocalAppRetainedDialogOwner provides owner) {
                    latestContent(progress)
                }
            }
        }
    }
    DisposableEffect(presenter, owner) { onDispose { presenter.dismiss(owner) } }
    return true
}

/** 无真实锚点的玻璃弹层共用退出时长；原生组件不读取此参数。 */
object AppGlassPopupMotionSpec {
    const val UnanchoredExitMillis = 200
    const val AnchoredReturnMillis = 500
}
