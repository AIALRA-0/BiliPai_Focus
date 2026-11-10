package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.data.model.response.DynamicAdditional
import com.android.purebilibili.data.model.response.DynamicAdditionalCommon
import com.android.purebilibili.data.model.response.DynamicAdditionalGoods
import com.android.purebilibili.data.model.response.DynamicAdditionalGoodsItem
import com.android.purebilibili.data.model.response.DynamicAdditionalReserve
import com.android.purebilibili.data.model.response.DynamicAdditionalText
import com.android.purebilibili.data.model.response.DynamicAdditionalUgc
import com.android.purebilibili.data.model.response.DynamicAdditionalVote
import com.android.purebilibili.data.model.response.DynamicCardButton
import com.android.purebilibili.data.model.response.DynamicCardButtonStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Checks how the feed's legacy `additional` payload enters the shared link-card model.
 * The detail page renders this same model, so these assertions preserve the migration boundary.
 */
class DynamicAdditionalCardPolicyTest {

    @Test
    fun ugcAdditionalMapsToTheSharedVideoCard() {
        val card = assertNotNull(
            resolveDynamicLinkCard(
                DynamicAdditional(
                    type = "ADDITIONAL_TYPE_UGC",
                    ugc = DynamicAdditionalUgc(
                        title = "投稿标题",
                        cover = "https://i0.hdslb.com/a.jpg",
                        desc_second = "1.2万播放",
                        jump_url = "https://b23.tv/av1",
                    ),
                )
            )
        )

        assertEquals(LINK_CARD_TYPE_UGC, card.type)
        assertEquals("投稿标题", card.title)
        assertEquals("1.2万播放", card.description)
        assertEquals("https://i0.hdslb.com/a.jpg", card.cover)
        assertEquals("https://b23.tv/av1", card.jumpUrl)
    }

    @Test
    fun voteAndGoodsMapTheirIdentifiersAndReadableFields() {
        val vote = assertNotNull(
            resolveDynamicLinkCard(
                DynamicAdditional(
                    type = "ADDITIONAL_TYPE_VOTE",
                    vote = DynamicAdditionalVote(desc = "今晚吃什么", join_num = 12, vote_id = 88L),
                )
            )
        )
        val goods = assertNotNull(
            resolveDynamicLinkCard(
                DynamicAdditional(
                    type = "ADDITIONAL_TYPE_GOODS",
                    goods = DynamicAdditionalGoods(
                        head_text = "UP主推荐",
                        jump_url = "https://uland.taobao.com/fallback",
                        items = listOf(
                            DynamicAdditionalGoodsItem(
                                name = "周边",
                                brief = "现货",
                                cover = "//i0.hdslb.com/goods.jpg",
                                jump_url = "https://uland.taobao.com/item",
                            )
                        ),
                    ),
                )
            )
        )

        assertEquals(LINK_CARD_TYPE_VOTE, vote.type)
        assertEquals("今晚吃什么", vote.title)
        assertEquals("12 人参与", vote.description)
        assertEquals(88L, resolveDynamicLinkCardVoteId(vote))
        assertEquals(LINK_CARD_TYPE_GOODS, goods.type)
        assertEquals("周边", goods.title)
        assertEquals("现货", goods.description)
        assertEquals("UP主推荐", goods.label)
        assertEquals("https://i0.hdslb.com/goods.jpg", goods.cover)
        assertEquals("https://uland.taobao.com/item", goods.jumpUrl)
    }

    @Test
    fun commonAdditionalUsesTheSharedDetailFields() {
        val card = assertNotNull(
            resolveDynamicLinkCard(
                DynamicAdditional(
                    type = "ADDITIONAL_TYPE_COMMON",
                    common = DynamicAdditionalCommon(
                        title = "游戏中心",
                        desc = "分类",
                        desc1 = "新活动",
                        desc2 = "即将开始",
                        jump_url = "https://www.bilibili.com/game",
                        // The unified detail card has no separate button-label field.
                        button = DynamicCardButton(
                            jump_url = "https://button-only.example",
                            jump_style = DynamicCardButtonStyle(text = "进入"),
                        ),
                    ),
                )
            )
        )

        assertEquals(LINK_CARD_TYPE_COMMON, card.type)
        assertEquals("游戏中心", card.title)
        assertEquals("分类\n新活动\n即将开始", card.description)
        assertEquals("https://www.bilibili.com/game", card.jumpUrl)
        assertEquals("", card.badgeText)
    }

    @Test
    fun reserveCardKeepsServerButtonStateAndActionParameters() {
        val card = assertNotNull(
            resolveDynamicLinkCard(
                DynamicAdditional(
                    type = "ADDITIONAL_TYPE_RESERVE",
                    reserve = DynamicAdditionalReserve(
                        title = "首播预约",
                        rid = 42L,
                        reserve_total = 99L,
                        desc1 = DynamicAdditionalText("明晚 8 点"),
                        desc2 = DynamicAdditionalText("99 人预约"),
                        button = DynamicCardButton(
                            status = 1,
                            type = 1,
                            check = DynamicCardButtonStyle(text = "已预约"),
                            uncheck = DynamicCardButtonStyle(text = "预约", disable = 0),
                        ),
                    ),
                )
            )
        )
        val reserveUi = assertNotNull(resolveDynamicReserveUi(card, "dynamic-1"))

        assertEquals(LINK_CARD_TYPE_RESERVE, card.type)
        assertEquals("已预约", card.badgeText)
        assertEquals("明晚 8 点 · 99 人预约", card.description)
        assertEquals(42L, reserveUi.action.reserveId)
        assertEquals(99L, reserveUi.action.reserveTotal)
        assertEquals(1, reserveUi.action.currentButtonStatus)
        assertEquals(1, reserveUi.action.buttonType)
        assertEquals(true, reserveUi.actionEnabled)
    }

    @Test
    fun reserveJumpAndDescriptionLinksStayAvailableThroughTheSharedUiModel() {
        val card = assertNotNull(
            resolveDynamicLinkCard(
                DynamicAdditional(
                    type = "ADDITIONAL_TYPE_RESERVE",
                    reserve = DynamicAdditionalReserve(
                        title = "预约抽奖",
                        rid = 42L,
                        desc3 = DynamicAdditionalText(
                            text = "预约后参与抽奖",
                            jump_url = "https://www.bilibili.com/blackboard/gift",
                        ),
                        button = DynamicCardButton(
                            status = 0,
                            type = 1,
                            jump_url = "https://www.bilibili.com/blackboard/reserve",
                            uncheck = DynamicCardButtonStyle(text = "查看详情", disable = 1),
                        ),
                    ),
                )
            )
        )
        val reserveUi = assertNotNull(resolveDynamicReserveUi(card, "dynamic-2"))

        assertEquals("https://www.bilibili.com/blackboard/reserve", card.jumpUrl)
        assertEquals("查看详情", card.badgeText)
        assertEquals("https://www.bilibili.com/blackboard/reserve", reserveUi.actionJumpUrl)
        assertEquals("https://www.bilibili.com/blackboard/gift", reserveUi.descriptionJumpUrl)
        // A valid jump target overrides the server's disabled style for the action.
        assertEquals(true, reserveUi.actionEnabled)
        assertEquals(true, card.description.contains("预约后参与抽奖"))
    }

    @Test
    fun missingOrUnknownAdditionalDoesNotCreateALinkCard() {
        assertNull(resolveDynamicLinkCard(null))
        assertNull(resolveDynamicLinkCard(DynamicAdditional(type = "ADDITIONAL_TYPE_UNKNOWN")))
    }
}
