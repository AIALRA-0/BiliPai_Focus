package com.android.purebilibili.feature.video.viewmodel

import androidx.compose.runtime.Immutable

/** Stable identity shared with video-scoped state holders without sharing ViewModels. */
@Immutable
data class VideoSubjectSnapshot(
    val bvid: String,
    val cid: Long,
    val aid: Long,
    val ownerMid: Long,
    val title: String,
    val coverUrl: String,
    val durationMs: Long,
    val generation: Long
)

internal fun VideoPlaybackUiState.Success.toSubjectSnapshot(
    generation: Long
): VideoSubjectSnapshot = VideoSubjectSnapshot(
    bvid = info.bvid,
    cid = info.cid,
    aid = info.aid,
    ownerMid = info.owner.mid,
    title = info.title,
    coverUrl = info.pic,
    durationMs = videoDurationMs,
    generation = generation
)

internal fun shouldAdvanceVideoSubjectGeneration(
    previous: VideoSubjectSnapshot?,
    next: VideoPlaybackUiState.Success
): Boolean = previous == null ||
    previous.bvid != next.info.bvid ||
    previous.cid != next.info.cid ||
    previous.aid != next.info.aid
