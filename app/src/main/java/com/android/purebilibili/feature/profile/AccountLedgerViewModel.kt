package com.android.purebilibili.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.coroutines.AccountScopedRequests
import com.android.purebilibili.core.store.AccountSessionIdentity
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.AccountLedgerKind
import com.android.purebilibili.data.model.response.AccountLedgerEntry
import com.android.purebilibili.data.repository.AccountLedgerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class AccountLedgerSection(
    val entries: List<AccountLedgerEntry>? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

internal data class AccountLedgerState(
    val identity: AccountSessionIdentity = TokenManager.accountIdentity.value,
    val sections: Map<AccountLedgerKind, AccountLedgerSection> = emptyMap(),
) {
    fun section(kind: AccountLedgerKind): AccountLedgerSection = sections[kind] ?: AccountLedgerSection()
    fun withSection(kind: AccountLedgerKind, section: AccountLedgerSection): AccountLedgerState = copy(sections = sections + (kind to section))
}

internal class AccountLedgerViewModel : ViewModel() {
    private val requests = AccountScopedRequests(viewModelScope)
    private val mutableState = MutableStateFlow(AccountLedgerState())
    val state = mutableState.asStateFlow()

    init { requests.observe { mutableState.value = AccountLedgerState(identity = requests.identity) } }

    fun load(kind: AccountLedgerKind, refresh: Boolean = false) {
        val current = mutableState.value
        val section = current.section(kind)
        if (!requests.isCurrent(current.identity) || (current.identity.mid ?: 0L) <= 0L || section.loading || (!refresh && section.entries != null)) return
        mutableState.update { it.withSection(kind, it.section(kind).copy(loading = true, error = null)) }
        requests.launch {
            val result = AccountLedgerRepository.load(current.identity, kind)
            if (!requests.isCurrent(current.identity)) return@launch
            result.fold(
                onSuccess = { entries -> mutableState.update { it.withSection(kind, AccountLedgerSection(entries = entries)) } },
                onFailure = { error -> mutableState.update { it.withSection(kind, it.section(kind).copy(loading = false, error = error.message ?: "加载明细失败，请重试")) } },
            )
        }
    }
}
