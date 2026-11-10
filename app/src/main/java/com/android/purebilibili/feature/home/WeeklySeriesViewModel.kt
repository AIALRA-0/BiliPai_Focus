package com.android.purebilibili.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.data.model.response.PopularSeriesPeriod
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.repository.VideoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class WeeklySeriesUiState(
    val number: Int? = null,
    val periods: List<PopularSeriesPeriod> = emptyList(),
    val videos: List<VideoItem> = emptyList(),
    val label: String = "",
    val subject: String = "",
    val reminder: String = "",
    val loading: Boolean = true,
    val error: String? = null,
    val periodsError: String? = null,
)

internal fun resolveWeeklyInitialNumber(requested: Int?, periods: List<PopularSeriesPeriod>): Int? =
    requested?.takeIf { it > 0 } ?: periods.maxOfOrNull { it.number }?.takeIf { it > 0 }

internal class WeeklySeriesViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    private val mutableState = MutableStateFlow(WeeklySeriesUiState())
    val state = mutableState.asStateFlow()
    private var loadJob: Job? = null
    private var initialized = false

    fun initialize(initialNumber: Int?) {
        if (initialized) return
        initialized = true
        load(savedState.get<Int>("weeklyNumber") ?: initialNumber)
    }

    fun select(number: Int) {
        if (number > 0 && number != state.value.number) load(number)
    }

    fun retry() = load(state.value.number)

    private fun load(requested: Int?) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null, videos = emptyList(), label = "", subject = "", reminder = "") }
            var periods = state.value.periods
            if (periods.isEmpty()) {
                VideoRepository.getWeeklyPeriods().fold(
                    onSuccess = { loaded ->
                        periods = loaded
                        mutableState.update { it.copy(periods = loaded, periodsError = null) }
                    },
                    onFailure = { failure ->
                        mutableState.update { it.copy(periodsError = failure.message ?: "期数加载失败") }
                    }
                )
            }
            val number = resolveWeeklyInitialNumber(requested, periods)
            if (number == null) {
                mutableState.update { it.copy(loading = false, error = it.periodsError ?: "暂无每周必看") }
                return@launch
            }
            savedState["weeklyNumber"] = number
            mutableState.update { it.copy(number = number) }
            VideoRepository.getWeeklyPeriod(number).fold(
                onSuccess = { data ->
                    val videos = data.list.orEmpty().map { it.toVideoItem() }.filter { it.bvid.isNotBlank() }
                    mutableState.update {
                        it.copy(loading = false, videos = videos, label = data.config?.label.orEmpty(),
                            subject = data.config?.subject.orEmpty(), reminder = data.reminder)
                    }
                },
                onFailure = { failure ->
                    mutableState.update { it.copy(loading = false, error = failure.message ?: "本期加载失败") }
                }
            )
        }
    }
}
