package com.android.purebilibili.feature.matchinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.MatchContestInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class MatchInfoUiState(
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
    val contest: MatchContestInfo? = null,
)

internal class MatchInfoViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(MatchInfoUiState())
    val state = mutableState.asStateFlow()
    private var request: Job? = null
    private var cid: Long = 0L

    fun load(cid: Long) {
        if (this.cid == cid && mutableState.value.contest != null) return
        this.cid = cid
        request?.cancel()
        mutableState.value = MatchInfoUiState(loading = true)
        request = launchLoad()
    }

    fun refresh() {
        if (cid <= 0L || mutableState.value.loading) return
        request?.cancel()
        mutableState.value = mutableState.value.copy(refreshing = true, error = null)
        request = launchLoad()
    }

    private fun launchLoad(): Job = viewModelScope.launch {
        val response = runCatching { NetworkModule.api.getMatchInfo(cid) }
            .getOrElse { failure ->
                mutableState.value = MatchInfoUiState(
                    error = failure.message?.takeIf { it.isNotBlank() } ?: "比赛详情加载失败",
                )
                return@launch
            }
        val contest = response.data?.contest
        when {
            response.code != 0 -> mutableState.value = MatchInfoUiState(
                error = response.message.ifBlank { "比赛详情加载失败" },
            )
            contest == null -> mutableState.value = MatchInfoUiState(
                error = "未找到该场比赛的信息",
            )
            else -> mutableState.value = MatchInfoUiState(contest = contest)
        }
    }
}
