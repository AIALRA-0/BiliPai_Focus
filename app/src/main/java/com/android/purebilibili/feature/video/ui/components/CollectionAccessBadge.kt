package com.android.purebilibili.feature.video.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.core.ui.components.VideoStatusBadges
import com.android.purebilibili.data.model.response.UgcEpisode
import com.android.purebilibili.data.model.response.ViewInfo
import com.android.purebilibili.data.model.response.resolveCollectionEpisodeAccessLabel
import com.android.purebilibili.data.repository.SharedContentRepository

/** 展示层只按当前条目补全状态；失败保留已有标签，不给整个合集推断权限。 */
@Composable
internal fun CollectionAccessBadge(episode: UgcEpisode, modifier: Modifier = Modifier, knownDetail: ViewInfo? = null) {
    val account by TokenManager.accountIdentity.collectAsState()
    val cached = SharedContentRepository.cachedDetailInfo(episode.bvid, episode.aid)
    val seed = resolveCollectionEpisodeAccessLabel(episode, knownDetail)
        ?: resolveCollectionEpisodeAccessLabel(episode, cached)
    var resolved by remember(episode.bvid, episode.aid, account, seed) { mutableStateOf(seed) }
    LaunchedEffect(episode.bvid, episode.aid, account, seed) {
        if (seed == null) {
            resolved = SharedContentRepository.collectionEpisodeAccessLabel(episode)
        }
    }
    CollectionAccessBadge(label = seed ?: resolved, modifier = modifier)
}

/** 封面权限标记沿用视频卡片的语义角标，由调用方放置在时长之外。 */
@Composable
internal fun CollectionAccessBadge(label: String?, modifier: Modifier = Modifier) {
    if (label.isNullOrBlank()) return
    VideoStatusBadges(labels = listOf(label), modifier = modifier)
}
