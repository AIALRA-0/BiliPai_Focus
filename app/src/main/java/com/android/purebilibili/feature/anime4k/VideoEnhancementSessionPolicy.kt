package com.android.purebilibili.feature.anime4k

/**
 * 新视频的画质增强默认关闭；只有用户明确允许跨视频记忆时才恢复最后状态。
 */
fun resolveInitialVideoEnhancementEnabled(
    pluginEnabled: Boolean,
    config: Anime4KConfig
): Boolean {
    return pluginEnabled && config.rememberAcrossVideos && config.rememberedEnabled
}

/** 当前视频切换开关时，仅在用户开启记忆后写入长期状态。 */
fun resolveConfigAfterVideoEnhancementToggle(
    config: Anime4KConfig,
    enabled: Boolean
): Anime4KConfig {
    return if (config.rememberAcrossVideos) {
        config.copy(rememberedEnabled = enabled)
    } else {
        config
    }
}

/** 开启记忆时从当前视频状态开始；关闭记忆时清除长期启用状态。 */
fun resolveConfigAfterRememberAcrossVideosChange(
    config: Anime4KConfig,
    rememberAcrossVideos: Boolean,
    currentVideoEnabled: Boolean
): Anime4KConfig {
    return config.copy(
        rememberAcrossVideos = rememberAcrossVideos,
        rememberedEnabled = if (rememberAcrossVideos) currentVideoEnabled else false
    )
}

/** 从默认关闭改为跨视频记忆时，UI 必须先展示性能警告。 */
fun shouldConfirmRememberAcrossVideosChange(
    currentValue: Boolean,
    requestedValue: Boolean
): Boolean {
    return !currentValue && requestedValue
}
