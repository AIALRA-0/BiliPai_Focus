package com.android.purebilibili.navigation

import com.android.purebilibili.core.util.BilibiliNavigationTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SearchSubmitPolicyTest {

    @Test
    fun `search submit opens native video for raw bv`() {
        val action = resolveSearchSubmitAction("BV1xx411c7mD")

        val target = assertIs<SearchSubmitAction.OpenNativeTarget>(action).target
        assertEquals(BilibiliNavigationTarget.Video("BV1xx411c7mD"), target)
    }

    @Test
    fun `search submit opens native video for bilibili link`() {
        val action = resolveSearchSubmitAction("https://www.bilibili.com/video/BV1xx411c7mD")

        val target = assertIs<SearchSubmitAction.OpenNativeTarget>(action).target
        assertEquals(BilibiliNavigationTarget.Video("BV1xx411c7mD"), target)
    }

    @Test
    fun `search submit keeps normal text as search keyword`() {
        val action = assertIs<SearchSubmitAction.OpenSearch>(resolveSearchSubmitAction("  猫和老鼠  "))

        assertEquals("猫和老鼠", action.keyword)
    }

    @Test
    fun `search submit unwraps search url keyword`() {
        val action = assertIs<SearchSubmitAction.OpenSearch>(
            resolveSearchSubmitAction("https://search.bilibili.com/all?keyword=%E7%8C%AB")
        )

        assertEquals("猫", action.keyword)
    }
}
