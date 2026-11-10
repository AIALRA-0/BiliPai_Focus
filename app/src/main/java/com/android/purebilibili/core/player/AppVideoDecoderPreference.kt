@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.android.purebilibili.core.player

import android.content.Context
import androidx.media3.exoplayer.DefaultRenderersFactory
import com.android.purebilibili.core.store.PlayerSettingsCache

/** 新建视频播放器读取当前偏好；复用中的播放器不重建，下次创建时生效。 */
internal fun DefaultRenderersFactory.withAppVideoDecoderPreference(context: Context): DefaultRenderersFactory =
    withVideoDecoderPreference(PlayerSettingsCache.isHwDecodeEnabled(context))

/** 在播放器创建时记住偏好；以后设置变更不改写当前实例的诊断。 */
internal fun androidx.media3.exoplayer.ExoPlayer.withAppVideoDecoderDiagnostics(context: Context): androidx.media3.exoplayer.ExoPlayer =
    VideoDecoderDiagnostics.attach(this, PlayerSettingsCache.isHwDecodeEnabled(context))
