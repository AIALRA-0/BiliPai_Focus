package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.rememberAppEmptyIcon
import com.android.purebilibili.core.ui.rememberAppWarningIcon

enum class AppContentStatePresentation {
    PAGE,
    INLINE,
}

@Immutable
data class AppContentStateAction(
    val label: String,
    val onClick: () -> Unit,
)

data class AppContentStateLayoutSpec(
    val fillsAvailableHeight: Boolean,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
)

fun resolveAppContentStateLayoutSpec(
    presentation: AppContentStatePresentation,
): AppContentStateLayoutSpec = when (presentation) {
    AppContentStatePresentation.PAGE -> AppContentStateLayoutSpec(
        fillsAvailableHeight = true,
        horizontalPadding = AppSpacingTokens.ExtraLarge,
        verticalPadding = AppSpacingTokens.DoubleExtraLarge,
    )

    AppContentStatePresentation.INLINE -> AppContentStateLayoutSpec(
        fillsAvailableHeight = false,
        horizontalPadding = AppSpacingTokens.Large,
        verticalPadding = AppSpacingTokens.Large,
    )
}

@Composable
fun AppErrorState(
    title: String,
    message: String? = null,
    modifier: Modifier = Modifier,
    presentation: AppContentStatePresentation = AppContentStatePresentation.PAGE,
    primaryAction: AppContentStateAction? = null,
    secondaryAction: AppContentStateAction? = null,
    showIcon: Boolean = true,
    iconOverride: ImageVector? = null,
) {
    AppContentState(
        title = title,
        message = message,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        presentation = presentation,
        primaryAction = primaryAction,
        secondaryAction = secondaryAction,
        showIcon = showIcon,
        icon = iconOverride ?: rememberAppWarningIcon(),
        isError = true,
    )
}

@Composable
fun AppEmptyState(
    title: String,
    message: String? = null,
    modifier: Modifier = Modifier,
    presentation: AppContentStatePresentation = AppContentStatePresentation.PAGE,
    primaryAction: AppContentStateAction? = null,
    secondaryAction: AppContentStateAction? = null,
    showIcon: Boolean = true,
    iconOverride: ImageVector? = null,
) {
    AppContentState(
        title = title,
        message = message,
        modifier = modifier,
        presentation = presentation,
        primaryAction = primaryAction,
        secondaryAction = secondaryAction,
        showIcon = showIcon,
        icon = iconOverride ?: rememberAppEmptyIcon(),
        isError = false,
    )
}

@Composable
private fun AppContentState(
    title: String,
    message: String?,
    modifier: Modifier,
    presentation: AppContentStatePresentation,
    primaryAction: AppContentStateAction?,
    secondaryAction: AppContentStateAction?,
    showIcon: Boolean,
    icon: ImageVector,
    isError: Boolean,
) {
    val layoutSpec = resolveAppContentStateLayoutSpec(presentation)
    val containerModifier = modifier
        .fillMaxWidth()
        .then(if (layoutSpec.fillsAvailableHeight) Modifier.fillMaxSize() else Modifier)
        .padding(
            horizontal = layoutSpec.horizontalPadding,
            vertical = layoutSpec.verticalPadding,
        )

    Box(
        modifier = containerModifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (showIcon) {
                AppIcon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isError) {
                        androidx.compose.material3.MaterialTheme.colorScheme.error
                    } else {
                        androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .height(AppSpacingTokens.TripleExtraLarge)
                        .widthIn(min = AppSpacingTokens.TripleExtraLarge),
                )
                Spacer(modifier = Modifier.height(AppSpacingTokens.Large))
            }

            AppText(
                text = title,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )

            if (!message.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(AppSpacingTokens.Small))
                AppText(
                    text = message,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            if (primaryAction != null || secondaryAction != null) {
                Spacer(modifier = Modifier.height(AppSpacingTokens.Medium))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
                ) {
                    primaryAction?.let { action ->
                        AppButton(
                            onClick = action.onClick,
                            modifier = Modifier.heightIn(min = AppSpacingTokens.TripleExtraLarge),
                        ) {
                            AppText(
                                text = action.label,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    secondaryAction?.let { action ->
                        AppOutlinedButton(
                            onClick = action.onClick,
                            modifier = Modifier.heightIn(min = AppSpacingTokens.TripleExtraLarge),
                        ) {
                            AppText(
                                text = action.label,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}
