package com.android.purebilibili.feature.settings

internal enum class SettingsRootCategory(
    val title: String,
    val subtitle: String,
    val searchTarget: SettingsSearchTarget
) {
    SOCIAL_SUPPORT(
        title = "关注与支持",
        subtitle = "Telegram 频道、Twitter / X 与打赏作者",
        searchTarget = SettingsSearchTarget.TELEGRAM
    ),
    INTERFACE_THEME(
        title = "界面与主题",
        subtitle = "UI 预设、主题、字体、DPI、动态图标与开屏",
        searchTarget = SettingsSearchTarget.INTERFACE_THEME
    ),
    HOME_FEED(
        title = "首页与推荐",
        subtitle = "首页展示、推荐流、刷新数量、动态栏位、首页壁纸与底栏搜索入口",
        searchTarget = SettingsSearchTarget.HOME_FEED
    ),
    NAVIGATION_LABELS(
        title = "导航与标签",
        subtitle = "底栏、顶部标签、平板侧边栏与底栏项目顺序",
        searchTarget = SettingsSearchTarget.NAVIGATION
    ),
    PLAYBACK_QUALITY(
        title = "播放与画质",
        subtitle = "解码、默认画质、自动最高画质、网络、省流量、字幕、倍速与连播",
        searchTarget = SettingsSearchTarget.PLAYBACK_QUALITY
    ),
    FULLSCREEN_GESTURE(
        title = "全屏与手势",
        subtitle = "自动横竖屏、全屏方向、锁定按钮、截图按钮、应用内截图与手势",
        searchTarget = SettingsSearchTarget.FULLSCREEN_GESTURE
    ),
    INTERACTION_COMMENT(
        title = "互动与评论",
        subtitle = "评论发送检测、评论装扮、AI 总结、双击点赞与视频简介",
        searchTarget = SettingsSearchTarget.INTERACTION_COMMENT
    ),
    DATA_BACKUP(
        title = "数据与备份",
        subtitle = "设置分享、WebDAV、下载位置与清除缓存",
        searchTarget = SettingsSearchTarget.DATA_BACKUP
    ),
    PRIVACY_PERMISSION(
        title = "隐私与权限",
        subtitle = "隐私无痕、权限管理与黑名单",
        searchTarget = SettingsSearchTarget.PRIVACY_PERMISSION
    ),
    DIAGNOSTICS_DEVELOPER(
        title = "诊断与开发",
        subtitle = "崩溃追踪、使用情况统计、播放器诊断日志、画质降档弹窗、插件与导出日志",
        searchTarget = SettingsSearchTarget.DIAGNOSTICS
    ),
    ABOUT_SUPPORT(
        title = "关于与支持",
        subtitle = "版本、更新、开源、发布渠道、小贴士、默认打开链接、社群与捐赠",
        searchTarget = SettingsSearchTarget.ABOUT_SUPPORT
    )
}

internal fun resolveSettingsRootCategoryOrder(): List<SettingsRootCategory> = listOf(
    SettingsRootCategory.SOCIAL_SUPPORT,
    SettingsRootCategory.PLAYBACK_QUALITY,
    SettingsRootCategory.INTERFACE_THEME,
    SettingsRootCategory.HOME_FEED,
    SettingsRootCategory.NAVIGATION_LABELS,
    SettingsRootCategory.FULLSCREEN_GESTURE,
    SettingsRootCategory.INTERACTION_COMMENT,
    SettingsRootCategory.DATA_BACKUP,
    SettingsRootCategory.PRIVACY_PERMISSION,
    SettingsRootCategory.DIAGNOSTICS_DEVELOPER,
    SettingsRootCategory.ABOUT_SUPPORT
)

internal fun resolveTabletSettingsRootCategoryOrder(): List<SettingsRootCategory> =
    resolveSettingsRootCategoryOrder()

