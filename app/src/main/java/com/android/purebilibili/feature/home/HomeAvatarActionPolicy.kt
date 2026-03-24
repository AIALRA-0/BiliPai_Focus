package com.android.purebilibili.feature.home

enum class HomeAvatarAction {
    OPEN_DRAWER,
    OPEN_PROFILE,
    OPEN_LOGIN
}

fun resolveHomeAvatarAction(
    isLoggedIn: Boolean,
    isHomeDrawerEnabled: Boolean
): HomeAvatarAction {
    return when {
        isLoggedIn && isHomeDrawerEnabled -> HomeAvatarAction.OPEN_DRAWER
        isLoggedIn -> HomeAvatarAction.OPEN_PROFILE
        else -> HomeAvatarAction.OPEN_LOGIN
    }
}
