package com.android.purebilibili.feature.home

import com.android.purebilibili.data.model.response.ArchiveMajor
import com.android.purebilibili.data.model.response.DynamicAuthorModule
import com.android.purebilibili.data.model.response.DynamicContentModule
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicMajor
import com.android.purebilibili.data.model.response.DynamicModules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeFollowFeedMappingPolicyTest {

    @Test
    fun `resolve dynamic archive aid parses archive aid string`() {
        assertEquals(1456400345L, resolveDynamicArchiveAid(archiveAid = "1456400345", fallbackId = 0L))
    }

    @Test
    fun `resolve dynamic archive aid falls back to existing id when archive aid invalid`() {
        assertEquals(9988L, resolveDynamicArchiveAid(archiveAid = "", fallbackId = 9988L))
    }

    @Test
    fun `should include home follow dynamic in video feed only when archive bvid exists`() {
        assertTrue(shouldIncludeHomeFollowDynamicInVideoFeed("BV1xx411c7mD"))
        assertFalse(shouldIncludeHomeFollowDynamicInVideoFeed(""))
    }

    @Test
    fun `home follow dynamic video mapping should preserve author publish timestamp`() {
        val mapped = mapHomeFollowDynamicItemsToVideoItems(
            listOf(
                DynamicItem(
                    id_str = "dyn-1",
                    modules = DynamicModules(
                        module_author = DynamicAuthorModule(
                            mid = 42L,
                            name = "up-42",
                            pub_ts = 123456789L
                        ),
                        module_dynamic = DynamicContentModule(
                            major = DynamicMajor(
                                archive = ArchiveMajor(
                                    aid = "1001",
                                    bvid = "BV1xx411c7mD",
                                    title = "test-video",
                                    cover = "cover"
                                )
                            )
                        )
                    )
                )
            )
        )

        assertEquals(1, mapped.size)
        assertEquals(123456789L, mapped.first().pubdate)
    }
}
