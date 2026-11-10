package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize

/** Keep vertical hit expansion inside the chrome slot; horizontal touch policy is unchanged. */
@Composable
internal fun PlayerControlTouchBounds(height: Dp, content: @Composable () -> Unit) {
    val platformConfiguration = LocalViewConfiguration.current
    val configuration = remember(platformConfiguration, height) {
        object : ViewConfiguration by platformConfiguration {
            override val minimumTouchTargetSize = DpSize(
                platformConfiguration.minimumTouchTargetSize.width,
                height,
            )
        }
    }
    CompositionLocalProvider(
        LocalViewConfiguration provides configuration,
        LocalMinimumInteractiveComponentSize provides height,
        content = content,
    )
}
