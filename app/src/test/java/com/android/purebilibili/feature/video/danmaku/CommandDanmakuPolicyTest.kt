package com.android.purebilibili.feature.video.danmaku

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
        assertEquals(VOTE_DANMAKU_OVERLAY_DURATION_MS, item.durationMs)
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
        assertEquals(VOTE_DANMAKU_OVERLAY_DURATION_MS, item.durationMs)
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
    fun `build attention command item uses three second overlay duration`() {
        val cmd = commandDm(
            command = "#ATTENTION#",
            content = "关注按钮",
            extra = """{"duration":6000,"posX":240,"posY":160,"icon":"https://example.com/follow.png","type":2}""",
            progress = 157818
        )

        val item = buildCommandDanmakuItem(cmd)

        assertNotNull(item)
        assertEquals(CommandDanmakuType.ATTENTION, item.type)
        assertEquals(157818L, item.startTimeMs)
        assertEquals(COMMAND_DANMAKU_OVERLAY_DURATION_MS, item.durationMs)
        assertEquals(240f, item.posX)
        assertEquals(160f, item.posY)
        assertEquals(2, item.attentionType)
        assertEquals("https://example.com/follow.png", item.iconUrl)
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
        progress: Int = 1000
    ): DanmakuProto.CommandDm {
        return DanmakuProto.CommandDm(
            id = 1L,
            command = command,
            content = content,
            extra = extra,
            progress = progress
        )
    }
}
