package com.android.bilipai.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import androidx.tv.material3.MaterialTheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.android.purebilibili.core.player.SharedPlaybackSession
import com.android.purebilibili.core.subtitle.*
import com.android.purebilibili.data.repository.SubtitleContentRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class TvSubtitleState(val bvid: String = "", val cid: Long = 0,
    val tracks: List<SubtitleTrackMeta> = emptyList(), val selected: SubtitleTrackMeta? = null,
    val cues: List<SubtitleCue> = emptyList(), val loading: Boolean = false, val error: String? = null)

/** No scope or player ownership; the route owns cancellation on CID/selection changes. */
internal class TvSubtitleStateHolder {
    private val mutable = MutableStateFlow(TvSubtitleState())
    val state = mutable.asStateFlow()
    private var language: String? = null // Off until the user explicitly selects a language.
    suspend fun loadMetadata(bvid: String, cid: Long) {
        mutable.value = TvSubtitleState(bvid = bvid, cid = cid, loading = true)
        val result = SubtitleContentRepository.playerInfo(bvid, cid)
        result.fold(onSuccess = { info ->
            val tracks = mapPlayerInfoSubtitleTracks(info.subtitle?.subtitles.orEmpty())
            mutable.value = TvSubtitleState(bvid, cid, tracks, tracks.firstOrNull { it.lan == language })
        }, onFailure = { error -> mutable.update { it.copy(loading = false, error = error.message ?: "字幕信息读取失败") } })
    }
    fun select(key: String) {
        val track = mutable.value.tracks.firstOrNull { it.trackKey == key }
        language = track?.lan
        mutable.update { it.copy(selected = track, cues = emptyList(), error = null) }
    }
    suspend fun loadCues() {
        val snapshot = mutable.value
        val track = snapshot.selected ?: return
        mutable.update { it.copy(loading = true, error = null) }
        val result = SubtitleContentRepository.cues(track.subtitleUrl, snapshot.bvid, snapshot.cid, track.id, track.idStr, track.lan)
        if (mutable.value.cid != snapshot.cid || mutable.value.selected?.trackKey != track.trackKey) return
        result.fold(onSuccess = { cues -> mutable.update { it.copy(cues = cues, loading = false) } },
            onFailure = { error -> mutable.update { it.copy(cues = emptyList(), loading = false, error = error.message ?: "字幕加载失败") } })
    }
}

@Composable
internal fun TvSubtitleOverlay(session: SharedPlaybackSession, cues: List<SubtitleCue>, controlsVisible: Boolean, modifier: Modifier = Modifier) {
    val owner = LocalLifecycleOwner.current
    var text by remember(cues) { mutableStateOf<String?>(null) }
    LaunchedEffect(session, cues, owner) {
        if (cues.isEmpty()) return@LaunchedEffect
        owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) { text = resolveSubtitleTextAt(cues, session.player.currentPosition); delay(100) }
        }
    }
    Box(modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp)) {
        text?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge, color = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (controlsVisible) 220.dp else 16.dp)
                    .background(TvMediaColors.OverlaidTextBackdrop, TvUiTokens.shape(com.android.purebilibili.core.ui.ContainerLevel.Card)).padding(12.dp))
        }
    }
}
