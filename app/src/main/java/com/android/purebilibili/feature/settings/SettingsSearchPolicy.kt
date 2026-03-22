package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.util.PinyinUtils

enum class SettingsSearchTarget {
    APPEARANCE,
    PLAYBACK,
    BOTTOM_BAR,
    FOCUS,
    PERMISSION,
    BLOCKED_LIST,
    SETTINGS_SHARE,
    WEBDAV_BACKUP,
    DOWNLOAD_PATH,
    CLEAR_CACHE,
    PLUGINS,
    EXPORT_LOGS,
    OPEN_SOURCE_LICENSES,
    OPEN_SOURCE_HOME,
    CHECK_UPDATE,
    VIEW_RELEASE_NOTES,
    REPLAY_ONBOARDING,
    TIPS,
    OPEN_LINKS,
    DONATE,
    TELEGRAM,
    TWITTER,
    DISCLAIMER
}

data class SettingsSearchResult(
    val target: SettingsSearchTarget,
    val title: String,
    val subtitle: String,
    val section: String
)

private data class SettingsSearchEntry(
    val target: SettingsSearchTarget,
    val title: String,
    val subtitle: String,
    val section: String,
    val aliases: List<String>
)

private val SETTINGS_SEARCH_INDEX: List<SettingsSearchEntry> = listOf(
    SettingsSearchEntry(
        target = SettingsSearchTarget.APPEARANCE,
        title = "外观设置",
        subtitle = "主题、图标、模糊效果",
        section = "常规",
        aliases = listOf(
            "外观",
            "主题",
            "图标",
            "动画",
            "模糊",
            "皮肤",
            "玻璃",
            "液态玻璃",
            "毛玻璃",
            "md3",
            "material",
            "android",
            "安卓",
            "原生"
        )
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.PLAYBACK,
        title = "播放设置",
        subtitle = "解码、手势、后台播放",
        section = "常规",
        aliases = listOf("播放", "解码", "手势", "后台播放", "ai总结", "ai 总结", "视频总结", "总结")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.BOTTOM_BAR,
        title = "底栏设置",
        subtitle = "自定义底栏项目",
        section = "常规",
        aliases = listOf("底栏", "标签栏", "导航栏", "tab")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.FOCUS,
        title = "Focus",
        subtitle = "专注模式设置",
        section = "常规",
        aliases = listOf(
            "focus",
            "定制",
            "首页入口",
            "热门搜索",
            "历史清空",
            "推荐隐藏",
            "分区按钮",
            "相关推荐",
            "关注分组",
            "动态过滤",
            "首页关注过滤",
            "关注过滤",
            "专注模式"
        )
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.PERMISSION,
        title = "权限管理",
        subtitle = "查看应用权限",
        section = "隐私与安全",
        aliases = listOf("权限", "存储权限", "通知权限")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.BLOCKED_LIST,
        title = "黑名单管理",
        subtitle = "管理已屏蔽的 UP 主",
        section = "隐私与安全",
        aliases = listOf("黑名单", "屏蔽", "up")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.SETTINGS_SHARE,
        title = "设置分享",
        subtitle = "导出并导入可分享设置",
        section = "数据与存储",
        aliases = listOf("设置分享", "分享设置", "导入", "导出", "json", "配置分享", "设置包")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.WEBDAV_BACKUP,
        title = "WebDAV 云备份",
        subtitle = "备份与恢复设置/插件",
        section = "数据与存储",
        aliases = listOf("webdav", "云备份", "备份", "恢复")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.DOWNLOAD_PATH,
        title = "下载位置",
        subtitle = "设置导出目录",
        section = "数据与存储",
        aliases = listOf("下载", "目录", "路径", "导出目录")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.CLEAR_CACHE,
        title = "清除缓存",
        subtitle = "清理应用缓存",
        section = "数据与存储",
        aliases = listOf("缓存", "清理", "释放空间")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.PLUGINS,
        title = "插件中心",
        subtitle = "管理扩展插件",
        section = "开发者选项",
        aliases = listOf("插件", "扩展", "json")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.EXPORT_LOGS,
        title = "导出日志",
        subtitle = "用于反馈问题",
        section = "开发者选项",
        aliases = listOf("日志", "log", "反馈")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.OPEN_SOURCE_LICENSES,
        title = "开源许可证",
        subtitle = "查看项目 License",
        section = "关于",
        aliases = listOf("license", "许可证", "开源协议")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.OPEN_SOURCE_HOME,
        title = "开源主页",
        subtitle = "GitHub",
        section = "关于",
        aliases = listOf("github", "git", "仓库", "源码")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.CHECK_UPDATE,
        title = "检查更新",
        subtitle = "检测最新版本",
        section = "关于",
        aliases = listOf("更新", "升级", "新版本", "检查")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.VIEW_RELEASE_NOTES,
        title = "查看更新日志",
        subtitle = "最新版本说明",
        section = "关于",
        aliases = listOf("更新日志", "changelog", "版本说明")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.REPLAY_ONBOARDING,
        title = "重播新手引导",
        subtitle = "了解应用功能",
        section = "关于",
        aliases = listOf("新手引导", "教程", "引导")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.TIPS,
        title = "小贴士 & 隐藏操作",
        subtitle = "探索更多功能",
        section = "帮助与系统",
        aliases = listOf("贴士", "技巧", "帮助", "隐藏操作")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.OPEN_LINKS,
        title = "默认打开链接",
        subtitle = "设置应用链接支持",
        section = "帮助与系统",
        aliases = listOf("链接", "默认打开", "deep link")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.DONATE,
        title = "打赏作者",
        subtitle = "支持开发",
        section = "关注作者",
        aliases = listOf("打赏", "赞助", "支持")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.TELEGRAM,
        title = "Telegram 频道",
        subtitle = "@BiliPai",
        section = "关注作者",
        aliases = listOf("telegram", "tg", "频道")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.TWITTER,
        title = "Twitter / X",
        subtitle = "@YangY_0x00",
        section = "关注作者",
        aliases = listOf("twitter", "x", "推特")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.DISCLAIMER,
        title = "发布渠道声明",
        subtitle = "仅 GitHub / Telegram",
        section = "关于",
        aliases = listOf("声明", "发布渠道", "安全")
    )
)

