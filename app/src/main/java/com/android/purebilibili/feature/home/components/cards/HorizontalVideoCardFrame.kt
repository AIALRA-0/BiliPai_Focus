package com.android.purebilibili.feature.home.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSpacingTokens

/**
 * Shared side-by-side video-card geometry, based on the related-video row.
 *
 * Callers own shell effects (click, selection, shared transition and native snapshot) while this
 * frame keeps cover geometry, spacing, info padding and the trailing action lane identical.
 */
@Composable
internal fun HorizontalVideoCardFrame(
    coverContent: @Composable BoxScope.() -> Unit,
    infoContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    coverModifier: Modifier = Modifier,
    coverOverlayModifier: Modifier = Modifier,
    infoModifier: Modifier = Modifier,
    coverOverlayContent: (@Composable BoxScope.() -> Unit)? = null,
    trailingContent: (@Composable BoxScope.() -> Unit)? = null,
    coverWidth: Dp = HORIZONTAL_VIDEO_CARD_COVER_WIDTH_DP.dp,
    coverAspectRatio: Float = HORIZONTAL_VIDEO_CARD_COVER_ASPECT_RATIO,
    stacked: Boolean = false,
    minimumHeight: Dp? = null,
    infoVerticalArrangement: Arrangement.Vertical = Arrangement.SpaceBetween,
) {
    val effectiveAspectRatio = coverAspectRatio.takeIf { it > 1f }
        ?: HORIZONTAL_VIDEO_CARD_COVER_ASPECT_RATIO
    val resolvedMinimumHeight = minimumHeight ?: (coverWidth / effectiveAspectRatio)

    Box(modifier = modifier) {
        if (stacked) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppSpacingTokens.Small),
            ) {
                Box(
                    modifier = coverModifier
                        .fillMaxWidth()
                        .aspectRatio(effectiveAspectRatio)
                        .clip(AppShapes.mediaCover())
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    coverContent()
                    if (coverOverlayContent != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(coverOverlayModifier),
                            content = coverOverlayContent,
                        )
                    }
                }
                Column(
                    modifier = infoModifier
                        .fillMaxWidth()
                        .padding(
                            top = AppSpacingTokens.Small,
                            bottom = AppSpacingTokens.Small,
                            end = if (trailingContent == null) {
                                AppSpacingTokens.Small
                            } else {
                                AppSpacingTokens.TripleExtraLarge
                            },
                        ),
                    verticalArrangement = infoVerticalArrangement,
                    content = infoContent,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = resolvedMinimumHeight),
                horizontalArrangement = Arrangement.spacedBy(
                    HORIZONTAL_VIDEO_CARD_COVER_INFO_GAP_DP.dp
                ),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = coverModifier
                        .width(coverWidth)
                        .aspectRatio(effectiveAspectRatio)
                        .clip(AppShapes.mediaCover())
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    coverContent()
                    if (coverOverlayContent != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(coverOverlayModifier),
                            content = coverOverlayContent,
                        )
                    }
                }

                Column(
                    modifier = infoModifier
                        .weight(1f)
                        .heightIn(min = resolvedMinimumHeight)
                        .padding(
                            top = AppSpacingTokens.Small,
                            bottom = AppSpacingTokens.Small,
                            end = if (trailingContent == null) {
                                AppSpacingTokens.Small
                            } else {
                                AppSpacingTokens.TripleExtraLarge
                            },
                        ),
                    verticalArrangement = infoVerticalArrangement,
                    content = infoContent,
                )
            }
        }

        trailingContent?.invoke(this)
    }
}
