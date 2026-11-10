package com.android.purebilibili.core.ui.renderer.miuix

import com.android.purebilibili.core.ui.AppChromeSizeTokens
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppContentColors
import com.android.purebilibili.core.ui.components.ProvideAppActionContentColor

@Composable
internal fun AppMiuixListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier,
    overlineContent: (@Composable () -> Unit)?,
    supportingContent: (@Composable () -> Unit)?,
    leadingContent: (@Composable () -> Unit)?,
    trailingContent: (@Composable () -> Unit)?,
) {
    if (com.android.purebilibili.core.ui.isMiuixNonGlassEnabled()) {
        top.yukonga.miuix.kmp.basic.BasicComponent(
            modifier = modifier,
            startAction = leadingContent,
            endActions = trailingContent?.let { slot -> { slot() } },
        ) {
            overlineContent?.let { ProvideAppActionContentColor(AppContentColors.secondaryText(), it) }
            ProvideAppActionContentColor(AppContentColors.primaryText(), headlineContent)
            supportingContent?.let { ProvideAppActionContentColor(AppContentColors.secondaryText(), it) }
        }
        return
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AppChromeSizeTokens.CompactRowMinHeightDp.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingContent != null) {
            leadingContent()
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            overlineContent?.let { ProvideAppActionContentColor(AppContentColors.secondaryText(), it) }
            ProvideAppActionContentColor(AppContentColors.primaryText(), headlineContent)
            supportingContent?.let { ProvideAppActionContentColor(AppContentColors.secondaryText(), it) }
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            ProvideAppActionContentColor(AppContentColors.secondaryText(), trailingContent)
        }
    }
}
