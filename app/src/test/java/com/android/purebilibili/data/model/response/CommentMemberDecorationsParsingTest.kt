package com.android.purebilibili.data.model.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CommentMemberDecorationsParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun decodeReplyResponse_exposesMemberDecorationFields() {
        val payload = """
            {
              "code": 0,
              "data": {
                "replies": [
                  {
                    "rpid": 1,
                    "mid": 2,
                    "ctime": 1700000000,
                    "member": {
                      "mid": "2",
                      "uname": "测试用户",
                      "avatar": "https://example.com/avatar.jpg",
                      "level_info": {
                        "current_level": 6
                      },
                      "fans_detail": {
                        "uid": 2,
                        "medal_id": 1001,
                        "medal_name": "测试粉丝团",
                        "score": 0,
                        "level": 18,
                        "intimacy": 0,
                        "master_status": 0,
                        "is_receive": 0
                      },
                      "pendant": {
                        "pid": 2511,
                        "name": "官方头像挂件",
                        "image": "https://example.com/pendant.png",
                        "image_enhance": "https://example.com/pendant_enhance.webp",
                        "image_enhance_frame": "https://example.com/pendant_frame.png"
                      },
                      "nameplate": {
                        "nid": 20,
                        "name": "有爱大佬",
                        "image": "https://example.com/nameplate.png",
                        "image_small": "https://example.com/nameplate_small.png",
                        "level": "普通勋章",
                        "condition": "test"
                      },
                      "user_sailing": {
                        "pendant": null,
                        "cardbg": {
                          "id": 10,
                          "name": "测试卡",
                          "image": "https://example.com/card.png",
                          "jump_url": "",
                          "type": "suit",
                          "fan": {
                            "is_fan": 1,
                            "number": 6607,
                            "color": "#FFAA00",
                            "name": "测试粉丝团",
                            "num_desc": "006607"
                          }
                        },
                        "cardbg_with_focus": null
                      }
                    },
                    "content": {
                      "message": "hello"
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<ReplyResponse>(payload)
        val member = response.data?.replies?.firstOrNull()?.member
        assertNotNull(member)

        val fansDetail = requireFieldValue(member, "fansDetail")
        assertNotNull(fansDetail)
        assertEquals("测试粉丝团", requireFieldValue(fansDetail, "medalName"))
        assertEquals(18, requireFieldValue(fansDetail, "level"))

        val pendant = requireFieldValue(member, "pendant")
        assertNotNull(pendant)
        assertEquals("https://example.com/pendant_frame.png", requireFieldValue(pendant, "imageEnhanceFrame"))

        val nameplate = requireFieldValue(member, "nameplate")
        assertNotNull(nameplate)
        assertEquals("https://example.com/nameplate_small.png", requireFieldValue(nameplate, "imageSmall"))

        val userSailing = requireFieldValue(member, "userSailing")
        assertNotNull(userSailing)
        val cardBg = requireFieldValue(userSailing, "cardBg")
        assertNotNull(cardBg)
        val fan = requireFieldValue(cardBg, "fan")
        assertNotNull(fan)
        assertEquals("006607", requireFieldValue(fan, "numDesc"))
    }

    @Test
    fun decodeReplyResponse_supportsStringNumericAndUserSailingV2() {
        val payload = """
            {
              "code": 0,
              "data": {
                "replies": [
                  {
                    "rpid": 2,
                    "mid": 3,
                    "ctime": 1700000001,
                    "member": {
                      "mid": "3",
                      "uname": "测试用户2",
                      "avatar": "https://example.com/avatar2.jpg",
                      "level_info": {
                        "current_level": "6"
                      },
                      "fans_detail": {
                        "uid": "3",
                        "medal_id": "2002",
                        "medal_name": "字符串粉丝团",
                        "score": "0",
                        "level": "21",
                        "intimacy": "0",
                        "master_status": "0",
                        "is_receive": "0"
                      },
                      "user_sailing": null,
                      "user_sailing_v2": {
                        "cardbg": {
                          "id": "88",
                          "name": "测试卡V2",
                          "image": "https://example.com/card2.png",
                          "jump_url": null,
                          "type": 1,
                          "fan": {
                            "is_fan": "1",
                            "number": "183",
                            "color": "#F25D5D",
                            "name": "字符串粉丝团",
                            "num_desc": 183
                          }
                        }
                      }
                    },
                    "content": {
                      "message": "hello2"
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<ReplyResponse>(payload)
        val member = response.data?.replies?.firstOrNull()?.member
        assertNotNull(member)

        assertEquals(6, requireFieldValue(requireFieldValue(member, "levelInfo")!!, "currentLevel"))
        assertEquals(21, requireFieldValue(requireFieldValue(member, "fansDetail")!!, "level"))

        val userSailingV2 = requireFieldValue(member, "userSailingV2")
        assertNotNull(userSailingV2)
        val cardBg = requireFieldValue(userSailingV2, "cardBg")
        assertNotNull(cardBg)
        val fan = requireFieldValue(cardBg, "fan")
        assertNotNull(fan)
        assertEquals(183L, requireFieldValue(fan, "number"))
        assertEquals("183", requireFieldValue(fan, "numDesc"))
    }

    @Test
    fun decodeReplyResponse_acceptsNullMemberFromLegacyReplies() {
        val payload = """
            {
              "code": 0,
              "data": {
                "replies": [
                  {
                    "rpid": 476670,
                    "mid": 58426,
                    "ctime": 1291350931,
                    "member": null,
                    "content": {
                      "message": "貌似没人来"
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<ReplyResponse>(payload)
        val reply = response.data?.replies?.firstOrNull()
        assertNotNull(reply)
        assertEquals("未知用户", reply.member.uname)
        assertEquals("貌似没人来", reply.content.message)
    }

    @Test
    fun decodeReplyResponse_supportsBooleanFanFlagAndLooseSailingShape() {
        val payload = """
            {
              "code": 0,
              "data": {
                "replies": [
                  {
                    "rpid": 3,
                    "mid": 4,
                    "ctime": 1700000002,
                    "member": {
                      "mid": "4",
                      "uname": "测试用户3",
                      "avatar": "https://example.com/avatar3.jpg",
                      "level_info": {
                        "current_level": 6
                      },
                      "user_sailing": [],
                      "user_sailing_v2": {
                        "cardbg": {
                          "id": 99,
                          "name": "测试卡V3",
                          "image": "https://example.com/card3.png",
                          "jump_url": "",
                          "type": "suit",
                          "fan": {
                            "is_fan": true,
                            "number": 11,
                            "color": "#f76a6b",
                            "name": "测试粉丝团",
                            "num_desc": "000011"
                          }
                        }
                      }
                    },
                    "content": {
                      "message": "hello3"
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<ReplyResponse>(payload)
        val member = response.data?.replies?.firstOrNull()?.member
        assertNotNull(member)

        val userSailingV2 = requireFieldValue(member, "userSailingV2")
        assertNotNull(userSailingV2)
        val cardBg = requireFieldValue(userSailingV2, "cardBg")
        assertNotNull(cardBg)
        val fan = requireFieldValue(cardBg, "fan")
        assertNotNull(fan)
        assertEquals(1, requireFieldValue(fan, "isFan"))
        assertEquals("000011", requireFieldValue(fan, "numDesc"))
    }

    @Test
    fun decodeReplyResponse_supportsObjectCardBgImageShape() {
        val payload = """
            {
              "code": 0,
              "data": {
                "replies": [
                  {
                    "rpid": 4,
                    "mid": 5,
                    "ctime": 1700000003,
                    "member": {
                      "mid": "5",
                      "uname": "测试用户4",
                      "avatar": "https://example.com/avatar4.jpg",
                      "level_info": {
                        "current_level": 6
                      },
                      "user_sailing_v2": {
                        "cardbg": {
                          "id": 100,
                          "name": "测试卡V4",
                          "image": {
                            "day": "https://example.com/card_day.png",
                            "night": "https://example.com/card_night.png"
                          },
                          "jump_url": "",
                          "type": "suit",
                          "fan": {
                            "is_fan": 1,
                            "number": 11,
                            "color": "#f76a6b",
                            "name": "测试粉丝团",
                            "num_desc": "000011"
                          }
                        }
                      }
                    },
                    "content": {
                      "message": "hello4"
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<ReplyResponse>(payload)
        val member = response.data?.replies?.firstOrNull()?.member
        assertNotNull(member)

        val userSailingV2 = requireFieldValue(member, "userSailingV2")
        assertNotNull(userSailingV2)
        val cardBg = requireFieldValue(userSailingV2, "cardBg")
        assertNotNull(cardBg)
        assertEquals("https://example.com/card_day.png", requireFieldValue(cardBg, "image"))
    }

    @Test
    fun decodeReplyResponse_supportsLegacyGarbCardFields() {
        val payload = """
            {
              "code": 0,
              "data": {
                "replies": [
                  {
                    "rpid": 5,
                    "mid": 6,
                    "ctime": 1700000004,
                    "member": {
                      "mid": "6",
                      "uname": "测试用户5",
                      "avatar": "https://example.com/avatar5.jpg",
                      "level_info": {
                        "current_level": 6
                      },
                      "garb_card_image": "https://example.com/garb_card.png",
                      "garb_card_image_with_focus": "https://example.com/garb_card_focus.png",
                      "garb_card_number": "021288",
                      "garb_card_fan_color": "#f76a6b",
                      "garb_card_is_fan": true
                    },
                    "content": {
                      "message": "hello5"
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<ReplyResponse>(payload)
        val member = response.data?.replies?.firstOrNull()?.member
        assertNotNull(member)
        assertEquals("https://example.com/garb_card.png", requireFieldValue(member, "garbCardImage"))
        assertEquals("https://example.com/garb_card_focus.png", requireFieldValue(member, "garbCardImageWithFocus"))
        assertEquals("021288", requireFieldValue(member, "garbCardNumber"))
        assertEquals("#f76a6b", requireFieldValue(member, "garbCardFanColor"))
        assertEquals(1, requireFieldValue(member, "garbCardIsFan"))
    }

    private fun requireFieldValue(instance: Any, fieldName: String): Any? {
        val field = instance.javaClass.declaredFields.firstOrNull { it.name == fieldName }
        assertNotNull(field, "Expected field '$fieldName' on ${instance.javaClass.simpleName}")
        field.isAccessible = true
        return field.get(instance)
    }
}
