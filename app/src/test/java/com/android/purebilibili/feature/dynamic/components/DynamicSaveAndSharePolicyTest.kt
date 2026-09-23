package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.data.model.response.DynamicAuthorModule
import com.android.purebilibili.data.model.response.DynamicBasic
import com.android.purebilibili.data.model.response.DynamicContentModule
import com.android.purebilibili.data.model.response.DynamicDesc
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicModules
import com.android.purebilibili.data.model.response.SessionAccountInfo
import com.android.purebilibili.data.model.response.SessionItem
import com.android.purebilibili.feature.message.UserBasicInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicSaveAndSharePolicyTest {
    private val item = DynamicItem(
        id_str = "123",
        basic = DynamicBasic(comment_type = 17),
        modules = DynamicModules(
            module_author = DynamicAuthorModule(mid = 42L, name = "作者", face = "face"),
            module_dynamic = DynamicContentModule(desc = DynamicDesc(text = "动态正文")),
        ),
    )

    @Test
    fun saveImageSpecContainsStableDynamicIdentity() {
        val spec = buildDynamicSaveImageSpec(item, generatedAtMillis = 0L)

        assertEquals("作者", spec.authorName)
        assertEquals("动态正文", spec.body)
        assertEquals("https://t.bilibili.com/123", spec.dynamicUrl)
    }

    @Test
    fun messageShareBuildsNativeDynamicCardPayload() {
        val payload = Json.parseToJsonElement(buildDynamicShareCardContent(item)).jsonObject

        assertEquals("123", payload.getValue("id").jsonPrimitive.content)
        assertEquals(11, payload.getValue("source").jsonPrimitive.int)
        assertEquals("作者", payload.getValue("author").jsonPrimitive.content)
        assertEquals("动态正文", payload.getValue("title").jsonPrimitive.content)
    }

    @Test
    fun messageShareSessionUsesFetchedProfileInsteadOfRawUserId() {
        val presentation = resolveDynamicShareSessionPresentation(
            session = SessionItem(talker_id = 471278344L, session_type = 1),
            userInfo = UserBasicInfo(
                mid = 471278344L,
                name = "真实昵称",
                face = "https://i0.hdslb.com/avatar.jpg",
            ),
            resolvingUserInfo = false,
        )

        assertEquals("真实昵称", presentation.name)
        assertEquals("https://i0.hdslb.com/avatar.jpg", presentation.avatarUrl)
    }

    @Test
    fun messageShareSessionShowsLoadingLabelWhileMissingProfileIsFetched() {
        val presentation = resolveDynamicShareSessionPresentation(
            session = SessionItem(
                talker_id = 471278344L,
                session_type = 1,
                account_info = SessionAccountInfo(),
            ),
            userInfo = null,
            resolvingUserInfo = true,
        )

        assertEquals("正在获取用户资料…", presentation.name)
        assertEquals(true, presentation.resolvingUserInfo)
    }

    @Test
    fun messageShareGroupSessionUsesGroupMetadataWithoutUserLookup() {
        val presentation = resolveDynamicShareSessionPresentation(
            session = SessionItem(
                talker_id = 88L,
                session_type = 2,
                group_name = "应援团",
                group_cover = "https://i0.hdslb.com/group.jpg",
            ),
            userInfo = null,
            resolvingUserInfo = false,
        )

        assertEquals("应援团", presentation.name)
        assertEquals("https://i0.hdslb.com/group.jpg", presentation.avatarUrl)
    }
}
