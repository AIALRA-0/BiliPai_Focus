package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf

/** Selected video-cover ratio shared by feeds, recommendations and personal lists.
 * Poster, article and collection artwork keep their explicit content-specific ratios.
 */
internal val LocalVideoCardCoverAspectRatio = compositionLocalOf { 16f / 10f }

@Composable
@ReadOnlyComposable
internal fun videoCardCoverAspectRatio(): Float = LocalVideoCardCoverAspectRatio.current
