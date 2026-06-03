package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class MobileSettingsRootSectionOrderTest {

    @Test
    fun shouldUseSceneBasedOrderForSettingsHome() {
        assertEquals(
            resolveSettingsRootCategoryOrder(),
            resolveTabletSettingsRootCategoryOrder()
        )
    }

    @Test
    fun rootSections_shouldUseSceneTitles() {
        assertEquals(
            listOf(
                "关注与支持",
                "播放与画质",
                "界面与主题",
                "首页与推荐",
                "导航与标签",
                "全屏与手势",
                "互动与评论",
                "数据与备份",
                "隐私与权限",
                "诊断与开发",
                "关于与支持"
            ),
            resolveSettingsRootCategoryOrder().map { it.title }
        )
    }
}