internal fun resolveSettingsSearchResults(
    query: String,
    maxResults: Int = 8
): List<SettingsSearchResult> {
    val normalizedQuery = normalizeSettingsSearchText(query)
    if (normalizedQuery.isBlank()) return emptyList()
    if (maxResults <= 0) return emptyList()

    return SETTINGS_SEARCH_INDEX
        .mapNotNull { entry ->
            scoreSettingsSearchMatch(entry, normalizedQuery)?.let { score ->
                score to SettingsSearchResult(
                    target = entry.target,
                    title = entry.title,
                    subtitle = entry.subtitle,
                    section = entry.section
                )
            }
        }
        .sortedWith(
            compareByDescending<Pair<Int, SettingsSearchResult>> { it.first }
                .thenBy { it.second.title.length }
                .thenBy { it.second.title }
        )
        .map { it.second }
        .take(maxResults)
}

private fun scoreSettingsSearchMatch(entry: SettingsSearchEntry, query: String): Int? {
    val title = normalizeSettingsSearchText(entry.title)
    val subtitle = normalizeSettingsSearchText(entry.subtitle)
    val section = normalizeSettingsSearchText(entry.section)
    val aliases = entry.aliases.map(::normalizeSettingsSearchText)

    if (title.startsWith(query)) return 160
    if (aliases.any { it.startsWith(query) }) return 140
    if (title.contains(query)) return 120
    if (aliases.any { it.contains(query) }) return 100
    if (matchesSettingsSearchPinyin(entry.title, query)) return 90
    if (entry.aliases.any { matchesSettingsSearchPinyin(it, query) }) return 80
    if (subtitle.contains(query)) return 70
    if (section.contains(query)) return 50
    return null
}

private fun normalizeSettingsSearchText(value: String): String {
    return value.trim().lowercase().replace(" ", "")
}

private fun matchesSettingsSearchPinyin(value: String, query: String): Boolean {
    return PinyinUtils.matches(
        text = value.replace(" ", ""),
        query = query
    )
}

