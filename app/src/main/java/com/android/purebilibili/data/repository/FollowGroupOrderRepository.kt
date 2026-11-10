package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.AccountSessionIdentity
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.RelationTagItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun followGroupOrderFields(ids: List<Long>, csrf: String): Map<String, String> {
    require(ids.isNotEmpty() && ids.all { it > 0L } && ids.distinct().size == ids.size) { "分组顺序无效" }
    require(csrf.isNotBlank()) { "请重新登录后操作" }
    return mapOf("tagids" to ids.joinToString(","), "csrf" to csrf)
}

internal object FollowGroupOrderRepository {
    private fun credentials(identity: AccountSessionIdentity): Pair<String, String> = synchronized(TokenManager) {
        check(identity == TokenManager.accountIdentity.value && identity.mid == TokenManager.midCache) { "账号已切换，请重新打开排序" }
        require((identity.mid ?: 0L) > 0L) { "请先登录" }
        val session = requireNotNull(TokenManager.sessDataCache?.takeIf { it.isNotBlank() }) { "请重新登录" }
        val csrf = TokenManager.csrfCache.orEmpty()
        "SESSDATA=$session; bili_jct=$csrf; DedeUserID=${identity.mid}" to csrf
    }

    private suspend fun <T> request(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try { Result.success(block()) }
        catch (error: CancellationException) { throw error }
        catch (error: Exception) { Result.failure(error) }
    }

    private suspend fun fetch(cookie: String): List<RelationTagItem> {
        val response = NetworkModule.api.getSortableFollowGroups(cookie)
        check(response.code == 0) { response.message.ifBlank { "读取关注分组失败（${response.code}）" } }
        val groups = requireNotNull(response.data) { "关注分组数据为空，请重试" }
        check(groups.map { it.tagid }.distinct().size == groups.size) { "关注分组数据重复，请重新读取" }
        return groups
    }

    suspend fun load(identity: AccountSessionIdentity): Result<List<RelationTagItem>> = request {
        fetch(credentials(identity).first)
    }

    suspend fun save(identity: AccountSessionIdentity, originalIds: List<Long>, orderedIds: List<Long>): Result<Unit> = request {
        val (cookie, csrf) = credentials(identity)
        val fields = followGroupOrderFields(orderedIds, csrf)
        require(originalIds.size == orderedIds.size && originalIds.toSet() == orderedIds.toSet()) { "分组集合已变化，请重新读取" }
        val currentIds = fetch(cookie).filter { it.tagid > 0L }.map { it.tagid }
        check(currentIds.size == originalIds.size && currentIds.toSet() == originalIds.toSet()) { "关注分组已新增或删除，请重新读取后排序" }
        check(identity == TokenManager.accountIdentity.value) { "账号已切换，请重新打开排序" }
        val response = NetworkModule.api.updateFollowGroupOrder(fields, cookie)
        check(response.code == 0) { response.message.ifBlank { "保存分组顺序失败（${response.code}）" } }
    }
}
