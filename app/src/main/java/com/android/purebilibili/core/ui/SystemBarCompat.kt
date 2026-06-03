package com.android.purebilibili.core.ui

import android.view.Window

@Suppress("DEPRECATION")
internal fun setWindowStatusBarColor(window: Window, color: Int) {
    window.statusBarColor = color
}

@Suppress("DEPRECATION")
internal fun setWindowNavigationBarColor(window: Window, color: Int) {
    window.navigationBarColor = color
}

@Suppress("DEPRECATION")
internal fun getWindowStatusBarColor(window: Window): Int = window.statusBarColor

@Suppress("DEPRECATION")
internal fun getWindowNavigationBarColor(window: Window): Int = window.navigationBarColor
