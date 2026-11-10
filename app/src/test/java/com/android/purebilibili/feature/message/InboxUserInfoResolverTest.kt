package com.android.purebilibili.feature.message

import com.android.purebilibili.data.model.response.SessionAccountInfo
import com.android.purebilibili.data.model.response.SessionItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNull

class InboxUserInfoResolverTest {

    @Test
    fun groupIdentityIsNotStoredAsAUserWithTheSameNumericId() {
        val group = SessionItem(
            talker_id = 42L,
            session_type = 2,
            account_info = SessionAccountInfo(name = "群名称", pic_url = "//cdn.example/group.jpg"),
        )
        val user = group.copy(
            session_type = 1,
            account_info = SessionAccountInfo(name = "用户昵称", pic_url = "//cdn.example/user.jpg"),
        )

        assertNull(InboxUserInfoResolver.sessionUserInfo(group))
        assertEquals(
            UserBasicInfo(42L, "用户昵称", "https://cdn.example/user.jpg"),
            InboxUserInfoResolver.sessionUserInfo(user),
        )
        assertNull(InboxUserInfoResolver.sessionUserInfo(user.copy(talker_id = 0L)))
    }

    @Test
    fun resolveDisplayName_fallsBackToSessionWhenCachedNameIsBlank() {
        val session = SessionItem(
            talker_id = 1001L,
            account_info = SessionAccountInfo(name = "会话用户名")
        )
        val cached = UserBasicInfo(mid = 1001L, name = "   ", face = "https://cdn.example/avatar.jpg")

        val displayName = InboxUserInfoResolver.resolveDisplayName(cached, session)

        assertEquals("会话用户名", displayName)
    }

    @Test
    fun resolveDisplayAvatar_fallsBackToSessionAndNormalizesProtocolWhenCachedFaceIsBlank() {
        val session = SessionItem(
            talker_id = 1002L,
            account_info = SessionAccountInfo(pic_url = "//i0.hdslb.com/bfs/face/abc.png")
        )
        val cached = UserBasicInfo(mid = 1002L, name = "用户", face = "")

        val displayAvatar = InboxUserInfoResolver.resolveDisplayAvatar(cached, session)

        assertTrue(displayAvatar.startsWith("https://"))
    }

    @Test
    fun shouldFetchUserInfo_returnsTrueWhenCacheMissingOrIncomplete() {
        val mid = 1003L
        val missingCache = emptyMap<Long, UserBasicInfo>()
        val incompleteCache = mapOf(
            mid to UserBasicInfo(mid = mid, name = "用户", face = "")
        )

        assertTrue(InboxUserInfoResolver.shouldFetchUserInfo(mid, missingCache))
        assertTrue(InboxUserInfoResolver.shouldFetchUserInfo(mid, incompleteCache))
    }

    @Test
    fun shouldFetchUserInfo_returnsFalseWhenCacheComplete() {
        val mid = 1004L
        val completeCache = mapOf(
            mid to UserBasicInfo(mid = mid, name = "完整用户", face = "https://cdn.example/avatar.jpg")
        )

        assertFalse(InboxUserInfoResolver.shouldFetchUserInfo(mid, completeCache))
    }

    @Test
    fun shouldFetchSessionUserInfo_returnsTrueForUserSessionWithoutAccountInfo() {
        val session = SessionItem(
            talker_id = 1006L,
            session_type = 1,
            account_info = null
        )

        assertTrue(InboxUserInfoResolver.shouldFetchSessionUserInfo(session, emptyMap()))
    }

    @Test
    fun shouldFetchSessionUserInfo_returnsFalseForNonUserSession() {
        val session = SessionItem(
            talker_id = 1007L,
            session_type = 2,
            account_info = null
        )

        assertFalse(InboxUserInfoResolver.shouldFetchSessionUserInfo(session, emptyMap()))
    }

    @Test
    fun shouldFetchSessionUserInfo_returnsFalseWhenSessionAccountInfoIsComplete() {
        val session = SessionItem(
            talker_id = 1008L,
            session_type = 1,
            account_info = SessionAccountInfo(
                name = "会话内用户名",
                pic_url = "https://cdn.example/session-avatar.jpg"
            )
        )

        assertFalse(InboxUserInfoResolver.shouldFetchSessionUserInfo(session, emptyMap()))
    }

    @Test
    fun selectMissingUserInfoMids_keepsAllMissingUsersBeyondFirstBatch() {
        val mids = (1L..30L).toList()

        val selected = InboxUserInfoResolver.selectMissingUserInfoMids(mids, emptyMap())

        assertEquals(mids, selected)
    }

    @Test
    fun mergeFetchedUserInfo_keepsExistingFieldsWhenIncomingIsBlank() {
        val existing = UserBasicInfo(
            mid = 1005L,
            name = "旧用户名",
            face = "https://cdn.example/old.jpg"
        )
        val fetched = UserBasicInfo(
            mid = 1005L,
            name = "",
            face = "   "
        )

        val merged = InboxUserInfoResolver.mergeFetchedUserInfo(existing, fetched)

        assertEquals("旧用户名", merged?.name)
        assertEquals("https://cdn.example/old.jpg", merged?.face)
    }

    @Test
    fun mergeFetchedUserInfo_keepsSessionIdentityWhenProfileRequestFails() {
        val sessionInfo = UserBasicInfo(1009L, "会话昵称", "https://cdn.example/session.jpg")

        assertEquals(sessionInfo, InboxUserInfoResolver.mergeFetchedUserInfo(sessionInfo, null))
    }

    @Test
    fun mergeFetchedUserInfo_completesAvatarWithoutDiscardingSessionName() {
        val sessionInfo = UserBasicInfo(1010L, "会话昵称", "")
        val partialProfile = UserBasicInfo(1010L, " ", "//cdn.example/profile.jpg")

        assertEquals(
            UserBasicInfo(1010L, "会话昵称", "https://cdn.example/profile.jpg"),
            InboxUserInfoResolver.mergeFetchedUserInfo(sessionInfo, partialProfile),
        )
    }
}
