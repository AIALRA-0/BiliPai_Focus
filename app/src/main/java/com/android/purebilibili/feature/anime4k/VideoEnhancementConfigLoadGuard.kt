package com.android.purebilibili.feature.anime4k

/**
 * 防止插件启用时读取到的旧配置覆盖本次进程中刚完成的画质增强设置。
 *
 * 插件未启用时设置页仍可修改算法；DataStore 写入与首次启用读取可能并发，
 * 因此一旦内存配置已被用户修改，本次进程内就以它为准。
 */
internal class VideoEnhancementConfigLoadGuard {
    private var hasLocalChanges = false

    fun markLocalChange() {
        hasLocalChanges = true
    }

    fun shouldApplyLoadedConfig(): Boolean = !hasLocalChanges
}
