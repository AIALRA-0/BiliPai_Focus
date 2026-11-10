package com.android.purebilibili.feature.following

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.coroutines.AccountScopedRequests
import com.android.purebilibili.core.store.AccountSessionIdentity
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.repository.FanManagementRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class FanManagementState(
    val identity: AccountSessionIdentity = TokenManager.accountIdentity.value,
    val isFan: Boolean? = null,
    val removing: Boolean = false,
    val message: String? = null,
)

internal class FanManagementViewModel(private val mid: Long) : ViewModel() {
    private val requests = AccountScopedRequests(viewModelScope)
    private val mutableState = MutableStateFlow(FanManagementState())
    val state = mutableState.asStateFlow()
    private var relationJob: Job? = null

    init {
        requests.observe {
            mutableState.value = FanManagementState(identity = requests.identity)
            load()
        }
        viewModelScope.launch {
            FanManagementRepository.removed.collect { event ->
                if (event.mid == mid && requests.isCurrent(event.identity) && event.identity == requests.identity) {
                    relationJob?.cancel()
                    mutableState.update { it.copy(isFan = false) }
                }
            }
        }
    }

    fun load() {
        val current = mutableState.value
        if (!requests.isCurrent(current.identity) || mid <= 0L || (current.identity.mid ?: 0L) <= 0L || current.identity.mid == mid || current.removing) return
        relationJob?.cancel()
        relationJob = requests.launch {
            val result = FanManagementRepository.isFan(current.identity, mid)
            if (requests.isCurrent(current.identity)) mutableState.update { it.copy(isFan = result.getOrNull()) }
        }
    }

    fun remove() {
        val current = mutableState.value
        if (!requests.isCurrent(current.identity) || current.isFan != true || current.removing) return
        relationJob?.cancel()
        mutableState.update { it.copy(removing = true, message = null) }
        requests.launch {
            val result = FanManagementRepository.remove(current.identity, mid)
            if (!requests.isCurrent(current.identity)) return@launch
            mutableState.update { it.copy(
                isFan = if (result.isSuccess) false else it.isFan,
                removing = false,
                message = result.exceptionOrNull()?.message ?: "已移除粉丝",
            ) }
        }
    }

    fun clearMessage() { mutableState.update { it.copy(message = null) } }
}
