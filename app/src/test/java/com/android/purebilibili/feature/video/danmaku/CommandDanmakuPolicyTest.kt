package com.android.purebilibili.feature.video.danmaku

import com.android.purebilibili.danmaku.parser.*
import com.android.purebilibili.data.model.response.GradeDanmakuSummary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommandDanmakuPolicyTest {

    @Test
    fun `build command danmaku with plain text content`() {
        val cmd = commandDm(
            command = "VIDEO_CONNECTION_MSG",
            content = "高能预警！"
        )

        val result = buildCommandDanmaku(cmd)

        assertNotNull(result)
        assertEquals("高能预警！", result.content)
        assertEquals(5000, result.durationMs)
    }

    @Test
    fun `extract display text from json content`() {
        val cmd = commandDm(
            content = """{"text":"这条是可读互动提示"}"""
        )

        val result = buildCommandDanmaku(cmd)

        assertNotNull(result)
        assertEquals("这条是可读互动提示", result.content)
    }

    @Test
    fun `build up command item from documented payload`() {
        val cmd = commandDm(
            command = "#UP#",
            content = "这个视频没有恰饭",
            extra = """{"icon":"https://example.com/up.jpg"}"""
        )

        val item = buildCommandDanmakuItem(cmd)

        assertNotNull(item)
        assertEquals(CommandDanmakuType.UP, item.type)
        assertEquals("这个视频没有恰饭", item.content)
        assertEquals(COMMAND_DANMAKU_OVERLAY_DURATION_MS, item.durationMs)
        assertEquals("https://example.com/up.jpg", item.iconUrl)
    }

    @Test
    fun `build link command item from documented payload`() {
        val cmd = commandDm(
            command = "#LINK#",
            content = "看看这个视频",
            extra = """{"aid":123,"bvid":"BV1xx411c7mD","title":"关联视频","icon":"https://example.com/link.png"}"""
        )

        val item = buildCommandDanmakuItem(cmd)

        assertNotNull(item)
        assertEquals(CommandDanmakuType.LINK, item.type)
        assertEquals(123L, item.linkAid)
        assertEquals("BV1xx411c7mD", item.linkBvid)
        assertEquals("关联视频", item.linkTitle)
        assertEquals(COMMAND_DANMAKU_OVERLAY_DURATION_MS, item.durationMs)
        assertEquals("https://example.com/link.png", item.iconUrl)
    }

    @Test
    fun `build text command item uses three second overlay duration`() {
        val cmd = commandDm(
            command = "VIDEO_VOTE_MSG",
            content = "投票提示"
        )

        val item = buildCommandDanmakuItem(cmd)

        assertNotNull(item)
        assertEquals(CommandDanmakuType.TEXT, item.type)
        assertEquals("投票提示", item.content)
        assertEquals(COMMAND_DANMAKU_OVERLAY_DURATION_MS, item.durationMs)
    }

    @Test
    fun `build vote command item from structured payload`() {
        val cmd = commandDm(
            command = "VIDEO_VOTE_MSG",
            content = "投票提示",
            extra = """{"vote_id":123,"title":"你更喜欢哪个？","options":[{"id":1,"title":"选项A"},{"id":2,"title":"选项B"}]}"""
        )

        val item = buildCommandDanmakuItem(cmd)

        assertNotNull(item)
        assertEquals(CommandDanmakuType.VOTE, item.type)
        assertEquals(VoteDanmakuKind.VOTE, item.voteKind)
        assertEquals("123", item.voteId)
        assertEquals("你更喜欢哪个？", item.voteTitle)
        assertEquals(2, item.voteOptions.size)
        assertEquals("选项A", item.voteOptions[0].label)
        assertEquals("选项B", item.voteOptions[1].label)
    }

    @Test
    fun `build vote command from hash vote command`() {
        val cmd = commandDm(
            command = "#VOTE#",
            content = """{"id":"v1","question":"来投票","options":["甲","乙"]}"""
        )

        val item = buildCommandDanmakuItem(cmd)

        assertNotNull(item)
        assertEquals(CommandDanmakuType.VOTE, item.type)
        assertEquals("v1", item.voteId)
        assertEquals("来投票", item.voteTitle)
        assertEquals(listOf("甲", "乙"), item.voteOptions.map { it.label })
    }

    @Test
    fun `actual vote desc options stay inline with canonical one based indices`() {
        val item = buildCommandDanmakuItem(commandDm(
            command = "#VOTE#",
            extra = """{"vote_id":21517957,"question":"感觉兹白以前不高冷啊","my_vote":0,"options":[{"idx":1,"desc":"挺近人的仙人"},{"idx":2,"desc":"她超爱人好嘛"},{"idx":3,"desc":"蓝疯子太疯了"},{"idx":4,"desc":"总会遇到神人"}]}"""
        ))

        assertNotNull(item)
        assertEquals(listOf("挺近人的仙人", "她超爱人好嘛", "蓝疯子太疯了", "总会遇到神人"), item.voteOptions.map { it.label })
        assertEquals(listOf(1, 2, 3, 4), item.voteOptions.map { it.optionIndex })
        assertNull(item.voteSelectedIndex)
    }

    @Test
    fun `vote indices and restored selection do not depend on payload array order`() {
        val item = buildCommandDanmakuItem(commandDm(
            command = "#VOTE#",
            extra = """{"vote_id":21517957,"question":"来投票","my_vote":4,"options":[{"idx":4,"desc":"第四项"},{"idx":1,"desc":"第一项"}]}"""
        ))

        assertNotNull(item)
        assertEquals(listOf(4, 1), item.voteOptions.map { it.optionIndex })
        assertEquals(4, item.voteSelectedIndex)
        assertEquals("第四项", item.voteOptions.first { it.optionIndex == item.voteSelectedIndex }.label)
    }

    @Test
    fun `build grade command item with default score options`() {
        val cmd = commandDm(
            command = "#GRADE#",
            content = "打分提示",
            extra = """{"grade_id":456}"""
        )

        val item = buildCommandDanmakuItem(cmd)

        assertNotNull(item)
        assertEquals(CommandDanmakuType.VOTE, item.type)
        assertEquals(VoteDanmakuKind.GRADE, item.voteKind)
        assertEquals("456", item.voteId)
        // 默认 5 档分数：2/4/6/8/10
        assertEquals(listOf(2, 4, 6, 8, 10), item.voteOptions.map { it.score })
    }

    @Test
    fun `phone reference video grade exposes its question and 76 real participants`() {
        val item = buildCommandDanmakuItem(commandDm(
            command = "#GRADE#",
            progress = 12400,
            extra = """{"msg":"合着你们认识啊","grade_id":8593978,"mid_score":0,"count":76,"avg_score":10,"duration":5000,"summary_duration":6000,"posX":333.5,"posY":198.75,"posX_2":50,"posY_2":53}"""
        ))

        assertNotNull(item)
        assertEquals("合着你们认识啊", item.voteTitle)
        assertEquals("合着你们认识啊", item.content)
        assertEquals("8593978", item.voteId)
        assertEquals(12400L, item.startTimeMs)
        assertEquals(GradeDanmakuSummary(76L, 10.0), item.gradeSummary)
        assertEquals(listOf(2, 4, 6, 8, 10), item.voteOptions.map { it.score })
    }

    @Test
    fun `reference video cards follow server durations without grade and triple overlap`() {
        val vote = assertNotNull(buildCommandDanmakuItem(commandDm(
            command = "#VOTE#",
            extra = """{"vote_id":21517957,"question":"感觉兹白以前不高冷啊","duration":7000}""",
            progress = 0,
            id = 1L
        )))
        val grade = assertNotNull(buildCommandDanmakuItem(commandDm(
            command = "#GRADE#",
            extra = """{"grade_id":8593978,"msg":"合着你们认识啊","duration":5000,"summary_duration":6000}""",
            progress = 12400,
            id = 2L
        )))
        val triple = assertNotNull(buildCommandDanmakuItem(commandDm(
            command = "#ATTENTION#",
            extra = """{"type":2,"duration":5000}""",
            progress = 20000,
            id = 3L
        )))
        val cards = listOf(vote, grade, triple)

        fun visibleIds(positionMs: Long) = cards.filter { it.isActiveAt(positionMs) }.map { it.id }

        assertEquals(listOf(vote.id), visibleIds(6999))
        assertEquals(emptyList(), visibleIds(7000))
        assertEquals(listOf(grade.id), visibleIds(12400))
        assertEquals(listOf(grade.id), visibleIds(17399))
        assertEquals(emptyList(), visibleIds(17400))
        assertEquals(emptyList(), visibleIds(19999))
        assertEquals(listOf(triple.id), visibleIds(20000))
        assertEquals(listOf(triple.id), visibleIds(20400))
        assertEquals(listOf(triple.id), visibleIds(24000))
        assertEquals(emptyList(), visibleIds(25000))
        // Seeking back must use the same playback window, not retain a later card.
        assertEquals(listOf(grade.id), visibleIds(15000))
    }

    @Test
    fun `vote duration is also read from a content payload`() {
        val item = assertNotNull(buildCommandDanmakuItem(commandDm(
            command = "#VOTE#",
            content = """{"vote_id":123,"question":"来投票","duration":1500}""",
            progress = 0
        )))

        assertTrue(item.isActiveAt(1499))
        assertEquals(false, item.isActiveAt(1500))
    }

    @Test
    fun `missing and invalid interactive durations retain a finite selection window`() {
        for (command in listOf("#VOTE#", "#GRADE#")) {
            for (duration in listOf("", ""","duration":0""", ""","duration":-1""", ""","duration":"unknown"""")) {
                val item = assertNotNull(buildCommandDanmakuItem(commandDm(
                    command = command,
                    extra = """{"vote_id":123,"grade_id":456,"title":"来选择"$duration}""",
                    progress = 0
                )))

                assertTrue(item.isActiveAt(0))
                assertTrue(item.isActiveAt(VOTE_DANMAKU_OVERLAY_DURATION_MS - 1))
                assertEquals(false, item.isActiveAt(VOTE_DANMAKU_OVERLAY_DURATION_MS))
            }
        }
    }

    @Test
    fun `grade command preserves content title and restored personal score`() {
        val item = buildCommandDanmakuItem(commandDm(
            command = "#GRADE#",
            content = "大家喜欢吃甜品吗",
            extra = """{"grade_id":456,"count":139,"avg_score":9.8,"mid_score":8}"""
        ))

        assertNotNull(item)
        assertEquals("大家喜欢吃甜品吗", item.voteTitle)
        assertEquals("大家喜欢吃甜品吗", item.content)
        assertEquals(GradeDanmakuSummary(139L, 9.8, 8), item.gradeSummary)
    }

    @Test
    fun `grade summary keeps actual zeros without treating zero as a personal grade`() {
        val item = buildCommandDanmakuItem(commandDm(
            command = "#GRADE#",
            extra = """{"grade_id":456,"msg":"来打分","count":0,"avg_score":0,"mid_score":0}"""
        ))

        assertNotNull(item)
        assertEquals(GradeDanmakuSummary(0L, 0.0), item.gradeSummary)
    }

    @Test
    fun `grade summary leaves missing malformed and out of range statistics unavailable`() {
        val extras = listOf(
            """{"grade_id":456,"msg":"来打分"}""",
            """{"grade_id":456,"count":-1,"avg_score":10.1,"mid_score":3}""",
            """{"grade_id":456,"count":"unknown","avg_score":"NaN","mid_score":12}""",
            """{"grade_id":456,"count":null,"avg_score":"Infinity","mid_score":null}""",
            """{"grade_id":456,"count":1.5,"avg_score":-1,"mid_score":-2}"""
        )
        for (extra in extras) {
            val item = buildCommandDanmakuItem(commandDm(command = "#GRADE#", extra = extra))
            assertNotNull(item)
            assertEquals(GradeDanmakuSummary(), item.gradeSummary)
        }
    }

    @Test
    fun `grade command with string options keeps scores`() {
        val cmd = commandDm(
            command = "VIDEO_GRADE_MSG",
            extra = """{"grade_id":789,"title":"给这个视频打分","options":[{"id":1,"score":2},{"id":2,"score":4}]}"""
        )

        val item = buildCommandDanmakuItem(cmd)

        assertNotNull(item)
        assertEquals(CommandDanmakuType.VOTE, item.type)
        assertEquals(VoteDanmakuKind.GRADE, item.voteKind)
        assertEquals("789", item.voteId)
        assertEquals(2, item.voteOptions.size)
        assertEquals(2, item.voteOptions[0].score)
        assertEquals(4, item.voteOptions[1].score)
    }

    @Test
    fun `grade star mapping follows legal scores instead of payload order`() {
        val firstTwo = VoteOption(id = "two", label = "two", score = 2)
        val duplicateTwo = VoteOption(id = "duplicate-two", label = "duplicate", score = 2)
        val six = VoteOption(id = "six", label = "six", score = 6)
        val ten = VoteOption(id = "ten", label = "ten", score = 10)
        val invalid = VoteOption(id = "three", label = "three", score = 3)

        val stars = resolveGradeStarOptions(
            listOf(ten, invalid, six, duplicateTwo, firstTwo)
        )

        assertEquals(listOf(2, null, 6, null, 10), stars.map { it?.score })
        assertEquals(listOf("duplicate-two", null, "six", null, "ten"), stars.map { it?.id })
    }

    @Test
    fun `grade star mapping disables missing and out of range scores`() {
        val stars = resolveGradeStarOptions(
            listOf(
                VoteOption(id = "one", label = "one", score = 1),
                VoteOption(id = "odd", label = "odd", score = 7),
                VoteOption(id = "high", label = "high", score = 12),
                VoteOption(id = "negative", label = "negative", score = -2)
            )
        )

        assertTrue(stars.all { it == null })
    }

    @Test
    fun `vote command falls back to text when no structured payload`() {
        // 无 voteId/title/options 时保持原有文本提示行为
        val cmd = commandDm(
            command = "VIDEO_VOTE_MSG",
            content = "投票提示"
        )

        val item = buildCommandDanmakuItem(cmd)

        assertNotNull(item)
        assertEquals(CommandDanmakuType.TEXT, item.type)
        assertEquals("投票提示", item.content)
    }

    @Test
    fun `vote command does not render through legacy advanced danmaku`() {
        val cmd = commandDm(
            command = "VIDEO_VOTE_MSG",
            content = "投票提示",
            extra = """{"vote_id":1,"title":"投票","options":["A","B"]}"""
        )

        assertNull(buildCommandDanmaku(cmd))
    }

    @Test
    fun `combined triple reference remains visible at the supplied 24 second timestamp`() {
        val cmd = commandDm(
            command = "#ATTENTION#",
            content = "关注弹幕",
            extra = """{"duration":5000,"posX":346.84,"posY":202.5,"posX_2":52,"posY_2":54,"type":2}""",
            progress = 20000
        )

        val item = buildCommandDanmakuItem(cmd)
        assertNotNull(item)
        assertEquals(2, item.attentionType)
        assertEquals(0.52f, item.positionXRatio)
        assertEquals(0.54f, item.positionYRatio)
        assertTrue(item.isActiveAt(24000L))
        assertEquals(false, item.isActiveAt(25000L))
    }

    @Test
    fun `standalone triple reference preserves its edge position and five second lifetime`() {
        val item = buildCommandDanmakuItem(commandDm(
            command = "#ATTENTION#",
            content = "关注弹幕",
            extra = """{"duration":5000,"posX":667,"posY":375,"posX_2":100,"posY_2":100,"type":1}""",
            progress = 0
        ))
        assertNotNull(item)
        assertEquals(1, item.attentionType)
        assertEquals(1f, item.positionXRatio)
        assertEquals(1f, item.positionYRatio)
        assertTrue(item.isActiveAt(4900L))
        assertEquals(false, item.isActiveAt(5000L))
    }

    @Test
    fun `attention command does not render through legacy advanced danmaku`() {
        val cmd = commandDm(
            command = "#ATTENTION#",
            content = "关注弹幕",
            extra = """{"duration":6000,"posX":240,"posY":160,"type":2}"""
        )

        val result = buildCommandDanmaku(cmd)

        assertNull(result)
    }

    @Test
    fun `interactive command overlay items can be hidden together`() {
        val attention = buildCommandDanmakuItem(
            commandDm(
                command = "#ATTENTION#",
                content = "关注弹幕",
                extra = """{"type":2}"""
            )
        )
        val up = buildCommandDanmakuItem(
            commandDm(
                command = "#UP#",
                content = "UP 主提示"
            )
        )
        val vote = buildCommandDanmakuItem(
            commandDm(
                command = "VIDEO_VOTE_MSG",
                content = "投票提示"
            )
        )

        assertNotNull(attention)
        assertNotNull(up)
        assertNotNull(vote)
        assertEquals(
            emptyList(),
            filterVisibleCommandDanmakuItems(
                items = listOf(attention, up, vote),
                hideInteractiveCommands = true
            )
        )
        assertEquals(
            listOf(attention, up, vote),
            filterVisibleCommandDanmakuItems(
                items = listOf(attention, up, vote),
                hideInteractiveCommands = false
            )
        )
    }

    @Test
    fun `filter structured payload gibberish`() {
        val cmd = commandDm(
            content = """"453dc8b380c6dba.png","type":2,"upower_state":1"""
        )

        val result = buildCommandDanmaku(cmd)

        assertNull(result)
    }

    @Test
    fun `filter non visual command type`() {
        val cmd = commandDm(
            command = "UPOWER_STATE",
            content = "这条文本不应展示"
        )

        val result = buildCommandDanmaku(cmd)

        assertNull(result)
    }

    @Test
    fun `invalid json command payload falls back to readable content`() {
        val cmd = commandDm(
            command = "#LINK#",
            content = "可读标题",
            extra = """{"broken":"""
        )

        val item = buildCommandDanmakuItem(cmd)

        assertNotNull(item)
        assertEquals(CommandDanmakuType.LINK, item.type)
        assertEquals("可读标题", item.content)
    }

    private fun commandDm(
        command: String = "",
        content: String = "",
        extra: String = "",
        progress: Int = 1000,
        id: Long = 1L
    ): DanmakuProto.CommandDm {
        return DanmakuProto.CommandDm(
            id = id,
            command = command,
            content = content,
            extra = extra,
            progress = progress
        )
    }
}
