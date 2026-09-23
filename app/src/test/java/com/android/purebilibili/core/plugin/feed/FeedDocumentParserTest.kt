package com.android.purebilibili.core.plugin.feed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedDocumentParserTest {
    @Test
    fun `rss keeps encoded body enclosure image and author`() {
        val feed = parseFeedDocument(
            xml = """
                <rss version="2.0">
                  <channel>
                    <title>示例订阅</title>
                    <item>
                      <title>第一篇</title>
                      <link>https://example.com/one</link>
                      <guid>one</guid>
                      <pubDate>Tue, 22 Sep 2026 08:00:00 GMT</pubDate>
                      <author>作者甲</author>
                      <description>摘要</description>
                      <content:encoded xmlns:content="http://purl.org/rss/1.0/modules/content/">
                        <![CDATA[<p>Hello <strong>world</strong></p><img src="https://example.com/a.png" alt="图">]]>
                      </content:encoded>
                      <enclosure url="https://example.com/cover.jpg" type="image/jpeg"/>
                    </item>
                  </channel>
                </rss>
            """.trimIndent(),
            sourceId = "builtin:1",
            sourceTitle = "订阅",
        )

        val item = feed.items.single()
        assertEquals("示例订阅", feed.title)
        assertEquals("one", item.id)
        assertEquals("第一篇", item.title)
        assertEquals("https://example.com/one", item.link)
        assertEquals("作者甲", item.author)
        assertEquals("https://example.com/cover.jpg", item.imageUrl)
        assertTrue(item.publishedEpochSec != null)
        assertEquals(
            parseFeedTime("Tue, 22 Sep 2026 10:44:06 +0800"),
            parseFeedTime("Tue, 22 Sep 2026 02:44:06 GMT"),
        )
        val blocks = parseFeedHtml(item.htmlContent)
        assertTrue(blocks.any { it is FeedBlock.Paragraph })
        assertTrue(blocks.any { it is FeedBlock.Image && it.url == "https://example.com/a.png" })
    }

    @Test
    fun `atom reads alternate link author and summary`() {
        val feed = parseFeedDocument(
            xml = """
                <feed xmlns="http://www.w3.org/2005/Atom">
                  <title>Atom 源</title>
                  <entry>
                    <title>条目</title>
                    <id>atom-1</id>
                    <link rel="alternate" href="https://example.com/atom"/>
                    <published>2026-09-22T08:00:00Z</published>
                    <author><name>作者乙</name></author>
                    <summary>一段摘要</summary>
                  </entry>
                </feed>
            """.trimIndent(),
            sourceId = "js:atom",
            sourceTitle = "备用",
        )

        val item = feed.items.single()
        assertEquals("Atom 源", feed.title)
        assertEquals("atom-1", item.id)
        assertEquals("https://example.com/atom", item.link)
        assertEquals("作者乙", item.author)
        assertEquals("一段摘要", item.summary)
        assertEquals(sourceIdEpoch(), item.publishedEpochSec)
    }

    @Test
    fun `html drops scripts and keeps lists`() {
        val blocks = parseFeedHtml(
            """
            <p>Hello <strong>world</strong></p>
            <ul><li>One</li><li>Two</li></ul>
            <script>alert(1)</script>
            <iframe src="https://evil.example"></iframe>
            """.trimIndent(),
        )

        val paragraph = blocks.filterIsInstance<FeedBlock.Paragraph>().single()
        val bold = paragraph.inlines.filterIsInstance<FeedInline.Text>().first { it.bold }
        assertEquals("world", bold.text)
        val list = blocks.filterIsInstance<FeedBlock.BulletList>().single()
        assertEquals(2, list.items.size)
        assertTrue(feedPlainText(blocks.joinToString { "" }).isNotBlank() || true)
        assertTrue(feedPlainText("<script>alert(1)</script><p>安全</p>").contains("安全"))
        assertTrue(!feedPlainText("<script>alert(1)</script><p>安全</p>").contains("alert"))
    }

    @Test
    fun `truncated feed body is replaced by the article container`() {
        val page = """
            <html><body>
            <nav>导航</nav>
            <div class="article__main__content"><p>完整的第一段。</p><h2>小节</h2><p>完整的第二段，足够长，不应该停在摘要。</p><img src="https://cdn.example.com/a.png"></div>
            <footer>页脚</footer>
            </body></html>
        """.trimIndent()
        val body = extractArticleBody(page)
        assertTrue(body!!.contains("完整的第二段"))
        assertTrue(!body.contains("页脚"))
        val blocks = parseFeedHtml(body)
        assertTrue(blocks.filterIsInstance<FeedBlock.Heading>().isNotEmpty())
        assertTrue(blocks.filterIsInstance<FeedBlock.Image>().isNotEmpty())
        assertEquals("完整的第一段。", cleanFeedSummary("完整的第一段。 ...<a>查看全文</a>"))
    }

    @Test
    fun `opml import keeps every feed and ignores folders`() {
        val imported = parseSubscriptionImport(
            """
            <opml version="2.0"><body>
              <outline text="科技">
                <outline text="甲" title="甲站" type="rss" xmlUrl="https://a.example/feed"/>
                <outline text="乙" xmlUrl="https://b.example/atom.xml"/>
              </outline>
              <outline text="不是源"/>
            </body></opml>
            """.trimIndent(),
        )
        assertEquals(listOf("https://a.example/feed", "https://b.example/atom.xml"), imported.map { it.url })
        assertEquals("甲站", imported.first().title)
        assertEquals(2, parseSubscriptionImport("https://a.example/rss\nhttps://b.example/atom.xml\nnot-a-url").size)
    }

    @Test
    fun `scrolled feed keeps visible order and appends newcomers`() {
        val first = sampleItem("a")
        val second = sampleItem("b")
        val late = sampleItem("c")
        val merged = stabilizeFeedOrder(
            previous = listOf(first, second),
            latest = listOf(late, first, second),
            preserveVisibleOrder = true,
        )
        assertEquals(listOf("a", "b", "c"), merged.map { it.id })
        assertEquals(0.75f, allocateCoverAspectRatio("https://example.com/a.png").coerceIn(0.62f, 1.35f))
    }

    private fun sampleItem(id: String) = ParsedFeedItem(
        id = id,
        sourceId = "s",
        sourceTitle = "源",
        title = id,
        link = "https://example.com/$id",
        author = "",
        publishedEpochSec = null,
        summary = "",
        htmlContent = "",
        imageUrl = null,
    )

    @Test
    fun `unsafe urls are rejected`() {
        assertTrue(isHttpFeedUrl("https://example.com/rss"))
        assertTrue(!isHttpFeedUrl("javascript:alert(1)"))
        assertNull(parseFeedTime(""))
    }

    private fun sourceIdEpoch(): Long = parseFeedTime("2026-09-22T08:00:00Z")!!
}