internal fun resolveSettingsRootCategoryForSearchTarget(
    target: SettingsSearchTarget
): SettingsRootCategory? = when (target) {
    SettingsSearchTarget.TELEGRAM,
    SettingsSearchTarget.TWITTER,
    SettingsSearchTarget.DONATE -> SettingsRootCategory.SOCIAL_SUPPORT

    SettingsSearchTarget.INTERFACE_THEME,
    SettingsSearchTarget.APPEARANCE,
    SettingsSearchTarget.ANIMATION -> SettingsRootCategory.INTERFACE_THEME

    SettingsSearchTarget.HOME_FEED -> SettingsRootCategory.HOME_FEED

    SettingsSearchTarget.NAVIGATION,
    SettingsSearchTarget.BOTTOM_BAR -> SettingsRootCategory.NAVIGATION_LABELS

    SettingsSearchTarget.PLAYBACK_QUALITY,
    SettingsSearchTarget.PLAYBACK -> SettingsRootCategory.PLAYBACK_QUALITY

    SettingsSearchTarget.FULLSCREEN_GESTURE -> SettingsRootCategory.FULLSCREEN_GESTURE

    SettingsSearchTarget.INTERACTION_COMMENT -> SettingsRootCategory.INTERACTION_COMMENT

    SettingsSearchTarget.DATA_BACKUP,
    SettingsSearchTarget.SETTINGS_SHARE,
    SettingsSearchTarget.WEBDAV_BACKUP,
    SettingsSearchTarget.DOWNLOAD_PATH,
    SettingsSearchTarget.IMAGE_SAVE_PATH,
    SettingsSearchTarget.CLEAR_CACHE -> SettingsRootCategory.DATA_BACKUP

    SettingsSearchTarget.PRIVACY_PERMISSION,
    SettingsSearchTarget.PERMISSION,
    SettingsSearchTarget.BLOCKED_LIST -> SettingsRootCategory.PRIVACY_PERMISSION

    SettingsSearchTarget.DIAGNOSTICS,
    SettingsSearchTarget.PLUGINS,
    SettingsSearchTarget.EXPORT_LOGS -> SettingsRootCategory.DIAGNOSTICS_DEVELOPER

    SettingsSearchTarget.ABOUT_SUPPORT,
    SettingsSearchTarget.OPEN_SOURCE_LICENSES,
    SettingsSearchTarget.OPEN_SOURCE_HOME,
    SettingsSearchTarget.CHECK_UPDATE,
    SettingsSearchTarget.VIEW_RELEASE_NOTES,
    SettingsSearchTarget.REPLAY_ONBOARDING,
    SettingsSearchTarget.TIPS,
    SettingsSearchTarget.OPEN_LINKS,
    SettingsSearchTarget.DISCLAIMER -> SettingsRootCategory.ABOUT_SUPPORT
}

internal fun isSceneSettingsSearchTarget(target: SettingsSearchTarget): Boolean = target in setOf(
    SettingsSearchTarget.TELEGRAM,
    SettingsSearchTarget.TWITTER,
    SettingsSearchTarget.DONATE,
    SettingsSearchTarget.INTERFACE_THEME,
    SettingsSearchTarget.HOME_FEED,
    SettingsSearchTarget.NAVIGATION,
    SettingsSearchTarget.PLAYBACK_QUALITY,
    SettingsSearchTarget.FULLSCREEN_GESTURE,
    SettingsSearchTarget.INTERACTION_COMMENT,
    SettingsSearchTarget.DATA_BACKUP,
    SettingsSearchTarget.PRIVACY_PERMISSION,
    SettingsSearchTarget.DIAGNOSTICS,
    SettingsSearchTarget.ABOUT_SUPPORT
)

internal fun resolveSettingsRootCategoryListIndex(category: SettingsRootCategory): Int {
    val orderIndex = resolveSettingsRootCategoryOrder().indexOf(category)
    if (orderIndex < 0) return 0
    return 1 + orderIndex
}
