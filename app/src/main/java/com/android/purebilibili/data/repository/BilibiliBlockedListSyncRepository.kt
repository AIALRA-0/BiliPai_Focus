package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.BilibiliApi
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.FollowingUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val BLOCKED_LIST_PAGE_SIZE = 50
private const val BLOCKED_LIST_MAX_PAGES = 120
private const val BLOCKED_LIST_PAGE_DELAY_MS = 220L

class BilibiliBlockedListSyncRepository(
    private val localRepository: BlockedUpRepository,
    private val api: BilibiliApi = NetworkModule.api
) {
    suspend fun importFromBilibili(): Result<BlockedUpImportResult> = withContext(Dispatchers.IO) {
        if (TokenManager.sessDataCache.isNullOrEmpty()) {
            return@withContext Result.failure(Exception("请先登录后再同步 B站黑名单"))
        }

        try {
            val remoteUsers = fetchBlockedListUsers()
            val importItems = buildBlockedUpImportItemsFromRemoteBlacks(remoteUsers)
            val importResult = localRepository.importBlockedUps(importItems)
            val refreshResult = localRepository.refreshBlockedUpProfiles()
            val message = "${importResult.message}；${refreshResult.message}"

            Result.success(importResult.copy(message = message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchBlockedListUsers(): List<FollowingUser> {
        val result = mutableListOf<FollowingUser>()
        var page = 1
        var total = 0

        while (page <= BLOCKED_LIST_MAX_PAGES) {
            val response = api.getRelationBlacks(
                pageSize = BLOCKED_LIST_PAGE_SIZE,
                page = page
            )
            if (response.code != 0) {
                throw Exception(response.message.ifBlank { "获取 B站黑名单成员失败: ${response.code}" })
            }

            val users = response.data.list
            total = response.data.total

            result.addAll(users)
            if (users.size < BLOCKED_LIST_PAGE_SIZE) break
            if (total > 0 && result.size >= total) break
            page += 1
            delay(BLOCKED_LIST_PAGE_DELAY_MS)
        }

        return result.toList()
    }
}

internal fun buildBlockedUpImportItemsFromRemoteBlacks(
    users: List<FollowingUser>
): List<BlockedUpImportItem> {
    return users.mapNotNull { user ->
        val mid = user.mid.takeIf { it > 0L } ?: return@mapNotNull null
        BlockedUpImportItem(
            mid = mid,
            name = user.uname.trim().takeIf { it.isNotEmpty() } ?: "UP主$mid",
            face = user.face,
            sign = user.sign
        )
    }
}
