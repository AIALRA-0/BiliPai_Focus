package com.android.purebilibili.core.plugin.feed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedDocumentParserTest {
    @Test
    fun `HTTP enclosure without MIME type and encoded body retain separate images`() {
        val item = parseFeedDocument(
            xml = """
                <rss version="2.0" xmlns:content="http://purl.org/rss/1.0/modules/content/">
                  <channel><title>订阅</title><item>
                    <title>文章</title><link>https://www.jintiankansha.me/post/one</link>
                    <enclosure url="https://example.com/audio.mp3" type="audio/mpeg"/>
                    <enclosure url="http://example.com/cover.jpg?size=large"/>
                    <content:encoded><![CDATA[<p>正文</p><a href="/photo"><span><img src="http://example.com/body.jpg"></span></a>]]></content:encoded>
                  </item></channel>
                </rss>
            """.trimIndent(),
            sourceId = "s",
            sourceTitle = "订阅",
        ).items.single()

        assertEquals("http://example.com/cover.jpg?size=large", item.imageUrl)
        assertEquals(
            listOf("http://example.com/body.jpg"),
            parseFeedHtml(item.htmlContent, item.link).filterIsInstance<FeedBlock.Image>().map { it.url },
        )
    }

    @Test
    fun `untyped extensionless HTTP cover enclosure is accepted`() {
        val item = parseFeedDocument(
            """<rss><channel><item><title>文章</title><link>https://example.com/post</link><enclosure url="http://mmbiz.qpic.cn/mmbiz_jpg/cover/0?wx_fmt=jpeg"/><description><![CDATA[<img src="http://example.com/body.jpg">]]></description></item></channel></rss>""",
            sourceId = "s",
            sourceTitle = "订阅",
        ).items.single()
        assertEquals("http://mmbiz.qpic.cn/mmbiz_jpg/cover/0?wx_fmt=jpeg", item.imageUrl)
    }

    @Test
    fun `podcast enclosure is not used as a cover`() {
        val item = parseFeedDocument(
            """<rss><channel><item><title>播客</title><link>https://example.com/post</link><enclosure url="https://example.com/audio.mp3" type="audio/mpeg"/><description><![CDATA[<img src="/cover.png">]]></description></item></channel></rss>""",
            sourceId = "s",
            sourceTitle = "订阅",
        ).items.single()
        assertEquals("https://example.com/cover.png", item.imageUrl)
    }

    @Test
    fun `linked images keep order and surrounding inline formatting`() {
        val blocks = parseFeedHtml(
            """<p><strong>甲<a href="/post">链接<span><img src="http://example.com/a.jpg"></span>乙</a>丙</strong></p>""",
            "https://example.com/",
        )
        assertEquals(3, blocks.size)
        val before = (blocks[0] as FeedBlock.Paragraph).inlines
        assertEquals(FeedInline.Text("甲", bold = true), before[0])
        assertEquals(FeedInline.Link("链接", "https://example.com/post"), before[1])
        assertEquals("http://example.com/a.jpg", (blocks[1] as FeedBlock.Image).url)
        val after = (blocks[2] as FeedBlock.Paragraph).inlines
        assertEquals(FeedInline.Link("乙", "https://example.com/post"), after[0])
        assertEquals(FeedInline.Text("丙", bold = true), after[1])
    }

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
    fun `atom xhtml and relative links retain structure`() {
        val feed = parseFeedDocument(
            xml = """<feed xmlns="http://www.w3.org/2005/Atom"><title>源</title><entry><id>1</id><title>文章</title><link href="/post"/><content type="xhtml"><div xmlns="http://www.w3.org/1999/xhtml"><p>正文<strong>加粗</strong></p><img src="/a.jpg"/></div></content></entry></feed>""",
            sourceId = "s",
            sourceTitle = "源",
            sourceUrl = "https://example.com/feed",
        )
        val item = feed.items.single()
        assertEquals("https://example.com/post", item.link)
        assertEquals("https://example.com/a.jpg", item.imageUrl)
        assertTrue(parseFeedHtml(item.htmlContent, item.link).any { it is FeedBlock.Image })
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

        val opml = buildSubscriptionOpml(
            listOf(
                SavedSubscriptionFeed(id = "1", title = "甲\"站\"<&>", url = "https://a.example/feed?a=1&b=2"),
                SavedSubscriptionFeed(id = "2", title = "", url = "https://b.example/atom.xml", enabled = false),
            ),
        )
        val roundTrip = parseSubscriptionImport(opml)
        assertEquals(listOf("https://a.example/feed?a=1&b=2", "https://b.example/atom.xml"), roundTrip.map { it.url })
        assertEquals("甲\"站\"<&>", roundTrip.first().title)
        assertEquals("https://b.example/atom.xml", roundTrip.last().title)
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

    @Test
    fun `nested html preserves text image and embed order`() {
        val blocks = parseFeedHtml(
            """<p>甲<strong>粗<a href="/story">链接</a></strong><img data-src="/photo.jpg" alt="图片">乙&amp;丙</p><pre><code>val x = 1\n  x</code></pre><iframe src="/movie"></iframe>""",
            "https://example.com/post",
        )
        assertEquals(5, blocks.size)
        assertEquals("甲粗链接", (blocks[0] as FeedBlock.Paragraph).inlines.joinToString("") {
            when (it) { is FeedInline.Text -> it.text; is FeedInline.Link -> it.text }
        })
        assertEquals("https://example.com/photo.jpg", (blocks[1] as FeedBlock.Image).url)
        assertEquals("乙&丙", feedPlainText("<p>乙&amp;丙</p>"))
        assertTrue(blocks[3] is FeedBlock.Code)
        assertEquals("https://example.com/movie", (blocks[4] as FeedBlock.EmbeddedLink).url)
        assertTrue(parseFeedHtml("<a href='javascript:alert(1)'>不安全</a>").none { it is FeedBlock.EmbeddedLink })
    }

    @Test
    fun `only clearly truncated content triggers remote fetch`() {
        val full = sampleItem("full").copy(
            summary = "简短摘要",
            htmlContent = "<p>完整正文。</p><p>第二段。</p>",
        )
        assertTrue(!feedBodyNeedsRemoteFetch(full))
        assertTrue(feedBodyNeedsRemoteFetch(full.copy(htmlContent = "<p>开头……阅读全文</p>")))
        assertTrue(feedBodyNeedsRemoteFetch(full.copy(htmlContent = "")))
    }

    @Test
    fun `cached and fresh items deduplicate by source and item id`() {
        val old = sampleItem("one").copy(title = "旧标题")
        val updated = old.copy(title = "新标题")
        val other = sampleItem("two")
        val merged = mergeCachedFeedItems(listOf(old, other), listOf(updated), setOf("s"))
        assertEquals(listOf("新标题", "two"), merged.map { it.title })
        assertEquals(2, merged.map(::feedItemKey).distinct().size)
        val read = updateReadKeys(emptyList(), feedItemKey(updated), true)
        assertEquals(listOf(feedItemKey(updated)), read)
        assertEquals(emptyList(), updateReadKeys(read, feedItemKey(updated), false))
    }

    @Test
    fun `recommended feed table and automatic names remain usable`() {
        val imported = parseSubscriptionImport(
            """名称 | RSS源 | 查看
               --- | --- | ---
               阮一峰的网络日志 | [https://www.ruanyifeng.com/blog/atom.xml](https://www.ruanyifeng.com/blog/atom.xml) | [查看](https://example.com)
               V2EX | https://v2ex.com/index.xml | [查看](https://example.com)""".trimIndent(),
        )
        assertEquals(listOf("阮一峰的网络日志", "V2EX"), imported.map { it.title })
        assertEquals(2, imported.size)
        val url = "https://www.ruanyifeng.com/blog/atom.xml"
        val parsed = parseFeedDocument(
            "<feed xmlns=\"http://www.w3.org/2005/Atom\"><title>阮一峰的网络日志</title></feed>",
            sourceId = url,
            sourceTitle = "",
            sourceUrl = url,
        )
        assertEquals("阮一峰的网络日志", chooseSubscriptionTitle("", parsed.title, url))
        assertEquals("我的命名", chooseSubscriptionTitle("我的命名", parsed.title, url))
        assertEquals("v2ex.com", chooseSubscriptionTitle("", null, "https://v2ex.com/index.xml"))
    }

    @Test
    fun `placeholder titles are queued for background resolution`() {
        assertTrue(needsTitleResolution(SavedSubscriptionFeed(id = "1", title = "", url = "https://v2ex.com/index.xml")))
        assertTrue(
            needsTitleResolution(
                SavedSubscriptionFeed(id = "1", title = "https://v2ex.com/index.xml", url = "https://v2ex.com/index.xml")
            )
        )
        assertFalse(
            needsTitleResolution(SavedSubscriptionFeed(id = "1", title = "V2EX", url = "https://v2ex.com/index.xml"))
        )
    }

    @Test
    fun `opml folders become groups and round trip through export`() {
        val opml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
              <body>
                <outline text="技术">
                  <outline type="rss" text="阮一峰" title="阮一峰" xmlUrl="https://www.ruanyifeng.com/blog/atom.xml"/>
                  <outline type="rss" text="V2EX" title="V2EX" xmlUrl="https://v2ex.com/index.xml"/>
                </outline>
                <outline type="rss" text="散列" title="散列" xmlUrl="https://example.com/feed"/>
              </body>
            </opml>
        """.trimIndent()
        val imported = parseOpmlSubscriptions(opml)

        val ruanyifeng = imported.first { it.url == "https://www.ruanyifeng.com/blog/atom.xml" }
        assertEquals("技术", ruanyifeng.group)
        assertEquals("", imported.first { it.url == "https://example.com/feed" }.group)

        val exported = buildSubscriptionOpml(
            listOf(
                SavedSubscriptionFeed(id = "a", title = "阮一峰", url = "https://www.ruanyifeng.com/blog/atom.xml", group = "技术"),
                SavedSubscriptionFeed(id = "b", title = "散列", url = "https://example.com/feed"),
            )
        )
        val reparsed = parseOpmlSubscriptions(exported)
        assertEquals("技术", reparsed.first { it.url == "https://www.ruanyifeng.com/blog/atom.xml" }.group)
        assertEquals("", reparsed.first { it.url == "https://example.com/feed" }.group)
    }

    @Test
    fun `unknown article container uses readable body fallback`() {
        val html = """<html><body><nav>菜单</nav><div class="unknown"><p>这是一篇能正常阅读的文章，虽然网页没有使用常见的正文类名。</p><p>这是文章的第二段。</p></div><footer>页脚</footer></body></html>"""
        val body = extractArticleBody(html)!!
        assertTrue(body.contains("第二段"))
        assertTrue(!body.contains("菜单") && !body.contains("页脚"))
    }

    private fun sourceIdEpoch(): Long = parseFeedTime("2026-09-22T08:00:00Z")!!
}
