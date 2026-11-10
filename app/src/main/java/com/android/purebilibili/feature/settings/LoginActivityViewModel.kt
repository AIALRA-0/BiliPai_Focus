package com.android.purebilibili.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.coroutines.AccountScopedRequests
import com.android.purebilibili.core.store.AccountSessionIdentity
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.AccountLoginDevice
import com.android.purebilibili.data.model.response.AccountLoginRecord
import com.android.purebilibili.data.repository.AccountLoginActivityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class LoginActivitySection<T>(
    val items: List<T>? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

internal data class LoginActivityUiState(
    val identity: AccountSessionIdentity = TokenManager.accountIdentity.value,
    val devices: LoginActivitySection<AccountLoginDevice> = LoginActivitySection(),
    val records: LoginActivitySection<AccountLoginRecord> = LoginActivitySection(),
)

internal class LoginActivityViewModel : ViewModel() {
    private val requests = AccountScopedRequests(viewModelScope)
    private val _state = MutableStateFlow(LoginActivityUiState())
    val state = _state.asStateFlow()

    init {
        requests.observe {
            _state.value = LoginActivityUiState(identity = requests.identity)
            refresh()
        }
    }

    fun refresh() {
        loadDevices()
        loadRecords()
    }

    fun loadDevices() {
        val current = _state.value
        val identity = current.identity
        if (!requests.isCurrent(identity) || (identity.mid ?: 0L) <= 0L || current.devices.loading) return
        _state.update { it.copy(devices = it.devices.copy(loading = true, error = null)) }
        requests.launch {
            val result = AccountLoginActivityRepository.devices(identity)
            if (!requests.isCurrent(identity)) return@launch
            result.fold(
                onSuccess = { devices -> _state.update { it.copy(devices = LoginActivitySection(items = devices)) } },
                onFailure = { error -> _state.update { it.copy(devices = it.devices.copy(loading = false, error = error.message ?: "加载登录设备失败")) } },
            )
        }
    }

    fun loadRecords() {
        val current = _state.value
        val identity = current.identity
        if (!requests.isCurrent(identity) || (identity.mid ?: 0L) <= 0L || current.records.loading) return
        _state.update { it.copy(records = it.records.copy(loading = true, error = null)) }
        requests.launch {
            val result = AccountLoginActivityRepository.records(identity)
            if (!requests.isCurrent(identity)) return@launch
            result.fold(
                onSuccess = { records -> _state.update { it.copy(records = LoginActivitySection(items = records)) } },
                onFailure = { error -> _state.update { it.copy(records = it.records.copy(loading = false, error = error.message ?: "加载登录记录失败")) } },
            )
        }
    }
}
