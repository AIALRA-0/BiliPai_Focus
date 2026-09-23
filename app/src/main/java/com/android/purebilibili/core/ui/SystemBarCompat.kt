package com.android.purebilibili.core.ui

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.WindowManager
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

internal data class AppWindowSystemUiSnapshot(
    val lightStatusBars: Boolean,
    val lightNavigationBars: Boolean,
    val statusBarColor: Int,
    val navigationBarColor: Int,
    val systemBarsVisible: Boolean,
)

/**
 * App-owned window policy for edge-to-edge, IME resizing and immersive playback.
 *
 * Feature composables should request a state through this object instead of writing legacy
 * `systemUiVisibility` flags or opaque system-bar colors directly. Android 15+ owns transparent
 * gesture system bars, while older releases retain the requested compatibility colors.
 */
internal object AppWindowSystemUiController {
    fun configureEdgeToEdgeHost(activity: Activity) {
        activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        ensureEdgeToEdge(activity.window)
    }

    fun ensureEdgeToEdge(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    fun capture(window: Window): AppWindowSystemUiSnapshot {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val systemBarsVisible = ViewCompat.getRootWindowInsets(window.decorView)
            ?.isVisible(WindowInsetsCompat.Type.systemBars())
            ?: true
        return AppWindowSystemUiSnapshot(
            lightStatusBars = controller.isAppearanceLightStatusBars,
            lightNavigationBars = controller.isAppearanceLightNavigationBars,
            statusBarColor = getWindowStatusBarColor(window),
            navigationBarColor = getWindowNavigationBarColor(window),
            systemBarsVisible = systemBarsVisible,
        )
    }

    fun enterImmersive(window: Window) {
        ensureEdgeToEdge(window)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    fun showSystemBars(window: Window) {
        WindowCompat.getInsetsController(window, window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    fun restore(window: Window, snapshot: AppWindowSystemUiSnapshot) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = snapshot.lightStatusBars
        controller.isAppearanceLightNavigationBars = snapshot.lightNavigationBars
        setWindowStatusBarColor(window, snapshot.statusBarColor)
        setWindowNavigationBarColor(window, snapshot.navigationBarColor)
        if (snapshot.systemBarsVisible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    /** Returns true when Android 17 accepted responsibility for the desktop transition. */
    fun requestDesktopFullscreen(activity: Activity, enter: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < 37 || (enter && !activity.isInMultiWindowMode)) return false
        activity.requestFullscreenMode(
            if (enter) Activity.FULLSCREEN_MODE_REQUEST_ENTER
            else Activity.FULLSCREEN_MODE_REQUEST_EXIT,
            null,
        )
        return true
    }
}

@Suppress("DEPRECATION")
internal fun setWindowStatusBarColor(window: Window, color: Int) {
    window.statusBarColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        Color.TRANSPARENT
    } else {
        color
    }
}

@Suppress("DEPRECATION")
internal fun setWindowNavigationBarColor(window: Window, color: Int) {
    window.navigationBarColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        Color.TRANSPARENT
    } else {
        color
    }
}

@Suppress("DEPRECATION")
internal fun getWindowStatusBarColor(window: Window): Int = window.statusBarColor

@Suppress("DEPRECATION")
internal fun getWindowNavigationBarColor(window: Window): Int = window.navigationBarColor
