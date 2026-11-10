package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.ReplyPicture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentMediaPolicyTest {

    @Test
    fun `emote segments split text around known tokens in order`() {
        val segments = resolveCommentEmoteSegments(
            "前缀[doge]中缀[笑哭]后缀",
            mapOf("[doge]" to "https://i0.hdslb.com/emote/doge.png", "[笑哭]" to "https://i0.hdslb.com/emote/xk.png"),
        )
        assertEquals(
            listOf(
                CommentEmoteSegment.Text("前缀"),
                CommentEmoteSegment.Emote("[doge]", "https://i0.hdslb.com/emote/doge.png"),
                CommentEmoteSegment.Text("中缀"),
                CommentEmoteSegment.Emote("[笑哭]", "https://i0.hdslb.com/emote/xk.png"),
                CommentEmoteSegment.Text("后缀"),
            ),
            segments,
        )
    }

    @Test
    fun `unknown bracket tokens and missing emote table stay plain text`() {
        val segments = resolveCommentEmoteSegments("文本[未知]继续", mapOf("[doge]" to "https://a.png"))
        assertEquals(listOf(CommentEmoteSegment.Text("文本[未知]继续")), segments)
        assertEquals(
            listOf(CommentEmoteSegment.Text("纯文本")),
            resolveCommentEmoteSegments("纯文本", emptyMap()),
        )
        assertTrue(resolveCommentEmoteSegments("", mapOf("[doge]" to "https://a.png")).isEmpty())
    }

    @Test
    fun `renderable keys only contain tokens present in emote table`() {
        val keys = resolveCommentRenderableEmoteKeys(
            "[doge][doge][ missing ]",
            mapOf("[doge]" to "https://a.png"),
        )
        assertEquals(setOf("[doge]"), keys)
        assertTrue(resolveCommentRenderableEmoteKeys("", mapOf("[doge]" to "https://a.png")).isEmpty())
    }

    @Test
    fun `picture urls fix protocol and strip size suffix`() {
        val urls = resolveCommentPictureUrls(listOf(
            ReplyPicture(imgSrc = "//i0.hdslb.com/bfs/article/abc.jpg@1000w_800h.webp"),
            ReplyPicture(imgSrc = "http://i0.hdslb.com/bfs/article/def.png"),
            ReplyPicture(imgSrc = ""),
        ))
        assertEquals(
            listOf("https://i0.hdslb.com/bfs/article/abc.jpg", "https://i0.hdslb.com/bfs/article/def.png"),
            urls,
        )
    }

    @Test
    fun `single picture aspect ratio is bounded with fallback`() {
        assertEquals(
            2f,
            resolveCommentSinglePictureAspectRatio(ReplyPicture(imgWidth = 4000, imgHeight = 1000)),
            0f,
        )
        assertEquals(
            0.5f,
            resolveCommentSinglePictureAspectRatio(ReplyPicture(imgWidth = 500, imgHeight = 2000)),
            0f,
        )
        assertEquals(
            1.25f,
            resolveCommentSinglePictureAspectRatio(ReplyPicture(imgWidth = 1000, imgHeight = 800)),
            0f,
        )
        assertEquals(
            4f / 3f,
            resolveCommentSinglePictureAspectRatio(ReplyPicture(imgWidth = 0, imgHeight = 0)),
            0f,
        )
    }

    @Test
    fun `grid columns follow mobile rule of two then three`() {
        assertEquals(2, resolveCommentPictureGridColumns(1))
        assertEquals(2, resolveCommentPictureGridColumns(4))
        assertEquals(3, resolveCommentPictureGridColumns(5))
        assertEquals(3, resolveCommentPictureGridColumns(COMMENT_PICTURE_MAX_COUNT))
    }
}
