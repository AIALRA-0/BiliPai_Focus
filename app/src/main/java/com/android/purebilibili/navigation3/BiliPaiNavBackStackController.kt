package com.android.purebilibili.navigation3

internal data class BiliPaiNavBackStackController(
    val backStack: List<BiliPaiNavKey>
) {
    val currentKey: BiliPaiNavKey
        get() = backStack.lastOrNull() ?: BiliPaiNavKey.MainHost

    fun push(key: BiliPaiNavKey): BiliPaiNavBackStackController {
        return copy(backStack = pushBiliPaiNavKey(backStack, key))
    }

    fun pop(): BiliPaiNavBackStackController {
        return copy(backStack = popBiliPaiNavKey(backStack))
    }

    fun replaceTop(key: BiliPaiNavKey): BiliPaiNavBackStackController {
        val base = backStack.ifEmpty { listOf(BiliPaiNavKey.MainHost) }.dropLast(1)
        return copy(backStack = base + key)
    }

    fun popToRoot(): BiliPaiNavBackStackController {
        return copy(backStack = listOf(backStack.firstOrNull() ?: BiliPaiNavKey.MainHost))
    }
}
