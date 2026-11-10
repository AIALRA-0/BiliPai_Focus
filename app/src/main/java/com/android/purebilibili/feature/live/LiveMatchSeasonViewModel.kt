package com.android.purebilibili.feature.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class LiveMatchSeasonUiState(
    val detail: LiveMatchSeasonDetail? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val contests: List<LiveMatchContest> = emptyList(),
    val scheduleLoading: Boolean = true,
    val scheduleError: String? = null,
    val hasMore: Boolean = false,
)

internal fun liveMatchStandingGroups(standing: LiveMatchStanding): List<LiveMatchStandingGroup> =
    if (standing.grouped) standing.groups.orEmpty() else listOf(
        LiveMatchStandingGroup(teams = standing.teams.orEmpty()),
    )

internal class LiveMatchSeasonViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(LiveMatchSeasonUiState())
    val state = mutableState.asStateFlow()
    private var detailRequest: Job? = null
    private var scheduleRequest: Job? = null
    private var seasonId: Long? = null
    private var page = 0

    fun start(id: Long) {
        if (id == seasonId) return
        seasonId = id
        mutableState.value = LiveMatchSeasonUiState()
        refresh()
    }

    fun stop() {
        detailRequest?.cancel()
        scheduleRequest?.cancel()
        seasonId = null
        mutableState.value = LiveMatchSeasonUiState()
    }

    fun refresh() {
        val id = seasonId ?: return
        detailRequest?.cancel()
        mutableState.update { it.copy(loading = true, error = null) }
        detailRequest = viewModelScope.launch {
            try {
                val response = NetworkModule.api.getLiveMatchSeason(id)
                ensureActive()
                check(response.code == 0) { response.message.ifBlank { "赛季详情加载失败" } }
                val detail = checkNotNull(response.data) { "赛季详情为空" }
                check(detail.season?.id == id) { "赛季详情与当前内容不一致" }
                mutableState.update { it.copy(detail = detail, loading = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update { it.copy(loading = false, error = error.message ?: "赛季详情加载失败") }
            }
        }
        loadSchedule(reset = true)
    }

    fun loadMore() {
        if (!state.value.scheduleLoading && state.value.hasMore) loadSchedule(reset = false)
    }

    fun retrySchedule() = loadSchedule(reset = page == 0)

    private fun loadSchedule(reset: Boolean) {
        val id = seasonId ?: return
        scheduleRequest?.cancel()
        val nextPage = if (reset) 1 else page + 1
        if (reset) page = 0
        mutableState.update {
            it.copy(
                contests = if (reset) emptyList() else it.contests,
                scheduleLoading = true, scheduleError = null,
                hasMore = if (reset) false else it.hasMore,
            )
        }
        scheduleRequest = viewModelScope.launch {
            try {
                val response = NetworkModule.api.getLiveMatchSeasonSchedule(id, nextPage)
                ensureActive()
                check(response.code == 0) { response.message.ifBlank { "赛季赛程加载失败" } }
                val data = checkNotNull(response.data) { "赛季赛程为空" }
                page = nextPage
                mutableState.update {
                    it.copy(
                        contests = (if (reset) data.contests else it.contests + data.contests).distinctBy { match -> match.id },
                        scheduleLoading = false,
                        hasMore = data.page.total?.let { total -> nextPage.toLong() * data.page.size < total }
                            ?: (data.contests.size >= 30),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update { it.copy(scheduleLoading = false, scheduleError = error.message ?: "赛季赛程加载失败") }
            }
        }
    }
}
