package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.data.model.response.DynamicAdditional
import com.android.purebilibili.data.model.response.DynamicAdditionalCommon
import com.android.purebilibili.data.model.response.DynamicAdditionalGoods
import com.android.purebilibili.data.model.response.DynamicAdditionalGoodsItem
import com.android.purebilibili.data.model.response.DynamicAdditionalMatch
import com.android.purebilibili.data.model.response.DynamicAdditionalReserve
import com.android.purebilibili.data.model.response.DynamicAdditionalText
import com.android.purebilibili.data.model.response.DynamicAdditionalUgc
import com.android.purebilibili.data.model.response.DynamicAdditionalVote
import com.android.purebilibili.data.model.response.DynamicCardButton
import com.android.purebilibili.data.model.response.DynamicCardButtonStyle
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicMatchInfo
import com.android.purebilibili.data.model.response.DynamicMatchTeam
import com.android.purebilibili.data.model.response.OpusContentBlock
import com.android.purebilibili.data.model.response.OpusLinkCard
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 附加内容卡在信息流（additional）与详情页（opus link_card）两条解析路径上的行为。
 *
 * 关键约束：同一个附加内容无论从哪个接口解析，都必须产出完全相同的 [OpusLinkCard]，
 * 否则卡片进详情时会整块重排——这正是「相关游戏」横幅闪烁的根因。
 */
class DynamicLinkCardPolicyTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private fun feedCard(payload: String): OpusLinkCard? {
        val item = json.decodeFromString<DynamicItem>(payload)
        return resolveDynamicLinkCard(item.modules.module_dynamic?.additional)
    }

    private fun detailCard(payload: String): OpusLinkCard? = json.decodeFromString<DynamicItem>(payload)
        .modules.module_dynamic?.major?.opus?.contentBlocks
        ?.filterIsInstance<OpusContentBlock.LinkCard>()
        ?.firstOrNull()
        ?.card

    @Test
    fun commonAdditional_matchesDetailLinkCardExactly() {
        val feed = feedCard(COMMON_FEED_PAYLOAD)
        val detail = detailCard(COMMON_DETAIL_PAYLOAD)

        assertEquals(detail, feed)
        assertEquals("明日方舟：终末地", feed?.title)
        // 详情侧按 "\n" 连接 desc/desc1/desc2，信息流必须一致，否则行数与高度都不同。
        assertEquals("角色扮演/模拟经营/二次元\n新版本「雪淞幽梦」现已开启！", feed?.description)
        assertEquals("https://i0.hdslb.com/game.jpg", feed?.cover)
        // 详情页从未展示这两个角标，信息流也不再展示，避免进详情时少一行 / 少一段文字。
        assertEquals("", feed?.label)
        assertEquals("", feed?.badgeText)
    }

    @Test
    fun reserveAdditional_matchesDetailLinkCardAndKeepsReserveAction() {
        val feed = feedCard(RESERVE_FEED_PAYLOAD)
        val detail = detailCard(RESERVE_DETAIL_PAYLOAD)

        assertEquals(detail, feed)
        assertEquals("预约", feed?.badgeText)
        assertEquals("3月2日 20:00 · 99 人预约", feed?.description)

        val reserveUi = assertNotNull(resolveDynamicReserveUi(assertNotNull(feed), "dynamic-1"))
        assertEquals(42L, reserveUi.action.reserveId)
        assertEquals(1, reserveUi.action.buttonType)
        assertEquals(0, reserveUi.action.currentButtonStatus)
        assertEquals(99L, reserveUi.action.reserveTotal)
        assertEquals(true, reserveUi.actionEnabled)
        // 描述里带时间时，详情页与信息流都会安排同一条开播提醒。
        assertNotNull(reserveUi.action.startAtMillis)
    }

    @Test
    fun reserveAdditional_hidesButtonWhenServerReportsCancelledState() {
        val card = assertNotNull(
            resolveDynamicLinkCard(
                DynamicAdditional(
                    type = "ADDITIONAL_TYPE_RESERVE",
                    reserve = DynamicAdditionalReserve(
                        title = "已结束的预约",
                        rid = 7L,
                        state = -1,
                    ),
                )
            )
        )

        assertNull(resolveDynamicReserveUi(card, "dynamic-1"))
    }

    @Test
    fun reserveAdditional_disablesButtonWhenServerMarksUnavailable() {
        val card = assertNotNull(
            resolveDynamicLinkCard(
                DynamicAdditional(
                    type = "ADDITIONAL_TYPE_RESERVE",
                    reserve = DynamicAdditionalReserve(
                        title = "不可预约",
                        rid = 7L,
                        button = DynamicCardButton(
                            jump_url = "",
                            uncheck = DynamicCardButtonStyle(text = "预约", disable = 1),
                        ),
                    ),
                )
            )
        )

        assertEquals(false, resolveReserveActionEnabled(card))
    }

    @Test
    fun matchAdditional_matchesDetailLinkCardAndKeepsScoreRow() {
        val feed = feedCard(MATCH_FEED_PAYLOAD)
        val detail = detailCard(MATCH_DETAIL_PAYLOAD)

        assertEquals(detail, feed)
        assertEquals("赛事", feed?.label)

        val matchUi = assertNotNull(resolveDynamicMatchUi(assertNotNull(feed)))
        assertEquals(listOf("BLG", "TES"), matchUi.teams.map { it.name })
        assertEquals(listOf("2", "1"), matchUi.teams.map { it.score })
        assertEquals("2026赛季 · 第 3 局", matchUi.centerLabel)
    }

    @Test
    fun voteAdditional_matchesDetailLinkCardAndExposesVoteId() {
        val feed = feedCard(VOTE_FEED_PAYLOAD)
        val detail = detailCard(VOTE_DETAIL_PAYLOAD)

        assertEquals(detail, feed)
        assertEquals("今晚吃什么", feed?.title)
        assertEquals("12 人参与", feed?.description)
        assertEquals(88L, resolveDynamicLinkCardVoteId(assertNotNull(feed)))
    }

    @Test
    fun goodsAdditional_mirrorsDetailFieldMapping() {
        val feed = assertNotNull(feedCard(GOODS_FEED_PAYLOAD))
        val detail = assertNotNull(detailCard(GOODS_DETAIL_PAYLOAD))

        assertEquals(detail.title, feed.title)
        assertEquals(detail.description, feed.description)
        assertEquals(detail.label, feed.label)
        assertEquals(detail.badgeText, feed.badgeText)
        assertEquals(detail.cover, feed.cover)
        assertEquals(detail.jumpUrl, feed.jumpUrl)
        assertEquals(detail.goodsItem, feed.goodsItem)
        assertEquals("UP主的推荐", feed.label)
        assertEquals("去看看", feed.badgeText)
        assertEquals("https://uland.taobao.com/item", feed.jumpUrl)
    }

    @Test
    fun ugcAdditional_matchesDetailLinkCard() {
        val feed = assertNotNull(feedCard(UGC_FEED_PAYLOAD))
        val detail = assertNotNull(detailCard(UGC_DETAIL_PAYLOAD))

        assertEquals(detail, feed)
        assertEquals("视频标题", feed.title)
        assertEquals("12:34", feed.description)
        assertEquals("", feed.label)
    }

    @Test
    fun resolveDynamicLinkCard_returnsNullForMissingOrUnknownAdditional() {
        assertNull(resolveDynamicLinkCard(null))
        assertNull(
            resolveDynamicLinkCard(
                DynamicAdditional(
                    type = "ADDITIONAL_TYPE_UNKNOWN",
                    common = DynamicAdditionalCommon(title = "忽略我"),
                )
            )
        )
    }

    @Test
    fun resolveDynamicLinkCard_skipsAdditionalWithoutUsableContent() {
        assertNull(
            resolveDynamicLinkCard(
                DynamicAdditional(
                    type = "ADDITIONAL_TYPE_COMMON",
                    common = DynamicAdditionalCommon(title = "", cover = "", jump_url = ""),
                )
            )
        )
    }

    @Test
    fun matchAdditional_withoutTeamsStillRendersCard() {
        val card = assertNotNull(
            resolveDynamicLinkCard(
                DynamicAdditional(
                    type = "ADDITIONAL_TYPE_MATCH",
                    match = DynamicAdditionalMatch(title = "决赛预告", sub_title = "今晚 20:00"),
                )
            )
        )

        assertEquals("赛事", card.label)
        assertEquals("今晚 20:00", card.description)
        assertNull(resolveDynamicMatchUi(card))
    }

    @Test
    fun matchUi_isBuiltFromLeftAndRightTeam() {
        val card = assertNotNull(
            resolveDynamicLinkCard(
                DynamicAdditional(
                    type = "ADDITIONAL_TYPE_MATCH",
                    match = DynamicAdditionalMatch(
                        title = "决赛",
                        matchInfo = DynamicMatchInfo(
                            centerTop = listOf("2026赛季"),
                            centerBottom = "第 3 局",
                            leftTeam = DynamicMatchTeam(name = "BLG", pic = "p1", score = "2"),
                            rightTeam = DynamicMatchTeam(name = "TES", pic = "p2", score = "1"),
                        ),
                    ),
                )
            )
        )

        val matchUi = assertNotNull(resolveDynamicMatchUi(card))
        assertEquals(2, matchUi.teams.size)
        assertEquals("p1", matchUi.teams.first().logoUrl)
        assertEquals("2026赛季 · 第 3 局", matchUi.centerLabel)
    }

    @Test
    fun ugcAdditionalKeepsCoverAndJumpUrl() {
        val card = assertNotNull(
            resolveDynamicLinkCard(
                DynamicAdditional(
                    type = "ADDITIONAL_TYPE_UGC",
                    ugc = DynamicAdditionalUgc(
                        title = "投稿标题",
                        cover = "//i0.hdslb.com/a.jpg",
                        desc_second = "1.2万播放",
                        jump_url = "https://b23.tv/av1",
                    ),
                )
            )
        )

        assertEquals("投稿标题", card.title)
        assertEquals("https://i0.hdslb.com/a.jpg", card.cover)
        assertEquals("https://b23.tv/av1", card.jumpUrl)
        assertEquals("", card.label)
    }

    @Test
    fun voteAdditionalWithOnlyIdStillProducesCard() {
        val card = assertNotNull(
            resolveDynamicLinkCard(
                DynamicAdditional(
                    type = "ADDITIONAL_TYPE_VOTE",
                    vote = DynamicAdditionalVote(desc = "", join_num = 0, vote_id = 88L),
                )
            )
        )

        assertEquals("投票", card.title)
        assertEquals("", card.description)
        assertEquals(88L, resolveDynamicLinkCardVoteId(card))
    }

    @Test
    fun reserveDescriptionIncludesAllServerLines() {
        val card = assertNotNull(
            resolveDynamicLinkCard(
                DynamicAdditional(
                    type = "ADDITIONAL_TYPE_RESERVE",
                    reserve = DynamicAdditionalReserve(
                        title = "首播预约",
                        rid = 5L,
                        desc1 = DynamicAdditionalText("明晚 8 点"),
                        desc2 = DynamicAdditionalText("5 人预约"),
                        desc3 = DynamicAdditionalText("预约后可抽奖"),
                    ),
                )
            )
        )

        assertEquals("明晚 8 点 · 5 人预约 · 预约后可抽奖", card.description)
    }

    @Test
    fun ugcAdditional_mapsHeadTextToLabelSource() {
        val card = assertNotNull(
            resolveDynamicLinkCard(
                DynamicAdditional(
                    type = "ADDITIONAL_TYPE_UGC",
                    ugc = DynamicAdditionalUgc(title = "投稿标题", head_text = "视频"),
                )
            )
        )

        // head_text 只用于补解析，卡片角标保持与详情页一致（不展示）。
        assertEquals("", card.label)
        assertTrue(card.title.isNotBlank())
    }

    private companion object {
        val COMMON_FEED_PAYLOAD = """
            {
              "id_str": "1",
              "modules": {
                "module_dynamic": {
                  "additional": {
                    "type": "ADDITIONAL_TYPE_COMMON",
                    "common": {
                      "id_str": "123",
                      "title": "明日方舟：终末地",
                      "desc1": "角色扮演/模拟经营/二次元",
                      "desc2": "新版本「雪淞幽梦」现已开启！",
                      "cover": "//i0.hdslb.com/game.jpg",
                      "head_text": "相关游戏",
                      "jump_url": "https://www.biligame.com/detail/1",
                      "button": {
                        "jump_url": "https://www.biligame.com/detail/1",
                        "jump_style": { "text": "进入" }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val COMMON_DETAIL_PAYLOAD = """
            {
              "id_str": "1",
              "modules": [
                {
                  "module_type": "MODULE_TYPE_CONTENT",
                  "module_content": {
                    "paragraphs": [
                      {
                        "para_type": 6,
                        "link_card": {
                          "card": {
                            "type": "LINK_CARD_TYPE_COMMON",
                            "oid": "123",
                            "common": {
                              "id_str": "123",
                              "title": "明日方舟：终末地",
                              "desc1": "角色扮演/模拟经营/二次元",
                              "desc2": "新版本「雪淞幽梦」现已开启！",
                              "cover": "//i0.hdslb.com/game.jpg",
                              "jump_url": "https://www.biligame.com/detail/1"
                            }
                          }
                        }
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val RESERVE_FEED_PAYLOAD = """
            {
              "id_str": "1",
              "modules": {
                "module_dynamic": {
                  "additional": {
                    "type": "ADDITIONAL_TYPE_RESERVE",
                    "reserve": {
                      "title": "直播预约",
                      "state": 0,
                      "rid": 42,
                      "reserve_total": 99,
                      "desc1": { "text": "3月2日 20:00" },
                      "desc2": { "text": "99 人预约" },
                      "jump_url": "",
                      "button": {
                        "status": 0,
                        "type": 1,
                        "uncheck": { "text": "预约", "disable": 0 },
                        "check": { "text": "已预约" }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val RESERVE_DETAIL_PAYLOAD = """
            {
              "id_str": "1",
              "modules": [
                {
                  "module_type": "MODULE_TYPE_CONTENT",
                  "module_content": {
                    "paragraphs": [
                      {
                        "para_type": 6,
                        "link_card": {
                          "card": {
                            "type": "LINK_CARD_TYPE_RESERVE",
                            "reserve": {
                              "title": "直播预约",
                              "state": 0,
                              "rid": 42,
                              "reserve_total": 99,
                              "desc1": { "text": "3月2日 20:00" },
                              "desc2": { "text": "99 人预约" },
                              "jump_url": "",
                              "button": {
                                "status": 0,
                                "type": 1,
                                "uncheck": { "text": "预约", "disable": 0 },
                                "check": { "text": "已预约" }
                              }
                            }
                          }
                        }
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val MATCH_FEED_PAYLOAD = """
            {
              "id_str": "1",
              "modules": {
                "module_dynamic": {
                  "additional": {
                    "type": "ADDITIONAL_TYPE_MATCH",
                    "match": {
                      "title": "BLG vs TES",
                      "sub_title": "第三局",
                      "cover": "//i0.hdslb.com/match.jpg",
                      "jump_url": "https://www.bilibili.com/match/1",
                      "match_info": {
                        "center_top": ["2026赛季"],
                        "center_bottom": "第 3 局",
                        "left_team": { "name": "BLG", "pic": "//i0.hdslb.com/blg.png", "score": "2" },
                        "right_team": { "name": "TES", "pic": "//i0.hdslb.com/tes.png", "score": "1" }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val MATCH_DETAIL_PAYLOAD = """
            {
              "id_str": "1",
              "modules": [
                {
                  "module_type": "MODULE_TYPE_CONTENT",
                  "module_content": {
                    "paragraphs": [
                      {
                        "para_type": 6,
                        "link_card": {
                          "card": {
                            "type": "LINK_CARD_TYPE_MATCH",
                            "match": {
                              "title": "BLG vs TES",
                              "sub_title": "第三局",
                              "cover": "//i0.hdslb.com/match.jpg",
                              "jump_url": "https://www.bilibili.com/match/1",
                              "match_info": {
                                "center_top": ["2026赛季"],
                                "center_bottom": "第 3 局",
                                "left_team": { "name": "BLG", "pic": "//i0.hdslb.com/blg.png", "score": "2" },
                                "right_team": { "name": "TES", "pic": "//i0.hdslb.com/tes.png", "score": "1" }
                              }
                            }
                          }
                        }
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val VOTE_FEED_PAYLOAD = """
            {
              "id_str": "1",
              "modules": {
                "module_dynamic": {
                  "additional": {
                    "type": "ADDITIONAL_TYPE_VOTE",
                    "vote": { "title": "", "desc": "今晚吃什么", "join_num": 12, "vote_id": 88 }
                  }
                }
              }
            }
        """.trimIndent()

        val VOTE_DETAIL_PAYLOAD = """
            {
              "id_str": "1",
              "modules": [
                {
                  "module_type": "MODULE_TYPE_CONTENT",
                  "module_content": {
                    "paragraphs": [
                      {
                        "para_type": 6,
                        "link_card": {
                          "card": {
                            "type": "LINK_CARD_TYPE_VOTE",
                            "vote": { "title": "", "desc": "今晚吃什么", "join_num": 12, "vote_id": 88 }
                          }
                        }
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val GOODS_FEED_PAYLOAD = """
            {
              "id_str": "1",
              "modules": {
                "module_dynamic": {
                  "additional": {
                    "type": "ADDITIONAL_TYPE_GOODS",
                    "goods": {
                      "head_text": "UP主的推荐",
                      "jump_url": "https://uland.taobao.com/fallback",
                      "items": [
                        {
                          "name": "绯乐影Phantom有线HiFi耳机",
                          "price": "¥1480",
                          "brief": "现货",
                          "cover": "//i0.hdslb.com/goods.jpg",
                          "jump_url": "https://uland.taobao.com/item",
                          "jump_desc": "去看看"
                        }
                      ]
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val GOODS_DETAIL_PAYLOAD = """
            {
              "id_str": "1",
              "modules": [
                {
                  "module_type": "MODULE_TYPE_CONTENT",
                  "module_content": {
                    "paragraphs": [
                      {
                        "para_type": 6,
                        "link_card": {
                          "card": {
                            "type": "LINK_CARD_TYPE_GOODS",
                            "oid": "118140798",
                            "goods": {
                              "head_text": "UP主的推荐",
                              "jump_url": "https://uland.taobao.com/fallback",
                              "items": [
                                {
                                  "name": "绯乐影Phantom有线HiFi耳机",
                                  "price": "¥1480",
                                  "brief": "现货",
                                  "cover": "//i0.hdslb.com/goods.jpg",
                                  "jump_url": "https://uland.taobao.com/item",
                                  "jump_desc": "去看看"
                                }
                              ]
                            }
                          }
                        }
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val UGC_FEED_PAYLOAD = """
            {
              "id_str": "1",
              "modules": {
                "module_dynamic": {
                  "additional": {
                    "type": "ADDITIONAL_TYPE_UGC",
                    "ugc": {
                      "title": "视频标题",
                      "desc_second": "12:34",
                      "cover": "//i0.hdslb.com/video.jpg",
                      "jump_url": "https://www.bilibili.com/video/BV1xx411c7mD"
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val UGC_DETAIL_PAYLOAD = """
            {
              "id_str": "1",
              "modules": [
                {
                  "module_type": "MODULE_TYPE_CONTENT",
                  "module_content": {
                    "paragraphs": [
                      {
                        "para_type": 6,
                        "link_card": {
                          "card": {
                            "type": "LINK_CARD_TYPE_UGC",
                            "ugc": {
                              "title": "视频标题",
                              "desc_second": "12:34",
                              "cover": "//i0.hdslb.com/video.jpg",
                              "jump_url": "https://www.bilibili.com/video/BV1xx411c7mD"
                            }
                          }
                        }
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()
    }
}
