package com.android.purebilibili.core.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import com.android.purebilibili.core.ui.AppWindowSystemUiController

object ScreenUtils {
    fun setFullScreen(context: Context, isFull: Boolean) {
        val activity = context.findActivity() ?: return
        if (isFull) {
            // 切横屏
            activity.applyPlayerRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
            AppWindowSystemUiController.requestDesktopFullscreen(activity, enter = true)
            AppWindowSystemUiController.enterImmersive(activity.window)
        } else {
            // 切竖屏
            activity.applyPlayerRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            AppWindowSystemUiController.showSystemBars(activity.window)
            AppWindowSystemUiController.requestDesktopFullscreen(activity, enter = false)
        }
    }

    private fun Context.findActivity(): Activity? {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        return null
    }
}
