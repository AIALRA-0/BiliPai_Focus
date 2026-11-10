package com.android.purebilibili.feature.message

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.AccountSessionIdentity
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.core.network.WbiKeyManager
import com.android.purebilibili.core.network.WbiUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Shared user-profile completion for inbox and message-share session pickers. */
internal object MessageUserInfoLoader {
    private var accountIdentity = TokenManager.accountIdentity.value
    private val users = mutableMapOf<Long, UserBasicInfo>()

    @Synchronized
    fun cached(mid: Long): UserBasicInfo? {
        if (accountIdentity != TokenManager.accountIdentity.value) {
            users.clear()
            accountIdentity = TokenManager.accountIdentity.value
        }
        return users[mid]
    }

    @Synchronized
    fun remember(
        info: UserBasicInfo,
        identity: AccountSessionIdentity = TokenManager.accountIdentity.value,
    ): UserBasicInfo? {
        if (identity != TokenManager.accountIdentity.value) return null
        val merged = InboxUserInfoResolver.mergeFetchedUserInfo(cached(info.mid), info)
        if (merged != null) users[info.mid] = merged
        return merged
    }

    suspend fun fetch(mid: Long): UserBasicInfo? = withContext(Dispatchers.IO) {
        if (mid <= 0L) return@withContext null
        val identity = TokenManager.accountIdentity.value
        val existing = cached(mid)
        if (InboxUserInfoResolver.hasCompleteUserInfo(existing)) return@withContext existing
        val cardInfo = InboxUserInfoResolver.mergeFetchedUserInfo(existing, fetchUserCardInfo(mid))
        val user = if (InboxUserInfoResolver.hasCompleteUserInfo(cardInfo)) {
            cardInfo
        } else {
            InboxUserInfoResolver.mergeFetchedUserInfo(cardInfo, fetchSpaceUserInfo(mid))
        }
        user?.let { remember(it, identity) }
    }

    private suspend fun fetchUserCardInfo(mid: Long): UserBasicInfo? {
        return try {
            val response = NetworkModule.api.getUserCard(mid = mid, photo = true)
            val card = response.data?.card
            if (response.code != 0 || card == null) return null
            InboxUserInfoResolver.mergeFetchedUserInfo(
                existing = null,
                fetched = UserBasicInfo(
                    mid = card.mid.toLongOrNull() ?: mid,
                    name = card.name,
                    face = card.face,
                ),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            android.util.Log.w("MessageUserInfo", "User card unavailable for $mid", error)
            null
        }
    }

    private suspend fun fetchSpaceUserInfo(mid: Long): UserBasicInfo? {
        return try {
            val keys = WbiKeyManager.getWbiKeys().getOrNull()
                ?: WbiKeyManager.refreshKeys().getOrNull()
                ?: return null
            val params = WbiUtils.sign(mapOf("mid" to mid.toString()), keys.first, keys.second)
            val response = NetworkModule.spaceApi.getSpaceInfo(params)
            val user = response.data
            if (response.code != 0 || user == null) return null
            InboxUserInfoResolver.mergeFetchedUserInfo(
                existing = null,
                fetched = UserBasicInfo(
                    mid = user.mid.takeIf { it > 0L } ?: mid,
                    name = user.name,
                    face = user.face,
                ),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            android.util.Log.w("MessageUserInfo", "Space profile unavailable for $mid", error)
            null
        }
    }
}
