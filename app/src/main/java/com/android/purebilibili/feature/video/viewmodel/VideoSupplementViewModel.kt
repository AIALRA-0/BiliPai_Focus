package com.android.purebilibili.feature.video.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.data.model.response.AiSummaryData
import com.android.purebilibili.data.model.response.VideoTag
import com.android.purebilibili.feature.video.note.VideoNoteUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VideoSupplementSeed(
    val aiSummary: AiSummaryData? = null,
    val videoNoteState: VideoNoteUiState = VideoNoteUiState(),
    val videoTags: List<VideoTag> = emptyList(),
    val onlineCount: String = "",
    val ownerFollowerCount: Int? = null,
    val ownerVideoCount: Int? = null
)

data class VideoSupplementUiState(
    val subject: VideoSubjectSnapshot? = null,
    val visible: Boolean = true,
    val aiSummary: AiSummaryData? = null,
    val videoNoteState: VideoNoteUiState = VideoNoteUiState(),
    val videoTags: List<VideoTag> = emptyList(),
    val onlineCount: String = "",
    val ownerFollowerCount: Int? = null,
    val ownerVideoCount: Int? = null
)

fun interface VideoSupplementLoader {
    suspend fun load(subject: VideoSubjectSnapshot): VideoSupplementSeed?
}

private val EmptyVideoSupplementLoader = VideoSupplementLoader { null }

class VideoSupplementViewModel(
    private val loader: VideoSupplementLoader = EmptyVideoSupplementLoader,
    private val startDelayMs: Long = 300L
) : ViewModel() {
    private val _uiState = MutableStateFlow(VideoSupplementUiState())
    val uiState = _uiState.asStateFlow()
    private var subjectJob: Job? = null

    fun bindSubject(subject: VideoSubjectSnapshot, seed: VideoSupplementSeed) {
        if (!shouldRebindVideoDomain(_uiState.value.subject, subject)) {
            sync(seed)
            return
        }
        subjectJob?.cancel()
        _uiState.value = VideoSupplementUiState(
            subject = subject,
            visible = _uiState.value.visible,
            aiSummary = seed.aiSummary,
            videoNoteState = seed.videoNoteState,
            videoTags = seed.videoTags,
            onlineCount = seed.onlineCount,
            ownerFollowerCount = seed.ownerFollowerCount,
            ownerVideoCount = seed.ownerVideoCount
        )
        startDeferredLoad(subject)
    }

    fun sync(seed: VideoSupplementSeed) {
        val current = _uiState.value
        _uiState.value = current.copy(
            aiSummary = seed.aiSummary,
            videoNoteState = seed.videoNoteState,
            videoTags = seed.videoTags,
            onlineCount = seed.onlineCount,
            ownerFollowerCount = seed.ownerFollowerCount,
            ownerVideoCount = seed.ownerVideoCount
        )
    }

    fun setVisible(visible: Boolean) {
        if (_uiState.value.visible == visible) return
        _uiState.value = _uiState.value.copy(visible = visible)
        if (!visible) subjectJob?.cancel()
        else _uiState.value.subject?.let(::startDeferredLoad)
    }

    private fun startDeferredLoad(subject: VideoSubjectSnapshot) {
        if (!_uiState.value.visible) return
        subjectJob?.cancel()
        subjectJob = viewModelScope.launch {
            delay(startDelayMs)
            val loaded = loader.load(subject) ?: return@launch
            val current = _uiState.value
            if (current.visible && current.subject?.generation == subject.generation) {
                sync(loaded)
            }
        }
    }

    override fun onCleared() {
        subjectJob?.cancel()
        super.onCleared()
    }
}

internal fun VideoPlaybackUiState.Success.toSupplementSeed(): VideoSupplementSeed =
    VideoSupplementSeed(
        aiSummary = aiSummary,
        videoNoteState = videoNoteState,
        videoTags = videoTags,
        onlineCount = onlineCount,
        ownerFollowerCount = ownerFollowerCount,
        ownerVideoCount = ownerVideoCount
    )
