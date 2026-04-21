package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.data.model.response.ArchiveMajor
import com.android.purebilibili.data.model.response.DynamicAuthorModule
import com.android.purebilibili.data.model.response.DynamicBasic
import com.android.purebilibili.data.model.response.DynamicContentModule
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicMajor
import com.android.purebilibili.data.model.response.DynamicModules
import com.android.purebilibili.data.model.response.LiveRcmdMajor
import com.android.purebilibili.data.model.response.OpusMajor
import com.android.purebilibili.data.model.response.UgcSeasonMajor
import com.android.purebilibili.feature.dynamic.components.DynamicCardMediaAction
import com.android.purebilibili.feature.dynamic.components.DynamicCardPrimaryAction
import com.android.purebilibili.feature.dynamic.components.resolveDynamicCardMediaAction
import com.android.purebilibili.feature.dynamic.components.resolveDynamicCardPrimaryAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class DynamicInteractionPolicyTest {

    @Test
    fun `video tab includes ugc season dynamics`() {
        val item = DynamicItem(type = "DYNAMIC_TYPE_UGC_SEASON")

        assertTrue(shouldIncludeDynamicItemInVideoTab(item))
    }

    @Test
    fun `video tab excludes pgc dynamics after server side tab parity change`() {
        val item = DynamicItem(type = "DYNAMIC_TYPE_PGC")

        assertFalse(shouldIncludeDynamicItemInVideoTab(item))
        assertTrue(shouldIncludeDynamicItemInPgcTab(item))
    }

    @Test
    fun `video tab excludes word dynamics`() {
        val item = DynamicItem(type = "DYNAMIC_TYPE_WORD")

        assertFalse(shouldIncludeDynamicItemInVideoTab(item))
    }

    @Test
    fun `resolve dynamic comment target prefers basic fields for video dynamics`() {
        val item = DynamicItem(
            id_str = "966281785469042740",
            type = "DYNAMIC_TYPE_AV",
            basic = DynamicBasic(
                comment_id_str = "1129813966",
                comment_type = 1,
                rid_str = "0"
            )
        )

        val target = resolveDynamicCommentTarget(item)

        assertEquals(DynamicCommentTarget(oid = 1129813966L, type = 1), target)
    }

    @Test
    fun `resolve dynamic comment target falls back to ugc season aid`() {
        val item = DynamicItem(
            id_str = "966281785469042740",
            type = "DYNAMIC_TYPE_UGC_SEASON",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_UGC_SEASON",
                        ugc_season = UgcSeasonMajor(aid = 1456253104L)
                    )
                )
            )
        )

        val target = resolveDynamicCommentTarget(item)

        assertEquals(DynamicCommentTarget(oid = 1456253104L, type = 1), target)
    }

    @Test
    fun `resolve dynamic comment target uses article type from basic`() {
        val item = DynamicItem(
            id_str = "718372214316990512",
            type = "DYNAMIC_TYPE_ARTICLE",
            basic = DynamicBasic(
                comment_id_str = "37231101",
                comment_type = 12,
                rid_str = "37231101"
            )
        )

        val target = resolveDynamicCommentTarget(item)

        assertEquals(DynamicCommentTarget(oid = 37231101L, type = 12), target)
    }

    @Test
    fun `resolve dynamic comment target falls back to archive aid for av`() {
        val item = DynamicItem(
            id_str = "123",
            type = "DYNAMIC_TYPE_AV",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_ARCHIVE",
                        archive = ArchiveMajor(aid = "1756441068")
                    )
                )
            )
        )

        val target = resolveDynamicCommentTarget(item)

        assertEquals(DynamicCommentTarget(oid = 1756441068L, type = 1), target)
    }

    @Test
    fun `resolve dynamic comment target prefers dynamic id for opus detail even when basic points elsewhere`() {
        val item = DynamicItem(
            id_str = "967717348014293017",
            type = "DYNAMIC_TYPE_DRAW",
            basic = DynamicBasic(
                comment_id_str = "326122895",
                comment_type = 11,
                rid_str = "326122895"
            ),
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_OPUS"
                    )
                )
            )
        )

        val target = resolveDynamicCommentTarget(item)

        assertEquals(DynamicCommentTarget(oid = 967717348014293017L, type = 17), target)
    }

    @Test
    fun `resolve dynamic comment targets keeps desktop dynamic target before legacy basic fallback`() {
        val item = DynamicItem(
            id_str = "967717348014293017",
            type = "DYNAMIC_TYPE_DRAW",
            basic = DynamicBasic(
                comment_id_str = "326122895",
                comment_type = 11,
                rid_str = "326122895"
            ),
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(type = "MAJOR_TYPE_OPUS")
                )
            )
        )

        val targets = resolveDynamicCommentTargets(item)

        assertEquals(
            listOf(
                DynamicCommentTarget(oid = 967717348014293017L, type = 17),
                DynamicCommentTarget(oid = 326122895L, type = 11)
            ),
            targets
        )
    }

    @Test
    fun `resolve primary action prefers video even when forwarded`() {
        val forwarded = DynamicItem(
            type = "DYNAMIC_TYPE_FORWARD",
            orig = DynamicItem(
                id_str = "orig",
                modules = DynamicModules(
                    module_dynamic = DynamicContentModule(
                        major = DynamicMajor(
                            archive = ArchiveMajor(bvid = "BV1xx411c7mD")
                        )
                    )
                )
            )
        )

        val action = resolveDynamicCardPrimaryAction(forwarded)

        val videoAction = assertIs<DynamicCardPrimaryAction.OpenVideo>(action)
        assertEquals("BV1xx411c7mD", videoAction.bvid)
    }

    @Test
    fun `resolve primary action supports pgc dynamic major`() {
        val item = DynamicItem(
            type = "DYNAMIC_TYPE_PGC",
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        type = "MAJOR_TYPE_PGC",
                        pgc = ArchiveMajor(
                            bvid = "BV1pgc123456",
                            aid = "11223344",
                            epid = 987654L,
                            season_id = 123456L
                        )
                    )
                )
            )
        )

        val action = resolveDynamicCardPrimaryAction(item)

        val bangumiAction = assertIs<DynamicCardPrimaryAction.OpenBangumi>(action)
        assertEquals(123456L, bangumiAction.seasonId)
        assertEquals(987654L, bangumiAction.epId)
    }

    @Test
    fun `resolve primary action returns live when content present`() {
        val liveJson = """{"live_play_info":{"room_id":123,"title":"Live","uid":456}}"""
        val item = DynamicItem(
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        live_rcmd = LiveRcmdMajor(content = liveJson)
                    )
                )
            )
        )

        val action = resolveDynamicCardPrimaryAction(item)

        val live = assertIs<DynamicCardPrimaryAction.OpenLive>(action)
        assertEquals(123L, live.roomId)
        assertEquals("Live", live.title)
        assertEquals("456", live.uname)
    }

    @Test
    fun `resolve primary action falls back to dynamic detail when no video`() {
        val action = resolveDynamicCardPrimaryAction(buildDynamicItem("123"))

        val detailAction = assertIs<DynamicCardPrimaryAction.OpenDynamicDetail>(action)
        assertEquals("123", detailAction.dynamicId)
    }

    @Test
    fun `resolve primary action opens author when nothing else`() {
        val action = resolveDynamicCardPrimaryAction(
            DynamicItem(
                modules = DynamicModules(
                    module_author = DynamicAuthorModule(mid = 987654321L)
                )
            )
        )

        val userAction = assertIs<DynamicCardPrimaryAction.OpenUser>(action)
        assertEquals(987654321L, userAction.mid)
    }

    @Test
    fun `resolve media action previews draw images`() {
        val item = DynamicItem(
            modules = DynamicModules(
                module_dynamic = DynamicContentModule(
                    major = DynamicMajor(
                        draw = com.android.purebilibili.data.model.response.DrawMajor(
                            items = listOf(
                                com.android.purebilibili.data.model.response.DrawItem(src = "a"),
                                com.android.purebilibili.data.model.response.DrawItem(src = "b")
                            )
                        )
                    )
                )
            )
        )

        val action = resolveDynamicCardMediaAction(item, clickedIndex = 1)

        assertTrue(action is DynamicCardMediaAction.PreviewImages)
        assertEquals(listOf("a", "b"), action.images)
        assertEquals(1, action.initialIndex)
    }

    @Test
    fun `resolve media action previews opus images from forwarded card`() {
        val item = DynamicItem(
            type = "DYNAMIC_TYPE_FORWARD",
            orig = DynamicItem(
                modules = DynamicModules(
                    module_dynamic = DynamicContentModule(
                        major = DynamicMajor(
                            opus = OpusMajor(
                                pics = listOf(
                                    com.android.purebilibili.data.model.response.OpusPic(url = "x"),
                                    com.android.purebilibili.data.model.response.OpusPic(url = "y")
                                )
                            )
                        )
                    )
                )
            )
        )

        val action = resolveDynamicCardMediaAction(item, clickedIndex = 0)

        assertTrue(action is DynamicCardMediaAction.PreviewImages)
        assertEquals(listOf("x", "y"), action.images)
        assertEquals(0, action.initialIndex)
    }

    @Test
    fun `like count policy returns copied items instead of mutating raw models`() {
        val original = buildDynamicItem("123").copy(
            modules = DynamicModules(
                module_stat = com.android.purebilibili.data.model.response.DynamicStatModule(
                    like = com.android.purebilibili.data.model.response.StatItem(count = 7)
                )
            )
        )

        val updated = applyDynamicLikeCountChange(
            items = listOf(original),
            dynamicId = "123",
            toLiked = true
        )

        assertNotSame(original, updated.first())
        assertEquals(7, original.modules.module_stat?.like?.count)
        assertEquals(8, updated.first().modules.module_stat?.like?.count)
    }
}

private fun buildDynamicItem(id: String) = DynamicItem(id_str = id)
