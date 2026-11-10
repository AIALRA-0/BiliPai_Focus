package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.AccountSessionIdentity
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.AccountLedgerKind
import com.android.purebilibili.data.model.response.AccountLedgerEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object AccountLedgerRepository {
    private fun cookie(identity: AccountSessionIdentity): String = synchronized(TokenManager) {
        check(identity == TokenManager.accountIdentity.value && identity.mid == TokenManager.midCache) { "账号已切换，请重新加载" }
        val mid = requireNotNull(identity.mid?.takeIf { it > 0L }) { "请先登录后查看明细" }
        val session = requireNotNull(TokenManager.sessDataCache?.takeIf { it.isNotBlank() }) { "登录状态已失效，请重新登录" }
        "SESSDATA=$session; DedeUserID=$mid"
    }

    suspend fun load(identity: AccountSessionIdentity, kind: AccountLedgerKind): Result<List<AccountLedgerEntry>> = withContext(Dispatchers.IO) {
        try {
            val cookie = cookie(identity)
            val response = when (kind) {
                AccountLedgerKind.COINS -> NetworkModule.api.getAccountCoinLedger(cookie)
                AccountLedgerKind.EXPERIENCE -> NetworkModule.api.getAccountExperienceLedger(cookie)
            }
            check(response.code == 0) { response.message.ifBlank { "加载明细失败（${response.code}）" } }
            Result.success(requireNotNull(response.data?.list) { "明细数据不完整，请重试" })
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
