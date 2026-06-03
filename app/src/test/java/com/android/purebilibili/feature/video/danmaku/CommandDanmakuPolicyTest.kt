package com.android.purebilibili.feature.video.danmaku

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
