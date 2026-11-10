package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.store.SettingsManager

@Composable
fun MaidLottieAnimationSettingsHost(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val enabled = SettingsManager.getMaidLottieAnimationEnabled(context)
        .collectAsStateWithLifecycle(initialValue = true)

    CompositionLocalProvider(
        LocalMaidLottieAnimationEnabled provides enabled.value,
        content = content,
    )
}
