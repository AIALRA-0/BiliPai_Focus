package com.android.purebilibili.feature.video.share

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.android.purebilibili.core.ui.AppModalPresentation

internal fun videoSharePresentation(isLandscape: Boolean): AppModalPresentation? =
    if (isLandscape) AppModalPresentation.SideSheet else null

@Composable
internal fun isLandscapeVideoShare(): Boolean =
    LocalConfiguration.current.let {
        it.orientation == Configuration.ORIENTATION_LANDSCAPE && it.screenWidthDp >= 600
    }
