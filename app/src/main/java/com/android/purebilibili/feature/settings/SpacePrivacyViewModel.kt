package com.android.purebilibili.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.coroutines.AccountScopedRequests
import com.android.purebilibili.core.store.AccountSessionIdentity
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.repository.SpacePrivacyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class SpacePrivacyUiState(
    val identity: AccountSessionIdentity = TokenManager.accountIdentity.value,
    val original: Map<String, Int>? = null,
    val draft: Map<String, Int> = emptyMap(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val message: String? = null,
) {
    val changes: Map<String, Int> get() = spacePrivacyChanges(original, draft)
}

internal class SpacePrivacyViewModel : ViewModel() {
    private val requests = AccountScopedRequests(viewModelScope)
    private val _state = MutableStateFlow(SpacePrivacyUiState())
    val state = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        requests.observe {
            _state.value = SpacePrivacyUiState(identity = requests.identity)
            load()
        }
    }

    fun load() {
        val current = _state.value
        if (!requests.isCurrent(current.identity) || current.loading || current.saving || current.changes.isNotEmpty()) return
        if ((current.identity.mid ?: 0L) <= 0L) {
            _state.update { it.copy(message = "请先登录后管理空间公开范围") }
            return
        }
        loadJob?.cancel()
        _state.update { it.copy(loading = true, message = null) }
        loadJob = requests.launch {
            val result = SpacePrivacyRepository.load(current.identity)
            if (!requests.isCurrent(current.identity)) return@launch
            result.fold(
                onSuccess = { values -> _state.update { it.copy(original = values, draft = values, loading = false) } },
                onFailure = { error -> _state.update { it.copy(loading = false, message = error.message ?: "加载失败，请重试") } },
            )
        }
    }

    fun change(option: SpacePrivacyOption, checked: Boolean) {
        _state.update { state ->
            if (!requests.isCurrent(state.identity) || state.loading || state.saving || option.checked(state.original?.get(option.field)) == null) state
            else state.copy(draft = state.draft + (option.field to option.raw(checked)), message = null)
        }
    }

    fun save() {
        val current = _state.value
        val changes = current.changes
        if (!requests.isCurrent(current.identity) || current.loading || current.saving || changes.isEmpty()) return
        _state.update { it.copy(saving = true, message = null) }
        requests.launch {
            val result = SpacePrivacyRepository.save(current.identity, changes)
            if (!requests.isCurrent(current.identity)) return@launch
            result.fold(
                onSuccess = { _state.update { it.copy(original = current.draft, saving = false, message = "空间公开范围已保存") } },
                onFailure = { error -> _state.update { it.copy(saving = false, message = error.message ?: "保存失败，修改已保留，请重试") } },
            )
        }
    }
}
