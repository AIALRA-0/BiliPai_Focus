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
                "外观与主题",
                "播放与画质",
                "首页与推荐",
                "导航与交互",
                "隐私与权限",
                "存储与备份",
                "插件与扩展",
                "系统与关于",
            ),
            resolveSettingsRootCategoryOrder().map { it.title },
        )
    }
}
