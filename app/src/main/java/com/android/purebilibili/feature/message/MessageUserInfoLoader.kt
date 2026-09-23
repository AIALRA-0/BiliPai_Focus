package com.android.purebilibili.feature.message

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.WbiKeyManager
import com.android.purebilibili.core.network.WbiUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Shared user-profile completion for inbox and message-share session pickers. */
internal object MessageUserInfoLoader {
    suspend fun fetch(mid: Long): UserBasicInfo? = withContext(Dispatchers.IO) {
        if (mid <= 0L) return@withContext null
        val cardInfo = fetchUserCardInfo(mid)
        if (InboxUserInfoResolver.hasCompleteUserInfo(cardInfo)) return@withContext cardInfo

        val spaceInfo = fetchSpaceUserInfo(mid)
        InboxUserInfoResolver.mergeFetchedUserInfo(cardInfo, spaceInfo)
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
