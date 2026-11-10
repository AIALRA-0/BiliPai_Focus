package com.android.purebilibili.feature.common

import com.android.purebilibili.core.ui.AppContentColors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton

@Composable
internal fun ListLoadError(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall),
    ) {
        AppText(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = AppContentColors.secondaryText(),
            textAlign = TextAlign.Center,
        )
        AppTextButton(onClick = onRetry) { AppText("重试") }
    }
}
