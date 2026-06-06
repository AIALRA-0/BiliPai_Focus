package com.android.purebilibili.data.model.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DynamicModulesFlexibleSerializerTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun dynamicDetailResponse_parsesModulesWhenModulesIsArray() {
        val payload = """
            {
              "code": 0,
              "data": {
                "item": {
                  "id_str": "172792986898006024",
                  "modules": [
                    {
                      "module_author": {
                        "mid": 123456,
                        "name": "tester"
                      }
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<DynamicDetailResponse>(payload)

        assertEquals(123456L, response.data?.item?.modules?.module_author?.mid)
    }

    @Test
    fun dynamicDetailResponse_parsesModulesWhenModulesIsObject() {
        val payload = """
            {
              "code": 0,
              "data": {
                "item": {
                  "id_str": "172792986898006024",
                  "modules": {
                    "module_author": {
                      "mid": 654321,
                      "name": "tester2"
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<DynamicDetailResponse>(payload)

        assertEquals(654321L, response.data?.item?.modules?.module_author?.mid)
    }

    @Test
    fun dynamicFeedResponse_acceptsNumericFollowingFlagInAuthorModule() {
        val payload = """
            {
              "code": 0,
              "data": {
                "items": [
                  {
                    "id_str": "1199344045210468386",
                    "modules": {
                      "module_author": {
                        "mid": 123456,
                        "name": "tester",
                        "following": 1,
                        "official": null,
                        "more": null,
                        "decorate_card": null
                      }
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<DynamicFeedResponse>(payload)

        assertTrue(response.data?.items?.single()?.modules?.module_author?.following == true)
    }

    @Test
    fun dynamicDetailResponse_mergesModulesWhenModulesIsArrayFragments() {
        val payload = """
            {
              "code": 0,
              "data": {
                "item": {
                  "id_str": "172792986898006024",
                  "modules": [
                    {
                      "module_author": {
                        "mid": 123456,
                        "name": "author"
                      }
                    },
                    {
                      "module_dynamic": {
                        "desc": {
                          "text": "hello world"
                        }
                      }
                    },
                    {
                      "module_stat": {
                        "comment": { "count": 7 },
                        "forward": { "count": 3 },
                        "like": { "count": 11 }
                      }
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<DynamicDetailResponse>(payload)
        val modules = response.data?.item?.modules

        assertEquals(123456L, modules?.module_author?.mid)
        assertEquals("hello world", modules?.module_dynamic?.desc?.text)
        assertEquals(7, modules?.module_stat?.comment?.count)
        assertEquals(3, modules?.module_stat?.forward?.count)
        assertEquals(11, modules?.module_stat?.like?.count)
    }

    @Test
    fun dynamicDetailResponse_buildsRenderableContentFromOpusModulesArray() {
        val payload = """
            {
              "code": 0,
              "data": {
                "item": {
                  "id_str": "172792986898006024",
                  "modules": [
                    {
                      "module_type": "MODULE_TYPE_TITLE",
                      "module_title": {
                        "text": "标题A"
                      }
                    },
                    {
                      "module_type": "MODULE_TYPE_CONTENT",
                      "module_content": {
                        "paragraphs": [
                          {
                            "para_type": 1,
                            "text": {
                              "nodes": [
                                {
                                  "word": {
                                    "words": "第一段"
                                  }
                                },
                                {
                                  "word": {
                                    "words": "第二段"
                                  }
                                }
                              ]
                            }
                          },
                          {
                            "para_type": 2,
                            "pic": {
                              "pics": [
                                {
                                  "url": "https://i0.hdslb.com/pic1.jpg",
                                  "width": 1200,
                                  "height": 800
                                }
                              ]
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<DynamicDetailResponse>(payload)
        val modules = response.data?.item?.modules

        assertEquals("第一段第二段", modules?.module_dynamic?.desc?.text)
        assertEquals("MAJOR_TYPE_OPUS", modules?.module_dynamic?.major?.type)
        assertEquals("标题A", modules?.module_dynamic?.major?.opus?.title)
        assertEquals(1, modules?.module_dynamic?.major?.opus?.pics?.size)
        assertEquals(
            "https://i0.hdslb.com/pic1.jpg",
            modules?.module_dynamic?.major?.opus?.pics?.firstOrNull()?.url
        )
    }

    @Test
    fun dynamicDetailResponse_parsesArticleMajorCoversFromDynamicDetail() {
        val payload = """
            {
              "code": 0,
              "data": {
                "item": {
                  "id_str": "1199344045210468386",
                  "type": "DYNAMIC_TYPE_ARTICLE",
                  "modules": {
                    "module_dynamic": {
                      "desc": {
                        "text": "卡片魔王 V260507 更新公告"
                      },
                      "major": {
                        "type": "MAJOR_TYPE_ARTICLE",
                        "article": {
                          "id": 123456,
                          "title": "卡片魔王 V260507 更新公告",
                          "desc": "新增功能",
                          "covers": [
                            "https://i0.hdslb.com/bfs/article/cover-a.jpg",
                            "https://i0.hdslb.com/bfs/article/cover-b.jpg"
                          ],
                          "jump_url": "https://www.bilibili.com/read/cv123456"
                        }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<DynamicDetailResponse>(payload)
        val article = response.data?.item?.modules?.module_dynamic?.major?.article

        assertEquals("MAJOR_TYPE_ARTICLE", response.data?.item?.modules?.module_dynamic?.major?.type)
        assertEquals(123456L, article?.id)
        assertEquals("卡片魔王 V260507 更新公告", article?.title)
        assertEquals(
            listOf(
                "https://i0.hdslb.com/bfs/article/cover-a.jpg",
                "https://i0.hdslb.com/bfs/article/cover-b.jpg"
            ),
            article?.covers
        )
    }

    @Test
    fun dynamicDetailResponse_parsesArchiveChargeBadgeFromDesktopPayload() {
        val payload = """
            {
              "code": 0,
              "data": {
                "item": {
                  "id_str": "1200000000000000000",
                  "type": "DYNAMIC_TYPE_AV",
                  "modules": {
                    "module_dynamic": {
                      "major": {
                        "type": "MAJOR_TYPE_ARCHIVE",
                        "archive": {
                          "aid": "123",
                          "bvid": "BV1xx411c7mD",
                          "title": "充电稿件",
                          "badge": {
                            "text": "充电专属",
                            "color": "#FFFFFF",
                            "bg_color": "#FB7299"
                          },
                          "is_charging_arc": true,
                          "elec_arc_type": 1,
                          "ugc_pay": 1,
                          "ugc_pay_preview": 0
                        }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<DynamicDetailResponse>(payload)
        val archive = response.data?.item?.modules?.module_dynamic?.major?.archive

        assertEquals("充电专属", archive?.badge?.text)
        assertEquals("#FB7299", archive?.badge?.bgColor)
        assertEquals(true, archive?.isChargingArc)
        assertEquals(1, archive?.elecArcType)
        assertEquals(1, archive?.ugcPay)
    }

    @Test
    fun dynamicDetailResponse_prefersFullOpusParagraphsOverPreviewDescWhenBothExist() {
        val payload = """
            {
              "code": 0,
              "data": {
                "item": {
                  "id_str": "172792986898006024",
                  "modules": [
                    {
                      "module_dynamic": {
                        "desc": {
                          "text": "预览摘要"
                        },
                        "major": {
                          "type": "MAJOR_TYPE_OPUS",
                          "opus": {
                            "summary": {
                              "text": "预览摘要"
                            }
                          }
                        }
                      }
                    },
                    {
                      "module_type": "MODULE_TYPE_TITLE",
                      "module_title": {
                        "text": "完整标题"
                      }
                    },
                    {
                      "module_type": "MODULE_TYPE_CONTENT",
                      "module_content": {
                        "paragraphs": [
                          {
                            "para_type": 1,
                            "text": {
                              "nodes": [
                                {
                                  "word": {
                                    "words": "第一段完整内容"
                                  }
                                }
                              ]
                            }
                          },
                          {
                            "para_type": 1,
                            "text": {
                              "nodes": [
                                {
                                  "word": {
                                    "words": "第二段完整内容"
                                  }
                                }
                              ]
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<DynamicDetailResponse>(payload)
        val modules = response.data?.item?.modules

        assertEquals("第一段完整内容\n第二段完整内容", modules?.module_dynamic?.desc?.text)
        assertEquals("完整标题", modules?.module_dynamic?.major?.opus?.title)
        assertEquals("第一段完整内容\n第二段完整内容", modules?.module_dynamic?.major?.opus?.summary?.text)
    }

    @Test
    fun dynamicDetailResponse_preservesOrderedOpusParagraphTextAndImagesFromContentModule() {
        val payload = """
            {
              "code": 0,
              "data": {
                "item": {
                  "id_str": "1201902028962398230",
                  "modules": [
                    {
                      "module_dynamic": {
                        "desc": {
                          "text": "预览摘要"
                        },
                        "major": {
                          "type": "MAJOR_TYPE_OPUS",
                          "opus": {
                            "summary": {
                              "text": "预览摘要"
                            },
                            "pics": [
                              {
                                "url": "https://i0.hdslb.com/preview.jpg",
                                "width": 720,
                                "height": 480
                              }
                            ]
                          }
                        }
                      }
                    },
                    {
                      "module_type": "MODULE_TYPE_TITLE",
                      "module_title": {
                        "text": "完整标题"
                      }
                    },
                    {
                      "module_type": "MODULE_TYPE_CONTENT",
                      "module_content": {
                        "paragraphs": [
                          {
                            "para_type": 1,
                            "text": {
                              "nodes": [
                                {
                                  "type": "TEXT_NODE_TYPE_WORD",
                                  "word": {
                                    "words": "第一段正文"
                                  }
                                },
                                {
                                  "type": "TEXT_NODE_TYPE_RICH",
                                  "rich": {
                                    "text": "[表情]",
                                    "orig_text": "[表情]"
                                  }
                                }
                              ]
                            }
                          },
                          {
                            "para_type": 2,
                            "pic": {
                              "pics": [
                                {
                                  "url": "http://i0.hdslb.com/full-1.jpg",
                                  "width": 900,
                                  "height": 1800,
                                  "size": 1024.5
                                }
                              ]
                            }
                          },
                          {
                            "para_type": 1,
                            "text": {
                              "nodes": [
                                {
                                  "type": "TEXT_NODE_TYPE_WORD",
                                  "word": {
                                    "words": "第二段正文"
                                  }
                                }
                              ]
                            }
                          },
                          {
                            "para_type": 2,
                            "pic": {
                              "pics": [
                                {
                                  "url": "https://i0.hdslb.com/full-2.jpg",
                                  "width": 1000,
                                  "height": 1000
                                }
                              ]
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<DynamicDetailResponse>(payload)
        val opus = response.data?.item?.modules?.module_dynamic?.major?.opus

        assertEquals("第一段正文[表情]\n第二段正文", opus?.summary?.text)
        assertTrue((opus?.summary?.text?.length ?: 0) > "预览摘要".length)
        assertEquals(
            listOf("https://i0.hdslb.com/full-1.jpg", "https://i0.hdslb.com/full-2.jpg"),
            opus?.pics?.map { it.url }
        )
        assertEquals(
            listOf(
                OpusContentBlock.Text("第一段正文[表情]"),
                OpusContentBlock.Image(
                    OpusPic(
                        url = "https://i0.hdslb.com/full-1.jpg",
                        width = 900,
                        height = 1800,
                        size = 1024.5
                    )
                ),
                OpusContentBlock.Text("第二段正文"),
                OpusContentBlock.Image(
                    OpusPic(
                        url = "https://i0.hdslb.com/full-2.jpg",
                        width = 1000,
                        height = 1000
                    )
                )
            ),
            opus?.contentBlocks
        )
    }

    @Test
    fun dynamicDetailResponse_parsesNumericTypeAndOrderedOpusLinkCards() {
        val payload = """
            {
              "code": 0,
              "data": {
                "item": {
                  "id_str": "1201902028962398230",
                  "type": 1,
                  "modules": [
                    {
                      "module_type": "MODULE_TYPE_CONTENT",
                      "module_content": {
                        "paragraphs": [
                          {
                            "para_type": 1,
                            "text": {
                              "nodes": [
                                { "word": { "words": "正文开头" } }
                              ]
                            }
                          },
                          {
                            "para_type": 2,
                            "pic": {
                              "url": "http://i0.hdslb.com/single.jpg",
                              "width": 640,
                              "height": 360
                            }
                          },
                          {
                            "para_type": 3,
                            "line": {
                              "pic": {
                                "url": "//i0.hdslb.com/line.jpg",
                                "width": 1000,
                                "height": 20
                              }
                            }
                          },
                          {
                            "para_type": 6,
                            "link_card": {
                              "card": {
                                "type": "LINK_CARD_TYPE_GOODS",
                                "oid": "118140798",
                                "goods": {
                                  "head_text": "UP主的推荐",
                                  "items": [
                                    {
                                      "cover": "https://i0.hdslb.com/goods.jpg",
                                      "name": "绯乐影Phantom有线HiFi耳机",
                                      "price": "¥1480",
                                      "jump_desc": "去看看",
                                      "jump_url": "https://uland.taobao.com/item"
                                    }
                                  ]
                                }
                              }
                            }
                          },
                          {
                            "para_type": 6,
                            "link_card": {
                              "card": {
                                "type": "LINK_CARD_TYPE_UGC",
                                "ugc": {
                                  "cover": "https://i0.hdslb.com/video.jpg",
                                  "title": "视频标题",
                                  "desc_second": "12:34",
                                  "jump_url": "https://www.bilibili.com/video/BV1xx411c7mD"
                                }
                              }
                            }
                          },
                          {
                            "para_type": 6,
                            "link_card": {
                              "card": {
                                "type": "LINK_CARD_TYPE_COMMON",
                                "common": {
                                  "cover": "https://i0.hdslb.com/common.jpg",
                                  "title": "普通链接",
                                  "desc1": "第一行描述",
                                  "desc2": "第二行描述",
                                  "jump_url": "https://www.bilibili.com/read/cv123456"
                                }
                              }
                            }
                          },
                          {
                            "para_type": 6,
                            "link_card": {
                              "card": {
                                "type": "LINK_CARD_TYPE_LIVE",
                                "live": {
                                  "cover": "https://i0.hdslb.com/live.jpg",
                                  "title": "直播标题",
                                  "desc_first": "正在直播",
                                  "desc_second": "主播名",
                                  "jump_url": "https://live.bilibili.com/6"
                                }
                              }
                            }
                          },
                          {
                            "para_type": 6,
                            "link_card": {
                              "card": {
                                "type": "LINK_CARD_TYPE_OPUS",
                                "opus": {
                                  "cover": "https://i0.hdslb.com/opus.jpg",
                                  "title": "图文标题",
                                  "jump_url": "https://www.bilibili.com/opus/1201902028962398230",
                                  "author": {
                                    "mid": 42,
                                    "name": "作者"
                                  },
                                  "stat": {
                                    "view": 1000
                                  }
                                }
                              }
                            }
                          },
                          {
                            "para_type": 6,
                            "link_card": {
                              "card": {
                                "type": "LINK_CARD_TYPE_ITEM_NULL",
                                "item_null": {
                                  "text": "内容已失效"
                                }
                              }
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<DynamicDetailResponse>(payload)
        val opus = response.data?.item?.modules?.module_dynamic?.major?.opus

        assertEquals("1", response.data?.item?.type)
        assertEquals(
            listOf(
                "https://i0.hdslb.com/single.jpg",
                "https://i0.hdslb.com/line.jpg"
            ),
            opus?.pics?.map { it.url }
        )
        assertEquals(9, opus?.contentBlocks?.size)
        assertEquals(OpusContentBlock.Text("正文开头"), opus?.contentBlocks?.get(0))
        assertEquals(
            OpusContentBlock.Image(OpusPic(url = "https://i0.hdslb.com/single.jpg", width = 640, height = 360)),
            opus?.contentBlocks?.get(1)
        )
        assertEquals(
            OpusContentBlock.Image(OpusPic(url = "https://i0.hdslb.com/line.jpg", width = 1000, height = 20)),
            opus?.contentBlocks?.get(2)
        )

        val goods = (opus?.contentBlocks?.get(3) as? OpusContentBlock.LinkCard)?.card
        assertEquals("LINK_CARD_TYPE_GOODS", goods?.type)
        assertEquals("UP主的推荐", goods?.label)
        assertEquals("绯乐影Phantom有线HiFi耳机", goods?.title)
        assertEquals("¥1480", goods?.description)
        assertEquals("去看看", goods?.badgeText)
        assertEquals("https://i0.hdslb.com/goods.jpg", goods?.cover)
        assertEquals("https://uland.taobao.com/item", goods?.jumpUrl)

        val ugc = (opus?.contentBlocks?.get(4) as? OpusContentBlock.LinkCard)?.card
        assertEquals("LINK_CARD_TYPE_UGC", ugc?.type)
        assertEquals("视频标题", ugc?.title)
        assertEquals("12:34", ugc?.description)

        val common = (opus?.contentBlocks?.get(5) as? OpusContentBlock.LinkCard)?.card
        assertEquals("LINK_CARD_TYPE_COMMON", common?.type)
        assertEquals("普通链接", common?.title)
        assertEquals("第一行描述\n第二行描述", common?.description)

        val live = (opus?.contentBlocks?.get(6) as? OpusContentBlock.LinkCard)?.card
        assertEquals("LINK_CARD_TYPE_LIVE", live?.type)
        assertEquals("直播标题", live?.title)
        assertEquals("正在直播\n主播名", live?.description)

        val opusCard = (opus?.contentBlocks?.get(7) as? OpusContentBlock.LinkCard)?.card
        assertEquals("LINK_CARD_TYPE_OPUS", opusCard?.type)
        assertEquals("图文标题", opusCard?.title)

        val invalid = (opus?.contentBlocks?.get(8) as? OpusContentBlock.LinkCard)?.card
        assertEquals("LINK_CARD_TYPE_ITEM_NULL", invalid?.type)
        assertEquals("内容已失效", invalid?.title)
    }
}
