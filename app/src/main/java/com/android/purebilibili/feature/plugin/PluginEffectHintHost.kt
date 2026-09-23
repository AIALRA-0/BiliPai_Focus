package com.android.purebilibili.feature.plugin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.plugin.PluginEffectHint
import com.android.purebilibili.core.plugin.PluginEffectHintBus
import com.android.purebilibili.core.plugin.PluginEffectHintKind
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.motion.emphasizedEnterTween
import com.android.purebilibili.core.ui.motion.emphasizedExitTween
import kotlinx.coroutines.delay

@Composable
fun PluginEffectHintHost(
    modifier: Modifier = Modifier
) {
    val hint by PluginEffectHintBus.current.collectAsStateWithLifecycle()
    var visibleHint by remember { mutableStateOf<PluginEffectHint?>(null) }
    if (hint != null) {
        visibleHint = hint
    }

    LaunchedEffect(hint?.issuedAtMs) {
        val current = hint ?: return@LaunchedEffect
        delay(current.visibleDurationMs)
        PluginEffectHintBus.dismiss(current.issuedAtMs)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = hint != null,
            enter = fadeIn(emphasizedEnterTween(220)) + slideInVertically(
                animationSpec = emphasizedEnterTween(220)
            ) { -it / 2 },
            exit = fadeOut(emphasizedExitTween(180)) + slideOutVertically(
                animationSpec = emphasizedExitTween(180)
            ) { -it / 2 }
        ) {
            visibleHint?.let { current ->
                PluginEffectHintChip(
                    hint = current,
                    onClick = { PluginEffectHintBus.dismiss(current.issuedAtMs) }
                )
            }
        }
    }
}

@Composable
private fun PluginEffectHintChip(
    hint: PluginEffectHint,
    onClick: () -> Unit
) {
    val icon = iconForHint(hint.kind)
    AppSurface(
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .widthIn(max = 360.dp),
        shape = AppShapes.container(ContainerLevel.Pill),
        color = AppSurfaceTokens.cardContainer(),
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppIcon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f, fill = false)) {
                AppText(
                    text = hint.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val subtitle = hint.subtitle
                if (!subtitle.isNullOrBlank()) {
                    AppText(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppSurfaceTokens.onSurfaceVariantSummary()
                    )
                }
            }
        }
    }
}

private fun iconForHint(kind: PluginEffectHintKind): ImageVector {
    return when (kind) {
        PluginEffectHintKind.EYE_CARE -> Icons.Outlined.DarkMode
        PluginEffectHintKind.FEED_FILTER -> Icons.Outlined.FilterList
        PluginEffectHintKind.DANMAKU -> Icons.Outlined.ChatBubbleOutline
        PluginEffectHintKind.GENERIC -> Icons.Outlined.Extension
    }
}
