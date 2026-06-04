package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.util.PinyinUtils

enum class SettingsSearchTarget {
    INTERFACE_THEME,
    HOME_FEED,
    NAVIGATION,
    PLAYBACK_QUALITY,
    FULLSCREEN_GESTURE,
    INTERACTION_COMMENT,
    DATA_BACKUP,
    PRIVACY_PERMISSION,
    DIAGNOSTICS,
    ABOUT_SUPPORT,
    APPEARANCE,
    ANIMATION,
    PLAYBACK,
    BOTTOM_BAR,
    PERMISSION,
    BLOCKED_LIST,
    SETTINGS_SHARE,
    WEBDAV_BACKUP,
    DOWNLOAD_PATH,
    IMAGE_SAVE_PATH,
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
    val section: String,
    val focusId: String? = null
)

private data class SettingsSearchEntry(
    val target: SettingsSearchTarget,
    val title: String,
    val subtitle: String,
    val section: String,
    val aliases: List<String>,
    val focusId: String? = null
)

private val SETTINGS_SEARCH_INDEX: List<SettingsSearchEntry> = listOf(
    SettingsSearchEntry(
        target = SettingsSearchTarget.INTERFACE_THEME,
        title = "界面与主题",
        subtitle = "UI 预设、主题、字体、DPI、动态图标与开屏",
        section = "设置",
        aliases = listOf("界面", "主题", "ui预设", "md3", "miuix", "字体", "dpi", "动态图标", "应用图标", "开屏", "开屏壁纸")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.HOME_FEED,
        title = "首页与推荐",
        subtitle = "首页展示、推荐流、刷新数量、动态栏位、首页壁纸与底栏搜索入口",
        section = "设置",
        aliases = listOf("首页", "推荐", "推荐流", "首页展示", "首页壁纸", "壁纸效果", "刷新数量", "动态栏位", "底栏搜索入口", "搜索入口")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.NAVIGATION,
        title = "导航与标签",
        subtitle = "底栏、顶部标签、平板侧边栏与底栏项目顺序",
        section = "设置",
        aliases = listOf("导航", "底栏", "底部栏", "顶部标签", "顶部标签页", "标签排序", "平板侧边栏", "侧边导航栏", "底栏顺序", "底栏项目")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.PLAYBACK_QUALITY,
        title = "播放与画质",
        subtitle = "解码、画质、字幕、倍速、连播与网络策略",
        section = "设置",
        aliases = listOf("播放", "解码", "画质", "默认画质", "最高画质", "自动最高画质", "省流量", "定向流量", "字幕", "倍速", "自动连播")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.FULLSCREEN_GESTURE,
        title = "全屏与手势",
        subtitle = "全屏方向、截图、锁定按钮、亮度、音量与进度手势",
        section = "设置",
        aliases = listOf("全屏", "全屏方向", "自动横竖屏", "锁定按钮", "截图", "截图按钮", "应用内截图", "应用内干净截图", "手选区域", "区域截图", "三指截图", "亮度", "音量", "进度手势", "手势")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.INTERACTION_COMMENT,
        title = "互动与评论",
        subtitle = "评论发送检测、评论装扮、AI 总结、双击点赞、视频简介与笔记",
        section = "设置",
        aliases = listOf("互动", "评论", "楼中楼", "评论楼中楼", "评论检测", "发评反诈", "评论发送检测", "评论装扮", "个性装扮", "ai总结", "视频总结", "双击点赞", "视频简介", "简介默认展开", "视频笔记", "显示视频笔记", "默认折叠视频笔记", "笔记折叠")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.DATA_BACKUP,
        title = "数据与备份",
        subtitle = "设置分享、WebDAV、下载位置与清除缓存",
        section = "设置",
        aliases = listOf("数据", "备份", "设置分享", "webdav", "云备份", "下载位置", "下载目录", "清除缓存", "清缓存", "缓存")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.PRIVACY_PERMISSION,
        title = "隐私与权限",
        subtitle = "隐私无痕、权限管理与黑名单",
        section = "设置",
        aliases = listOf("隐私", "无痕", "权限", "权限管理", "黑名单", "屏蔽", "拉黑")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.DIAGNOSTICS,
        title = "诊断与开发",
        subtitle = "崩溃追踪、统计、播放器诊断日志、画质降档弹窗、插件与导出日志",
        section = "设置",
        aliases = listOf("诊断", "开发", "崩溃追踪", "使用情况统计", "播放器诊断日志", "画质降档诊断弹窗", "降档弹窗", "仅提示一次", "仅弹窗一次", "插件", "导出日志", "日志")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.ABOUT_SUPPORT,
        title = "关于与支持",
        subtitle = "版本、更新、开源、发布渠道、小贴士、默认打开链接、社群与捐赠",
        section = "设置",
        aliases = listOf("关于", "支持", "版本", "更新", "开源", "发布渠道", "小贴士", "默认打开链接", "telegram", "twitter", "捐赠", "打赏")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.APPEARANCE,
        title = "外观设置",
        subtitle = "主题、图标、动画效果",
        section = "常规",
        aliases = listOf(
            "外观",
            "主题",
            "图标",
            "动画",
            "动画与效果",
            "过渡动画",
            "进场动画",
            "触感反馈",
            "震动",
            "haptic",
            "底栏搜索",
            "底栏搜索入口",
            "搜索入口",
            "悬浮搜索",
            "模糊",
            "皮肤",
            "玻璃",
            "液态玻璃",
            "安卓原生液态玻璃",
            "Android Native 液态玻璃",
            "全局液态玻璃",
            "评论区液态玻璃",
            "毛玻璃",
            "推荐流卡片宽度",
            "卡片宽度",
            "首页卡片宽度",
            "动态取色",
            "自定义md3颜色",
            "自定义 MD3 颜色",
            "自定义颜色",
            "md3颜色",
            "主题色",
            "hex",
            "material you",
            "materialyou",
            "动态颜色",
            "语言",
            "字体",
            "字体大小",
            "应用字体",
            "本地字体",
            "导入字体",
            "自定义字体",
            "ttf",
            "otf",
            "界面缩放",
            "dpi",
            "开屏",
            "开屏壁纸",
            "自定义壁纸",
            "相册壁纸",
            "首页壁纸",
            "首页壁纸效果",
            "随机壁纸",
            "开屏图标遮罩动画",
            "图标遮罩动画",
            "显示开屏图标",
            "隐藏开屏图标",
            "开屏图标动画",
            "应用图标",
            "统计信息贴封面",
            "up主标识",
            "up标识",
            "UP主标识",
            "UP标识",
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
        aliases = listOf(
            "播放",
            "解码",
            "硬件解码",
            "编码",
            "avc",
            "hevc",
            "播放速度",
            "倍速",
            "默认播放速度",
            "记忆上次播放速度",
            "续播",
            "续播弹窗",
            "自动连播",
            "自动播放下一个",
            "连续播放",
            "列表连续播放",
            "收藏夹连续播放",
            "播放顺序",
            "随机播放",
            "顺序播放",
            "后台播放",
            "后台播放模式",
            "离开播放页后停止",
            "停止播放",
            "音频焦点",
            "听视频",
            "画中画",
            "pip",
            "小窗",
            "自动进入画中画",
            "自动进入全屏",
            "自动退出全屏",
            "全屏",
            "全屏方向",
            "固定全屏比例",
            "横屏适配",
            "平板评论区宽度",
            "评论区宽度",
            "楼中楼",
            "评论楼中楼",
            "评论检测",
            "发评反诈",
            "评论发送检测",
            "评论装扮",
            "个性装扮",
            "评论区个性装扮",
            "图片长按保存",
            "长按保存图片",
            "查看图片保存",
            "播放页隐藏状态栏",
            "隐藏状态栏",
            "状态栏",
            "自动横竖屏",
            "自动旋转",
            "全屏手势反向",
            "锁定按钮",
            "截图按钮",
            "应用内干净截图",
            "应用内截图",
            "三指下滑截图",
            "右上角双指长按",
            "截图触发方式",
            "截图范围",
            "手选区域",
            "区域截图",
            "全屏显示时间",
            "全屏显示电量",
            "互动按钮",
            "观看人数",
            "底部进度条",
            "播放器缩小策略",
            "上滑隐藏播放器",
            "暂停时缩小",
            "暂停评论缩小",
            "竖屏上滑进入全屏",
            "中部滑动切换全屏",
            "亮度",
            "音量",
            "系统亮度",
            "左右侧滑动",
            "双击点赞",
            "ai总结",
            "ai 总结",
            "视频总结",
            "总结",
            "字幕",
            "自动启用字幕",
            "最高画质",
            "默认画质",
            "无线网络默认画质",
            "流量默认画质",
            "省流量模式",
            "定向流量",
            "b站定向流量",
            "详细统计信息",
            "播放器诊断日志",
            "画质降档诊断弹窗",
            "降档弹窗",
            "高画质不可用弹窗",
            "仅提示一次",
            "仅弹窗一次",
            "点击视频直接播放",
            "视频简介",
            "默认展开视频简介",
            "简介默认展开",
            "手势"
        )
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.BOTTOM_BAR,
        title = "导航设置",
        subtitle = "底栏、顶部标签、平板侧边栏",
        section = "常规",
        aliases = listOf(
            "导航",
            "导航设置",
            "底栏",
            "标签栏",
            "导航栏",
            "tab",
            "顶部标签",
            "顶部标签页",
            "侧边导航栏",
            "侧边栏",
            "平板导航",
            "底部导航",
            "底部栏",
            "底栏顺序",
            "底栏图标",
            "底栏文字",
            "底栏项目",
            "底栏隐藏",
            "底栏显示",
            "悬浮底栏"
        )
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.PERMISSION,
        title = "权限管理",
        subtitle = "查看应用权限",
        section = "隐私与安全",
        aliases = listOf("权限", "存储权限", "通知权限", "相册权限", "文件权限", "系统设置权限")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.BLOCKED_LIST,
        title = "黑名单管理",
        subtitle = "管理已屏蔽的 UP 主",
        section = "隐私与安全",
        aliases = listOf("黑名单", "屏蔽", "up", "拉黑", "屏蔽up", "已屏蔽up", "屏蔽用户")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.SETTINGS_SHARE,
        title = "设置分享",
        subtitle = "导出并导入可分享设置",
        section = "数据与存储",
        aliases = listOf("设置分享", "分享设置", "导入", "导出", "json", "配置分享", "设置包", "备份设置", "恢复设置")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.WEBDAV_BACKUP,
        title = "WebDAV 云备份",
        subtitle = "备份与恢复设置/插件",
        section = "数据与存储",
        aliases = listOf("webdav", "云备份", "备份", "恢复", "自动备份", "测试连接", "远端目录", "服务器", "用户名")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.DOWNLOAD_PATH,
        title = "下载位置",
        subtitle = "设置导出目录",
        section = "数据与存储",
        aliases = listOf("下载", "目录", "路径", "导出目录", "下载目录", "存储位置", "文件夹")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.IMAGE_SAVE_PATH,
        title = "图片保存位置",
        subtitle = "选择动态图片、头像和评论图片保存目录",
        section = "数据与存储",
        aliases = listOf("图片保存", "保存目录", "保存位置", "相册", "图片目录", "图片文件夹", "动态图片", "头像保存", "bili")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.CLEAR_CACHE,
        title = "清除缓存",
        subtitle = "清理应用缓存",
        section = "数据与存储",
        aliases = listOf("缓存", "清理", "释放空间", "清缓存", "删除缓存", "空间清理")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.PLUGINS,
        title = "插件中心",
        subtitle = "管理扩展插件",
        section = "开发者选项",
        aliases = listOf("插件", "扩展", "json", "脚本", "规则", "屏蔽规则")
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.EXPORT_LOGS,
        title = "导出日志",
        subtitle = "用于反馈问题",
        section = "开发者选项",
        aliases = listOf("日志", "log", "反馈", "诊断", "导出log", "播放器日志", "问题反馈")
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
        aliases = listOf("更新", "升级", "新版本", "检查", "自动检查更新", "版本更新")
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
        aliases = listOf("贴士", "技巧", "帮助", "隐藏操作", "摸鱼模式", "空降助手", "自动连播", "自动横竖屏")
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
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.APPEARANCE,
        title = "自定义 MD3 颜色",
        subtitle = "HEX、HSV 取色器和预设主题色",
        section = "外观设置",
        aliases = listOf("自定义md3颜色", "自定义颜色", "md3颜色", "主题色", "hex", "material you"),
        focusId = SettingsSearchFocusIds.APPEARANCE_THEME
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.APPEARANCE,
        title = "界面预设 / 主题模式",
        subtitle = "iOS、安卓原生、深色风格、MD3 颜色来源、应用语言",
        section = "外观设置",
        aliases = listOf("界面预设", "主题模式", "深色风格", "应用语言", "语言", "material you", "动态取色", "自定义md3颜色", "自定义 MD3 颜色", "自定义颜色", "md3颜色", "主题色", "hex"),
        focusId = SettingsSearchFocusIds.APPEARANCE_THEME
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.APPEARANCE,
        title = "安卓原生液态玻璃",
        subtitle = "全局启用顶部、底栏和评论区控件",
        section = "外观设置",
        aliases = listOf("全局液态玻璃", "评论区液态玻璃", "Android Native 液态玻璃"),
        focusId = SettingsSearchFocusIds.APPEARANCE_THEME
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.APPEARANCE,
        title = "字体大小 / 界面缩放 / DPI",
        subtitle = "显示与排版",
        section = "外观设置",
        aliases = listOf("字体大小", "界面缩放", "dpi", "显示与排版", "应用内dpi", "缩放"),
        focusId = SettingsSearchFocusIds.APPEARANCE_DISPLAY
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.APPEARANCE,
        title = "开屏壁纸 / 启动画面",
        subtitle = "开屏壁纸、自定义壁纸、随机壁纸、图标遮罩动画",
        section = "外观设置",
        aliases = listOf("开屏壁纸", "自定义壁纸", "相册壁纸", "启动画面", "随机壁纸", "开屏图标遮罩动画", "图标遮罩动画", "显示开屏图标", "隐藏开屏图标", "开屏图标动画", "启动壁纸"),
        focusId = SettingsSearchFocusIds.APPEARANCE_SPLASH
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.ANIMATION,
        title = "动画与效果 / 触感反馈",
        subtitle = "动画、触感反馈、底栏搜索入口",
        section = "外观设置",
        aliases = listOf("动画与效果", "触感反馈", "动画设置", "应用图标", "底栏搜索", "底栏搜索入口", "搜索入口", "悬浮搜索"),
        focusId = SettingsSearchFocusIds.ANIMATION_VISUAL_EFFECTS
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.APPEARANCE,
        title = "首页展示",
        subtitle = "展示样式、首页壁纸效果、推荐流卡片宽度",
        section = "外观设置",
        aliases = listOf("首页展示", "展示样式", "首页壁纸", "首页壁纸效果", "原图壁纸", "壁纸模糊", "强模糊", "推荐流卡片宽度", "首页卡片宽度", "卡片宽度", "统计信息贴封面", "UP主标识", "UP标识", "up主标识", "up标识"),
        focusId = SettingsSearchFocusIds.APPEARANCE_HOME
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.PLAYBACK,
        title = "硬件解码 / 编码偏好",
        subtitle = "解码",
        section = "播放设置",
        aliases = listOf("硬件解码", "首选编码", "次选编码", "hevc", "avc", "av1", "解码"),
        focusId = SettingsSearchFocusIds.PLAYBACK_DECODER
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.PLAYBACK,
        title = "播放速度",
        subtitle = "默认播放速度、记忆上次播放速度",
        section = "播放设置",
        aliases = listOf("播放速度", "倍速", "默认播放速度", "记忆上次播放速度"),
        focusId = SettingsSearchFocusIds.PLAYBACK_SPEED
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.PLAYBACK,
        title = "后台播放 / 画中画 / 小窗",
        subtitle = "小窗播放",
        section = "播放设置",
        aliases = listOf("后台播放", "画中画", "pip", "小窗", "小窗画中画", "音频焦点", "自动进入画中画", "离开播放页后停止"),
        focusId = SettingsSearchFocusIds.PLAYBACK_MINI_PLAYER
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.PLAYBACK,
        title = "手势灵敏度",
        subtitle = "手势控制",
        section = "播放设置",
        aliases = listOf("手势灵敏度", "手势控制", "灵敏度"),
        focusId = SettingsSearchFocusIds.PLAYBACK_GESTURE
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.PLAYBACK,
        title = "自动连播 / 双击点赞 / 双击跳转 / 弹幕屏蔽 / 字幕 / 笔记",
        subtitle = "交互",
        section = "播放设置",
        aliases = listOf("自动连播", "自动播放下一个", "进入视频自动播放", "进入视频不要自动播放", "不要自动播放", "双击点赞", "双击跳转", "取消双击跳转", "关闭双击跳转", "双击快进", "双击后退", "快进秒数", "后退秒数", "关注点赞弹幕", "关注弹幕", "点赞弹幕", "三连弹幕", "弹幕屏蔽", "字幕", "自动启用字幕", "ai总结", "视频简介", "默认展开视频简介", "简介默认展开", "视频笔记", "显示视频笔记", "默认折叠视频笔记", "笔记折叠", "播放器缩小策略", "竖屏视频缩小", "竖屏评论区缩小", "评论上滑缩小播放器", "横屏视频缩小", "上滑隐藏播放器", "暂停时缩小", "暂停评论缩小", "点击视频直接播放"),
        focusId = SettingsSearchFocusIds.PLAYBACK_INTERACTION
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.PLAYBACK,
        title = "自动横竖屏 / 全屏方向 / 平板布局",
        subtitle = "交互",
        section = "播放设置",
        aliases = listOf("自动横竖屏", "自动旋转", "全屏方向", "固定全屏比例", "全屏手势反向", "自动进入全屏", "自动退出全屏", "横屏适配", "平板评论区宽度", "评论区宽度", "评论折叠数量", "评论回复预览", "评论预览数量", "楼中楼", "评论楼中楼", "评论检测", "发评反诈", "评论发送检测", "评论装扮", "个性装扮", "评论区个性装扮", "图片长按保存", "长按保存图片", "查看图片保存", "播放页隐藏状态栏", "隐藏状态栏", "状态栏"),
        focusId = SettingsSearchFocusIds.PLAYBACK_FULLSCREEN
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.PLAYBACK,
        title = "网络与画质",
        subtitle = "自动最高画质、默认画质、定向流量",
        section = "播放设置",
        aliases = listOf("网络与画质", "自动最高画质", "默认画质", "无线网络默认画质", "流量默认画质", "定向流量", "b站定向流量"),
        focusId = SettingsSearchFocusIds.PLAYBACK_NETWORK
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.PLAYBACK,
        title = "省流量模式",
        subtitle = "省流量",
        section = "播放设置",
        aliases = listOf("省流量", "省流量模式", "节省流量"),
        focusId = SettingsSearchFocusIds.PLAYBACK_DATA_SAVER
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.PLAYBACK,
        title = "播放器诊断 / 统计信息",
        subtitle = "调试",
        section = "播放设置",
        aliases = listOf("播放器诊断日志", "详细统计信息", "调试", "日志"),
        focusId = SettingsSearchFocusIds.PLAYBACK_DEBUG
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.BOTTOM_BAR,
        title = "底栏显示模式 / 标签样式",
        subtitle = "底部导航、标签显示",
        section = "导航设置",
        aliases = listOf(
            "显示模式",
            "标签样式",
            "底栏显示模式",
            "底栏标签样式"
        ),
        focusId = SettingsSearchFocusIds.BOTTOM_BAR_DISPLAY
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.ANIMATION,
        title = "顶部 Dock / 搜索框 / 底栏液态玻璃",
        subtitle = "顶部 Dock、首页搜索框、底栏材质",
        section = "动画与效果",
        aliases = listOf(
            "顶部 Dock 液态玻璃",
            "顶部dock栏液态玻璃",
            "顶部玻璃效果",
            "首页搜索框液态玻璃",
            "首页搜索液态玻璃",
            "搜索框玻璃效果",
            "底栏玻璃效果",
            "底栏液态玻璃",
            "BiliPai 调校"
        ),
        focusId = SettingsSearchFocusIds.ANIMATION_VISUAL_EFFECTS
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.BOTTOM_BAR,
        title = "顶部标签管理",
        subtitle = "显示/隐藏、排序、右上角入口",
        section = "导航设置",
        aliases = listOf(
            "顶部标签",
            "顶部标签样式",
            "顶部模糊",
            "顶部标签管理",
            "标签排序",
            "标签显示",
            "推荐分类",
            "直播标签",
            "首页右上角",
            "首页右上角入口",
            "首页右上角消息",
            "消息入口",
            "设置图标",
            "右上角设置",
            "右上角消息"
        ),
        focusId = SettingsSearchFocusIds.BOTTOM_BAR_TOP_TABS
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.BOTTOM_BAR,
        title = "平板侧边导航栏",
        subtitle = "平板导航",
        section = "导航设置",
        aliases = listOf("平板布局", "平板导航", "侧边导航栏", "侧边栏"),
        focusId = SettingsSearchFocusIds.BOTTOM_BAR_TABLET
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.BOTTOM_BAR,
        title = "当前底栏预览",
        subtitle = "底栏顺序预览",
        section = "导航设置",
        aliases = listOf("当前底栏", "底栏预览", "底栏顺序"),
        focusId = SettingsSearchFocusIds.BOTTOM_BAR_CURRENT
    ),
    SettingsSearchEntry(
        target = SettingsSearchTarget.BOTTOM_BAR,
        title = "可用底栏项目",
        subtitle = "显示/隐藏底栏项目",
        section = "导航设置",
        aliases = listOf("可用项目", "底栏项目", "显示隐藏项目", "底栏图标", "底栏文字"),
        focusId = SettingsSearchFocusIds.BOTTOM_BAR_AVAILABLE
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
                    section = entry.section,
                    focusId = entry.focusId
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
    return value
        .trim()
        .lowercase()
        .replace(Regex("[\\s/&+\\-_:：·()（）]+"), "")
}

private fun matchesSettingsSearchPinyin(value: String, query: String): Boolean {
    return PinyinUtils.matches(
        text = value.replace(" ", ""),
        query = query
    )
}
