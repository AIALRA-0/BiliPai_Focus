package com.android.purebilibili.feature.article

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class ArticleContentBlockParserTest {

    @Test
    fun `parseArticleContentBlocks extracts heading paragraph and image from structured paragraphs`() {
        val blocks = parseArticleContentBlocks(
            structuredParagraphs = listOf(
                paragraph(
                    headingWords = listOf("主标题")
                ),
                paragraph(
                    textWords = listOf("第一段", "内容")
                ),
                paragraph(
                    imageUrl = "https://i0.hdslb.com/bfs/article/test-cover.png",
                    imageWidth = 1080,
                    imageHeight = 720
                )
            ),
            htmlContent = null
        )

        assertEquals(
            listOf(
                ArticleContentBlock.Heading(text = "主标题"),
                ArticleContentBlock.Paragraph(text = "第一段内容"),
                ArticleContentBlock.Image(
                    url = "https://i0.hdslb.com/bfs/article/test-cover.png",
                    width = 1080,
                    height = 720
                )
            ),
            blocks
        )
    }

    @Test
    fun `parseArticleContentBlocks falls back to html paragraphs and images when structured content is empty`() {
        val blocks = parseArticleContentBlocks(
            structuredParagraphs = emptyList(),
            htmlContent = """
                <h1>老专栏标题</h1>
                <p>第一段文字</p>
                <figure><img data-src="//i0.hdslb.com/bfs/article/test-inline.png" width="640" height="480" /></figure>
                <p><strong>第二段</strong>文字</p>
            """.trimIndent()
        )

        assertEquals(
            listOf(
                ArticleContentBlock.Heading(text = "老专栏标题"),
                ArticleContentBlock.Paragraph(text = "第一段文字"),
                ArticleContentBlock.Image(
                    url = "https://i0.hdslb.com/bfs/article/test-inline.png",
                    width = 640,
                    height = 480
                ),
                ArticleContentBlock.Paragraph(text = "第二段文字")
            ),
            blocks
        )
    }

    @Test
    fun `parseArticleContentBlocks extracts line image paragraphs`() {
        val blocks = parseArticleContentBlocks(
            structuredParagraphs = listOf(
                JsonObject(
                    mapOf(
                        "line" to JsonObject(
                            mapOf(
                                "pic" to JsonObject(
                                    mapOf(
                                        "url" to JsonPrimitive("//i0.hdslb.com/bfs/article/line.png"),
                                        "width" to JsonPrimitive(1440),
                                        "height" to JsonPrimitive(320)
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            htmlContent = null
        )

        assertEquals(
            listOf(
                ArticleContentBlock.Image(
                    url = "https://i0.hdslb.com/bfs/article/line.png",
                    width = 1440,
                    height = 320
                )
            ),
            blocks
        )
    }

    @Test
    fun `parseArticleContentBlocks falls back to legacy ops text and image cards`() {
        val blocks = parseArticleContentBlocks(
            structuredParagraphs = emptyList(),
            htmlContent = null,
            ops = listOf(
                JsonObject(mapOf("insert" to JsonPrimitive("第一段\n第二段\n"))),
                JsonObject(
                    mapOf(
                        "insert" to JsonObject(
                            mapOf(
                                "image-card" to JsonObject(
                                    mapOf(
                                        "url" to JsonPrimitive("//i0.hdslb.com/bfs/article/ops.png"),
                                        "width" to JsonPrimitive(900),
                                        "height" to JsonPrimitive(1600)
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        assertEquals(
            listOf(
                ArticleContentBlock.Paragraph("第一段"),
                ArticleContentBlock.Paragraph("第二段"),
                ArticleContentBlock.Image(
                    url = "https://i0.hdslb.com/bfs/article/ops.png",
                    width = 900,
                    height = 1600
                )
            ),
            blocks
        )
    }

    @Test
    fun `parseArticleContentBlocks reads json content ops and native images from article api`() {
        val blocks = parseArticleContentBlocks(
            structuredParagraphs = emptyList(),
            htmlContent = """
                {
                  "ops": [
                    { "insert": "第一段 JSON 正文\n" },
                    {
                      "insert": {
                        "native-image": {
                          "url": "//i0.hdslb.com/bfs/article/native.png",
                          "width": 1080,
                          "height": 1920
                        }
                      }
                    }
                  ]
                }
            """.trimIndent()
        )

        assertEquals(
            listOf(
                ArticleContentBlock.Paragraph("第一段 JSON 正文"),
                ArticleContentBlock.Image(
                    url = "https://i0.hdslb.com/bfs/article/native.png",
                    width = 1080,
                    height = 1920
                )
            ),
            blocks
        )
    }

    @Test
    fun `parseArticleContentBlocks keeps quotes lists and code from opus para types`() {
        val blocks = parseArticleContentBlocks(
            structuredParagraphs = listOf(
                JsonObject(
                    mapOf(
                        "para_type" to JsonPrimitive(4),
                        "text" to JsonObject(
                            mapOf(
                                "nodes" to JsonArray(
                                    listOf(
                                        JsonObject(
                                            mapOf(
                                                "word" to JsonObject(
                                                    mapOf("words" to JsonPrimitive("an open source pastebin"))
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                ),
                JsonObject(
                    mapOf(
                        "para_type" to JsonPrimitive(5),
                        "list" to JsonObject(
                            mapOf(
                                "style" to JsonPrimitive(1),
                                "items" to JsonArray(
                                    listOf(
                                        JsonObject(
                                            mapOf(
                                                "nodes" to JsonArray(
                                                    listOf(
                                                        JsonObject(
                                                            mapOf(
                                                                "word" to JsonObject(
                                                                    mapOf("words" to JsonPrimitive("glot-www"))
                                                                )
                                                            )
                                                        )
                                                    )
                                                )
                                            )
                                        ),
                                        JsonObject(
                                            mapOf(
                                                "nodes" to JsonArray(
                                                    listOf(
                                                        JsonObject(
                                                            mapOf(
                                                                "word" to JsonObject(
                                                                    mapOf("words" to JsonPrimitive("code-runner"))
                                                                )
                                                            )
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                ),
                JsonObject(
                    mapOf(
                        "para_type" to JsonPrimitive(7),
                        "code" to JsonObject(
                            mapOf(
                                "lang" to JsonPrimitive("language-json"),
                                "content" to JsonPrimitive("{&quot;stdout&quot;:&quot;42\\n&quot;}")
                            )
                        )
                    )
                )
            ),
            htmlContent = null
        )

        assertEquals(
            listOf(
                ArticleContentBlock.Quote("an open source pastebin"),
                ArticleContentBlock.ListBlock(
                    ordered = true,
                    items = listOf("glot-www", "code-runner")
                ),
                ArticleContentBlock.Code(
                    language = "json",
                    content = "{\"stdout\":\"42\\n\"}"
                )
            ),
            blocks
        )
    }

    @Test
    fun `parseArticleContentBlocks keeps nested images inside html paragraphs`() {
        val blocks = parseArticleContentBlocks(
            structuredParagraphs = emptyList(),
            htmlContent = """
                <p>开头文字<img data-src="//i0.hdslb.com/bfs/article/nested.png" width="640" height="360">结尾文字</p>
            """.trimIndent()
        )

        assertEquals(
            listOf(
                ArticleContentBlock.Paragraph("开头文字"),
                ArticleContentBlock.Image(
                    url = "https://i0.hdslb.com/bfs/article/nested.png",
                    width = 640,
                    height = 360
                ),
                ArticleContentBlock.Paragraph("结尾文字")
            ),
            blocks
        )
    }

    @Test
    fun `parseArticleContentBlocks prefers complete html over sparse structured preview`() {
        val blocks = parseArticleContentBlocks(
            structuredParagraphs = listOf(
                paragraph(
                    imageUrl = "https://i0.hdslb.com/bfs/article/preview.png",
                    imageWidth = 800,
                    imageHeight = 600
                )
            ),
            htmlContent = """
                <p>第一段完整正文</p>
                <p><img data-src="//i0.hdslb.com/bfs/article/full.png" width="800" height="600" /></p>
                <p>第二段完整正文</p>
                <p>第三段完整正文</p>
            """.trimIndent()
        )

        assertEquals(
            listOf(
                ArticleContentBlock.Paragraph("第一段完整正文"),
                ArticleContentBlock.Image(
                    url = "https://i0.hdslb.com/bfs/article/full.png",
                    width = 800,
                    height = 600
                ),
                ArticleContentBlock.Paragraph("第二段完整正文"),
                ArticleContentBlock.Paragraph("第三段完整正文")
            ),
            blocks
        )
    }

    @Test
    fun `parseArticleContentBlocks reads para type 2 pics at the paragraph root`() {
        val blocks = parseArticleContentBlocks(
            structuredParagraphs = listOf(
                JsonObject(
                    mapOf(
                        "para_type" to JsonPrimitive(2),
                        "pics" to JsonArray(
                            listOf(
                                JsonObject(
                                    mapOf(
                                        "url" to JsonPrimitive("//i0.hdslb.com/bfs/article/root-pic.png"),
                                        "width" to JsonPrimitive(800),
                                        "height" to JsonPrimitive(600)
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            htmlContent = null
        )

        assertEquals(
            listOf(
                ArticleContentBlock.Image(
                    url = "https://i0.hdslb.com/bfs/article/root-pic.png",
                    width = 800,
                    height = 600
                )
            ),
            blocks
        )
    }

    @Test
    fun `parseArticleContentBlocks keeps article view legacy formatted list rows`() {
        val blocks = parseArticleContentBlocks(
            structuredParagraphs = listOf(
                legacyFormattedListParagraph(order = 1, text = "第一项"),
                legacyFormattedListParagraph(order = 2, text = "第二项")
            ),
            htmlContent = null
        )

        assertEquals(
            listOf(
                ArticleContentBlock.ListBlock(
                    ordered = false,
                    items = listOf("第一项", "第二项")
                )
            ),
            blocks
        )
    }

    @Test
    fun `parseArticleContentBlocks applies quill newline block attributes`() {
        val blocks = parseArticleContentBlocks(
            structuredParagraphs = emptyList(),
            htmlContent = null,
            ops = listOf(
                JsonObject(mapOf("insert" to JsonPrimitive("章节"))),
                JsonObject(
                    mapOf(
                        "attributes" to JsonObject(mapOf("header" to JsonPrimitive(2))),
                        "insert" to JsonPrimitive("\n")
                    )
                ),
                JsonObject(mapOf("insert" to JsonPrimitive("第一项"))),
                JsonObject(
                    mapOf(
                        "attributes" to JsonObject(mapOf("list" to JsonPrimitive("bullet"))),
                        "insert" to JsonPrimitive("\n")
                    )
                ),
                JsonObject(mapOf("insert" to JsonPrimitive("第二项"))),
                JsonObject(
                    mapOf(
                        "attributes" to JsonObject(mapOf("list" to JsonPrimitive("bullet"))),
                        "insert" to JsonPrimitive("\n")
                    )
                ),
                JsonObject(mapOf("insert" to JsonPrimitive("引用内容"))),
                JsonObject(
                    mapOf(
                        "attributes" to JsonObject(mapOf("blockquote" to JsonPrimitive(true))),
                        "insert" to JsonPrimitive("\n")
                    )
                )
            )
        )

        assertEquals(
            listOf(
                ArticleContentBlock.Heading("章节"),
                ArticleContentBlock.ListBlock(
                    ordered = false,
                    items = listOf("第一项", "第二项")
                ),
                ArticleContentBlock.Quote("引用内容")
            ),
            blocks
        )
    }

    private fun legacyFormattedListParagraph(order: Int, text: String): JsonObject {
        return JsonObject(
            mapOf(
                "para_type" to JsonPrimitive(6),
                "format" to JsonObject(
                    mapOf(
                        "list_format" to JsonObject(
                            mapOf(
                                "level" to JsonPrimitive(1),
                                "order" to JsonPrimitive(order)
                            )
                        )
                    )
                ),
                "text" to JsonObject(
                    mapOf(
                        "nodes" to JsonArray(
                            listOf(
                                JsonObject(
                                    mapOf(
                                        "word" to JsonObject(
                                            mapOf("words" to JsonPrimitive(text))
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    private fun paragraph(
        textWords: List<String> = emptyList(),
        headingWords: List<String> = emptyList(),
        imageUrl: String? = null,
        imageWidth: Int? = null,
        imageHeight: Int? = null
    ): JsonObject {
        val content = linkedMapOf<String, kotlinx.serialization.json.JsonElement>()

        if (textWords.isNotEmpty()) {
            content["text"] = JsonObject(
                mapOf(
                    "nodes" to JsonArray(
                        textWords.map { word ->
                            JsonObject(
                                mapOf(
                                    "word" to JsonObject(
                                        mapOf("words" to JsonPrimitive(word))
                                    )
                                )
                            )
                        }
                    )
                )
            )
        }

        if (headingWords.isNotEmpty()) {
            content["heading"] = JsonObject(
                mapOf(
                    "nodes" to JsonArray(
                        headingWords.map { word ->
                            JsonObject(
                                mapOf(
                                    "word" to JsonObject(
                                        mapOf("words" to JsonPrimitive(word))
                                    )
                                )
                            )
                        }
                    )
                )
            )
        }

        if (imageUrl != null) {
            content["pic"] = JsonObject(
                mapOf(
                    "pics" to JsonArray(
                        listOf(
                            JsonObject(
                                mapOf(
                                    "url" to JsonPrimitive(imageUrl),
                                    "width" to JsonPrimitive(imageWidth ?: 0),
                                    "height" to JsonPrimitive(imageHeight ?: 0)
                                )
                            )
                        )
                    )
                )
            )
        }

        return JsonObject(content)
    }
}
