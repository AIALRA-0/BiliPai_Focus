package com.android.purebilibili.core.ui.motion

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * 系统是否处于「减弱动效」状态。
 *
 * 以开发者选项/无障碍的动画时长缩放为准:`ANIMATOR_DURATION_SCALE == 0` 表示用户
 * 关闭了动画(无障碍「移除动画」也会把它置 0)。通过 ContentObserver 实时跟随变化。
 */
internal fun readSystemReduceMotion(resolver: ContentResolver): Boolean {
    val scale = Settings.Global.getFloat(
        resolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    )
    return scale == 0f
}

@Composable
fun rememberSystemReduceMotion(): Boolean {
    val resolver = LocalContext.current.applicationContext.contentResolver
    var reduceMotion by remember(resolver) { mutableStateOf(readSystemReduceMotion(resolver)) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduceMotion = readSystemReduceMotion(resolver)
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer
        )
        // 注册后再同步一次,避免注册窗口内的变更被漏掉。
        reduceMotion = readSystemReduceMotion(resolver)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduceMotion
}
