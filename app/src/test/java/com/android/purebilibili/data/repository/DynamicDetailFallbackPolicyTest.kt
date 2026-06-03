package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.DynamicAuthorModule
import com.android.purebilibili.data.model.response.DynamicContentModule
import com.android.purebilibili.data.model.response.DynamicDesc
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicMajor
import com.android.purebilibili.data.model.response.DynamicModules
import com.android.purebilibili.data.model.response.OpusMajor
import com.android.purebilibili.data.model.response.OpusSummary
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class DynamicDetailFallbackPolicyTest {

    @Test
    fun shouldFallback_returnsTrue_whenAuthorAndContentMissing() {
        val item = DynamicItem(modules = DynamicModules())
        assertTrue(shouldFallbackForDynamicDetail(item))
    }

    @Test
    fun shouldFallback_returnsFalse_whenDescTextExists() {
        val item = DynamicItem(
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    desc = DynamicDesc(text = "text")
                )
            )
        )
        assertFalse(shouldFallbackForDynamicDetail(item))
    }

    @Test
    fun shouldFallback_returnsTrue_whenOnlyAuthorExists() {
        val item = DynamicItem(
            modules = DynamicModules(
                module_author = DynamicAuthorModule(mid = 1, name = "author")
            )
        )
        assertTrue(shouldFallbackForDynamicDetail(item))
    }

    @Test
    fun shouldFetchOpusDetail_returnsTrueForPreviewOnlyOpusMajor() {
        val item = DynamicItem(
            id_str = "1201902028962398230",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    desc = DynamicDesc(text = "预览摘要"),
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_OPUS",
                        opus = OpusMajor(summary = OpusSummary(text = "预览摘要"))
                    )
                )
            )
        )

        assertTrue(shouldFetchOpusDetailForDynamicDetail(item))
    }

    @Test
    fun shouldFetchOpusDetail_returnsFalseForOrdinaryTextDynamic() {
        val item = DynamicItem(
            id_str = "987654321",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    desc = DynamicDesc(text = "普通动态正文")
                )
            )
        )

        assertFalse(shouldFetchOpusDetailForDynamicDetail(item))
    }
}
